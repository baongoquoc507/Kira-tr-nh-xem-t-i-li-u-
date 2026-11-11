package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// iOS 27 Liquid Glass Color Palette
val IosBlue = Color(0xFF007AFF)
val IosBlueDark = Color(0xFF0A84FF)
val IosCyan = Color(0xFF32ADE6)
val IosIndigo = Color(0xFF5856D6)
val IosPurple = Color(0xFFAF52DE)
val IosPink = Color(0xFFFF2D55)
val IosGreen = Color(0xFF34C759)
val IosOrange = Color(0xFFFF9500)
val IosYellow = Color(0xFFFFCC00)
val IosRed = Color(0xFFFF3B30)

// Liquid Glass Translucency Tokens
val LightGlassBackground = Color(0x99F6F7F9)
val LightGlassSurface = Color(0xCCFFFFFF)
val LightGlassBorder = Color(0x66FFFFFF)
val LightGlassSubtle = Color(0x55E5E7EB)

val DarkGlassBackground = Color(0x990C1017)
val DarkGlassSurface = Color(0xCC1A202C)
val DarkGlassBorder = Color(0x33FFFFFF)
val DarkGlassSubtle = Color(0x442D3748)

// Document Types Color Accents
val WordBlue = Color(0xFF185ABD)
val ExcelGreen = Color(0xFF107C41)
val PptOrange = Color(0xFFC43E1C)
val PdfRed = Color(0xFFD83B01)
val TextGray = Color(0xFF5C6B73)

// Curated Wallpaper Presets
data class WallpaperPreset(
    val id: String,
    val nameVi: String,
    val nameEn: String,
    val colors: List<Color>,
    val isDark: Boolean
)

val WallpaperPresets = listOf(
    WallpaperPreset(
        id = "aurora",
        nameVi = "Cực quang thủy tinh",
        nameEn = "Liquid Aurora",
        colors = listOf(Color(0xFF0D1B2A), Color(0xFF1B4965), Color(0xFF0077B6), Color(0xFF5856D6)),
        isDark = true
    ),
    WallpaperPreset(
        id = "midnight",
        nameVi = "Đêm huyền ảo",
        nameEn = "Midnight Velvet",
        colors = listOf(Color(0xFF0A0E17), Color(0xFF141E30), Color(0xFF243B55)),
        isDark = true
    ),
    WallpaperPreset(
        id = "deep_ocean",
        nameVi = "Đại dương sâu",
        nameEn = "Deep Ocean",
        colors = listOf(Color(0xFF03001E), Color(0xFF7303C0), Color(0xFFEC38BC), Color(0xFFFDEFF9)),
        isDark = true
    ),
    WallpaperPreset(
        id = "emerald_frost",
        nameVi = "Sương ngọc bích",
        nameEn = "Emerald Mist",
        colors = listOf(Color(0xFF09203F), Color(0xFF537895), Color(0xFF2E7D32)),
        isDark = true
    ),
    WallpaperPreset(
        id = "sunset_glass",
        nameVi = "Hoàng hôn lăng kính",
        nameEn = "Sunset Glass",
        colors = listOf(Color(0xFF2C3E50), Color(0xFFFD746C), Color(0xFFFF9068)),
        isDark = false
    ),
    WallpaperPreset(
        id = "frosted_pearl",
        nameVi = "Ngọc trai mờ",
        nameEn = "Frosted Pearl",
        colors = listOf(Color(0xFFE0EAFC), Color(0xFFCFDEF3), Color(0xFFF8F9FA)),
        isDark = false
    ),
    WallpaperPreset(
        id = "cyber_violet",
        nameVi = "Tím điện tử",
        nameEn = "Cyber Violet",
        colors = listOf(Color(0xFF16002A), Color(0xFF38006B), Color(0xFF6A0DAD)),
        isDark = true
    ),
    WallpaperPreset(
        id = "pure_slate",
        nameVi = "Đá phiến tối giản",
        nameEn = "Minimal Slate",
        colors = listOf(Color(0xFF1E222A), Color(0xFF282C34)),
        isDark = true
    )
)
