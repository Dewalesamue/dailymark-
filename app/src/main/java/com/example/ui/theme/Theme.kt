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
    primary = SunlitClay,
    onPrimary = OnSunlitClay,
    primaryContainer = SunlitClayContainer,
    onPrimaryContainer = Color(0xFF423010),
    secondary = SandyClay,
    onSecondary = OnSandyClay,
    secondaryContainer = SandyClayContainer,
    onSecondaryContainer = Color(0xFF4D2A14),
    tertiary = AshGrey,
    onTertiary = OnSunlitClay,
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
    primary = SunlitClay,
    onPrimary = OnSunlitClay,
    primaryContainer = SunlitClayDarkContainer,
    onPrimaryContainer = SunlitClayLight,
    secondary = SandyClay,
    onSecondary = OnSandyClay,
    secondaryContainer = SandyClayDarkContainer,
    onSecondaryContainer = SandyClayLight,
    tertiary = AshGrey,
    onTertiary = OnSunlitClay,
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

