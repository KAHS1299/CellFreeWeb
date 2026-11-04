package com.example.cellfreeapp.adapters

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.cellfreeapp.R
import com.example.cellfreeapp.models.CalendarEvent

class CalendarEventAdapter(
    private var events: List<CalendarEvent>,
    private val onEventClick: (CalendarEvent) -> Unit
) : RecyclerView.Adapter<CalendarEventAdapter.EventViewHolder>() {

    class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val eventColor: View = itemView.findViewById(R.id.v_event_color)
        val eventTitle: TextView = itemView.findViewById(R.id.tv_event_title)
        val eventTime: TextView = itemView.findViewById(R.id.tv_event_time)
        val eventDescription: TextView = itemView.findViewById(R.id.tv_event_description)
        val btnEventOptions: ImageButton = itemView.findViewById(R.id.btn_event_options)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]

        // Configurar los datos del evento
        holder.eventTitle.text = event.title
        holder.eventTime.text = "${event.startTime}"
        
        // Mostrar descripción o tipo del evento
        if (event.description.isNotBlank()) {
            holder.eventDescription.text = event.description
            holder.eventDescription.visibility = View.VISIBLE
        } else {
            val typeText = when (event.type) {
                "tarea" -> "Tarea"
                "examen" -> "Examen"
                "estudio" -> "Estudio"
                else -> "Evento"
            }
            holder.eventDescription.text = typeText
            holder.eventDescription.visibility = View.VISIBLE
        }

        val typeColor = ContextCompat.getColor(
            holder.itemView.context,
            CalendarEvent.getTypeColor(event.type)
        )
        val density = holder.itemView.resources.displayMetrics.density
        holder.eventDescription.setTextColor(Color.WHITE)
        holder.eventDescription.setPadding(
            (10 * density).toInt(),
            (4 * density).toInt(),
            (10 * density).toInt(),
            (4 * density).toInt()
        )
        holder.eventDescription.background = GradientDrawable().apply {
            setColor(typeColor)
            cornerRadius = 12 * density
        }

        holder.eventColor.setBackgroundColor(
            ContextCompat.getColor(holder.itemView.context, event.getStatusColor())
        )

        // Mostrar estado de completado
        if (event.completed) {
            holder.eventTitle.alpha = 0.7f
            holder.eventTime.alpha = 0.7f
            holder.eventDescription.alpha = 0.7f
            holder.eventTime.text = "${event.startTime} ✓"
        } else {
            holder.eventTitle.alpha = 1.0f
            holder.eventTime.alpha = 1.0f
            holder.eventDescription.alpha = 1.0f
        }

        // Configurar click listeners
        holder.btnEventOptions.setOnClickListener {
            onEventClick(event)
        }

        holder.itemView.setOnClickListener {
            onEventClick(event)
        }
    }

    override fun getItemCount(): Int = events.size

    fun updateEvents(newEvents: List<CalendarEvent>) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = events.size

            override fun getNewListSize(): Int = newEvents.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val oldEvent = events[oldItemPosition]
                val newEvent = newEvents[newItemPosition]
                return oldEvent.id != null && oldEvent.id == newEvent.id
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                events[oldItemPosition] == newEvents[newItemPosition]
        })
        events = newEvents.toList()
        diff.dispatchUpdatesTo(this)
    }
}