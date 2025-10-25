package com.tutorly.ui.theme

import androidx.compose.ui.graphics.Color

internal data class ThemePalette(
    val topBarStart: Color,
    val topBarEnd: Color,
    val accent: Color,
    val chipFill: Color,
    val backgroundTop: Color,
    val backgroundBottom: Color
)

internal val OriginalPalette = ThemePalette(
    topBarStart = Color(0xFF4E998C),
    topBarEnd = Color(0xFF3B7D72),
    accent = Color(0xFF4E998C),
    chipFill = Color(0xFFE7EFEA),
    backgroundTop = Color(0xFFF3F8F6),
    backgroundBottom = Color(0xFFF1F6F6)
)

internal val PlumPalette = ThemePalette(
    topBarStart = Color(0xFF6F497E),
    topBarEnd = Color(0xFF5C2D64),
    accent = Color(0xFF6F497E),
    chipFill = Color(0xFFE6D3EE),
    backgroundTop = Color(0xFFF4ECFA),
    backgroundBottom = Color(0xFFE9D8F4)
)

internal val RoyalPalette = ThemePalette(
    topBarStart = Color(0xFF416BC0),
    topBarEnd = Color(0xFF2E4FA6),
    accent = Color(0xFF416BC0),
    chipFill = Color(0xFFE3E9F7),
    backgroundTop = Color(0xFFF2F6FD),
    backgroundBottom = Color(0xFFE6EEFB)
)

val CardSurface = Color(0xFFFFFFFF)
val PrimaryTextColor = Color(0xFF2C2C33)
val SecondaryTextColor = Color(0xFF49484E)
val OutlineLavender = Color(0xFFB9C9C4)
val OutlineVariantLavender = Color(0xFFEEF9F6)

val PaidChipContent = Color(0xFFFFFFFF)
val DebtChipContent = Color(0xFFFFFFFF)
val DebtChipFill = Color(0xFFD05E6E)

val ErrorRed = Color(0xFFBA1A1A)
val OnErrorRed = Color(0xFFFFFFFF)
val ErrorRedContainer = Color(0xFFFFDAD6)
val OnErrorRedContainer = Color(0xFF410002)
