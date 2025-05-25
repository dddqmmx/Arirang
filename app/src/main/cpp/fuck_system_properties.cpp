#include <jni.h>
#include <dlfcn.h>
#include <string>
#include <android/log.h>
#include "lsp_native.h"

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  "LSP-HOOK", __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,  "LSP-HOOK", __VA_ARGS__)

// The global hook installer:
static HookFunType hook_func = nullptr;

// Backup pointer for the original SystemProperties_getSS
static jstring (*backup_SystemProperties_getSS)(
        JNIEnv* env,
        jclass   clazz,
        jstring  keyJ,
        jstring  defJ
);

// Our fake implementation: override "ro.build.version.release" to always return "13"
static jstring fake_SystemProperties_getSS(
        JNIEnv* env,
        jclass   clazz,
        jstring  keyJ,
        jstring  defJ
) {
    const char* key = env->GetStringUTFChars(keyJ, nullptr);
    if (key) {
        if (strcmp(key, "ro.build.version.release") == 0) {
            env->ReleaseStringUTFChars(keyJ, key);
            // Return our spoofed value
            return env->NewStringUTF("13");
        }
        env->ReleaseStringUTFChars(keyJ, key);
    }
    // Fallback to the original
    return backup_SystemProperties_getSS(env, clazz, keyJ, defJ);
}

// Called whenever a new native library is loaded into the process
static void on_library_loaded(const char* name, void* handle) {
    // We only care about android_runtime
    if (std::string(name).ends_with("libandroid_runtime.so")) {
        // Look up the mangled/unmangled symbol for SystemProperties_getSS
        void* sym = dlsym(handle, "SystemProperties_getSS");
        if (sym) {
            LOGI("Found SystemProperties_getSS at %p, installing hook", sym);
            hook_func(
                    sym,
                    (void*) fake_SystemProperties_getSS,
                    (void**) &backup_SystemProperties_getSS
            );
        } else {
            LOGW("Could not find SystemProperties_getSS symbol");
        }
    }
}

extern "C" JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    // Nothing to do here; we just return the JNI version so that
    // native_init() will be called by your loader infrastructure.
    return JNI_VERSION_1_6;
}

extern "C" JNIEXPORT NativeOnModuleLoaded native_init(
        const NativeAPIEntries* entries
) {
    // Save the hook function pointer provided by the host
    hook_func = entries->hook_func;
    // Tell the host to call on_library_loaded() for each loaded .so
    return on_library_loaded;
}
