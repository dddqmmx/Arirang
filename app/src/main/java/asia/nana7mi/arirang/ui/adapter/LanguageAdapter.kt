package asia.nana7mi.arirang.ui.adapter

import asia.nana7mi.arirang.R
import asia.nana7mi.arirang.model.LanguageItem
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LanguageAdapter(
    private val languages: List<LanguageItem>,
    private val onItemClick: (LanguageItem) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_language, parent, false)
        return LanguageViewHolder(view)
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        val item = languages[position]
        holder.languageName.text = item.name
        holder.itemView.setOnClickListener { onItemClick(item) }
        holder.radioButton.isChecked = item.isSelected
    }

    override fun getItemCount(): Int = languages.size

    inner class LanguageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val languageName: TextView = view.findViewById(R.id.languageName)
        val radioButton: RadioButton = view.findViewById(R.id.radioButton)
    }
}