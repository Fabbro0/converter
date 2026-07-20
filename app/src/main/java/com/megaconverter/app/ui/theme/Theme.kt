package com.megaconverter.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Nothing-OS-inspired look: near-black surfaces, a single red accent, sharp
 * (barely rounded) corners and tracked-out monospace headers standing in for
 * Nothing's proprietary Ndot/NType fonts (which aren't freely licensable here).
 */
private val NothingBlack = Color(0xFF000000)
private val NothingSurface = Color(0xFF121212)
private val NothingSurfaceVariant = Color(0xFF1E1E1E)
private val NothingRed = Color(0xFFD1272B)
private val NothingWhite = Color(0xFFF5F5F5)
private val NothingGray = Color(0xFF9A9A9A)
private val NothingOutline = Color(0xFF2C2C2C)

private val NothingDarkColors = darkColorScheme(
    primary = NothingRed,
    onPrimary = NothingWhite,
    secondary = NothingRed,
    onSecondary = NothingWhite,
    tertiary = NothingGray,
    onTertiary = NothingBlack,
    background = NothingBlack,
    onBackground = NothingWhite,
    surface = NothingSurface,
    onSurface = NothingWhite,
    surfaceVariant = NothingSurfaceVariant,
    onSurfaceVariant = NothingGray,
    outline = NothingOutline,
    outlineVariant = NothingOutline,
    error = NothingRed,
    onError = NothingWhite,
)

private val NothingLightColors = lightColorScheme(
    primary = NothingRed,
    onPrimary = Color.White,
    secondary = NothingRed,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color(0xFFF7F7F7),
    onSurface = Color.Black,
    outline = Color(0xFFD8D8D8),
    error = NothingRed,
)

private val DotMatrix = FontFamily.Monospace

private val NothingTypography = Typography().let { base ->
    Typography(
        displayLarge = base.displayLarge.trackedOut(),
        displayMedium = base.displayMedium.trackedOut(),
        displaySmall = base.displaySmall.trackedOut(),
        headlineLarge = base.headlineLarge.trackedOut(),
        headlineMedium = base.headlineMedium.trackedOut(),
        headlineSmall = base.headlineSmall.trackedOut(),
        titleLarge = base.titleLarge.trackedOut(),
        titleMedium = base.titleMedium.trackedOut(letterSpacing = 1.sp),
        titleSmall = base.titleSmall.trackedOut(letterSpacing = 1.sp),
        labelLarge = base.labelLarge.trackedOut(letterSpacing = 2.sp),
        labelMedium = base.labelMedium.trackedOut(letterSpacing = 1.5.sp),
        labelSmall = base.labelSmall.trackedOut(letterSpacing = 1.5.sp),
        bodyLarge = base.bodyLarge,
        bodyMedium = base.bodyMedium,
        bodySmall = base.bodySmall,
    )
}

private fun TextStyle.trackedOut(letterSpacing: androidx.compose.ui.unit.TextUnit = 1.sp): TextStyle =
    copy(fontFamily = DotMatrix, fontWeight = FontWeight.Bold, letterSpacing = letterSpacing)

/** Sharp, barely-rounded corners instead of Material's default pill/rounded look. */
private val NothingShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(2.dp),
    medium = RoundedCornerShape(2.dp),
    large = RoundedCornerShape(4.dp),
    extraLarge = RoundedCornerShape(4.dp),
)

@Composable
fun MegaConverterTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) NothingDarkColors else NothingLightColors,
        typography = NothingTypography,
        shapes = NothingShapes,
        content = content,
    )
}
