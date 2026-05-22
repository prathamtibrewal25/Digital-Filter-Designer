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


private val DarkColorScheme =
  darkColorScheme(
    primary = LightIndigo,
    onPrimary = NavyDeep,
    primaryContainer = NavyDeep,
    onPrimaryContainer = LightIndigo,
    secondary = SkyCyan,
    onSecondary = DarkSlate,
    secondaryContainer = Color(0xFF25282B),
    onSecondaryContainer = SkyCyan,
    background = DarkSlate,
    onBackground = SoftWhite,
    surface = DarkSlate,
    onSurface = SoftWhite,
    surfaceVariant = Color(0xFF2A2D30),
    onSurfaceVariant = Color(0xFFE2E5EC), // Increased contrast from MutedSlate (0xFFC4C6CF)
    outline = SlateGrey,
    outlineVariant = Color(0xFF43474E)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = NavyDeep,
    onPrimary = Color.White,
    primaryContainer = LightIndigo,
    onPrimaryContainer = NavyDeep,
    secondary = NavyDeep,
    onSecondary = Color.White,
    secondaryContainer = LightIndigo,
    onSecondaryContainer = NavyDeep,
    background = SoftWhite,
    onBackground = OnSurfaceColor,
    surface = SurfaceColor,
    onSurface = OnSurfaceColor,
    surfaceVariant = InputBg,
    onSurfaceVariant = Color(0xFF212529), // Increased contrast from SlateLabel (0xFF43474E)
    outline = MutedSlate,
    outlineVariant = LightIndigo
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disable dynamic color by default to guarantee Geometric Balance styling is applied
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

