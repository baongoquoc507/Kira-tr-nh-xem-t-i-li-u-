package com.example.ui.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.AppDatabase
import com.example.data.AppPreferencesEntity
import com.example.ui.components.WallpaperConfig
import com.example.ui.components.WallpaperMode
import com.example.ui.theme.WallpaperPresets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SettingsViewModel(private val database: AppDatabase) : ViewModel() {
    private val _preferences = MutableStateFlow(AppPreferencesEntity())
    val preferences = _preferences.asStateFlow()

    private val _wallpaperConfig = MutableStateFlow(WallpaperConfig())
    val wallpaperConfig = _wallpaperConfig.asStateFlow()

    init {
        viewModelScope.launch {
            database.preferencesDao().getPreferences().collectLatest { pref ->
                if (pref != null) {
                    _preferences.value = pref
                    _wallpaperConfig.value = convertToWallpaperConfig(pref)
                } else {
                    // Seed default
                    val defaultPref = AppPreferencesEntity()
                    database.preferencesDao().savePreferences(defaultPref)
                }
            }
        }
    }

    private fun convertToWallpaperConfig(entity: AppPreferencesEntity): WallpaperConfig {
        val mode = try {
            WallpaperMode.valueOf(entity.wallpaperMode)
        } catch (_: Exception) {
            WallpaperMode.BUNDLED_IMAGE
        }

        val solid = try {
            Color(android.graphics.Color.parseColor(entity.solidColorHex))
        } catch (_: Exception) {
            Color(0xFF0F172A)
        }

        val preset = WallpaperPresets.find { it.id == entity.presetId } ?: WallpaperPresets[0]

        val bundledRes = when (entity.presetId) {
            "frosted_silk" -> R.drawable.img_wallpaper_frosted_silk
            else -> R.drawable.img_wallpaper_glass_aurora
        }

        return WallpaperConfig(
            mode = mode,
            presetId = entity.presetId,
            solidColor = solid,
            gradientColors = preset.colors,
            abstractPatternIndex = entity.abstractPatternIndex,
            bundledDrawableRes = bundledRes,
            customPhotoUri = entity.customPhotoUri,
            blurRadius = entity.blurRadius,
            brightness = entity.brightness
        )
    }

    fun setWallpaperPreset(presetId: String) {
        val preset = WallpaperPresets.find { it.id == presetId } ?: return
        val current = _preferences.value
        val updated = current.copy(
            wallpaperMode = "GRADIENT",
            presetId = preset.id
        )
        save(updated)
    }

    fun setBundledWallpaper(resId: Int, idString: String) {
        val current = _preferences.value
        val updated = current.copy(
            wallpaperMode = "BUNDLED_IMAGE",
            presetId = idString
        )
        save(updated)
    }

    fun setSolidColor(hex: String) {
        val current = _preferences.value
        val updated = current.copy(
            wallpaperMode = "SOLID",
            solidColorHex = hex
        )
        save(updated)
    }

    fun setAbstractPattern(index: Int) {
        val current = _preferences.value
        val updated = current.copy(
            wallpaperMode = "ABSTRACT_CANVAS",
            abstractPatternIndex = index
        )
        save(updated)
    }

    fun setCustomPhotoUri(uriString: String) {
        val current = _preferences.value
        val updated = current.copy(
            wallpaperMode = "CUSTOM_PHOTO",
            customPhotoUri = uriString
        )
        save(updated)
    }

    fun setBlurRadius(blur: Float) {
        val current = _preferences.value
        val updated = current.copy(blurRadius = blur)
        save(updated)
    }

    fun setBrightness(brightness: Float) {
        val current = _preferences.value
        val updated = current.copy(brightness = brightness)
        save(updated)
    }

    fun setGlassOpacity(opacity: Float) {
        val current = _preferences.value
        val updated = current.copy(glassOpacity = opacity.coerceIn(0.15f, 0.95f))
        save(updated)
    }

    fun setReduceTransparency(reduce: Boolean) {
        val current = _preferences.value
        val updated = current.copy(reduceTransparency = reduce)
        save(updated)
    }

    fun setThemeMode(mode: String) {
        val current = _preferences.value
        val updated = current.copy(themeMode = mode)
        save(updated)
    }

    fun setLanguage(lang: String) {
        val current = _preferences.value
        val updated = current.copy(language = lang)
        save(updated)
    }

    fun resetToDefault() {
        val defaultPref = AppPreferencesEntity()
        save(defaultPref)
    }

    private fun save(entity: AppPreferencesEntity) {
        _preferences.value = entity
        _wallpaperConfig.value = convertToWallpaperConfig(entity)
        viewModelScope.launch {
            database.preferencesDao().savePreferences(entity)
        }
    }
}
