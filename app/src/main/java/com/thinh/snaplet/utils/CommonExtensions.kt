package com.thinh.snaplet.utils

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

val Throwable.safeMessage: String
    get() = message?.takeIf { it.isNotBlank() } ?: "Lỗi không xác định"

fun formatTimeAgo(createdAt: String): String {
    return try {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        val createdTime = dateFormat.parse(createdAt)?.time ?: return ""
        
        val now = System.currentTimeMillis()
        val diff = now - createdTime
        
        when {
            diff < 60_000 -> "${diff / 1000}s" // seconds
            diff < 3_600_000 -> "${diff / 60_000}m" // minutes
            diff < 86_400_000 -> "${diff / 3_600_000}h" // hours
            diff < 604_800_000 -> "${diff / 86_400_000}d" // days
            diff < 2_592_000_000 -> "${diff / 604_800_000}w" // weeks
            diff < 31_536_000_000 -> "${diff / 2_592_000_000}mo" // months
            else -> "${diff / 31_536_000_000}y" // years
        }
    } catch (e: Exception) {
        ""
    }
}