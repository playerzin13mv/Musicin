package com.musicin.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF277247),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA9FF99),
    onPrimaryContainer = Color(0xFF06361A),
    secondary = Color(0xFF3F6B4D),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD5F2CE),
    onSecondaryContainer = Color(0xFF0B2E18),
    background = Color(0xFFF4FFF1),
    onBackground = Color(0xFF111E15),
    surface = Color(0xFFF4FFF1),
    onSurface = Color(0xFF111E15),
    surfaceVariant = Color(0xFFDCE8D8),
    onSurfaceVariant = Color(0xFF414941),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA9FF99),
    onPrimary = Color(0xFF06361A),
    primaryContainer = Color(0xFF277247),
    onPrimaryContainer = Color(0xFFD6FFCC),
    secondary = Color(0xFFB4D2B8),
    onSecondary = Color(0xFF1F3A27),
    secondaryContainer = Color(0xFF1F3A27),
    onSecondaryContainer = Color(0xFFD5F2CE),
    background = Color(0xFF0E1710),
    onBackground = Color(0xFFDFEBDD),
    surface = Color(0xFF0E1710),
    onSurface = Color(0xFFDFEBDD),
    surfaceVariant = Color(0xFF2B382E),
    onSurfaceVariant = Color(0xFFBFCBBD),
)

@Composable
fun MusicinTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
