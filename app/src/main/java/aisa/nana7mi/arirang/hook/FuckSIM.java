package aisa.nana7mi.arirang.hook;

import android.os.Build;
import android.os.IBinder;
import android.os.Parcel;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

import dalvik.system.DexClassLoader;
import dalvik.system.DexFile;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class FuckSIM implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        XposedBridge.log("SIM hook loading for package: " + lpparam.packageName + ", process: " + lpparam.processName);

        try {
            Class<?> phoneInterfaceManager = XposedHelpers.findClassIfExists(
                    "com.android.internal.telephony.MiuiSubscriptionControllerStub", lpparam.classLoader);
            if (phoneInterfaceManager == null) {
                XposedBridge.log("Class found");
                return;
            }
            hookMethods(phoneInterfaceManager);

            XposedBridge.log("Class found: " + phoneInterfaceManager.getName());
        } catch (Throwable t) {
            XposedBridge.log("Hook failed: " + t.getMessage() + "\n" + Log.getStackTraceString(t));
        }
    }

    private void hookMethods(Class<?> phoneInterfaceManager) {
        try {
            XposedHelpers.findAndHookMethod(phoneInterfaceManager, "getActiveSubscriptionInfoList",
                    String.class, String.class, boolean.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            XposedBridge.log("wtffffffffffffffffffffffffffffffffffffffff");
                        }
                    });
        } catch (Exception e) {
            XposedBridge.log("Error hooking methods: " + e.getMessage() + "\n" + Log.getStackTraceString(e));
        }
    }

}