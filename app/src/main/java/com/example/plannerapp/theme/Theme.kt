package com.example.plannerapp.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = AppPrimaryDark,
    onPrimary = Color.White,
    primaryContainer = AppSurfaceTintDark,
    onPrimaryContainer = AppTextPrimaryDark,
    secondary = AppSecondary,
    onSecondary = AppBackgroundDark,
    tertiary = AppAccentDark,
    background = AppBackgroundDark,
    onBackground = AppTextPrimaryDark,
    surface = AppSurfaceDark,
    onSurface = AppTextPrimaryDark,
    surfaceVariant = AppFieldBgDark,
    onSurfaceVariant = AppTextSecondaryDark,
    outline = AppBorderDark,
    outlineVariant = AppBorderDark,
    error = AppError,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = AppPrimary,
    onPrimary = Color.White,
    primaryContainer = AppSurfaceTintSelectedChip,
    onPrimaryContainer = AppPrimaryTextOnLightTint,
    secondary = AppSecondary,
    onSecondary = Color.White,
    tertiary = AppAccent,
    background = AppBackgroundLight,
    onBackground = AppTextPrimaryLight,
    surface = AppSurfaceLight,
    onSurface = AppTextPrimaryLight,
    surfaceVariant = AppFieldBgLight,
    onSurfaceVariant = AppTextSecondaryLight,
    outline = AppBorderLight,
    outlineVariant = AppBorderLight,
    error = AppError,
    onError = Color.White
)

@Composable
fun PlannerAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Default to false so custom brand colors are consistently applied on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
