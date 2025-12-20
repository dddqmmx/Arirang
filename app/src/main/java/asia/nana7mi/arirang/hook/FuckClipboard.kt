package asia.nana7mi.arirang.hook;

import android.app.AppOpsManager;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import asia.nana7mi.arirang.BuildConfig;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class FuckClipboard implements IXposedHookLoadPackage {
    private static int opWriteClipboard;
    private static final String PREFS_NAME = "clipboard_whitelist_prefs";
    private static final String ENABLED_KEY = "enabled";
    private static final String MODE_KEY = "mode";
    private static final String WHITELIST_KEY = "whitelist";
    private static final String BLACKLIST_KEY = "blacklist";
    private static final String LAST_MODIFIED_KEY = "last_modified";

    public enum Mode { WHITELIST, BLACKLIST }

    private static Set<String> PACKAGE_SET = Collections.emptySet();
    private static boolean ENABLED = false;
    private static Mode MODE = Mode.WHITELIST;
    private static long lastLoadedTimestamp = -1;

    static {
        try {
            Field opWriteClipboardField = XposedHelpers.findField(AppOpsManager.class, "OP_WRITE_CLIPBOARD");
            opWriteClipboard = (int) opWriteClipboardField.get(null);
        } catch (Exception e) {
            XposedBridge.log("Error initializing AppOpsManager field: " + e);
        }
    }

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!"android".equals(lpparam.packageName)) return;

        try {
            loadConfigIfUpdated(); // Initial load
            XposedBridge.log("Clipboard hook loaded with mode: " + MODE + ", enabled: " + ENABLED + ", package set: " + PACKAGE_SET);

            Class<?> clipboardService = XposedHelpers.findClass("com.android.server.clipboard.ClipboardService", lpparam.classLoader);
            hookClipboardAccessMethod(clipboardService);
            XposedBridge.log("FuckClipboard hooked successfully");
        } catch (Throwable t) {
            XposedBridge.log("Hook failed: " + t + " 包名: " + lpparam.packageName + " 进程名: " + lpparam.processName);
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
        int modeInt = pref.getInt(MODE_KEY, 0);  // 0 = WHITELIST, 1 = BLACKLIST
        MODE = (modeInt == 1) ? Mode.BLACKLIST : Mode.WHITELIST;

        Set<String> rawSet = pref.getStringSet(MODE == Mode.WHITELIST ? WHITELIST_KEY : BLACKLIST_KEY, null);
        PACKAGE_SET = rawSet != null ? new HashSet<>(rawSet) : Collections.emptySet();
    }

    private void hookClipboardAccessMethod(Class<?> clipboardService) {
        try {
            XposedHelpers.findAndHookMethod(clipboardService, "clipboardAccessAllowed", int.class, String.class, String.class, int.class, int.class, int.class, boolean.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            loadConfigIfUpdated();

                            int op = (int) param.args[0];
                            if (op == opWriteClipboard) return;

                            if (!ENABLED) return;

                            String callingPackage = (String) param.args[1];
                            boolean match = PACKAGE_SET.contains(callingPackage);

                            if ((MODE == Mode.WHITELIST && !match) || (MODE == Mode.BLACKLIST && match)) {
                                param.setResult(false); // Block access
                            }
                        }
                    });
        } catch (Exception e) {
            XposedBridge.log("Error hooking clipboardAccessAllowed: " + e);
        }
    }
}