package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = NeutralWhite,
    primaryContainer = EmeraldDark,
    onPrimaryContainer = EmeraldLight,
    background = NeutralDarkBackground,
    onBackground = TextPrimaryDark,
    surface = NeutralDarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = NeutralDarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = NeutralDarkBorder,
    outlineVariant = NeutralDarkBorder.copy(alpha = 0.6f)
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = NeutralWhite,
    primaryContainer = EmeraldLight,
    onPrimaryContainer = EmeraldContentDark,
    background = NeutralBackgroundLight,
    onBackground = TextPrimaryLight,
    surface = NeutralWhite,
    onSurface = TextPrimaryLight,
    surfaceVariant = NeutralSurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = NeutralBorderLight,
    outlineVariant = NeutralBorderLight.copy(alpha = 0.6f)
)

@Composable
fun SymptoMedTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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
