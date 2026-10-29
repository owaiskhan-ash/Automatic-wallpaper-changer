package com.autowall.changer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Bg = Color(0xFF08080A)
val BgDeep = Color(0xFF050506)
val Surface1 = Color(0xFF101013)
val Surface2 = Color(0xFF17171C)
val Surface3 = Color(0xFF1F1F26)
val Ink = Color(0xFFF3F0E8)
val Muted = Color(0xFF83838D)
val Faint = Color(0xFF5A5A64)
val Line = Color(0xFF232329)
val Accent = Color(0xFFE9E4D6)

private val AutoWallScheme = darkColorScheme(
    primary = Accent,
    onPrimary = Color(0xFF0B0B0C),
    secondary = Ink,
    onSecondary = Color(0xFF0B0B0C),
    background = Bg,
    onBackground = Ink,
    surface = Surface1,
    onSurface = Ink,
    surfaceVariant = Surface2,
    onSurfaceVariant = Muted,
    outline = Line,
    outlineVariant = Line,
    error = Color(0xFFFF8A80),
)

private val AutoWallTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.8).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.4).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.1).sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        letterSpacing = 1.6.sp,
    ),
)

@Composable
fun AutoWallTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AutoWallScheme,
        typography = AutoWallTypography,
        content = content,
    )
}
