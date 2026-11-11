package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.AbstractCanvasWallpaper
import com.example.ui.components.GlassSurface
import com.example.ui.components.IosGroupedItem
import com.example.ui.components.IosGroupedSection
import com.example.ui.components.IosSegmentedControl
import com.example.ui.components.IosSlider
import com.example.ui.components.IosSwitch
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.IosBlue
import com.example.ui.theme.LocalLiquidGlass
import com.example.ui.theme.WallpaperPresets
import com.example.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val glass = LocalLiquidGlass.current
    val pref by viewModel.preferences.collectAsState()

    // Photo Picker for Custom Gallery Wallpaper
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.setCustomPhotoUri(uri.toString())
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Header
            item {
                Column(modifier = Modifier.padding(bottom = 12.dp)) {
                    Text(
                        text = "KIRA DOCS",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.2.sp,
                        color = glass.accentColor
                    )
                    Text(
                        text = "Cài đặt",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        color = glass.contentPrimary
                    )
                }
            }

            // Section: WALLPAPER PREVIEWS & SELECTION
            item {
                IosGroupedSection(
                    title = "Hình nền khung ứng dụng",
                    footer = "Hình nền áp dụng cho màn hình chính, danh sách và cài đặt. Vùng tài liệu luôn giữ nền trung tính để đảm bảo độ tương phản cao."
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Bộ sưu tập lăng kính Liquid Glass",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = glass.contentPrimary,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        // Curated Presets Carousel
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Bundled Aurora
                            item {
                                WallpaperCardThumb(
                                    title = "Aurora Thủy Tinh",
                                    isSelected = pref.presetId == "aurora" && pref.wallpaperMode == "BUNDLED_IMAGE",
                                    backgroundBrush = Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF007AFF))),
                                    onClick = { viewModel.setBundledWallpaper(R.drawable.img_wallpaper_glass_aurora, "aurora") }
                                )
                            }
                            // Bundled Silk
                            item {
                                WallpaperCardThumb(
                                    title = "Ngọc Trai Mờ",
                                    isSelected = pref.presetId == "frosted_silk" && pref.wallpaperMode == "BUNDLED_IMAGE",
                                    backgroundBrush = Brush.verticalGradient(listOf(Color(0xFFE2E8F0), Color(0xFFF8FAFC))),
                                    onClick = { viewModel.setBundledWallpaper(R.drawable.img_wallpaper_frosted_silk, "frosted_silk") }
                                )
                            }
                            // Presets
                            itemsIndexed(WallpaperPresets) { _, preset ->
                                WallpaperCardThumb(
                                    title = preset.nameVi,
                                    isSelected = pref.presetId == preset.id && pref.wallpaperMode == "GRADIENT",
                                    backgroundBrush = Brush.verticalGradient(preset.colors),
                                    onClick = { viewModel.setWallpaperPreset(preset.id) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Mẫu trừu tượng động (Canvas)",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = glass.contentPrimary,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        // Abstract Canvas Patterns
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val patternNames = listOf("Sóng dải lụa", "Khối cầu kính", "Lưới Cyber", "Lăng kính hoàng hôn")
                            itemsIndexed(patternNames) { idx, name ->
                                val isSel = pref.wallpaperMode == "ABSTRACT_CANVAS" && pref.abstractPatternIndex == idx
                                Box(
                                    modifier = Modifier
                                        .width(88.dp)
                                        .height(118.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .border(
                                            width = if (isSel) 2.5.dp else 1.dp,
                                            color = if (isSel) IosBlue else Color.White.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable { viewModel.setAbstractPattern(idx) }
                                ) {
                                    AbstractCanvasWallpaper(
                                        patternIndex = idx,
                                        isDark = glass.isDarkTheme,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .align(Alignment.BottomCenter)
                                            .background(Color.Black.copy(alpha = 0.6f))
                                            .padding(vertical = 4.dp, horizontal = 2.dp)
                                    ) {
                                        Text(
                                            text = name,
                                            fontFamily = InterFontFamily,
                                            fontSize = 10.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            modifier = Modifier.align(Alignment.Center)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Solid Colors Row
                        Text(
                            text = "Màu trơn tối giản",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = glass.contentPrimary,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        val solidColors = listOf(
                            "#0A0F1D", "#0F172A", "#1E293B", "#18181B",
                            "#022C22", "#1E1B4B", "#31103F", "#F8FAFC", "#F1F5F9"
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            solidColors.forEach { hex ->
                                val c = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { Color.Black }
                                val isSel = pref.wallpaperMode == "SOLID" && pref.solidColorHex.equals(hex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(c)
                                        .border(
                                            width = if (isSel) 2.5.dp else 1.dp,
                                            color = if (isSel) IosBlue else Color.White.copy(alpha = 0.3f),
                                            shape = CircleShape
                                        )
                                        .clickable { viewModel.setSolidColor(hex) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Custom Gallery Photo Button
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = glass.surfaceColor),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, glass.borderColor, RoundedCornerShape(14.dp))
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = glass.accentColor, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Chọn ảnh từ thư viện thiết bị", fontFamily = InterFontFamily, color = glass.contentPrimary)
                        }
                    }
                }
            }

            // Section: LIQUID GLASS TUNING (iOS 27 style)
            item {
                IosGroupedSection(
                    title = "Hiệu ứng kính mờ (Liquid Glass)",
                    footer = "Thanh trượt điều chỉnh độ trong suốt của các tấm thẻ kính. 'Giảm trong suốt' giúp nền đục và tối ưu khả năng đọc trong môi trường chói sáng."
                ) {
                    // Glass Opacity Slider
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Độ trong suốt của kính",
                                fontFamily = InterFontFamily,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = glass.contentPrimary
                            )
                            Text(
                                text = "${(pref.glassOpacity * 100).toInt()}%",
                                fontFamily = InterFontFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = glass.accentColor
                            )
                        }
                        IosSlider(
                            value = pref.glassOpacity,
                            onValueChange = { viewModel.setGlassOpacity(it) },
                            valueRange = 0.2f..0.95f
                        )
                    }

                    // Brightness Slider
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Độ sáng nền (Brightness)",
                                fontFamily = InterFontFamily,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = glass.contentPrimary
                            )
                            Text(
                                text = "${(pref.brightness * 100).toInt()}%",
                                fontFamily = InterFontFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = glass.accentColor
                            )
                        }
                        IosSlider(
                            value = pref.brightness,
                            onValueChange = { viewModel.setBrightness(it) },
                            valueRange = 0.5f..1.5f
                        )
                    }

                    // Reduce Transparency Switch
                    IosGroupedItem(
                        icon = Icons.Default.Opacity,
                        title = "Giảm trong suốt (Đục hơn)",
                        subtitle = "Tăng độ tương phản chữ và tiết kiệm pin",
                        showDivider = false
                    ) {
                        IosSwitch(
                            checked = pref.reduceTransparency,
                            onCheckedChange = { viewModel.setReduceTransparency(it) }
                        )
                    }
                }
            }

            // Section: THEME & DISPLAY
            item {
                IosGroupedSection(title = "Giao diện & Chủ đề") {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(
                            text = "Chế độ hiển thị",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            color = glass.contentPrimary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        val themeOptions = listOf("Theo hệ thống", "Sáng", "Tối")
                        val currentIdx = when (pref.themeMode) {
                            "LIGHT" -> 1
                            "DARK" -> 2
                            else -> 0
                        }
                        IosSegmentedControl(
                            items = themeOptions,
                            selectedIndex = currentIdx,
                            onItemSelected = { idx ->
                                val modeStr = when (idx) {
                                    1 -> "LIGHT"
                                    2 -> "DARK"
                                    else -> "SYSTEM"
                                }
                                viewModel.setThemeMode(modeStr)
                            },
                            labelProvider = { it }
                        )
                    }

                    IosGroupedItem(
                        icon = Icons.Default.Language,
                        title = "Ngôn ngữ",
                        subtitle = "Tiếng Việt (Mặc định)",
                        trailingText = if (pref.language == "vi") "Tiếng Việt" else "English",
                        showChevron = false,
                        showDivider = false
                    )
                }
            }

            // Section: RESET & PRIVACY
            item {
                IosGroupedSection(title = "Hệ thống & Quyền riêng tư") {
                    IosGroupedItem(
                        icon = Icons.Default.Refresh,
                        title = "Đặt lại giao diện mặc định",
                        subtitle = "Khôi phục hình nền và độ trong suốt ban đầu",
                        onClick = { viewModel.resetToDefault() }
                    )

                    IosGroupedItem(
                        icon = Icons.Default.Security,
                        iconTint = Color(0xFF34C759),
                        title = "Quyền riêng tư tuyệt đối",
                        subtitle = "Không quyền mạng, không quảng cáo, chạy 100% offline",
                        showDivider = false
                    )
                }
            }

            // Footer app info
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp, bottom = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Trình xem tài liệu Kira v1.0.0",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = glass.contentSecondary
                    )
                    Text(
                        text = "Thiết kế phong cách iOS 27 Liquid Glass • Bản quyền 2026",
                        fontFamily = InterFontFamily,
                        fontSize = 11.sp,
                        color = glass.contentSecondary.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun WallpaperCardThumb(
    title: String,
    isSelected: Boolean,
    backgroundBrush: Brush,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(90.dp)
    ) {
        Box(
            modifier = Modifier
                .width(88.dp)
                .height(118.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(backgroundBrush)
                .border(
                    width = if (isSelected) 2.5.dp else 1.dp,
                    color = if (isSelected) IosBlue else Color.White.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(14.dp)
                )
                .clickable { onClick() }
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .padding(6.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(IosBlue)
                        .align(Alignment.TopEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✓", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            fontFamily = InterFontFamily,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) IosBlue else Color.Gray,
            maxLines = 1
        )
    }
}
