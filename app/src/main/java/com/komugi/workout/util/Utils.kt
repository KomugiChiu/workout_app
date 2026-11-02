package com.komugi.workout.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

fun formatEpochDay(epochDay: Long): String {
    val date = LocalDate.ofEpochDay(epochDay)
    val formatter = DateTimeFormatter.ofPattern("M/d（E）", Locale.TAIWAN)
    val base = date.format(formatter)
    return if (date == LocalDate.now()) "$base · 今天" else base
}

fun formatWeight(value: Double): String =
    if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        String.format(Locale.US, "%.1f", value)
    }

fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}
