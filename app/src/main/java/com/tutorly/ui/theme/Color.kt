package com.tutorly.ui.theme

import androidx.compose.ui.graphics.Color

// Brand palette
val TopBarGradientStart = Color(0xFF3B7D72)
val TopBarGradientEnd = Color(0xFF4E998C)
val PrimaryPurple = TopBarGradientStart
val OnPrimaryPurple = Color(0xFFFFFFFF)
val PrimaryPurpleContainer = TopBarGradientEnd
val OnPrimaryPurpleContainer = Color(0xFF0F2C26)

val SecondaryCoral = Color(0xFFC1477F)
val OnSecondaryCoral = Color(0xFFFFFFFF)
val SecondaryCoralContainer = Color(0xFFFFD1E2)
val OnSecondaryCoralContainer = Color(0xFF47132E)

val TertiaryPeach = Color(0xFFFF7A66)
val OnTertiaryPeach = Color(0xFFFFFFFF)
val TertiaryPeachContainer = Color(0xFFFFDCD3)
val OnTertiaryPeachContainer = Color(0xFF55180B)

// Neutral surfaces and content
val ScreenGradientStart = Color(0xFFF3F8F6)
val ScreenGradientEnd = Color(0xFFF1F6F6)
val PrimaryTextColor = Color(0xFF2C2C33)
val SecondaryTextColor = Color(0xFF49484E)
val AvatarFill = Color(0xFFDDE9E5)
val SurfaceBlush = ScreenGradientStart
val OnSurfaceBlush = PrimaryTextColor
val SurfaceVariantSoft = AvatarFill
val OnSurfaceVariantSoft = SecondaryTextColor
val OutlineLavender = Color(0xFFB9C9C4)
val OutlineVariantLavender = Color(0xFFE3ECE8)

val PaidChipContent = Color(0xFF4E998C)
val PaidChipFill = Color(0xFFFFFFFF)
val DebtChipContent = Color(0xFFD05E6E)
val DebtChipFill = Color(0xFFFFFFFF)

val SurfaceContainerLowest = Color(0xFFFFFFFF)
val SurfaceContainerLow = Color(0xFFF8FCFB)
val SurfaceContainer = Color(0xFFF1F6F5)
val SurfaceContainerHigh = Color(0xFFE8F0EE)
val SurfaceContainerHighest = Color(0xFFDDE9E5)

// Semantic
val ErrorRed = Color(0xFFBA1A1A)
val OnErrorRed = Color(0xFFFFFFFF)
val ErrorRedContainer = Color(0xFFFFDAD6)
val OnErrorRedContainer = Color(0xFF410002)

// Dark theme counterparts
val DarkPrimaryPurple = Color(0xFFD6BBE0)
val DarkOnPrimaryPurple = Color(0xFF341139)
val DarkPrimaryPurpleContainer = Color(0xFF45204E)
val DarkOnPrimaryPurpleContainer = Color(0xFFF3D7FF)

val DarkSecondaryCoral = Color(0xFFFFAACF)
val DarkOnSecondaryCoral = Color(0xFF4A122D)
val DarkSecondaryCoralContainer = Color(0xFF641D41)
val DarkOnSecondaryCoralContainer = Color(0xFFFFD7E8)

val DarkTertiaryPeach = Color(0xFFFFB5A6)
val DarkOnTertiaryPeach = Color(0xFF571F11)
val DarkTertiaryPeachContainer = Color(0xFF772F21)
val DarkOnTertiaryPeachContainer = Color(0xFFFFDACF)

val DarkBackground = Color(0xFF201924)
val DarkOnBackground = Color(0xFFECE0F0)
val DarkSurface = Color(0xFF201924)
val DarkOnSurface = Color(0xFFECE0F0)
val DarkSurfaceVariant = Color(0xFF3B3141)
val DarkOnSurfaceVariant = Color(0xFFCFC1D3)
val DarkOutline = Color(0xFF9A8FA2)
val DarkOutlineVariant = Color(0xFF372C3A)

val DarkSurfaceContainerLowest = Color(0xFF140F18)
val DarkSurfaceContainerLow = Color(0xFF1A1420)
val DarkSurfaceContainer = Color(0xFF221B27)
val DarkSurfaceContainerHigh = Color(0xFF2B2430)
val DarkSurfaceContainerHighest = Color(0xFF352E3A)


// Legacy references updated to new palette
val RailAccent = PrimaryPurple  // вертикальная рейка слота
val NowAccent = SecondaryCoral  // линия текущего времени
val CardBg = SurfaceVariantSoft // фон карточек
val GridLine = Color(0x142E2A35)  // тонкие линии сетки

val AppBg = SurfaceBlush
val PaidBg = TertiaryPeach
val UnpaidBg = SurfaceVariantSoft
val MutedText = OnSurfaceVariantSoft
