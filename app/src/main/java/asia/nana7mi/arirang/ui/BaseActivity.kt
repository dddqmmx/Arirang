package asia.nana7mi.arirang.ui

import asia.nana7mi.arirang.util.LocaleHelper
import android.content.Context
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {
    override fun attachBaseContext(base: Context) {
        val prefs = base.getSharedPreferences("settings", MODE_PRIVATE)
        val lang = prefs.getString("language", null)
        val context = if (lang.isNullOrEmpty() || lang == "null") {
            base
        } else {
            LocaleHelper.setLocale(base, lang)
        }
        super.attachBaseContext(context)
    }
}