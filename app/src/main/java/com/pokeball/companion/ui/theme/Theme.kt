package com.pokeball.companion.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PokeRed,
    onPrimary = Color.White,
    secondary = PokeAccent,
    onSecondary = Color.Black,
    tertiary = PokeGreen,
    background = Color(0xFF121416),
    surface = PokeDarkSurface,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = PokeRed,
    onPrimary = Color.White,
    secondary = PokeAccent,
    onSecondary = Color.Black,
    tertiary = PokeGreen,
    background = Color(0xFFF7F9FA),
    surface = Color.White,
    onBackground = Color(0xFF1E2124),
    onSurface = Color(0xFF1E2124)
)

@Composable
fun PokeballTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
