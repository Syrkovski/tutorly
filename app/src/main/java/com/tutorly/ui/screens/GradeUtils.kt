package com.tutorly.ui.screens

private val gradeKeywordRegex = Regex("(?i)\\b(класс|кл\\.?)(\\b|$)")

internal fun normalizeGrade(value: String?): String? {
    if (value == null) return null
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return null
    if (gradeKeywordRegex.containsMatchIn(trimmed)) {
        return trimmed
    }
    val hasDigits = trimmed.any { it.isDigit() }
    return if (hasDigits) {
        "$trimmed класс"
    } else {
        trimmed
    }
}
