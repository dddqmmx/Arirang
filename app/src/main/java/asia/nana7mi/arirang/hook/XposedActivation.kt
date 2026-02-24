package asia.nana7mi.arirang.hook

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class XposedActivation : BaseHookModule(matchClient = true) {
    override fun onHook(lpparam: XC_LoadPackage.LoadPackageParam) {
        val point = XposedHelpers.findClass("asia.nana7mi.arirang.ui.fragment.HomeFragment",lpparam.classLoader)
        XposedHelpers.findAndHookMethod(point,"isXposedActivation", object :XC_MethodHook(){
            override fun beforeHookedMethod(param: MethodHookParam) {
                param.setResult(true);
            }
        })
    }
}