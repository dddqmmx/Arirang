#include <jni.h>
#include <dlfcn.h>
#include <string>
#include <android/log.h>
#include "lsp_native.h"

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  "LSP-HOOK", __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,  "LSP-HOOK", __VA_ARGS__)

static HookFunType hook_func = nullptr;

static int (*backup_system_property_get)(const char* name, char* value);

static int fake_system_property_get(const char* name, char* value) {
    if (strcmp(name, "ro.build.version.release") == 0) {
        strcpy(value, "17");
        return static_cast<int>(strlen("17"));
    }
    return backup_system_property_get(name, value);
}

static void on_library_loaded(const char* name, void* handle) {
    if (std::string(name).ends_with("libc.so")) {
        void* target = dlsym(handle, "__system_property_get");
        if (target) {
            LOGI("Found __system_property_get at %p, installing hook", target);
            hook_func(target,(void*) fake_system_property_get,(void**) &backup_system_property_get);
        } else {
            LOGW("Could not find __system_property_get symbol");
        }
    }
}

extern "C" JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    return JNI_VERSION_1_6;
}

extern "C" JNIEXPORT NativeOnModuleLoaded native_init(const NativeAPIEntries* entries) {
    hook_func = entries->hook_func;
    return on_library_loaded;
}