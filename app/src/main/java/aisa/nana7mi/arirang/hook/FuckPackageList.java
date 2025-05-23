package aisa.nana7mi.arirang.hook;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import aisa.nana7mi.arirang.BuildConfig;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class FuckPackageList implements IXposedHookLoadPackage {
    private static final String PREFS_NAME = "clipboard_visibility_prefs";
    private static final String ENABLED_KEY = "enabled";
    private static final String MODE_KEY = "mode";
    private static final String VISIBLE_LIST_KEY = "visible_list";
    private static final String INVISIBLE_LIST_KEY = "invisible_list";
    private static final String LAST_MODIFIED_KEY = "last_modified";

    public enum Mode { VISIBLE, INVISIBLE }

    private static Set<String> PACKAGE_SET = Collections.emptySet();
    private static boolean ENABLED = false;
    private static Mode MODE = Mode.VISIBLE;
    private static long lastLoadedTimestamp = -1;

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) {
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
                            loadConfigIfUpdated();
                            if (!ENABLED) return;
                            List<ApplicationInfo> originalList = (List<ApplicationInfo>) param.getResult();
                            List<ApplicationInfo> filteredList = new ArrayList<>();
                            for (ApplicationInfo appInfo : originalList) {
                                if (shouldKeep(appInfo.packageName)) {
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
                            loadConfigIfUpdated();
                            if (!ENABLED) return;

                            Object parceledListSlice = param.getResult(); // 假设是 ParceledListSlice<PackageInfo>
                            if (parceledListSlice != null) {
                                try {
                                    // 反射调用 hidden 方法 getList()
                                    List<?> list = (List<?>) XposedHelpers.callMethod(parceledListSlice, "getList");
                                    List<PackageInfo> filteredList  = new ArrayList<>();
                                    for (Object item : list) {
                                        if (item instanceof PackageInfo) {
                                            PackageInfo info = (PackageInfo) item;
                                            if (shouldKeep(info.packageName)) {
                                                filteredList.add(info);
                                            }
                                        }
                                    }
                                    param.setResult(XposedHelpers.newInstance(parceledListSlice.getClass(),filteredList));
                                } catch (Throwable t) {
                                    XposedBridge.log("Failed to get list from ParceledListSlice: " + t.getMessage());
                                }
                            }
                        }
                    });
        } catch (Exception e) {
            XposedBridge.log("Error hooking methods: " + e.getMessage() + "\n" + Log.getStackTraceString(e));
        }
    }

    private static boolean shouldKeep(String packageName) {
        if (MODE == Mode.VISIBLE) {
            return PACKAGE_SET.contains(packageName);
        } else {
            return !PACKAGE_SET.contains(packageName);
        }
    }


    private static XSharedPreferences getPref() {
        XSharedPreferences pref = new XSharedPreferences(BuildConfig.APPLICATION_ID, PREFS_NAME);
        return pref.getFile().canRead() ? pref : null;
    }

    private static void loadConfigIfUpdated() {
        XSharedPreferences pref = getPref();
        if (pref == null) return;

        pref.reload();
        long newTimestamp = pref.getLong(LAST_MODIFIED_KEY, -1);
        if (newTimestamp == lastLoadedTimestamp) return;

        lastLoadedTimestamp = newTimestamp;

        ENABLED = pref.getBoolean(ENABLED_KEY, false);
        int modeInt = pref.getInt(MODE_KEY, 0);  // 0 = VISIBLE, 1 = INVISIBLE
        MODE = (modeInt == 1) ? Mode.INVISIBLE : Mode.VISIBLE;

        Set<String> rawSet = pref.getStringSet(MODE == Mode.VISIBLE ? VISIBLE_LIST_KEY : INVISIBLE_LIST_KEY, null);
        PACKAGE_SET = rawSet != null ? new HashSet<>(rawSet) : Collections.emptySet();
    }

}