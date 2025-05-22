package aisa.nana7mi.arirang.model

import android.graphics.drawable.Drawable

data class AppInfo(
    val appName: String,
    val packageName: String,
    var icon: Drawable?,
    val hasPermission: Boolean
)
