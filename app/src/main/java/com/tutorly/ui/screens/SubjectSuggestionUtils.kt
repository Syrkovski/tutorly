package com.tutorly.ui.screens

import java.util.Locale

internal fun formatSubjectName(value: String, locale: Locale): String {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return ""
    val first = trimmed.first().uppercase(locale)
    return first + trimmed.drop(1)
}

internal fun parseSubjectNames(value: String?, locale: Locale): List<String> {
    if (value.isNullOrBlank()) return emptyList()
    return value.split(',')
        .map { formatSubjectName(it, locale) }
        .filter { it.isNotEmpty() }
}
