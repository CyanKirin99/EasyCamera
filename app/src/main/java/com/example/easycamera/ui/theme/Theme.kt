package com.example.easycamera.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.easycamera.BuildConfig

private val DarkColorScheme = darkColorScheme(
    primary = DarkGreenPrimary,
    secondary = DarkGreenSecondary,
    tertiary = DarkGreenTertiary
)

private val LightColorScheme = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = GreenOnPrimary,
    primaryContainer = GreenPrimaryContainer,
    onPrimaryContainer = GreenOnPrimaryContainer,
    secondary = GreenSecondary,
    onSecondary = GreenOnSecondary,
    secondaryContainer = GreenSecondaryContainer,
    onSecondaryContainer = GreenOnSecondaryContainer,
    tertiary = GreenTertiary,
    onTertiary = GreenOnTertiary,
    tertiaryContainer = GreenTertiaryContainer,
    onTertiaryContainer = GreenOnTertiaryContainer,
    background = GreenBackground,
    onBackground = GreenOnBackground,
    surface = GreenSurface,
    onSurface = GreenOnSurface,
    surfaceVariant = GreenSurfaceVariant,
    onSurfaceVariant = GreenOnSurfaceVariant,
    outline = GreenOutline
)

private val DarkAmberColorScheme = darkColorScheme(
    primary = DarkAmberPrimary,
    secondary = DarkAmberSecondary,
    tertiary = DarkAmberTertiary
)

private val LightAmberColorScheme = lightColorScheme(
    primary = AmberPrimary,
    onPrimary = AmberOnPrimary,
    primaryContainer = AmberPrimaryContainer,
    onPrimaryContainer = AmberOnPrimaryContainer,
    secondary = AmberSecondary,
    onSecondary = AmberOnSecondary,
    secondaryContainer = AmberSecondaryContainer,
    onSecondaryContainer = AmberOnSecondaryContainer,
    tertiary = AmberTertiary,
    onTertiary = AmberOnTertiary,
    tertiaryContainer = AmberTertiaryContainer,
    onTertiaryContainer = AmberOnTertiaryContainer,
    background = AmberBackground,
    onBackground = AmberOnBackground,
    surface = AmberSurface,
    onSurface = AmberOnSurface,
    surfaceVariant = AmberSurfaceVariant,
    onSurfaceVariant = AmberOnSurfaceVariant,
    outline = AmberOutline
)

@Composable
fun EasyCameraTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val isNonIdeal = BuildConfig.IS_NON_IDEAL
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> if (isNonIdeal) DarkAmberColorScheme else DarkColorScheme
        else -> if (isNonIdeal) LightAmberColorScheme else LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}