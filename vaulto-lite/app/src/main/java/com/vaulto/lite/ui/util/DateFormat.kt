package com.vaulto.lite.ui.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Returns the start-of-day epoch millis for [millis] (midnight, device timezone). */
fun startOfDay(millis: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = millis
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

/**
 * Human-friendly date header label: "Today", "Yesterday", or "EEE, MMM d"
 * (e.g. "Mon, Jun 15") for older dates.
 */
fun dateHeaderLabel(millis: Long): String {
    val today = startOfDay(System.currentTimeMillis())
    val yesterday = today - 24L * 60 * 60 * 1000
    val day = startOfDay(millis)

    return when (day) {
        today -> "Today"
        yesterday -> "Yesterday"
        else -> SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(millis))
    }
}
