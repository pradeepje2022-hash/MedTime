package com.medtime.reminder.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val TealPrimary = Color(0xFF0E9488)
val TealDark = Color(0xFF0B7A70)
val BgLight = Color(0xFFF5F8F7)
val BgDark = Color(0xFF101414)
val CardLight = Color(0xFFFFFFFF)
val CardDark = Color(0xFF1B211F)
val Danger = Color(0xFFE05252)
val Warning = Color(0xFFE0A426)
val Success = Color(0xFF2FA36B)

private val LightColors = lightColorScheme(
    primary = TealPrimary,
    secondary = TealDark,
    background = BgLight,
    surface = CardLight,
    error = Danger
)

private val DarkColors = darkColorScheme(
    primary = TealPrimary,
    secondary = TealDark,
    background = BgDark,
    surface = CardDark,
    error = Danger
)

@Composable
fun MedTimeTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, typography = MaterialTheme.typography, content = content)
}
