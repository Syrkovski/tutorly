package com.tutorly.ui.screens

internal fun formatMoneyInput(cents: Int?): String = formatMoneyInput(cents?.toLong())

internal fun formatMoneyInput(cents: Long?): String {
    if (cents == null) return ""
    val decimal = java.math.BigDecimal(cents)
        .divide(java.math.BigDecimal(100))
        .stripTrailingZeros()
    val separator = java.text.DecimalFormatSymbols.getInstance().decimalSeparator
    return decimal.toPlainString().replace('.', separator)
}

internal fun parseMoneyInput(input: String): Int? {
    val raw = input.trim()
    if (raw.isEmpty()) return null
    val normalized = raw.replace(',', '.')
    val decimal = runCatching { java.math.BigDecimal(normalized) }.getOrNull() ?: return null
    val cents = decimal.multiply(java.math.BigDecimal(100)).setScale(0, java.math.RoundingMode.HALF_UP)
    val bigInt = runCatching { cents.toBigIntegerExact() }.getOrNull() ?: return null
    val max = java.math.BigInteger.valueOf(Int.MAX_VALUE.toLong())
    val min = java.math.BigInteger.valueOf(Int.MIN_VALUE.toLong())
    if (bigInt > max || bigInt < min) return null
    return bigInt.toInt()
}
