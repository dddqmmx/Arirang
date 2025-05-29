package asia.nana7mi.arirang.ui.adapter

import asia.nana7mi.arirang.R
import asia.nana7mi.arirang.model.AppInfo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppListAdapter(
    private val appList: MutableList<AppInfo>,
    private val onPermissionChange: (AppInfo, Boolean) -> Unit,
    private val getMode: () -> Int,       // 0=白名单, 1=黑名单
    private val getWhitelist: () -> Set<String>,
    private val getBlacklist: () -> Set<String>
) : RecyclerView.Adapter<AppListAdapter.ViewHolder>() {

    private var lastMode = getMode()
    private var lastWhitelist = getWhitelist()
    private var lastBlacklist = getBlacklist()

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val appIcon: ImageView = itemView.findViewById(R.id.appIcon)
        val appName: TextView = itemView.findViewById(R.id.appName)
        val packageName: TextView = itemView.findViewById(R.id.packageName)
        val permissionCheckBox: CheckBox = itemView.findViewById(R.id.permissionCheckBox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_info, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = appList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = appList[position]
        holder.appName.text = app.appName
        holder.packageName.text = app.packageName

        // 勾选状态来自最新 whitelist/blacklist
        val isChecked = when (getMode()) {
            0 -> getWhitelist().contains(app.packageName)
            else -> getBlacklist().contains(app.packageName)
        }
        holder.permissionCheckBox.setOnCheckedChangeListener(null)
        holder.permissionCheckBox.isChecked = isChecked
        holder.permissionCheckBox.setOnCheckedChangeListener { _, nowChecked ->
            onPermissionChange(app, nowChecked)
            // 注意：仅更新回调，排序留给 setList() 或 refreshModeChange()
        }

        // 异步加载图标
        if (app.icon == null) {
            (holder.itemView.context as? AppCompatActivity)?.lifecycleScope?.launch(Dispatchers.IO) {
                val icon = holder.itemView.context.packageManager
                    .getApplicationIcon(app.packageName)
                withContext(Dispatchers.Main) {
                    app.icon = icon
                    // 只有当前 bind 位置没变才设置，避免复用坑
                    if (holder.adapterPosition == position) {
                        holder.appIcon.setImageDrawable(icon)
                    }
                }
            }
        } else {
            holder.appIcon.setImageDrawable(app.icon)
        }
    }

    /** 分页加载场景下可用；这里只做简单追加，不影响排序 */
    fun appendList(newItems: List<AppInfo>) {
        val start = appList.size
        appList.addAll(newItems)
        notifyItemRangeInserted(start, newItems.size)
    }

    /**
     * 核心：接收外部的新列表（可能是全量或搜索结果），
     * 先按当前模式对白/黑名单做「已选前置」排序，
     * 再用 DiffUtil 高效更新 RecyclerView。
     */
    fun setList(newListRaw: List<AppInfo>) {
        val mode = getMode()
        val whitelist = getWhitelist()
        val blacklist = getBlacklist()

        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = appList.size
            override fun getNewListSize() = newListRaw.size

            override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
                return appList[oldPos].packageName == newListRaw[newPos].packageName && appList[oldPos].hasPermission == newListRaw[newPos].hasPermission
            }

            override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean {
                val old = appList[oldPos]
                val new = newListRaw[newPos]
                return old.packageName == new.packageName && old.hasPermission == new.hasPermission
            }
        })

        // 更新数据源并通知 RecyclerView
        appList.clear()
        appList.addAll(newListRaw)
        diff.dispatchUpdatesTo(this)
        // 保存上次模式与列表，用于 refreshModeChange()
        lastMode = mode
        lastWhitelist = whitelist
        lastBlacklist = blacklist
    }

    /**
     * 模式切换后，仅更新勾选状态，不重排位置。
     * 若要重排，请调用 setList(...)
     */
    fun refreshModeChange() {
        val newMode = getMode()
        val wl = getWhitelist()
        val bl = getBlacklist()

        appList.forEachIndexed { idx, app ->
            val oldChecked = when (lastMode) {
                0 -> lastWhitelist.contains(app.packageName)
                else -> lastBlacklist.contains(app.packageName)
            }
            val newChecked = when (newMode) {
                0 -> wl.contains(app.packageName)
                else -> bl.contains(app.packageName)
            }
            if (oldChecked != newChecked) {
                notifyItemChanged(idx)
            }
        }

        lastMode = newMode
        lastWhitelist = wl
        lastBlacklist = bl
    }
}