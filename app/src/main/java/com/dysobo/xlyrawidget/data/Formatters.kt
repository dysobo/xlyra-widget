package com.dysobo.xlyrawidget.data

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
private val dateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(ZoneId.systemDefault())
private val shortDateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(ZoneId.systemDefault())

fun formatEpochSeconds(seconds: Long?): String {
    if (seconds == null || seconds <= 0L) return "--"
    return timeFormatter.format(Instant.ofEpochSecond(seconds))
}

fun formatMillis(millis: Long?): String {
    if (millis == null || millis <= 0L) return "--"
    return dateTimeFormatter.format(Instant.ofEpochMilli(millis))
}

fun formatEpochShort(seconds: Long?): String {
    if (seconds == null || seconds <= 0L) return "--"
    return shortDateTimeFormatter.format(Instant.ofEpochSecond(seconds))
}

fun formatCost(value: Double?): String {
    if (value == null) return "--"
    return if (value >= 100) {
        String.format(Locale.US, "$%.0f", value)
    } else {
        String.format(Locale.US, "$%.2f", value)
    }
}

fun formatCount(value: Long?): String {
    if (value == null) return "--"
    return when {
        value >= 1_000_000 -> String.format(Locale.US, "%.1fM", value / 1_000_000.0)
        value >= 10_000 -> String.format(Locale.US, "%.1f万", value / 10_000.0)
        value >= 1_000 -> String.format(Locale.US, "%.1fk", value / 1_000.0)
        else -> value.toString()
    }
}

fun formatLimit(used: Long?, limit: Long?): String {
    val usedText = formatCount(used)
    return if (limit == null || limit <= 0L) usedText else "$usedText/${formatCount(limit)}"
}
