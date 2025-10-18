package com.tutorly.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.tutorly.models.AppThemePreset

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimaryPurple,
    onPrimary = DarkOnPrimaryPurple,
    primaryContainer = DarkPrimaryPurpleContainer,
    onPrimaryContainer = DarkOnPrimaryPurpleContainer,
    secondary = DarkSecondaryCoral,
    onSecondary = DarkOnSecondaryCoral,
    secondaryContainer = DarkSecondaryCoralContainer,
    onSecondaryContainer = DarkOnSecondaryCoralContainer,
    tertiary = DarkTertiaryPeach,
    onTertiary = DarkOnTertiaryPeach,
    tertiaryContainer = DarkTertiaryPeachContainer,
    onTertiaryContainer = DarkOnTertiaryPeachContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceTint = DarkPrimaryPurple,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    surfaceContainerLowest = DarkSurfaceContainerLowest,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
    error = ErrorRed,
    onError = OnErrorRed,
    errorContainer = ErrorRedContainer,
    onErrorContainer = OnErrorRedContainer,
    inverseSurface = Color(0xFFECE6FF),
    inverseOnSurface = Color(0xFF211835),
    inversePrimary = PrimaryPurple
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryPurple,
    onPrimary = OnPrimaryPurple,
    primaryContainer = PrimaryPurpleContainer,
    onPrimaryContainer = OnPrimaryPurpleContainer,
    secondary = SecondaryCoral,
    onSecondary = OnSecondaryCoral,
    secondaryContainer = SecondaryCoralContainer,
    onSecondaryContainer = OnSecondaryCoralContainer,
    tertiary = TertiaryPeach,
    onTertiary = OnTertiaryPeach,
    tertiaryContainer = TertiaryPeachContainer,
    onTertiaryContainer = OnTertiaryPeachContainer,
    background = SurfaceBlush,
    onBackground = OnSurfaceBlush,
    surface = SurfaceContainerLowest,
    onSurface = OnSurfaceBlush,
    surfaceTint = PrimaryPurple,
    surfaceVariant = SurfaceVariantSoft,
    onSurfaceVariant = OnSurfaceVariantSoft,
    outline = OutlineLavender,
    outlineVariant = OutlineVariantLavender,
    surfaceContainerLowest = SurfaceContainerLowest,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
    error = ErrorRed,
    onError = OnErrorRed,
    errorContainer = ErrorRedContainer,
    onErrorContainer = OnErrorRedContainer,
    inverseSurface = Color(0xFF2F2546),
    inverseOnSurface = Color(0xFFF5F1FF),
    inversePrimary = PrimaryPurple
)

@Composable
fun TutorlyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

private val LightColors = lightColorScheme(
    primary = PrimaryPurple,
    onPrimary = OnPrimaryPurple,
    surface = SurfaceContainerLowest,
    onSurface = OnSurfaceBlush,
    background = SurfaceBlush,
    onBackground = OnSurfaceBlush,
    secondary = SecondaryCoral
)

@Composable
fun AppTheme(
    preset: AppThemePreset = AppThemePreset.OCEAN,
    content: @Composable () -> Unit
) {
    val colorScheme = remember(preset) { colorSchemeForPreset(preset) }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

private fun colorSchemeForPreset(preset: AppThemePreset) = when (preset) {
    AppThemePreset.OCEAN -> LightColors.copy(
        primary = Color(0xFF1E88E5),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD6E4FF),
        onPrimaryContainer = Color(0xFF001B3D),
        secondary = Color(0xFF1565C0),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFD7E3FF),
        onSecondaryContainer = Color(0xFF001C3F),
        background = Color(0xFFF5F9FF),
        surface = Color(0xFFF5F9FF),
        surfaceTint = Color(0xFF1E88E5)
    )

    AppThemePreset.FOREST -> LightColors.copy(
        primary = Color(0xFF2E7D32),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFCBEACD),
        onPrimaryContainer = Color(0xFF063815),
        secondary = Color(0xFF558B2F),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFDDEFD2),
        onSecondaryContainer = Color(0xFF10290A),
        background = Color(0xFFF4FFF4),
        surface = Color(0xFFF4FFF4),
        surfaceTint = Color(0xFF2E7D32)
    )

    AppThemePreset.SUNSET -> LightColors.copy(
        primary = Color(0xFFF4511E),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFFDAD1),
        onPrimaryContainer = Color(0xFF3B0B00),
        secondary = Color(0xFFFF8A50),
        onSecondary = Color(0xFF4A1500),
        secondaryContainer = Color(0xFFFFDEC8),
        onSecondaryContainer = Color(0xFF321000),
        background = Color(0xFFFFF6F2),
        surface = Color(0xFFFFF6F2),
        surfaceTint = Color(0xFFF4511E)
    )
}
