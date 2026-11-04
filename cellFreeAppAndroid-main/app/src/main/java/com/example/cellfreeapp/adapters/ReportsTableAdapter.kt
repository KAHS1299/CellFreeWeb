package com.example.cellfreeapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.cellfreeapp.R
import com.example.cellfreeapp.data.model.ReportTableItem

class ReportsTableAdapter : RecyclerView.Adapter<ReportsTableAdapter.ReportViewHolder>() {

    private var reportItems = mutableListOf<ReportTableItem>()

    fun updateData(newItems: List<ReportTableItem>) {
        reportItems.clear()
        reportItems.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_report_table, parent, false)
        return ReportViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        holder.bind(reportItems[position], position)
    }

    override fun getItemCount() = reportItems.size

    class ReportViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvAppName: TextView = itemView.findViewById(R.id.tvAppName)
        private val tvDaily: TextView = itemView.findViewById(R.id.tvDaily)
        private val tvWeekly: TextView = itemView.findViewById(R.id.tvWeekly)
        private val tvMonthly: TextView = itemView.findViewById(R.id.tvMonthly)
        private val vCategoryIndicator: View = itemView.findViewById(R.id.vCategoryIndicator)

        fun bind(item: ReportTableItem, position: Int) {
            tvAppName.text = "📱 ${item.nombreApp}"
            tvDaily.text = item.diario
            tvWeekly.text = item.semanal
            tvMonthly.text = item.mensual

            // Aplicar color del indicador basado en categoría de la app
            val categoryColor = when (item.categoria.lowercase()) {
                "social" -> ContextCompat.getColor(itemView.context, R.color.category_social)
                "juegos", "games" -> ContextCompat.getColor(itemView.context, R.color.category_games)
                "entretenimiento", "entertainment" -> ContextCompat.getColor(itemView.context, R.color.category_entertainment)
                "productividad", "productivity" -> ContextCompat.getColor(itemView.context, R.color.category_productivity)
                else -> ContextCompat.getColor(itemView.context, R.color.cellfree_primary)
            }
            
            vCategoryIndicator.setBackgroundColor(categoryColor)
            tvAppName.setTextColor(categoryColor)
        }
    }
}