package aisa.nana7mi.arirang.hook;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class FuckPackageList implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!"android".equals(lpparam.packageName)) return;

        XposedBridge.log("Package hook loading for package: " + lpparam.packageName + ", process: " + lpparam.processName);

        try {
            Class<?> computerEngine = XposedHelpers.findClassIfExists("com.android.server.pm.ComputerEngine", lpparam.classLoader);
            if (computerEngine == null) {
                XposedBridge.log("Class not found ComputerEngine");
                return;
            }
            XposedBridge.log("Class found: " + computerEngine.getClassLoader());
            hookMethods(computerEngine);
        } catch (Throwable t) {
            XposedBridge.log("Hook failed: " + t.getMessage() + "\n" + Log.getStackTraceString(t));
        }
    }
    private void hookMethods(Class<?> computerEngine) {
        try {
            XposedHelpers.findAndHookMethod(computerEngine,"getInstalledApplications", long.class, int.class, int.class, boolean.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            List<ApplicationInfo> originalList = (List<ApplicationInfo>) param.getResult();
                            List<ApplicationInfo> filteredList = new ArrayList<>();
                            for (ApplicationInfo appInfo : originalList) {
                                if (!"com.yek.android.kfc.activitys".equals(appInfo.processName)) {
                                    filteredList.add(appInfo);
                                }
                            }
                            param.setResult(filteredList);
                        }
            });
            XposedHelpers.findAndHookMethod(computerEngine,"getInstalledPackages", long.class, int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            XposedBridge.log("PackageList: " +  param.getResult());
                            Object parceledListSlice = param.getResult(); // 假设是 ParceledListSlice<PackageInfo>
                            if (parceledListSlice != null) {
                                try {
                                    // 反射调用 hidden 方法 getList()
                                    List<?> list = (List<?>) XposedHelpers.callMethod(parceledListSlice, "getList");

                                    // 你可以将其转换为具体类型，例如：
                                    for (Object item : list) {
                                        if (item instanceof PackageInfo) {
                                            PackageInfo info = (PackageInfo) item;
                                            XposedBridge.log("Found package: " + info.packageName);
                                        }
                                    }
                                } catch (Throwable t) {
                                    XposedBridge.log("Failed to get list from ParceledListSlice: " + t.getMessage());
                                }
                            }
//                            Class<?> parceledListSliceClass = XposedHelpers.findClass("android.content.pm.ParceledListSlice", null);
//                            Object emptyList = XposedHelpers.callStaticMethod(parceledListSliceClass, "emptyList");
//                            param.setResult(emptyList);

                        }
                    });
        } catch (Exception e) {
            XposedBridge.log("Error hooking methods: " + e.getMessage() + "\n" + Log.getStackTraceString(e));
        }
    }

}