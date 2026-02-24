package asia.nana7mi.arirang.hook

import asia.nana7mi.arirang.BuildConfig

abstract class BaseHookModule(
    private val targetPackages: Set<String> = emptySet(),
    private val matchSystem: Boolean = false,
    private val matchClient: Boolean = false
): HookModule {
    override fun matches(packageName: String): Boolean {
        if (matchSystem && packageName == "android") return true
        if (matchClient && BuildConfig.APPLICATION_ID == packageName) return true
        return packageName in targetPackages
    }

}