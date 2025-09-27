package com.cehpoint.netwin.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Helper functions
 fun formatDateTime(timestamp: Long?): String {
    if (timestamp == null) return "N/A"
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}