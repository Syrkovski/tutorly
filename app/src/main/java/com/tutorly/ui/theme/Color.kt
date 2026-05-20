package com.tutorly.ui.theme

import androidx.compose.ui.graphics.Color

internal data class MetricPalette(
    val border: Color,
    val accent: Color = border
)

internal data class ThemePalette(
    val topBarStart: Color,
    val topBarEnd: Color,
    val accent: Color,
    val chipFill: Color,
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val lessonsHighlight: MetricPalette,
    val rateHighlight: MetricPalette,
    val earnedHighlight: MetricPalette,
    val prepaymentHighlight: MetricPalette
)

internal val OriginalPalette = ThemePalette(
    topBarStart = Color(0xFF3B7D72),
    topBarEnd = Color(0xFF4E998C),
    accent = Color(0xFF4E998C),
    chipFill = Color(0xFFE7EFEA),
    backgroundTop = Color.White,
    backgroundBottom = Color.White,
    lessonsHighlight = MetricPalette(
        border = Color(0xFF4C6388)
    ),
    rateHighlight = MetricPalette(
        border = Color(0xFFCEA969)
    ),
    earnedHighlight = MetricPalette(
        border = Color(0xFF6E68A1)
    ),
    prepaymentHighlight = MetricPalette(
        border = Color(0xFFCE9169)
    )
)

internal val PlumPalette = ThemePalette(
    topBarStart = Color(0xFF5C2D64),
    topBarEnd = Color(0xFF6F497E),
    accent = Color(0xFF6F497E),
    chipFill = Color(0xFFE6D3EE),
    backgroundTop = Color(0xFFFAF3FE),
    backgroundBottom = Color(0xFFF0E3F8),
    lessonsHighlight = MetricPalette(
        border = Color(0xFF4C6388)
    ),
    rateHighlight = MetricPalette(
        border = Color(0xFFCEA969)
    ),
    earnedHighlight = MetricPalette(
        border = Color(0xFF6E68A1)
    ),
    prepaymentHighlight = MetricPalette(
        border = Color(0xFFCE9169)
    )
)

internal val RoyalPalette = ThemePalette(
    topBarStart = Color(0xFF2E4FA6),
    topBarEnd = Color(0xFF416BC0),
    accent = Color(0xFF416BC0),
    chipFill = Color(0xFFE3E9F7),
    backgroundTop = Color(0xFFF5F8FE),
    backgroundBottom = Color(0xFFEBF2FC),
    lessonsHighlight = MetricPalette(
        border = Color(0xFF4C6388)
    ),
    rateHighlight = MetricPalette(
        border = Color(0xFFCEA969)
    ),
    earnedHighlight = MetricPalette(
        border = Color(0xFF6E68A1)
    ),
    prepaymentHighlight = MetricPalette(
        border = Color(0xFFCE9169)
    )
)

internal val PremiumPalette = ThemePalette(
    topBarStart = Color(0xFFFFFFFF),
    topBarEnd = Color(0xFFFFFFFF),
    accent = Color(0xFF2F2BFF),
    chipFill = Color(0xFFF1F2FF),
    backgroundTop = Color.White,
    backgroundBottom = Color(0xFFF5F7FF),
    lessonsHighlight = MetricPalette(border = Color(0xFF6C5CFF)),
    rateHighlight = MetricPalette(border = Color(0xFFFF9F43)),
    earnedHighlight = MetricPalette(border = Color(0xFF36C76B)),
    prepaymentHighlight = MetricPalette(border = Color(0xFFFF4D5E))
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
