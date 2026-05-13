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
    topBarStart = Color(0xFF5D50E6),
    topBarEnd = Color(0xFF7E74F9),
    accent = Color(0xFF5D50E6),
    chipFill = Color(0xFFF1F0FD),
    backgroundTop = Color(0xFFF8F7FE),
    backgroundBottom = Color(0xFFF3F2FC),
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
    topBarStart = Color(0xFF5B45D8),
    topBarEnd = Color(0xFF7A69EB),
    accent = Color(0xFF5B45D8),
    chipFill = Color(0xFFF0EDFC),
    backgroundTop = Color(0xFFF8F6FE),
    backgroundBottom = Color(0xFFF3F0FC),
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
    topBarStart = Color(0xFF4A59D8),
    topBarEnd = Color(0xFF6B7AF0),
    accent = Color(0xFF4A59D8),
    chipFill = Color(0xFFEEF1FE),
    backgroundTop = Color(0xFFF7F8FF),
    backgroundBottom = Color(0xFFF0F2FD),
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

val CardSurface = Color(0xFFFFFFFF)
val PrimaryTextColor = Color(0xFF1F2340)
val SecondaryTextColor = Color(0xFF6B6F86)
val OutlineLavender = Color(0xFFD8DBEC)
val OutlineVariantLavender = Color(0xFFEDEEFA)

val PaidChipContent = Color(0xFFFFFFFF)
val DebtChipContent = Color(0xFFFFFFFF)
val DebtChipFill = Color(0xFFD05E6E)

val ErrorRed = Color(0xFFBA1A1A)
val OnErrorRed = Color(0xFFFFFFFF)
val ErrorRedContainer = Color(0xFFFFDAD6)
val OnErrorRedContainer = Color(0xFF410002)
