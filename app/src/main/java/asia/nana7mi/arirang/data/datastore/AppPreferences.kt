package asia.nana7mi.arirang.data.datastore

import android.content.Context
import androidx.core.content.edit

object AppPreferences {
    private const val PREFS_NAME = "arirang_prefs"
    private const val KEY_SETUP_COMPLETED = "is_setup_completed"

    fun setSetupCompleted(context: Context, isCompleted: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_SETUP_COMPLETED, isCompleted) }
    }

    // 检查是否已完成初始化
    fun isSetupCompleted(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SETUP_COMPLETED, false)
    }
}