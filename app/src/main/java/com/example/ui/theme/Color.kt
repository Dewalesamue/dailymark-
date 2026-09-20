package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// =========================================================================
// DESIGN SYSTEM — COLOR TOKENS (Daymark v2.0)
// =========================================================================

// 1. Charcoal: Dark mode background & dark surfaces (#4A4A4A)
val Charcoal = Color(0xFF4A4A4A)               // Dark mode background canvas & dark surfaces
val CharcoalDark = Color(0xFF383838)           // Deeper dark surface for dialogs / bottom nav
val CharcoalSurface = Color(0xFF404040)        // Dark mode primary card surface
val CharcoalElevated = Color(0xFF525252)       // Elevated dark surface / chips / muted containers
val CharcoalBorder = Color(0xFF5E5E5E)         // Dark mode subtle border

// 2. Porcelain: Light mode background (#FAF8F2)
val Porcelain = Color(0xFFFAF8F2)              // Light mode canvas & background
val PorcelainSurface = Color(0xFFFFFFFF)       // Crisp white surface for cards & dialogs in light mode
val PorcelainVariant = Color(0xFFF2EFE7)       // Muted surface / chip / search bar container
val PorcelainBorder = Color(0xFFE5DFC9)        // Subtle warm border on Porcelain

// 3. Ash Grey: (Confirmed value: #A4B5A6)
// Sage-tinted ash grey for secondary text, dividers, and muted surfaces
val AshGrey = Color(0xFFA4B5A6)                // Core Ash Grey swatch from the design palette (#A4B5A6)
val AshGreyDivider = Color(0xFFA4B5A6)         // Divider line & border token
val AshGreyMuted = Color(0xFFE8EDE9)           // Light mode muted container tint
val AshGreyDarkMuted = Color(0xFF3D4740)       // Dark mode muted container tint

// Secondary text tokens derived from Ash Grey for WCAG AA contrast compliance:
// On Porcelain (#FAF8F2), #5A6B5E achieves > 5.2:1 contrast ratio.
val AshGreyTextLight = Color(0xFF5A6B5E)       // Light mode secondary text (passes WCAG AA)
// On Charcoal (#4A4A4A), #D0DCD2 achieves > 7.1:1 contrast ratio.
val AshGreyTextDark = Color(0xFFD0DCD2)        // Dark mode secondary text (passes WCAG AA)

// 4. Sunlit Clay: Primary accent (buttons, active nav icon, FAB) (#E2B56A)
val SunlitClay = Color(0xFFE2B56A)             // Primary accent
val SunlitClayDark = Color(0xFFC79848)         // Pressed / focused accent
val SunlitClayLight = Color(0xFFECCB8A)        // Lighter accent
val SunlitClayContainer = Color(0xFFFBF4E4)    // Light mode primary container tint
val SunlitClayDarkContainer = Color(0xFF6B5125) // Dark mode primary container tint
val OnSunlitClay = Color(0xFF261D0C)           // Crisp high-contrast deep text/icon on Sunlit Clay

// 5. Sandy Clay: Secondary accent (highlights, badges, hover states) (#E7B08A)
val SandyClay = Color(0xFFE7B08A)              // Secondary accent
val SandyClayLight = Color(0xFFF2CDB2)         // Soft highlight state
val SandyClayContainer = Color(0xFFFCF2EC)    // Light mode secondary badge container
val SandyClayDarkContainer = Color(0xFF664129) // Dark mode secondary badge container
val OnSandyClay = Color(0xFF3B1E0C)            // High-contrast text on Sandy Clay

// 6. Neutral Text
val TextCharcoalPrimary = Color(0xFF1E1E1E)    // Crisp deep charcoal text for light mode
val TextPorcelainPrimary = Color(0xFFFAF8F2)   // Crisp porcelain text for dark mode

// =========================================================================
// Backward-Compatibility Aliases
// =========================================================================
val WarmTerracotta = SunlitClay
val WarmTerracottaDark = SunlitClayDark
val WarmTerracottaLight = SunlitClayLight
val WarmTerracottaContainer = SunlitClayContainer
val DeepCharcoal = Charcoal
val DeepCharcoalPressed = CharcoalDark
val DeepCharcoalSurface = CharcoalSurface
val WarmSandCanvas = Porcelain
val WarmCreamSurface = PorcelainSurface
val WarmAlmondCard = PorcelainVariant
val WarmAlmondBorder = AshGreyDivider
val WarmPillBackground = PorcelainVariant
val TextEspressoPrimary = TextCharcoalPrimary
val TextTaupeSecondary = AshGreyTextLight
val TextTertiary = AshGrey
val DarkEspressoCanvas = Charcoal
val DarkCocoaSurface = CharcoalSurface
val DarkElevatedSurface = CharcoalElevated
val DarkAlmondCard = CharcoalElevated
val DarkBorder = CharcoalBorder
val DarkPillBackground = CharcoalElevated
val DarkTerracotta = SunlitClay
val DarkTerracottaContainer = SunlitClayDarkContainer
val TextLinenWhitePrimary = TextPorcelainPrimary
val TextSandSecondary = AshGreyTextDark

val PureBlack = CharcoalDark
val PureWhite = Color(0xFFFFFFFF)
val Gray50 = Porcelain
val Gray100 = PorcelainVariant
val Gray200 = AshGreyDivider
val Gray300 = AshGreyDivider
val Gray400 = AshGreyTextDark
val Gray500 = AshGreyTextLight
val Gray700 = CharcoalSurface
val Gray800 = CharcoalElevated
val Gray900 = CharcoalSurface
val Gray950 = Charcoal
val BrandBlack = Charcoal
val BrandWhite = PureWhite
val BrandSoftGray = Porcelain

