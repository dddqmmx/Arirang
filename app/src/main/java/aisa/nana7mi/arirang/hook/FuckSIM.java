package aisa.nana7mi.arirang.hook;

import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.IBinder;
import android.os.Parcel;
import android.telephony.SubscriptionInfo;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import aisa.nana7mi.arirang.BuildConfig;
import dalvik.system.DexClassLoader;
import dalvik.system.DexFile;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class FuckSIM implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) {
        if (!"com.android.phone".equals(lpparam.packageName)) return;
        XposedBridge.log("SIM hook loading for package: " + lpparam.packageName + ", process: " + lpparam.processName);

        try {
            Class<?> subscriptionManagerService = XposedHelpers.findClassIfExists("com.android.internal.telephony.subscription.SubscriptionManagerService", lpparam.classLoader);
            if (subscriptionManagerService == null) {
                XposedBridge.log("Class not found subscriptionManagerService");
                return;
            }
            XposedBridge.log("Class found");
            hookMethods(subscriptionManagerService);
//            methodsTest(subscriptionManagerService);
        } catch (Throwable t) {
            XposedBridge.log("Hook failed: " + t.getMessage() + "\n" + Log.getStackTraceString(t));
        }
    }

    private void hookMethods(Class<?> computerEngine) {
        try {
            XposedHelpers.findAndHookMethod(computerEngine,"getActiveSubscriptionInfoList", String.class, String.class, boolean.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            param.setResult(Collections.emptyList());
                        }
                    });
        }catch (Exception e) {
            XposedBridge.log("Error hooking methods: " + e.getMessage() + "\n" + Log.getStackTraceString(e));
        }
    }

    private void methodsTest(Class<?> clazz) {
        Method[] methods = clazz.getDeclaredMethods();
        Set<String> hookedMethods = new HashSet<>();

        for (Method method : methods) {
            try {
                if (method.isBridge() || method.isSynthetic()) continue;

                String methodName = method.getName();

                // 避免对同名方法重复 hook（因为 hookAllMethods 是对所有重载生效）
                if (hookedMethods.contains(methodName)) continue;
                hookedMethods.add(methodName);

                XposedBridge.hookAllMethods(clazz, methodName, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log("[Before] " + methodName + " called with args: " + Arrays.toString(param.args));
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log("[After] " + methodName + " returned: " + param.getResult());
                    }
                });

//                XposedBridge.log("Hooked all methods named: " + methodName);
            } catch (Throwable t) {
                XposedBridge.log("Failed to hook method: " + method.getName() + " - " + t);
            }
        }
    }





}