package aisa.nana7mi.arirang.hook;

import android.util.Log;
import aisa.nana7mi.arirang.BuildConfig;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class XposedActivationTest implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) {
        if (!BuildConfig.APPLICATION_ID.equals(lpparam.packageName)) return;
        try {
            Class<?> point = XposedHelpers.findClassIfExists("aisa.nana7mi.arirang.ui.fragment.HomeFragment", lpparam.classLoader);
            hookMethods(point);
        } catch (Throwable t) {
            XposedBridge.log("Hook failed: " + t.getMessage() + "\n" + Log.getStackTraceString(t));
        }
    }

    private void hookMethods(Class<?> point) {
        try {
            XposedHelpers.findAndHookMethod(point, "isXposedActivation",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            param.setResult(true);
                        }
                    });
        } catch (Exception e) {
            XposedBridge.log("Error hooking methods: " + e.getMessage() + "\n" + Log.getStackTraceString(e));
        }
    }

}