package com.example.cellfreeapp.adapters

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.cellfreeapp.R

data class CalendarDayCell(
    val dateInMillis: Long,
    val dayOfMonth: Int,
    val belongsToDisplayedMonth: Boolean,
    val hasEvents: Boolean,
    val isSelected: Boolean,
    val isToday: Boolean
)

class CalendarDayAdapter(
    private val onDaySelected: (Long) -> Unit
) : RecyclerView.Adapter<CalendarDayAdapter.DayViewHolder>() {

    private var days = emptyList<CalendarDayCell>()

    class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dayNumber: TextView = itemView.findViewById(R.id.tv_calendar_day)
        val eventDot: View = itemView.findViewById(R.id.v_calendar_day_event_dot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_day, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val day = days[position]
        holder.dayNumber.text = day.dayOfMonth.toString()
        holder.dayNumber.setTextColor(
            when {
                day.isSelected -> Color.WHITE
                !day.belongsToDisplayedMonth -> Color.LTGRAY
                else -> Color.BLACK
            }
        )
        holder.dayNumber.background = when {
            day.isSelected -> dayBackground(Color.parseColor("#7193AE"), Color.TRANSPARENT)
            day.isToday -> dayBackground(Color.TRANSPARENT, Color.parseColor("#7193AE"))
            else -> null
        }
        holder.eventDot.visibility = if (day.hasEvents) View.VISIBLE else View.INVISIBLE
        if (day.isSelected && day.hasEvents) {
            holder.eventDot.background = dayBackground(Color.WHITE, Color.WHITE)
        } else {
            holder.eventDot.setBackgroundResource(R.drawable.calendar_event_dot)
        }

        val eventDescription = if (day.hasEvents) ", hay tareas o eventos" else ", sin tareas ni eventos"
        holder.itemView.contentDescription =
            "${day.dayOfMonth}$eventDescription"
        holder.itemView.isSelected = day.isSelected
        holder.itemView.setOnClickListener {
            onDaySelected(day.dateInMillis)
        }
    }

    override fun getItemCount(): Int = days.size

    fun submitDays(newDays: List<CalendarDayCell>) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = days.size

            override fun getNewListSize(): Int = newDays.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                days[oldItemPosition].dateInMillis == newDays[newItemPosition].dateInMillis

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                days[oldItemPosition] == newDays[newItemPosition]
        })
        days = newDays.toList()
        diff.dispatchUpdatesTo(this)
    }

    private fun dayBackground(fillColor: Int, strokeColor: Int): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(fillColor)
            if (strokeColor != Color.TRANSPARENT) {
                setStroke(2, strokeColor)
            }
        }
}
