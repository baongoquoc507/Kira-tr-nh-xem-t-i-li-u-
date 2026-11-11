package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_documents")
data class RecentDocumentEntity(
    @PrimaryKey val uri: String,
    val name: String,
    val type: String, // WORD, EXCEL, POWERPOINT, PDF, TEXT
    val sizeBytes: Long,
    val lastOpenedTimestamp: Long = System.currentTimeMillis(),
    val lastPage: Int = 0,
    val isFavorite: Boolean = false,
    val readingProgress: Float = 0f
)

@Entity(tableName = "app_preferences")
data class AppPreferencesEntity(
    @PrimaryKey val id: Int = 1,
    val wallpaperMode: String = "BUNDLED_IMAGE",
    val presetId: String = "aurora",
    val solidColorHex: String = "#0F172A",
    val abstractPatternIndex: Int = 0,
    val customPhotoUri: String? = null,
    val blurRadius: Float = 0f,
    val brightness: Float = 1.0f,
    val glassOpacity: Float = 0.72f,
    val reduceTransparency: Boolean = false,
    val themeMode: String = "SYSTEM", // SYSTEM, LIGHT, DARK
    val language: String = "vi" // vi, en
)
