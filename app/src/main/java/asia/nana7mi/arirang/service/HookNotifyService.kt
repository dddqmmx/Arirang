package asia.nana7mi.arirang.service

import android.app.Service
import android.content.SharedPreferences
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ResultReceiver
import androidx.core.content.edit
import asia.nana7mi.arirang.hook.IHookNotify
import asia.nana7mi.arirang.ui.ConfirmDialogActivity
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class HookNotifyService : Service() {
    companion object {
        private const val DECISION_DENY = 0
        private const val DECISION_ALLOW = 1

        private const val UI_RESULT_DENY_ONCE = 0
        private const val UI_RESULT_ALLOW_ONCE = 1
        private const val UI_RESULT_ALLOW_ALWAYS = 2
        private const val UI_RESULT_DENY_ALWAYS = 3

        private const val DEFAULT_TIMEOUT_MS = 2500L
        private const val MAX_TIMEOUT_MS = 3000L
        private const val MAX_PENDING_REQUESTS = 8
        private const val LATE_DECISION_GRACE_MS = 15_000L

        private const val POLICY_PREFS = "clipboard_prompt_policy_prefs"
        private const val KEY_ALWAYS_ALLOW = "always_allow"
        private const val KEY_ALWAYS_DENY = "always_deny"
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val requestIdGenerator = AtomicLong(1L)
    private val pendingRequests = ConcurrentHashMap<Long, PendingRequest>()
    private val policyLock = Any()

    private lateinit var policyPrefs: SharedPreferences
    private var alwaysAllowPackages = mutableSetOf<String>()
    private var alwaysDenyPackages = mutableSetOf<String>()

    private data class PendingRequest(
        val latch: CountDownLatch = CountDownLatch(1),
        @Volatile var decision: Int? = null,
        @Volatile var timedOut: Boolean = false
    )

    // Binder 用于 IPC 回调
    private val binder = object : IHookNotify.Stub() {
        override fun requestClipboardRead(pkgName: String, uid: Int, userId: Int, timeoutMs: Long): Int {
            if (isAlwaysAllowed(pkgName)) return DECISION_ALLOW
            if (isAlwaysDenied(pkgName)) return DECISION_DENY

            if (pendingRequests.size >= MAX_PENDING_REQUESTS) return DECISION_DENY

            val requestId = requestIdGenerator.getAndIncrement()
            val pending = PendingRequest()
            pendingRequests[requestId] = pending

            val receiver = buildDecisionReceiver(requestId, pkgName)
            mainHandler.post {
                launchDialog(pkgName, receiver)
            }

            return try {
                val effectiveTimeout = timeoutMs.coerceIn(200L, MAX_TIMEOUT_MS)
                val completed = pending.latch.await(
                    if (effectiveTimeout > 0L) effectiveTimeout else DEFAULT_TIMEOUT_MS,
                    TimeUnit.MILLISECONDS
                )
                if (!completed) {
                    pending.timedOut = true
                    scheduleCleanup(requestId)
                    DECISION_DENY
                } else {
                    pendingRequests.remove(requestId)
                    pending.decision ?: DECISION_DENY
                }
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                pendingRequests.remove(requestId)
                DECISION_DENY
            }
        }

        override fun onPermissionUsed(pkgName: String, opName: String) {
            mainHandler.post {
                launchDialog(pkgName, null)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        policyPrefs = getSharedPreferences(POLICY_PREFS, MODE_PRIVATE)
        loadPolicy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder = binder

    private fun launchDialog(pkgName: String, receiver: ResultReceiver?) {
        val intent = Intent(this, ConfirmDialogActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("pkg_name", pkgName)
            if (receiver != null) {
                putExtra("receiver", receiver)
            }
        }

        val options = android.app.ActivityOptions.makeBasic()
        options.setPendingIntentBackgroundActivityStartMode(
            android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
        )

        try {
            startActivity(intent, options.toBundle())
        } catch (_: Exception) {
            receiver?.send(UI_RESULT_DENY_ONCE, Bundle.EMPTY)
        }
    }

    private fun buildDecisionReceiver(
        requestId: Long,
        pkgName: String
    ): ResultReceiver {
        return object : ResultReceiver(mainHandler) {
            override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                when (resultCode) {
                    UI_RESULT_ALLOW_ALWAYS -> setAlwaysAllowed(pkgName)
                    UI_RESULT_DENY_ALWAYS -> setAlwaysDenied(pkgName)
                }

                val resolvedDecision = when (resultCode) {
                    UI_RESULT_ALLOW_ONCE, UI_RESULT_ALLOW_ALWAYS -> DECISION_ALLOW
                    else -> DECISION_DENY
                }

                val pending = pendingRequests.remove(requestId)
                if (pending == null || pending.timedOut) {
                    return
                }

                pending.decision = resolvedDecision
                pending.latch.countDown()
            }
        }
    }

    private fun scheduleCleanup(requestId: Long) {
        mainHandler.postDelayed({
            pendingRequests.remove(requestId)
        }, LATE_DECISION_GRACE_MS)
    }

    private fun loadPolicy() {
        synchronized(policyLock) {
            alwaysAllowPackages = policyPrefs.getStringSet(KEY_ALWAYS_ALLOW, emptySet())?.toMutableSet() ?: mutableSetOf()
            alwaysDenyPackages = policyPrefs.getStringSet(KEY_ALWAYS_DENY, emptySet())?.toMutableSet() ?: mutableSetOf()
        }
    }

    private fun setAlwaysAllowed(pkgName: String) {
        synchronized(policyLock) {
            alwaysAllowPackages.add(pkgName)
            alwaysDenyPackages.remove(pkgName)
            persistPolicyLocked()
        }
    }

    private fun setAlwaysDenied(pkgName: String) {
        synchronized(policyLock) {
            alwaysDenyPackages.add(pkgName)
            alwaysAllowPackages.remove(pkgName)
            persistPolicyLocked()
        }
    }

    private fun isAlwaysAllowed(pkgName: String): Boolean {
        synchronized(policyLock) {
            return alwaysAllowPackages.contains(pkgName)
        }
    }

    private fun isAlwaysDenied(pkgName: String): Boolean {
        synchronized(policyLock) {
            return alwaysDenyPackages.contains(pkgName)
        }
    }

    private fun persistPolicyLocked() {
        policyPrefs.edit {
            putStringSet(KEY_ALWAYS_ALLOW, alwaysAllowPackages)
            putStringSet(KEY_ALWAYS_DENY, alwaysDenyPackages)
        }
    }

}
