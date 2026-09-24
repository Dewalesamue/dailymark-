package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme =
  lightColorScheme(
    primary = Accent,
    onPrimary = OnAccent,
    primaryContainer = AccentContainer,
    onPrimaryContainer = Color(0xFF38150A),
    secondary = Accent,
    onSecondary = OnAccent,
    secondaryContainer = AccentContainer,
    onSecondaryContainer = Color(0xFF38150A),
    tertiary = AshGrey,
    onTertiary = OnAccent,
    tertiaryContainer = AshGreyMuted,
    onTertiaryContainer = AshGreyTextLight,
    background = Porcelain,
    onBackground = TextCharcoalPrimary,
    surface = PorcelainSurface,
    onSurface = TextCharcoalPrimary,
    surfaceVariant = PorcelainVariant,
    onSurfaceVariant = AshGreyTextLight,
    outline = AshGreyDivider,
    outlineVariant = AshGreyMuted,
  )

private val DarkColorScheme =
  darkColorScheme(
    primary = Accent,
    onPrimary = OnAccent,
    primaryContainer = AccentDarkContainer,
    onPrimaryContainer = AccentLight,
    secondary = Accent,
    onSecondary = OnAccent,
    secondaryContainer = AccentDarkContainer,
    onSecondaryContainer = AccentLight,
    tertiary = AshGrey,
    onTertiary = OnAccent,
    tertiaryContainer = AshGreyDarkMuted,
    onTertiaryContainer = AshGreyTextDark,
    background = Charcoal,
    onBackground = TextPorcelainPrimary,
    surface = CharcoalSurface,
    onSurface = TextPorcelainPrimary,
    surfaceVariant = CharcoalElevated,
    onSurfaceVariant = AshGreyTextDark,
    outline = AshGreyDivider.copy(alpha = 0.5f),
    outlineVariant = CharcoalBorder,
  )

val OrganicShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    shapes = OrganicShapes,
    content = content
  )
}

