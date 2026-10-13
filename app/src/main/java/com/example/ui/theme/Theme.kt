package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val StadiumNightColorScheme = darkColorScheme(
  primary = FloodlightCyan,
  onPrimary = Color.Black,
  primaryContainer = StadiumSurfaceElevated,
  onPrimaryContainer = FloodlightCyan,
  secondary = NeonGold,
  onSecondary = Color.Black,
  secondaryContainer = StadiumSurfaceDark,
  onSecondaryContainer = NeonGold,
  tertiary = TeamRedPrimary,
  onTertiary = Color.White,
  background = StadiumNightBg,
  onBackground = TextWhitePrimary,
  surface = StadiumSurfaceDark,
  onSurface = TextWhitePrimary,
  surfaceVariant = StadiumSurfaceElevated,
  onSurfaceVariant = TextMuted,
  outline = StadiumSurfaceBorder
)

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit
) {
  MaterialTheme(
    colorScheme = StadiumNightColorScheme,
    typography = Typography,
    content = content
  )
}
