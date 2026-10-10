package com.helinesca.bugunneyemek.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Warm kitchen colours: terracotta, olive and cream. Follows the phone's light/dark setting. */
private val Light = lightColorScheme(
    primary = Color(0xFFB5542D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBCC),
    onPrimaryContainer = Color(0xFF3A0B00),
    secondary = Color(0xFF6B7B3A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE5EDC0),
    onSecondaryContainer = Color(0xFF1D2600),
    background = Color(0xFFFFF8F0),
    onBackground = Color(0xFF2B2118),
    surface = Color(0xFFFFF8F0),
    onSurface = Color(0xFF2B2118),
    surfaceVariant = Color(0xFFF3E4D7),
    onSurfaceVariant = Color(0xFF52443A),
    errorContainer = Color(0xFFFFDAD4),
    onErrorContainer = Color(0xFF410001),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFFFB597),
    onPrimary = Color(0xFF5C1A00),
    primaryContainer = Color(0xFF8A3A17),
    onPrimaryContainer = Color(0xFFFFDBCC),
    secondary = Color(0xFFC9D6A0),
    onSecondary = Color(0xFF323F0A),
    secondaryContainer = Color(0xFF4A5A20),
    onSecondaryContainer = Color(0xFFE5EDC0),
    background = Color(0xFF1E1813),
    onBackground = Color(0xFFEFE0D5),
    surface = Color(0xFF1E1813),
    onSurface = Color(0xFFEFE0D5),
    surfaceVariant = Color(0xFF52443A),
    onSurfaceVariant = Color(0xFFD7C3B5),
    errorContainer = Color(0xFF930006),
    onErrorContainer = Color(0xFFFFDAD4),
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
