package com.tutorly.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.tutorly.models.AppThemePreset

data class MetricTileColors(
    val container: Color,
    val border: Color,
    val content: Color
)

private data class PaletteResult(
    val scheme: androidx.compose.material3.ColorScheme,
    val extended: ExtendedColors
)

data class ExtendedColors(
    val topBarStart: Color,
    val topBarEnd: Color,
    val accent: Color,
    val chipSelected: Color,
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val lessonsMetric: MetricTileColors,
    val rateMetric: MetricTileColors,
    val earnedMetric: MetricTileColors,
    val prepaymentMetric: MetricTileColors
)

private val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        topBarStart = OriginalPalette.topBarStart,
        topBarEnd = OriginalPalette.topBarEnd,
        accent = OriginalPalette.accent,
        chipSelected = OriginalPalette.chipFill,
        backgroundTop = OriginalPalette.backgroundTop,
        backgroundBottom = OriginalPalette.backgroundBottom,
        lessonsMetric = OriginalPalette.lessonsHighlight.toMetricTileColors(),
        rateMetric = OriginalPalette.rateHighlight.toMetricTileColors(),
        earnedMetric = OriginalPalette.earnedHighlight.toMetricTileColors(),
        prepaymentMetric = OriginalPalette.prepaymentHighlight.toMetricTileColors()
    )
}

val MaterialTheme.extendedColors: ExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalExtendedColors.current

@Composable
fun AppTheme(
    preset: AppThemePreset = AppThemePreset.ORIGINAL,
    content: @Composable () -> Unit
) {
    val palette = remember(preset) { paletteForPreset(preset) }
    CompositionLocalProvider(LocalExtendedColors provides palette.extended) {
        MaterialTheme(
            colorScheme = palette.scheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}

private fun paletteForPreset(preset: AppThemePreset): PaletteResult = when (preset) {
    AppThemePreset.ORIGINAL -> createPalette(OriginalPalette)
    AppThemePreset.PLUM -> createPalette(PlumPalette)
    AppThemePreset.ROYAL -> createPalette(RoyalPalette)
}

private fun createPalette(themePalette: ThemePalette): PaletteResult {
    val scheme = lightColorScheme(
        primary = themePalette.topBarStart,
        onPrimary = Color.White,
        primaryContainer = themePalette.topBarEnd,
        onPrimaryContainer = Color.White,
        secondary = themePalette.accent,
        onSecondary = Color.White,
        secondaryContainer = themePalette.chipFill,
        onSecondaryContainer = PrimaryTextColor,
        tertiary = themePalette.accent,
        onTertiary = Color.White,
        tertiaryContainer = themePalette.chipFill,
        onTertiaryContainer = PrimaryTextColor,
        background = themePalette.backgroundTop,
        onBackground = PrimaryTextColor,
        surface = CardSurface,
        onSurface = PrimaryTextColor,
        surfaceVariant = themePalette.chipFill,
        onSurfaceVariant = SecondaryTextColor,
        surfaceTint = themePalette.accent,
        outline = OutlineLavender,
        outlineVariant = OutlineVariantLavender,
        surfaceContainerLowest = CardSurface,
        surfaceContainerLow = themePalette.backgroundBottom,
        surfaceContainer = themePalette.backgroundTop,
        surfaceContainerHigh = themePalette.backgroundBottom,
        surfaceContainerHighest = themePalette.backgroundTop,
        error = ErrorRed,
        onError = OnErrorRed,
        errorContainer = ErrorRedContainer,
        onErrorContainer = OnErrorRedContainer,
        inverseSurface = Color(0xFF1F2326),
        inverseOnSurface = Color.White,
        inversePrimary = themePalette.accent
    )
    val extended = ExtendedColors(
        topBarStart = themePalette.topBarStart,
        topBarEnd = themePalette.topBarEnd,
        accent = themePalette.accent,
        chipSelected = themePalette.chipFill,
        backgroundTop = themePalette.backgroundTop,
        backgroundBottom = themePalette.backgroundBottom,
        lessonsMetric = themePalette.lessonsHighlight.toMetricTileColors(),
        rateMetric = themePalette.rateHighlight.toMetricTileColors(),
        earnedMetric = themePalette.earnedHighlight.toMetricTileColors(),
        prepaymentMetric = themePalette.prepaymentHighlight.toMetricTileColors()
    )
    return PaletteResult(scheme, extended)
}

private fun MetricPalette.toMetricTileColors(): MetricTileColors =
    MetricTileColors(container = container, border = border, content = content)
