package com.example.cellfreeapp

import com.example.cellfreeapp.R
import com.example.cellfreeapp.data.model.CalendarSyncResponse
import com.example.cellfreeapp.data.model.RemindersResponse
import com.example.cellfreeapp.models.CalendarEvent
import com.example.cellfreeapp.utils.CalendarIcsExporter
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class CalendarReminderTest {

    @Test
    fun parsesReminderResponseFromApi() {
        val json = """
            {
              "status": "success",
              "data": {
                "recordatorios": [
                  {
                    "id": 42,
                    "titulo": "Preparar examen",
                    "descripcion": "Repasar apuntes",
                    "fecha_hora": "2026-10-27 14:30:00",
                    "tipo": "examen",
                    "completado": false
                  }
                ]
              }
            }
        """.trimIndent()

        val response = Gson().fromJson(json, RemindersResponse::class.java)
        val reminder = response.data.recordatorios?.single()

        assertEquals("success", response.status)
        assertEquals(42, reminder?.id)
        assertEquals("Preparar examen", reminder?.titulo)
        assertEquals("2026-10-27 14:30:00", reminder?.fechaHora)
    }

    @Test
    fun parsesCalendarSyncResponseFromApi() {
        val json = """
            {
              "status": "success",
              "data": {
                "sincronizaciones": [
                  {
                    "id": 8,
                    "nombre": "Calendario académico",
                    "url_calendario": "https://example.com/calendar.ics",
                    "estado": "activo",
                    "intervalo_sincronizacion": 60,
                    "ultima_sincronizacion": null
                  }
                ]
              }
            }
        """.trimIndent()

        val response = Gson().fromJson(json, CalendarSyncResponse::class.java)
        val sync = response.data?.sincronizaciones?.single()

        assertEquals("success", response.status)
        assertEquals(8, sync?.id)
        assertEquals("activo", sync?.estado)
        assertEquals(60, sync?.intervaloSincronizacion)
    }

    @Test
    fun parsesApiDateAndIsoDateWithTimezone() {
        val apiDate = CalendarEvent(
            title = "Tarea",
            description = "",
            dateTime = "2026-10-27 14:30:00",
            type = "tarea"
        )
        val isoDate = apiDate.copy(dateTime = "2026-10-27T14:30:00Z")
        val expectedUtc = SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ssX",
            Locale.US
        ).parse("2026-10-27T14:30:00Z")

        assertNotNull(apiDate.date)
        assertEquals(expectedUtc, isoDate.date)
    }

    @Test
    fun eventIndicatorColorReflectsCompletionAndDueDate() {
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            .parse("2026-10-27 12:00:00")!!
        val pendingEvent = CalendarEvent(
            title = "Tarea pendiente",
            description = "",
            dateTime = "2026-10-27 13:00:00",
            type = "tarea"
        )
        val overdueEvent = pendingEvent.copy(dateTime = "2026-10-27 11:00:00")
        val completedEvent = overdueEvent.copy(completed = true)

        assertEquals(R.color.calendar_event_pending, pendingEvent.getStatusColor(now))
        assertEquals(R.color.calendar_event_overdue, overdueEvent.getStatusColor(now))
        assertEquals(R.color.calendar_event_completed, completedEvent.getStatusColor(now))
    }

    @Test
    fun eventTypeColorsUseColorResources() {
        assertEquals(R.color.calendar_event_type_task, CalendarEvent.getTypeColor("tarea"))
        assertEquals(R.color.calendar_event_type_exam, CalendarEvent.getTypeColor("examen"))
        assertEquals(R.color.calendar_event_type_study, CalendarEvent.getTypeColor("estudio"))
        assertEquals(R.color.calendar_event_type_other, CalendarEvent.getTypeColor("otro"))
    }

    @Test
    fun exportsCalendarEventsAsEscapedIcs() {
        val event = CalendarEvent(
            id = 12,
            title = "Repaso, tema; uno",
            description = "Primera línea\nSegunda línea",
            dateTime = "2026-10-27 14:30:00",
            type = "tarea",
            completed = true
        )

        val ics = CalendarIcsExporter.export(listOf(event))

        assertTrue(ics.startsWith("BEGIN:VCALENDAR\r\nVERSION:2.0\r\n"))
        assertTrue(ics.contains("UID:12@cellfreeapp\r\n"))
        assertTrue(ics.contains("SUMMARY:Repaso\\, tema\\; uno\r\n"))
        assertTrue(ics.contains("DESCRIPTION:Primera línea\\nSegunda línea\r\n"))
        assertTrue(ics.contains("X-CELLFREE-COMPLETED:true\r\n"))
        assertTrue(ics.endsWith("END:VCALENDAR\r\n"))
    }

    @Test
    fun foldsIcsLinesWithinSeventyFiveUtf8Bytes() {
        val event = CalendarEvent(
            id = 13,
            title = "Tarea " + "😀".repeat(30),
            description = "",
            dateTime = "2026-10-27 14:30:00",
            type = "tarea"
        )

        val ics = CalendarIcsExporter.export(listOf(event))

        ics.split("\r\n").forEach { line ->
            assertTrue(line.toByteArray(Charsets.UTF_8).size <= 75)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidReminderDateInsteadOfUsingCurrentTime() {
        CalendarEvent(
            title = "Tarea",
            description = "",
            dateTime = "fecha-invalida",
            type = "tarea"
        ).date
    }
}
