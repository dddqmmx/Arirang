package aisa.nana7mi.arirang.hook;

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
import java.util.List;
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
        if (!BuildConfig.APPLICATION_ID.equals(lpparam.packageName)) return;
        XposedBridge.log("SIM hook loading for package: " + lpparam.packageName + ", process: " + lpparam.processName);

        try {
            XposedHelpers.findAndHookMethod("android.os.ServiceManager",
                    lpparam.classLoader,
                    "getService",
                    String.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            String  name = (String) param.args[0];
                            XposedBridge.log("Found service registration! Name:" + name);
                            IBinder binder = (IBinder) param.getResult();
                            if (binder != null){
                                XposedBridge.log("IBinder class: " + binder.getClass().getName());
                            }
                        }
                    });


//            Class<?> proxyClass = XposedHelpers.findClassIfExists("com.android.internal.telephony.SubscriptionController", lpparam.classLoader);
//            methodsTest(proxyClass);


//            XposedHelpers.findAndHookMethod(proxyClass, "getActiveSubscriptionInfoList",String.class,String.class,boolean.class, new XC_MethodHook() {
//                @Override
//                protected void beforeHookedMethod(MethodHookParam param) {
//                    XposedBridge.log("getActiveSubscriptionInfoList called with args: " + Arrays.toString(param.args));
//                }
//                @Override
//                protected void afterHookedMethod(MethodHookParam param) {
//                    XposedBridge.log("Setting result to empty list");
//                    param.setResult(Collections.emptyList());
//                }
//            });
        } catch (Throwable t) {
            XposedBridge.log("Hook failed: " + t.getMessage() + "\n" + Log.getStackTraceString(t));
        }
    }

    private void methodsTest(Class<?> clazz) {
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            try {
                if (method.isBridge() || method.isSynthetic()) continue; // 忽略编译器自动生成的方法

                String methodName = method.getName();
                Class<?>[] paramTypes = method.getParameterTypes();

                XposedBridge.log("Trying to hook method: " + methodName + "(" + Arrays.toString(paramTypes) + ")");

                // 构建参数数组并添加 hook
                Object[] hookArgs = combine(paramTypes, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log("[Before] " + methodName + " called with args: " + Arrays.toString(param.args));
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log("[After] " + methodName + " returned: " + param.getResult());
                    }
                });

                // 使用参数类型确定唯一的方法签名，避免重载问题
                XposedHelpers.findAndHookMethod(clazz, methodName, hookArgs);
            } catch (Throwable t) {
                XposedBridge.log("Failed to hook method: " + method.getName() + " - " + t);
            }
        }
    }

    private Object[] combine(Class<?>[] paramTypes, XC_MethodHook hook) {
        Object[] combined = new Object[paramTypes.length + 1];
        System.arraycopy(paramTypes, 0, combined, 0, paramTypes.length);
        combined[paramTypes.length] = hook;
        return combined;
    }




}