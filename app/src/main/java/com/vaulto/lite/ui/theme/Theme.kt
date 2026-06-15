package com.vaulto.lite.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = SaffronPrimary,
    onPrimary = OnSaffron,
    primaryContainer = SaffronPrimary.copy(alpha = 0.16f),
    onPrimaryContainer = SaffronPrimaryDark,
    secondary = ChartDustyBlue,
    background = CreamBackground,
    onBackground = TextPrimaryLight,
    surface = CreamSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = CreamSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    error = StatusRed,
    outline = TextSecondaryLight.copy(alpha = 0.3f)
)

private val DarkColors = darkColorScheme(
    primary = SaffronPrimary,
    onPrimary = TextPrimaryDark,
    primaryContainer = SaffronPrimary.copy(alpha = 0.24f),
    onPrimaryContainer = SaffronPrimary,
    secondary = ChartDustyBlue,
    background = CharcoalBackground,
    onBackground = TextPrimaryDark,
    surface = CharcoalSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = CharcoalSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    error = StatusRed,
    outline = TextSecondaryDark.copy(alpha = 0.3f)
)

/**
 * App-wide Material3 theme.
 * [darkTheme] should be driven by the persisted user preference from
 * SettingsRepository (DataStore), defaulting to system setting.
 */
@Composable
fun VaultoLiteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = VaultoTypography,
        shapes = VaultoShapes,
        content = content
    )
}
