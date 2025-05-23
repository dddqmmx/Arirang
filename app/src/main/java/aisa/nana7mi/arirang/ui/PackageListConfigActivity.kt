package aisa.nana7mi.arirang.ui

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import aisa.nana7mi.arirang.R
import aisa.nana7mi.arirang.model.AppInfo
import aisa.nana7mi.arirang.ui.adapter.AppListAdapter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatSpinner
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

class PackageListConfigActivity : BaseActivity() {

    private val PREFS_NAME = "clipboard_visibility_prefs"
    private val ENABLED_KEY = "enabled"
    private val MODE_KEY = "mode"
    private val VISIBLE_LIST_KEY = "visible_list"
    private val INVISIBLE_LIST_KEY = "invisible_list"
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
    private var visibleList = mutableSetOf<String>()
    private var invisibleList = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_package_list_config)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        prefs = getSharedPreferences(PREFS_NAME, MODE_WORLD_READABLE)

        featureStatusIcon = findViewById<ImageView>(R.id.featureStatusIcon)
        searchEditText = findViewById<EditText>(R.id.searchEditText)
        filterSpinner = findViewById<AppCompatSpinner>(R.id.filterSpinner)

        recyclerView = findViewById(R.id.appListRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = AppListAdapter(
            filteredApps,
            onPermissionChange = { appInfo, hasPermission ->
                onAppPermissionChanged(
                    appInfo,
                    hasPermission
                )
            },
            getMode = { mode },
            getWhitelist = { visibleList },
            getBlacklist = { invisibleList }
        )

        recyclerView.adapter = adapter

        loadClipboardConfig()
        setupListeners()
        loadInstalledApps()
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
                mode = position
                saveClipboardConfig()
                adapter.refreshModeChange()
                lifecycleScope.launch(Dispatchers.IO) {

                    val items = allApps.map { pkg ->
                        AppInfo(
                            appName = pkg.appName,
                            packageName = pkg.packageName,
                            icon = null,
                            hasPermission = when (mode) {
                                0 -> visibleList.contains(pkg.packageName)
                                else -> invisibleList.contains(pkg.packageName)
                            }
                        )
                    }.sortedByDescending  { it.hasPermission }

                    withContext(Dispatchers.Main) {
                        adapter.setList(items)
                        allApps.clear()
                        allApps.addAll(items)
                        filteredApps.clear()
                        filteredApps.addAll(items)
                        recyclerView.scrollToPosition(0)
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        featureStatusIcon.setOnClickListener {
            enabled = !enabled
            featureStatusIcon.setImageResource(
                if (enabled) R.drawable.ic_status_enabled else R.drawable.ic_status_disabled
            )
            saveClipboardConfig()
        }
    }

    private fun onAppPermissionChanged(app: AppInfo, granted: Boolean) {
        val key = app.packageName
        if (mode == 0) {
            if (granted) visibleList.add(key) else visibleList.remove(key)
        } else {
            if (granted) invisibleList.add(key) else invisibleList.remove(key)
        }
        saveClipboardConfig()
    }


    private fun loadInstalledApps() {
        val pm = packageManager
        lifecycleScope.launch(Dispatchers.IO) {

            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val items = packages.map { pkg ->
                AppInfo(
                    appName = pm.getApplicationLabel(pkg).toString(),
                    packageName = pkg.packageName,
                    icon = null,
                    hasPermission = when (mode) {
                        0 -> visibleList.contains(pkg.packageName)
                        else -> invisibleList.contains(pkg.packageName)
                    }
                )
            }.sortedByDescending { it.hasPermission }


            withContext(Dispatchers.Main) {
                adapter.setList(items)
                allApps.clear()
                allApps.addAll(items)
                filteredApps.clear()
                filteredApps.addAll(items)
                recyclerView.scrollToPosition(0)
            }
        }
    }

    private fun filterApps(query: String) {
        val newList = if (query.isBlank()) {
            allApps
        } else {
            allApps.filter {
                it.appName.contains(query, ignoreCase = true) ||
                        it.packageName.contains(query, ignoreCase = true)
            }
        }
        adapter.setList(newList)
        recyclerView.scrollToPosition(0)
    }

    private fun loadClipboardConfig() {
        enabled = prefs.getBoolean(ENABLED_KEY, false)
        mode = prefs.getInt(MODE_KEY, 0)
        visibleList = prefs.getStringSet(VISIBLE_LIST_KEY, emptySet())?.toMutableSet() ?: mutableSetOf()
        invisibleList = prefs.getStringSet(INVISIBLE_LIST_KEY, emptySet())?.toMutableSet() ?: mutableSetOf()

        featureStatusIcon.setImageResource(
            if (enabled) R.drawable.ic_status_enabled else R.drawable.ic_status_disabled
        )
        filterSpinner.setSelection(mode)
    }

    private fun saveClipboardConfig() {
        prefs.edit() {
            apply {
                putBoolean(ENABLED_KEY, enabled)
                putInt(MODE_KEY, mode)
                putStringSet(VISIBLE_LIST_KEY, visibleList)
                putStringSet(INVISIBLE_LIST_KEY, invisibleList)
                putLong(LAST_MODIFIED_KEY, Date().time)
            }
        }
    }

}