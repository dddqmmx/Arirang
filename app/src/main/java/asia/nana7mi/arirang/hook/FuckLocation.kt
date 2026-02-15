package asia.nana7mi.arirang.hook;

import android.location.Location;
import android.location.LocationRequest;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class FuckLocation  implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!"android".equals(lpparam.packageName)) return;

        try {
            Class<?> locationManagerService  = XposedHelpers.findClass("com.android.server.LocationManagerService ", lpparam.classLoader);
            hookClipboardAccessMethod(locationManagerService);
            XposedBridge.log("FuckClipboard hooked successfully");
        } catch (Throwable t) {
            XposedBridge.log("Hook failed: " + t + " 包名: " + lpparam.packageName + " 进程名: " + lpparam.processName);
        }
    }
    private void hookClipboardAccessMethod(Class<?> clipboardService) {
        try {
            XposedHelpers.findAndHookMethod(clipboardService, "getLastLocation", LocationRequest.class, String.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            String callingPackage = (String) param.args[1];
                            Location location = (Location) param.getResult();
                            XposedBridge.log(location.toString());
                            param.setResult(location);
                        }
                    });
        } catch (Exception e) {
            XposedBridge.log("Error hooking clipboardAccessAllowed: " + e);
        }
    }
}
