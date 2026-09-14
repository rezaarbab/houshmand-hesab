package com.houshmandhesab.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.core.view.WindowCompat
import com.houshmandhesab.app.R

val IncomeGreen = Color(0xFF34D399)
val ExpenseRed = Color(0xFFF87171)
val TransferBlue = Color(0xFF60A5FA)
val AccentViolet = Color(0xFFA78BFA)
val AccentAmber = Color(0xFFF59E0B)

val Vazirmatn = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_bold, FontWeight.Bold)
)

private val defaultTypography = Typography()

val HesabTypography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(fontFamily = Vazirmatn),
    displayMedium = defaultTypography.displayMedium.copy(fontFamily = Vazirmatn),
    displaySmall = defaultTypography.displaySmall.copy(fontFamily = Vazirmatn),
    headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = Vazirmatn),
    headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = Vazirmatn),
    headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = Vazirmatn),
    titleLarge = defaultTypography.titleLarge.copy(fontFamily = Vazirmatn),
    titleMedium = defaultTypography.titleMedium.copy(fontFamily = Vazirmatn),
    titleSmall = defaultTypography.titleSmall.copy(fontFamily = Vazirmatn),
    bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = Vazirmatn),
    bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = Vazirmatn),
    bodySmall = defaultTypography.bodySmall.copy(fontFamily = Vazirmatn),
    labelLarge = defaultTypography.labelLarge.copy(fontFamily = Vazirmatn),
    labelMedium = defaultTypography.labelMedium.copy(fontFamily = Vazirmatn),
    labelSmall = defaultTypography.labelSmall.copy(fontFamily = Vazirmatn)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF34D399),
    onPrimary = Color(0xFF04291A),
    primaryContainer = Color(0xFF14532D),
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = Color(0xFFA78BFA),
    onSecondary = Color(0xFF241547),
    tertiary = Color(0xFFF59E0B),
    background = Color(0xFF0B1220),
    onBackground = Color(0xFFE2E8F0),
    surface = Color(0xFF111A2C),
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF1B2740),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF059669),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA7F3D0),
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = Color(0xFF7C3AED),
    onSecondary = Color.White,
    tertiary = Color(0xFFD97706),
    background = Color(0xFFF4F7FB),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE8EEF6),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFCBD5E1),
    error = Color(0xFFDC2626),
    onError = Color.White
)

@Composable
fun HesabTheme(themeMode: String = "SYSTEM", content: @Composable () -> Unit) {
    val darkTheme = when (themeMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemInDarkTheme()
    }
    val scheme = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(colorScheme = scheme, typography = HesabTypography, content = content)
}
