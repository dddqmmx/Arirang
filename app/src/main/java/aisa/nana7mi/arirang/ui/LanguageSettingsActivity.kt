package aisa.nana7mi.arirang.ui

import aisa.nana7mi.arirang.R
import aisa.nana7mi.arirang.model.LanguageItem
import aisa.nana7mi.arirang.ui.adapter.LanguageAdapter
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.core.content.edit

class LanguageSettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_language_settings)

        // 加载语言显示名和对应的代码
        val names = resources.getStringArray(R.array.languages)
        val codes = resources.getStringArray(R.array.language_codes)

        // 合并成 LanguageItem 列表
        val savedLang = getSharedPreferences("settings", MODE_PRIVATE)
            .getString("language", null)

        val currentLang = if (savedLang == null || savedLang == "null") null else savedLang

        val languageList = names.zip(codes).map { (name, code) ->
            val normalizedCode = if (code == "null") null else code
            LanguageItem(name, normalizedCode ?: "null", isSelected = normalizedCode == currentLang)
        }

        // 初始化 RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.languageRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = LanguageAdapter(languageList) { selected ->

            val currentLang = getSharedPreferences("settings", MODE_PRIVATE)
                .getString("language", null)
            if (currentLang == selected.code) return@LanguageAdapter

            // 保存语言设置
            getSharedPreferences("settings", MODE_PRIVATE)
                .edit { putString("language", selected.code).commit() }

            val intent = applicationContext.packageManager
                .getLaunchIntentForPackage(applicationContext.packageName)
            intent?.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
            )
            applicationContext.startActivity(intent)

            // 杀掉旧进程，防止语言错乱
            Runtime.getRuntime().exit(0)
        }
    }
}
