package asia.nana7mi.arirang.hook

import android.app.AppOpsManager
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class FuckClipboard : IXposedHookLoadPackage {
    private val config = HookConfig("clipboard_whitelist_prefs")

    companion object {
        private var opWriteClipboard: Int = -1
        init {
            runCatching {
                opWriteClipboard = XposedHelpers.findField(AppOpsManager::class.java, "OP_WRITE_CLIPBOARD").get(null) as Int
            }.onFailure { XposedBridge.log("FuckClipboard: Error getting OP_WRITE_CLIPBOARD: $it") }
        }
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "android") return

        runCatching {
            val clipboardService = XposedHelpers.findClass("com.android.server.clipboard.ClipboardService", lpparam.classLoader)

            hookClipboard(clipboardService)

            XposedBridge.log("FuckClipboard: Hooked successfully")
        }.onFailure {
            XposedBridge.log("FuckClipboard: Hook failed: $it")
        }
    }

    fun hookClipboard(clipboardService: Class<*>) {
        val hookCallback = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                config.loadIfUpdated("whitelist", "blacklist")

                val op = param.args[0] as Int
                if (op == opWriteClipboard) return

                val callingPackage = param.args[1] as String
                if (config.shouldBlock(callingPackage)) {
                    param.result = false
                }
            }
        }

        // Android 15（8 参数）
        runCatching {
            XposedHelpers.findAndHookMethod(
                clipboardService,
                "clipboardAccessAllowed",
                Int::class.java,
                String::class.java,
                String::class.java,
                Int::class.java,
                Int::class.java,
                Int::class.java,
                Boolean::class.java,
                Boolean::class.java,
                hookCallback
            )
        }

        // Android 16（7 参数）
        runCatching {
            XposedHelpers.findAndHookMethod(
                clipboardService,
                "clipboardAccessAllowed",
                Int::class.java,
                String::class.java,
                String::class.java,
                Int::class.java,
                Int::class.java,
                Int::class.java,
                Boolean::class.java,
                hookCallback
            )
        }
    }


}