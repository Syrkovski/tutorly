package com.tutorly.ui.screens

private val gradeKeywordRegex = Regex("(?i)\\b(класс|кл\\.?)(\\b|$)")
private val capitalClassRegex = Regex("(?i)\\bКласс\\b")
private val capitalAbbreviationRegex = Regex("(?i)\\bКл\\.\b")

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
        return normalizeGradeKeywords(titleCaseWords(trimmed))
    }
    val hasDigits = trimmed.any { it.isDigit() }
    val normalized = if (hasDigits) {
        "$trimmed класс"
    } else {
        trimmed
    }

    return normalizeGradeKeywords(titleCaseWords(normalized))
}

internal fun normalizeSubject(value: String?): String? {
    val trimmed = value?.trim() ?: return null
    if (trimmed.isEmpty()) return null
    return normalizeSubjectLabel(trimmed)
}

internal fun normalizeSubjectLabel(value: String): String {
    if (value.isEmpty()) return value
    val tokens = value.split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (tokens.isEmpty()) return ""
    return tokens.mapIndexed { index, token ->
        formatSubjectToken(token, index == 0)
    }.joinToString(" ")
}

private fun normalizeGradeKeywords(value: String): String {
    return value
        .replace(capitalClassRegex) { "класс" }
        .replace(capitalAbbreviationRegex) { "кл." }
}

private fun formatSubjectToken(token: String, isFirst: Boolean): String {
    if (token.isEmpty()) return token
    if (isFirst && token.all { it.isLetter() && it.isUpperCase() }) {
        return token
    }
    val firstLetterIndex = token.indexOfFirst { it.isLetter() }
    if (firstLetterIndex < 0) return token
    val prefix = token.substring(0, firstLetterIndex)
    val target = token.substring(firstLetterIndex)
    val formatted = if (isFirst) {
        val firstChar = target.first().uppercaseChar()
        val remainder = target.drop(1).lowercase()
        buildString {
            append(firstChar)
            append(remainder)
        }
    } else {
        target.lowercase()
    }
    return buildString {
        append(prefix)
        append(formatted)
    }
}
