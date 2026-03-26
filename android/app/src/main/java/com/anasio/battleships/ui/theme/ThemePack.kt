package com.anasio.battleships.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Identifiers for built-in theme packs. */
enum class ThemeId(val label: String, val icon: String) {
    CLASSIC("Classic Navy", "🚢"),
    NEON("Neon Cyber", "🌃"),
    PIRATE("Pirate Gold", "🏴‍☠️"),
    ARCTIC("Arctic Ice", "❄️"),
    VOLCANO("Volcano", "🌋"),
}

/** Full colour palette used across the app. */
data class BattleshipColorPalette(
    // Surfaces
    val background: Color,
    val surface: Color,
    val card: Color,
    val border: Color,
    val textPrimary: Color,
    val textDim: Color,
    // Accents
    val primary: Color,
    val primaryDark: Color,
    val accent: Color,
    val yellow: Color,
    val green: Color,
    val emerald: Color,
    val red: Color,
    val orange: Color,
    // Board cells
    val cellWater: Color,
    val cellShip: Color,
    val cellHit: Color,
    val cellMiss: Color,
    val cellSunk: Color,
    val cellSafe: Color,
)

val LocalColorPalette = staticCompositionLocalOf { ClassicPalette }

// ═══════════════════════════════════════════
// CLASSIC NAVY (original)
// ═══════════════════════════════════════════
val ClassicPalette = BattleshipColorPalette(
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B),
    card = Color(0xFF334155),
    border = Color(0xFF475569),
    textPrimary = Color(0xFFCBD5E1),
    textDim = Color(0xFF94A3B8),
    primary = Color(0xFF3B82F6),
    primaryDark = Color(0xFF2563EB),
    accent = Color(0xFF8B5CF6),
    yellow = Color(0xFFFDE047),
    green = Color(0xFF22C55E),
    emerald = Color(0xFF10B981),
    red = Color(0xFFEF4444),
    orange = Color(0xFFF97316),
    cellWater = Color(0xFF1E3A5F),
    cellShip = Color(0xFF22C55E),
    cellHit = Color(0xFFEF4444),
    cellMiss = Color(0xFF475569),
    cellSunk = Color(0xFF7F1D1D),
    cellSafe = Color(0xFF1E293B),
)

// ═══════════════════════════════════════════
// NEON CYBER
// ═══════════════════════════════════════════
val NeonPalette = BattleshipColorPalette(
    background = Color(0xFF0A0A1A),
    surface = Color(0xFF12122B),
    card = Color(0xFF1C1C3D),
    border = Color(0xFF2D2D5E),
    textPrimary = Color(0xFFE0E0FF),
    textDim = Color(0xFF9090C0),
    primary = Color(0xFF00F0FF),
    primaryDark = Color(0xFF00C4CC),
    accent = Color(0xFFFF00FF),
    yellow = Color(0xFFFFFF00),
    green = Color(0xFF39FF14),
    emerald = Color(0xFF00FF88),
    red = Color(0xFFFF1744),
    orange = Color(0xFFFF6D00),
    cellWater = Color(0xFF0D0D30),
    cellShip = Color(0xFF39FF14),
    cellHit = Color(0xFFFF1744),
    cellMiss = Color(0xFF2D2D5E),
    cellSunk = Color(0xFF6A0020),
    cellSafe = Color(0xFF12122B),
)

// ═══════════════════════════════════════════
// PIRATE GOLD
// ═══════════════════════════════════════════
val PiratePalette = BattleshipColorPalette(
    background = Color(0xFF1A0F00),
    surface = Color(0xFF2C1A05),
    card = Color(0xFF3D260B),
    border = Color(0xFF5C3D15),
    textPrimary = Color(0xFFFFE4B5),
    textDim = Color(0xFFB8956A),
    primary = Color(0xFFD4A520),
    primaryDark = Color(0xFFB8860B),
    accent = Color(0xFFFF6B35),
    yellow = Color(0xFFFFD700),
    green = Color(0xFF228B22),
    emerald = Color(0xFF2E8B57),
    red = Color(0xFFDC143C),
    orange = Color(0xFFFF8C00),
    cellWater = Color(0xFF1A3040),
    cellShip = Color(0xFFD4A520),
    cellHit = Color(0xFFDC143C),
    cellMiss = Color(0xFF5C3D15),
    cellSunk = Color(0xFF5C1010),
    cellSafe = Color(0xFF2C1A05),
)

// ═══════════════════════════════════════════
// ARCTIC ICE
// ═══════════════════════════════════════════
val ArcticPalette = BattleshipColorPalette(
    background = Color(0xFF0C1929),
    surface = Color(0xFF152238),
    card = Color(0xFF1E3050),
    border = Color(0xFF2A4570),
    textPrimary = Color(0xFFE0F0FF),
    textDim = Color(0xFF88B0D0),
    primary = Color(0xFF60C8FF),
    primaryDark = Color(0xFF38A8E0),
    accent = Color(0xFFB0E0FF),
    yellow = Color(0xFFFFF3B0),
    green = Color(0xFF6AE8A0),
    emerald = Color(0xFF48D0A0),
    red = Color(0xFFFF6B8A),
    orange = Color(0xFFFFB060),
    cellWater = Color(0xFF102840),
    cellShip = Color(0xFF60C8FF),
    cellHit = Color(0xFFFF6B8A),
    cellMiss = Color(0xFF2A4570),
    cellSunk = Color(0xFF702040),
    cellSafe = Color(0xFF152238),
)

// ═══════════════════════════════════════════
// VOLCANO
// ═══════════════════════════════════════════
val VolcanoPalette = BattleshipColorPalette(
    background = Color(0xFF1A0A0A),
    surface = Color(0xFF2A1212),
    card = Color(0xFF3A1A1A),
    border = Color(0xFF5A2A2A),
    textPrimary = Color(0xFFFFD5C5),
    textDim = Color(0xFFB08070),
    primary = Color(0xFFFF6600),
    primaryDark = Color(0xFFE05500),
    accent = Color(0xFFFFCC00),
    yellow = Color(0xFFFFEE44),
    green = Color(0xFF44BB44),
    emerald = Color(0xFF33AA66),
    red = Color(0xFFFF2222),
    orange = Color(0xFFFF8800),
    cellWater = Color(0xFF1A1530),
    cellShip = Color(0xFFFF8800),
    cellHit = Color(0xFFFF2222),
    cellMiss = Color(0xFF5A2A2A),
    cellSunk = Color(0xFF660000),
    cellSafe = Color(0xFF2A1212),
)

/** Look up palette by [ThemeId]. */
fun paletteFor(id: ThemeId): BattleshipColorPalette = when (id) {
    ThemeId.CLASSIC -> ClassicPalette
    ThemeId.NEON    -> NeonPalette
    ThemeId.PIRATE  -> PiratePalette
    ThemeId.ARCTIC  -> ArcticPalette
    ThemeId.VOLCANO -> VolcanoPalette
}
