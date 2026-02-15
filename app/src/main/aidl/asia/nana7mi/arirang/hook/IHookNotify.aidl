package asia.nana7mi.arirang.hook;

oneway interface IHookNotify {
    void onPermissionUsed(String pkgName, String opName);
}
