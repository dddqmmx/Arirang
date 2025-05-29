package asia.nana7mi.arirang

import asia.nana7mi.arirang.util.LocaleHelper
import android.app.Application
import android.content.Context
import android.util.Log

class ArirangApp : Application() {
    override fun attachBaseContext(base: Context) {
        val prefs = base.getSharedPreferences("settings", MODE_PRIVATE)
        val lang = prefs.getString("language", null)
        Log.v("language", lang.toString())
        val context = if (lang.isNullOrEmpty() || lang == "null") {
            base
        } else {
            LocaleHelper.setLocale(base, lang)
        }
        super.attachBaseContext(context)
    }
}
