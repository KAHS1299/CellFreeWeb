package com.example.cellfreeapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cellfreeapp.data.model.ReportTableItem

/**
 * Adaptador simplificado para dispositivos problemáticos como Motorola
 */
class ReportsTableAdapterFallback(
    private var items: List<ReportTableItem>
) : RecyclerView.Adapter<ReportsTableAdapterFallback.ViewHolder>() {

    companion object {
        private const val TAG = "ReportsAdapterFallback"
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val appName: TextView = view.findViewById(R.id.tvAppName)
        val daily: TextView = view.findViewById(R.id.tvDaily)
        val weekly: TextView = view.findViewById(R.id.tvWeekly)
        val monthly: TextView = view.findViewById(R.id.tvMonthly)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_report_table_fallback, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        
        holder.appName.text = item.nombreApp
        holder.daily.text = item.diario
        holder.weekly.text = item.semanal
        holder.monthly.text = item.mensual
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<ReportTableItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}