package asia.nana7mi.arirang.hook

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.UserHandle
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

object HookNotifyClient {

    private const val TAG = "HookNotifyClient"
    private const val TARGET_PKG = "asia.nana7mi.arirang"
    private const val ACTION_BIND = "asia.nana7mi.arirang.BIND_NOTIFY"
    private const val BIND_TIMEOUT_MS = 3000L

    @Volatile
    private var sService: IHookNotify? = null

    @Volatile
    private var sBinding = false

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
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            XposedBridge.log("$TAG onServiceDisconnected $name")
            sService = null
            sBinding = false
        }
    }

    fun notifyPermissionUsed(pkgName: String, opName: String) {
        val ctx = getSystemContext() ?: return

        val service = sService
        if (service == null) {
            ensureBound(ctx)
            return
        }

        try {
            service.onPermissionUsed(pkgName, opName)
        } catch (t: Throwable) {
            XposedBridge.log("$TAG notify failed: ${t.stackTraceToString()}")
            sService = null
        }
    }

    private fun ensureBound(ctx: Context) {
        synchronized(LOCK) {
            if (sBinding || sService != null) return
            sBinding = true
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
                }
            }, BIND_TIMEOUT_MS)

        } catch (t: Throwable) {
            XposedBridge.log("$TAG bind exception: ${t.stackTraceToString()}")
            sBinding = false
        }
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
