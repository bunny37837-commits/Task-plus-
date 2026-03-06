package com.taskpulse.app.presentation.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.taskpulse.app.R

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

val PlusJakartaSans = GoogleFont("Plus Jakarta Sans")
val fontFamily = androidx.compose.ui.text.font.FontFamily(
    Font(googleFont = PlusJakartaSans, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = PlusJakartaSans, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = PlusJakartaSans, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = PlusJakartaSans, fontProvider = provider, weight = FontWeight.Bold),
)

val AppTypography = Typography(
    displayLarge  = TextStyle(fontFamily = fontFamily, fontSize = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontFamily = fontFamily, fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
    titleLarge    = TextStyle(fontFamily = fontFamily, fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
    titleMedium   = TextStyle(fontFamily = fontFamily, fontSize = 15.sp, fontWeight = FontWeight.Medium),
    bodyLarge     = TextStyle(fontFamily = fontFamily, fontSize = 15.sp, fontWeight = FontWeight.Normal, lineHeight = 22.sp),
    bodyMedium    = TextStyle(fontFamily = fontFamily, fontSize = 13.sp, fontWeight = FontWeight.Normal),
    labelLarge    = TextStyle(fontFamily = fontFamily, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp),
    labelMedium   = TextStyle(fontFamily = fontFamily, fontSize = 12.sp, fontWeight = FontWeight.Medium),
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryPurple,
    onPrimary = Color.White,
    secondary = AccentCyan,
    onSecondary = Color.White,
    background = Background,
    surface = SurfaceCard,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = BorderColor,
    error = Danger,
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryPurple,
    onPrimary = Color.White,
    secondary = AccentCyan,
    onSecondary = Color.White,
    background = LightBackground,
    surface = LightSurface,
    onBackground = Color(0xFF0D0D0F),
    onSurface = Color(0xFF0D0D0F),
    outline = LightBorder,
    error = Danger,
)

@Composable
fun TaskPulseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}
