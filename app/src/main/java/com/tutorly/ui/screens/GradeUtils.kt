package com.tutorly.ui.screens

private val gradeKeywordRegex = Regex("(?i)\\b(класс|кл\\.?)(\\b|$)")

internal fun titleCaseWords(value: String): String {
    if (value.isEmpty()) return value
    return value
        .split(Regex("\\s+"))
        .joinToString(" ") { word ->
            capitalizeWord(word)
        }
}

private fun capitalizeWord(word: String): String {
    if (word.isEmpty()) return word
    val firstLetterIndex = word.indexOfFirst { it.isLetter() }
    if (firstLetterIndex < 0) return word
    val prefix = word.substring(0, firstLetterIndex)
    val target = word.substring(firstLetterIndex)
    if (target.isEmpty()) return word
    val firstChar = target.first().uppercaseChar()
    val remainder = target.drop(1).lowercase()
    return buildString {
        append(prefix)
        append(firstChar)
        append(remainder)
    }
}

internal fun normalizeGrade(value: String?): String? {
    if (value == null) return null
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return null
    if (gradeKeywordRegex.containsMatchIn(trimmed)) {
        return titleCaseWords(trimmed)
    }
    val hasDigits = trimmed.any { it.isDigit() }
    val normalized = if (hasDigits) {
        "$trimmed класс"
    } else {
        trimmed
    }

    return titleCaseWords(normalized)
}
