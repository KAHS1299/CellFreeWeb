package com.example.cellfreeapp.utils

import com.example.cellfreeapp.models.CalendarEvent
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

object CalendarIcsExporter {

    fun export(events: List<CalendarEvent>, generatedAt: Date = Date()): String {
        val utcFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val lines = mutableListOf(
            "BEGIN:VCALENDAR",
            "VERSION:2.0",
            "PRODID:-//CellFree//Calendar//ES",
            "CALSCALE:GREGORIAN",
            "METHOD:PUBLISH"
        )

        events.forEach { event ->
            lines += listOf(
                "BEGIN:VEVENT",
                "UID:${event.id?.let { "$it@cellfreeapp" } ?: "${UUID.randomUUID()}@cellfreeapp"}",
                "DTSTAMP:${utcFormat.format(generatedAt)}",
                "DTSTART:${utcFormat.format(event.date)}",
                "SUMMARY:${escapeText(event.title)}",
                "DESCRIPTION:${escapeText(event.description)}",
                "CATEGORIES:${escapeText(event.type)}",
                "X-CELLFREE-COMPLETED:${event.completed}",
                "END:VEVENT"
            )
        }
        lines += "END:VCALENDAR"

        return lines.joinToString("\r\n", transform = ::foldLine) + "\r\n"
    }

    private fun escapeText(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\r\n", "\n")
        .replace("\r", "\n")
        .replace("\n", "\\n")
        .replace(",", "\\,")
        .replace(";", "\\;")

    private fun foldLine(line: String): String {
        val folded = StringBuilder()
        var index = 0
        var bytesOnLine = 0

        while (index < line.length) {
            val charCount = if (
                Character.isHighSurrogate(line[index]) &&
                index + 1 < line.length &&
                Character.isLowSurrogate(line[index + 1])
            ) {
                2
            } else {
                1
            }
            val character = line.substring(index, index + charCount)
            val characterBytes = character.toByteArray(StandardCharsets.UTF_8).size

            if (bytesOnLine + characterBytes > 75) {
                folded.append("\r\n ")
                bytesOnLine = 1
            }
            folded.append(character)
            bytesOnLine += characterBytes
            index += charCount
        }

        return folded.toString()
    }
}
