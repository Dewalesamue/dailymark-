package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// =========================================================================
// DESIGN SYSTEM — COLOR TOKENS (Daymark Single Accent Revision)
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

// 3. Ash Grey: (#A4B5A6)
// Sage-tinted ash grey for secondary text, dividers, and muted surfaces
val AshGrey = Color(0xFFA4B5A6)                // Core Ash Grey swatch from the design palette (#A4B5A6)
val AshGreyDivider = Color(0xFFA4B5A6)         // Divider line & border token
val AshGreyMuted = Color(0xFFE8EDE9)           // Light mode muted container tint
val AshGreyDarkMuted = Color(0xFF3D4740)       // Dark mode muted container tint

// Secondary text tokens derived from Ash Grey for WCAG AA contrast compliance:
val AshGreyTextLight = Color(0xFF5A6B5E)       // Light mode secondary text (passes WCAG AA on Porcelain)
val AshGreyTextDark = Color(0xFFD0DCD2)        // Dark mode secondary text (passes WCAG AA on Charcoal)

// 4. Accent: Single Accent across whole app (#DA7756 - warm terracotta/clay orange)
// Used for: primary buttons, active nav icon, the camera FAB, toggles in the "on" state,
// and any highlight/badge color.
val Accent = Color(0xFFDA7756)                 // Core single accent (#DA7756)
val AccentTerracotta = Accent                  // Semantic alias
val AccentDark = Color(0xFFBF6142)             // Pressed / focused accent
val AccentLight = Color(0xFFE48E72)            // Soft accent highlight
val AccentContainer = Color(0xFFFBECE6)        // Light mode accent container tint
val AccentDarkContainer = Color(0xFF4E2316)    // Dark mode accent container tint
val OnAccent = Color(0xFFFFFFFF)               // Crisp white text/icon on Accent (#DA7756)

// Every place that previously referenced Sunlit Clay or Sandy Clay now uses #DA7756 instead — no second accent color:
val SunlitClay = Accent
val SunlitClayDark = AccentDark
val SunlitClayLight = AccentLight
val SunlitClayContainer = AccentContainer
val SunlitClayDarkContainer = AccentDarkContainer
val OnSunlitClay = OnAccent

val SandyClay = Accent
val SandyClayLight = AccentLight
val SandyClayContainer = AccentContainer
val SandyClayDarkContainer = AccentDarkContainer
val OnSandyClay = OnAccent

// 5. Neutral Text
val TextCharcoalPrimary = Color(0xFF1E1E1E)    // Crisp deep charcoal text for light mode
val TextPorcelainPrimary = Color(0xFFFAF8F2)   // Crisp porcelain text for dark mode

// =========================================================================
// Backward-Compatibility Aliases
// =========================================================================
val WarmTerracotta = Accent
val WarmTerracottaDark = AccentDark
val WarmTerracottaLight = AccentLight
val WarmTerracottaContainer = AccentContainer
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
val DarkTerracotta = Accent
val DarkTerracottaContainer = AccentDarkContainer
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

