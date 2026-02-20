package asia.nana7mi.arirang.hook

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.DeadObjectException
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.UserHandle
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object HookNotifyClient {

    private const val TAG = "HookNotifyClient"
    private const val TARGET_PKG = "asia.nana7mi.arirang"
    private const val ACTION_BIND = "asia.nana7mi.arirang.BIND_NOTIFY"
    private const val BIND_TIMEOUT_MS = 3000L
    private const val BIND_WAIT_MS = 300L
    private const val DEFAULT_REQUEST_TIMEOUT_MS = 2500L
    private const val RESULT_ALLOW = 1
    private const val FAIL_OPEN_WHEN_SERVICE_UNAVAILABLE = true

    @Volatile
    private var sService: IHookNotify? = null

    @Volatile
    private var sBinding = false

    @Volatile
    private var sConnectLatch: CountDownLatch? = null

    private val LOCK = Any()
    private val handler = Handler(Looper.getMainLooper())

    /** ⚠ system_server 必须是成员变量，不能匿名 */
    private val connection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            XposedBridge.log("$TAG onServiceConnected $name")
            try {
                sService = IHookNotify.Stub.asInterface(service)
            } catch (t: Throwable) {
                XposedBridge.log("$TAG asInterface failed: ${t.stackTraceToString()}")
                sService = null
            } finally {
                sBinding = false
                sConnectLatch?.countDown()
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            XposedBridge.log("$TAG onServiceDisconnected $name")
            sService = null
            sBinding = false
            sConnectLatch?.countDown()
        }
    }

    fun notifyPermissionUsed(pkgName: String, opName: String) {
        val ctx = getSystemContext() ?: return

        val service = getOrBindService(ctx) ?: return

        try {
            service.onPermissionUsed(pkgName, opName)
        } catch (t: Throwable) {
            XposedBridge.log("$TAG notify failed: ${t.stackTraceToString()}")
            sService = null
        }
    }

    fun requestClipboardReadAccess(
        pkgName: String,
        uid: Int,
        userId: Int,
        timeoutMs: Long = DEFAULT_REQUEST_TIMEOUT_MS
    ): Boolean {
        val ctx = getSystemContext()
        if (ctx == null) {
            XposedBridge.log("$TAG requestClipboardReadAccess: no system context")
            return FAIL_OPEN_WHEN_SERVICE_UNAVAILABLE
        }

        val service = getOrBindService(ctx)
        if (service == null) {
            XposedBridge.log("$TAG requestClipboardReadAccess: service unavailable, fail-open=$FAIL_OPEN_WHEN_SERVICE_UNAVAILABLE")
            return FAIL_OPEN_WHEN_SERVICE_UNAVAILABLE
        }

        return try {
            service.requestClipboardRead(pkgName, uid, userId, timeoutMs.coerceIn(200L, 3000L)) == RESULT_ALLOW
        } catch (_: DeadObjectException) {
            sService = null
            FAIL_OPEN_WHEN_SERVICE_UNAVAILABLE
        } catch (t: Throwable) {
            XposedBridge.log("$TAG requestClipboardReadAccess failed: ${t.stackTraceToString()}")
            sService = null
            FAIL_OPEN_WHEN_SERVICE_UNAVAILABLE
        }
    }

    private fun ensureBound(ctx: Context) {
        var connectLatch: CountDownLatch? = null
        synchronized(LOCK) {
            if (sBinding || sService != null) return
            sBinding = true
            connectLatch = CountDownLatch(1)
            sConnectLatch = connectLatch
        }

        val intent = Intent(ACTION_BIND).apply {
            setPackage(TARGET_PKG)
        }

        try {
            val systemUser = XposedHelpers.getStaticObjectField(
                UserHandle::class.java,
                "SYSTEM"
            ) as UserHandle

            ctx.bindServiceAsUser(
                intent,
                connection,
                Context.BIND_AUTO_CREATE,
                systemUser
            )

            /** 🔴 必须有兜底，否则必死锁 */
            handler.postDelayed({
                if (sService == null) {
                    XposedBridge.log("$TAG bind timeout, reset binding")
                    sBinding = false
                    connectLatch?.countDown()
                }
            }, BIND_TIMEOUT_MS)

        } catch (t: Throwable) {
            XposedBridge.log("$TAG bind exception: ${t.stackTraceToString()}")
            sBinding = false
            connectLatch?.countDown()
        }
    }

    private fun getOrBindService(ctx: Context): IHookNotify? {
        sService?.let { return it }
        ensureBound(ctx)

        val latch = sConnectLatch
        if (latch != null) {
            runCatching {
                latch.await(BIND_WAIT_MS, TimeUnit.MILLISECONDS)
            }
        }
        return sService
    }

    @SuppressLint("PrivateApi")
    private fun getSystemContext(): Context? {
        return try {
            val atClass = Class.forName("android.app.ActivityThread")
            val at = XposedHelpers.callStaticMethod(atClass, "currentActivityThread")
                ?: return null
            XposedHelpers.callMethod(at, "getSystemContext") as Context
        } catch (_: Throwable) {
            null
        }
    }
}
