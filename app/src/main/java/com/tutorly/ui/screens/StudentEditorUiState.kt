package com.tutorly.ui.screens

data class StudentEditorFormState(
    val studentId: Long? = null,
    val name: String = "",
    val phone: String = "",
    val messenger: String = "",
    val rate: String = "",
    val subject: String = "",
    val grade: String = "",
    val note: String = "",
    val isArchived: Boolean = false,
    val isActive: Boolean = true,
    val nameError: Boolean = false,
    val isSaving: Boolean = false,
)

internal fun formatRateInput(cents: Int?): String {
    if (cents == null) return ""
    val decimal = java.math.BigDecimal(cents)
        .divide(java.math.BigDecimal(100))
        .stripTrailingZeros()
    val separator = java.text.DecimalFormatSymbols.getInstance().decimalSeparator
    return decimal.toPlainString().replace('.', separator)
}

internal fun parseRateInput(input: String): Int? {
    val raw = input.trim()
    if (raw.isEmpty()) return null
    val normalized = raw.replace(',', '.')
    val decimal = runCatching { java.math.BigDecimal(normalized) }.getOrNull() ?: return null
    if (decimal < java.math.BigDecimal.ZERO) return null
    val cents = decimal.multiply(java.math.BigDecimal(100)).setScale(0, java.math.RoundingMode.HALF_UP)
    val bigInt = runCatching { cents.toBigIntegerExact() }.getOrNull() ?: return null
    val max = java.math.BigInteger.valueOf(Int.MAX_VALUE.toLong())
    if (bigInt > max) return null
    return bigInt.toInt()
}
