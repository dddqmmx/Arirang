package asia.nana7mi.arirang.hook

import android.os.Binder
import android.os.Process
import android.os.UserHandle
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class FuckClipboard : IXposedHookLoadPackage {
    private val config = HookConfig("clipboard_whitelist_prefs")

    companion object {
        private const val WHITE_LIST_KEY = "whitelist"
        private const val BLACK_LIST_KEY = "blacklist"
        private const val PER_USER_RANGE = 100_000
        private const val SYSTEM_UID_MAX = Process.FIRST_APPLICATION_UID - 1
        private val bypassPackages = setOf(
            "android",
            "com.android.systemui",
            "com.android.shell",
            "asia.nana7mi.arirang"
        )
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "android") return

        runCatching {
            val clipboardService = XposedHelpers.findClass("com.android.server.clipboard.ClipboardService", lpparam.classLoader)
            val clipboardImpl = XposedHelpers.findClassIfExists(
                "com.android.server.clipboard.ClipboardService\$ClipboardImpl",
                lpparam.classLoader
            )
            hookClipboard(clipboardImpl ?: clipboardService)
            XposedBridge.log("FuckClipboard: hooked")
        }.onFailure {
            XposedBridge.log("FuckClipboard: hook failed: $it")
        }
    }

    private fun hookClipboard(targetClass: Class<*>) {
        val hookCallback = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val callingPackage = param.args.firstOrNull { it is String } as? String ?: return
                val uid = Binder.getCallingUid()
                val userId = (param.args.firstOrNull { it is Int } as? Int)
                    ?.takeIf { it >= 0 }
                    ?: runCatching {
                        XposedHelpers.callStaticMethod(UserHandle::class.java, "getUserId", uid) as Int
                    }.getOrDefault(uid / PER_USER_RANGE)

                if (uid == Process.INVALID_UID || shouldBypass(callingPackage, uid)) return

                config.loadIfUpdated(WHITE_LIST_KEY, BLACK_LIST_KEY)
                if (!config.enabled || !config.shouldBlock(callingPackage)) return

                val allowed = HookNotifyClient.requestClipboardReadAccess(callingPackage, uid, userId)
                if (!allowed) {
                    XposedBridge.log("FuckClipboard: denied read for $callingPackage uid=$uid")
                    param.result = null
                }
            }
        }

        XposedBridge.hookAllMethods(targetClass, "getPrimaryClip", hookCallback)
    }

    private fun shouldBypass(callingPackage: String, uid: Int): Boolean {
        if (callingPackage in bypassPackages) return true
        if (uid in 0..SYSTEM_UID_MAX) return true
        return false
    }
}
