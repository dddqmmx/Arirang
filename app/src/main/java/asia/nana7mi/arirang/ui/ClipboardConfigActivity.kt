package asia.nana7mi.arirang.ui

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import asia.nana7mi.arirang.R
import asia.nana7mi.arirang.model.AppInfo
import asia.nana7mi.arirang.ui.adapter.AppListAdapter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatSpinner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Date
import androidx.core.content.edit

class ClipboardConfigActivity : BaseActivity() {

    private val PREFS_NAME = "clipboard_whitelist_prefs"
    private val ENABLED_KEY = "enabled"
    private val MODE_KEY = "mode"
    private val WHITELIST_KEY = "whitelist"
    private val BLACKLIST_KEY = "blacklist"
    private val LAST_MODIFIED_KEY = "last_modified"

    private lateinit var featureStatusIcon: ImageView
    private lateinit var filterSpinner: AppCompatSpinner
    private lateinit var searchEditText: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AppListAdapter
    private val allApps = mutableListOf<AppInfo>() // 完整数据
    private val filteredApps = mutableListOf<AppInfo>() // 当前显示的数据（adapter 绑定的）

    private lateinit var prefs: SharedPreferences
    private var enabled = false
    private var mode = 0
    private var whitelist = mutableSetOf<String>()
    private var blacklist = mutableSetOf<String>()

    // 添加互斥锁防止并发访问问题
    private val prefsMutex = Mutex()
    private val dataLock = Any()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_clipboard_setting)

        try {
            //这是Lsposed的专有API无需修改或修复
            prefs = getSharedPreferences(PREFS_NAME, MODE_WORLD_READABLE)
        } catch (e: Exception) {
            // 如果 SharedPreferences 初始化失败，使用默认值并记录错误
            android.util.Log.e("ClipboardConfig", "Failed to initialize SharedPreferences", e)
            // 可以考虑显示错误提示给用户或使用内存存储作为后备方案
            return
        }

        initViews()
        loadClipboardConfigSafely()
        setupListeners()
        loadInstalledApps()
    }

    private fun initViews() {
        featureStatusIcon = findViewById<ImageView>(R.id.featureStatusIcon)
        searchEditText = findViewById<EditText>(R.id.searchEditText)
        filterSpinner = findViewById<AppCompatSpinner>(R.id.filterSpinner)

        recyclerView = findViewById(R.id.appListRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = AppListAdapter(
            filteredApps,
            onPermissionChange = { appInfo, hasPermission ->
                onAppPermissionChanged(appInfo, hasPermission)
            },
            getMode = { synchronized(dataLock) { mode } },
            getWhitelist = { synchronized(dataLock) { whitelist.toSet() } },
            getBlacklist = { synchronized(dataLock) { blacklist.toSet() } }
        )

        recyclerView.adapter = adapter
    }

    private fun setupListeners() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterApps(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                synchronized(dataLock) {
                    mode = position
                }

                lifecycleScope.launch {
                    saveClipboardConfigSafely()
                    adapter.refreshModeChange()
                    updateAppListForMode()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        featureStatusIcon.setOnClickListener {
            synchronized(dataLock) {
                enabled = !enabled
            }
            featureStatusIcon.setImageResource(
                if (enabled) R.drawable.ic_status_enabled else R.drawable.ic_status_disabled
            )
            lifecycleScope.launch {
                saveClipboardConfigSafely()
            }
        }
    }

    private suspend fun updateAppListForMode() {
        withContext(Dispatchers.IO) {
            val currentMode: Int
            val currentWhitelist: Set<String>
            val currentBlacklist: Set<String>

            synchronized(dataLock) {
                currentMode = mode
                currentWhitelist = whitelist.toSet()
                currentBlacklist = blacklist.toSet()
            }

            val items = synchronized(dataLock) {
                allApps.map { pkg ->
                    AppInfo(
                        appName = pkg.appName,
                        packageName = pkg.packageName,
                        icon = null,
                        hasPermission = when (currentMode) {
                            0 -> currentWhitelist.contains(pkg.packageName)
                            else -> currentBlacklist.contains(pkg.packageName)
                        }
                    )
                }.sortedByDescending { it.hasPermission }
            }

            withContext(Dispatchers.Main) {
                adapter.setList(items)
                synchronized(dataLock) {
                    allApps.clear()
                    allApps.addAll(items)
                    filteredApps.clear()
                    filteredApps.addAll(items)
                }
                recyclerView.scrollToPosition(0)
            }
        }
    }

    private fun onAppPermissionChanged(app: AppInfo, granted: Boolean) {
        val key = app.packageName
        synchronized(dataLock) {
            if (mode == 0) {
                if (granted) whitelist.add(key) else whitelist.remove(key)
            } else {
                if (granted) blacklist.add(key) else blacklist.remove(key)
            }
        }

        lifecycleScope.launch {
            saveClipboardConfigSafely()
        }
    }

    private fun loadInstalledApps() {
        val pm = packageManager
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                val currentMode: Int
                val currentWhitelist: Set<String>
                val currentBlacklist: Set<String>

                synchronized(dataLock) {
                    currentMode = mode
                    currentWhitelist = whitelist.toSet()
                    currentBlacklist = blacklist.toSet()
                }

                val items = packages.map { pkg ->
                    AppInfo(
                        appName = pm.getApplicationLabel(pkg).toString(),
                        packageName = pkg.packageName,
                        icon = null,
                        hasPermission = when (currentMode) {
                            0 -> currentWhitelist.contains(pkg.packageName)
                            else -> currentBlacklist.contains(pkg.packageName)
                        }
                    )
                }.sortedByDescending { it.hasPermission }

                withContext(Dispatchers.Main) {
                    adapter.setList(items)
                    synchronized(dataLock) {
                        allApps.clear()
                        allApps.addAll(items)
                        filteredApps.clear()
                        filteredApps.addAll(items)
                    }
                    recyclerView.scrollToPosition(0)
                }
            } catch (e: Exception) {
                android.util.Log.e("ClipboardConfig", "Failed to load installed apps", e)
                // 可以在主线程显示错误提示
            }
        }
    }

    private fun filterApps(query: String) {
        val newList = synchronized(dataLock) {
            if (query.isBlank()) {
                allApps.toList()
            } else {
                allApps.filter {
                    it.appName.contains(query, ignoreCase = true) ||
                            it.packageName.contains(query, ignoreCase = true)
                }
            }
        }
        adapter.setList(newList)
        recyclerView.scrollToPosition(0)
    }

    // 修复3: 安全的配置加载，添加异常处理
    private fun loadClipboardConfigSafely() {
        try {
            synchronized(dataLock) {
                enabled = prefs.getBoolean(ENABLED_KEY, false)
                mode = prefs.getInt(MODE_KEY, 0)

                // 修复2: 更安全的 StringSet 处理
                whitelist = try {
                    prefs.getStringSet(WHITELIST_KEY, null)?.toMutableSet() ?: mutableSetOf()
                } catch (e: Exception) {
                    android.util.Log.w("ClipboardConfig", "Failed to load whitelist", e)
                    mutableSetOf()
                }

                blacklist = try {
                    prefs.getStringSet(BLACKLIST_KEY, null)?.toMutableSet() ?: mutableSetOf()
                } catch (e: Exception) {
                    android.util.Log.w("ClipboardConfig", "Failed to load blacklist", e)
                    mutableSetOf()
                }
            }

            // UI 更新放在数据加载之后
            featureStatusIcon.setImageResource(
                if (enabled) R.drawable.ic_status_enabled else R.drawable.ic_status_disabled
            )
            filterSpinner.setSelection(mode)

        } catch (e: Exception) {
            android.util.Log.e("ClipboardConfig", "Failed to load clipboard config", e)
            // 使用默认值
            synchronized(dataLock) {
                enabled = false
                mode = 0
                whitelist = mutableSetOf()
                blacklist = mutableSetOf()
            }
        }
    }

    // 修复4: 异步安全的配置保存
    private suspend fun saveClipboardConfigSafely() {
        try {
            prefsMutex.withLock {
                val currentEnabled: Boolean
                val currentMode: Int
                val currentWhitelist: Set<String>
                val currentBlacklist: Set<String>

                synchronized(dataLock) {
                    currentEnabled = enabled
                    currentMode = mode
                    currentWhitelist = whitelist.toSet()
                    currentBlacklist = blacklist.toSet()
                }

                prefs.edit {
                    putBoolean(ENABLED_KEY, currentEnabled)
                    putInt(MODE_KEY, currentMode)
                    putStringSet(WHITELIST_KEY, currentWhitelist)
                    putStringSet(BLACKLIST_KEY, currentBlacklist)
                    putLong(LAST_MODIFIED_KEY, Date().time)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ClipboardConfig", "Failed to save clipboard config", e)
        }
    }
}