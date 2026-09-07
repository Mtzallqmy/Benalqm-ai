package ai.moataz.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Typography

val MoatazInk = Color(0xFF080B14)
val MoatazInkRaised = Color(0xFF111625)
val MoatazSurface = Color(0xFF171D2D)
val MoatazSurfaceHigh = Color(0xFF20283B)
val MoatazViolet = Color(0xFF7857FF)
val MoatazIndigo = Color(0xFF4E6BFF)
val MoatazCyan = Color(0xFF25D0E8)
val MoatazMint = Color(0xFF55E6B5)
val MoatazAmber = Color(0xFFFFBE55)
val MoatazDanger = Color(0xFFFF5D73)
val MoatazText = Color(0xFFF5F7FF)
val MoatazTextMuted = Color(0xFFA8B0C5)

private val DarkColors = darkColorScheme(
    primary = MoatazViolet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2A225A),
    onPrimaryContainer = Color(0xFFEAE4FF),
    secondary = MoatazCyan,
    onSecondary = MoatazInk,
    tertiary = MoatazMint,
    background = MoatazInk,
    onBackground = MoatazText,
    surface = MoatazInkRaised,
    onSurface = MoatazText,
    surfaceVariant = MoatazSurface,
    onSurfaceVariant = MoatazTextMuted,
    outline = Color(0xFF343D53),
    outlineVariant = Color(0xFF272E40),
    error = MoatazDanger,
    onError = Color.White,
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF6242E9),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9E3FF),
    onPrimaryContainer = Color(0xFF24145E),
    secondary = Color(0xFF007C8D),
    onSecondary = Color.White,
    tertiary = Color(0xFF087A5A),
    background = Color(0xFFF8F9FE),
    onBackground = Color(0xFF161A26),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF161A26),
    surfaceVariant = Color(0xFFF0F2FA),
    onSurfaceVariant = Color(0xFF5B6376),
    outline = Color(0xFFCDD1DE),
    outlineVariant = Color(0xFFE2E5EF),
    error = Color(0xFFB3263D),
    onError = Color.White,
)

/**
 * Locale-aware Android sans-serif is intentionally used for the first native
 * release: Android selects its Arabic Noto face for Arabic text and keeps Latin
 * text crisp without bundling a third-party font license into the APK.
 */
private val MoatazTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp,
        lineHeight = 42.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 26.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
)

@Composable
fun MoatazTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MoatazTypography,
        content = content,
    )
}
