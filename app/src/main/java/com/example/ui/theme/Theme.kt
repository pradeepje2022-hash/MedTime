package com.example.ui.theme

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
    primary = MedTealPrimaryDark,
    onPrimary = MedTealOnPrimaryDark,
    primaryContainer = MedTealPrimaryContainerDark,
    onPrimaryContainer = MedTealOnPrimaryContainerDark,
    secondary = MedOceanSecondaryDark,
    onSecondary = Color(0xFF003258),
    secondaryContainer = MedOceanSecondaryContainerDark,
    onSecondaryContainer = Color(0xFFD4E7FA),
    tertiary = Color(0xFFFFB4A6),
    onTertiary = Color(0xFF5E1708),
    tertiaryContainer = Color(0xFF7E2A1B),
    onTertiaryContainer = Color(0xFFFFDAD3),
    background = MedBackgroundDark,
    onBackground = MedOnSurfaceDark,
    surface = MedSurfaceDark,
    onSurface = MedOnSurfaceDark,
    surfaceVariant = MedSurfaceVariantDark,
    onSurfaceVariant = MedOnSurfaceVariantDark,
    outline = MedOutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = MedTealPrimary,
    onPrimary = MedTealOnPrimary,
    primaryContainer = MedTealPrimaryContainer,
    onPrimaryContainer = MedTealOnPrimaryContainer,
    secondary = MedOceanSecondary,
    onSecondary = MedOceanOnSecondary,
    secondaryContainer = MedOceanSecondaryContainer,
    onSecondaryContainer = MedOceanOnSecondaryContainer,
    tertiary = MedCoralTertiary,
    onTertiary = Color.White,
    tertiaryContainer = MedCoralTertiaryContainer,
    onTertiaryContainer = MedCoralOnTertiaryContainer,
    background = MedBackgroundLight,
    onBackground = MedOnSurfaceLight,
    surface = MedSurfaceLight,
    onSurface = MedOnSurfaceLight,
    surfaceVariant = MedSurfaceVariantLight,
    onSurfaceVariant = MedOnSurfaceVariantLight,
    outline = MedOutlineLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our tailored cohesive healthcare theme by default
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
