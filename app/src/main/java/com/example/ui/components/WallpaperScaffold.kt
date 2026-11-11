package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.ui.theme.LocalLiquidGlass
import com.example.ui.theme.WallpaperPresets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import kotlin.math.cos
import kotlin.math.sin

enum class WallpaperMode {
    SOLID,
    GRADIENT,
    ABSTRACT_CANVAS,
    BUNDLED_IMAGE,
    CUSTOM_PHOTO
}

data class WallpaperConfig(
    val mode: WallpaperMode = WallpaperMode.BUNDLED_IMAGE,
    val presetId: String = "aurora",
    val solidColor: Color = Color(0xFF0F172A),
    val gradientColors: List<Color> = WallpaperPresets[0].colors,
    val abstractPatternIndex: Int = 0,
    val bundledDrawableRes: Int = R.drawable.img_wallpaper_glass_aurora,
    val customPhotoUri: String? = null,
    val blurRadius: Float = 0f,
    val brightness: Float = 1.0f
)

@Composable
fun WallpaperScaffold(
    config: WallpaperConfig,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val glass = LocalLiquidGlass.current
    val context = LocalContext.current

    Box(modifier = modifier.fillMaxSize()) {
        // Base Wallpaper Layer
        Crossfade(targetState = config, label = "wallpaper_crossfade") { currentConfig ->
            when (currentConfig.mode) {
                WallpaperMode.SOLID -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(currentConfig.solidColor)
                    )
                }
                WallpaperMode.GRADIENT -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = currentConfig.gradientColors
                                )
                            )
                    )
                }
                WallpaperMode.BUNDLED_IMAGE -> {
                    Image(
                        painter = painterResource(id = currentConfig.bundledDrawableRes),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                WallpaperMode.ABSTRACT_CANVAS -> {
                    AbstractCanvasWallpaper(
                        patternIndex = currentConfig.abstractPatternIndex,
                        isDark = glass.isDarkTheme,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                WallpaperMode.CUSTOM_PHOTO -> {
                    CustomPhotoWallpaper(
                        uriString = currentConfig.customPhotoUri,
                        context = context,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Brightness & Dimming Tint Layer
        val overlayColor = when {
            config.brightness < 1.0f -> {
                Color.Black.copy(alpha = (1.0f - config.brightness).coerceIn(0f, 0.85f))
            }
            config.brightness > 1.0f -> {
                Color.White.copy(alpha = ((config.brightness - 1.0f) * 0.5f).coerceIn(0f, 0.5f))
            }
            else -> Color.Transparent
        }

        if (overlayColor != Color.Transparent) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(overlayColor)
            )
        }

        // Foreground App Content
        content()
    }
}

/**
 * Procedural Abstract Wallpapers (Liquid ribbons, Glass waves, Cosmic prisms)
 */
@Composable
fun AbstractCanvasWallpaper(
    patternIndex: Int,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val baseBg = if (isDark) Color(0xFF0A0F1D) else Color(0xFFF1F5F9)

    Canvas(modifier = modifier.background(baseBg)) {
        val width = size.width
        val height = size.height

        when (patternIndex % 4) {
            0 -> {
                // Liquid Glass Aurora Waves
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = if (isDark) {
                            listOf(Color(0xFF050B14), Color(0xFF0F172A), Color(0xFF1E1B4B))
                        } else {
                            listOf(Color(0xFFE0E7FF), Color(0xFFF8FAFC), Color(0xFFE2E8F0))
                        }
                    )
                )

                val wavePath = Path().apply {
                    moveTo(0f, height * 0.35f)
                    cubicTo(
                        width * 0.3f, height * 0.2f,
                        width * 0.7f, height * 0.55f,
                        width, height * 0.4f
                    )
                    lineTo(width, height)
                    lineTo(0f, height)
                    close()
                }

                drawPath(
                    path = wavePath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF007AFF).copy(alpha = 0.35f),
                            Color(0xFF5856D6).copy(alpha = 0.3f),
                            Color(0xFFAF52DE).copy(alpha = 0.25f)
                        )
                    )
                )

                val wavePath2 = Path().apply {
                    moveTo(0f, height * 0.6f)
                    cubicTo(
                        width * 0.4f, height * 0.75f,
                        width * 0.6f, height * 0.45f,
                        width, height * 0.65f
                    )
                    lineTo(width, height)
                    lineTo(0f, height)
                    close()
                }

                drawPath(
                    path = wavePath2,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF32ADE6).copy(alpha = 0.25f),
                            Color(0xFF007AFF).copy(alpha = 0.3f)
                        ),
                        start = Offset(0f, height * 0.5f),
                        end = Offset(width, height)
                    )
                )
            }
            1 -> {
                // Frosted Glass Orbs
                drawRect(
                    brush = Brush.radialGradient(
                        colors = if (isDark) listOf(Color(0xFF1E293B), Color(0xFF090D16))
                        else listOf(Color(0xFFF8FAFC), Color(0xFFCBD5E1)),
                        center = Offset(width * 0.5f, height * 0.4f),
                        radius = width
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF007AFF).copy(alpha = 0.45f), Color.Transparent),
                        center = Offset(width * 0.25f, height * 0.25f),
                        radius = width * 0.55f
                    ),
                    radius = width * 0.55f,
                    center = Offset(width * 0.25f, height * 0.25f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFAF52DE).copy(alpha = 0.4f), Color.Transparent),
                        center = Offset(width * 0.75f, height * 0.65f),
                        radius = width * 0.6f
                    ),
                    radius = width * 0.6f,
                    center = Offset(width * 0.75f, height * 0.65f)
                )
            }
            2 -> {
                // Cyber Mesh & Light Caustics
                drawRect(
                    brush = Brush.linearGradient(
                        colors = if (isDark) listOf(Color(0xFF030712), Color(0xFF111827), Color(0xFF064E3B))
                        else listOf(Color(0xFFECFDF5), Color(0xFFF1F5F9), Color(0xFFE0F2FE))
                    )
                )
                for (i in 0..5) {
                    val progress = i / 5f
                    drawCircle(
                        color = Color(0xFF34D399).copy(alpha = 0.08f),
                        radius = width * (0.3f + progress * 0.4f),
                        center = Offset(width * (0.2f + progress * 0.6f), height * progress)
                    )
                }
            }
            else -> {
                // Minimal Sunset Refraction
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = if (isDark) listOf(Color(0xFF1C1917), Color(0xFF451A03), Color(0xFF0C0A09))
                        else listOf(Color(0xFFFFF7ED), Color(0xFFFFEDD5), Color(0xFFF8FAFC))
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFF9500).copy(alpha = 0.35f), Color.Transparent),
                        center = Offset(width * 0.5f, height * 0.8f),
                        radius = width * 0.7f
                    ),
                    radius = width * 0.7f,
                    center = Offset(width * 0.5f, height * 0.8f)
                )
            }
        }
    }
}

/**
 * Loads custom gallery image securely via SAF Uri and BitmapFactory
 */
@Composable
fun CustomPhotoWallpaper(
    uriString: String?,
    context: Context,
    modifier: Modifier = Modifier
) {
    var bitmap by remember(uriString) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(uriString) {
        if (!uriString.isNullOrEmpty()) {
            withContext(Dispatchers.IO) {
                try {
                    val uri = Uri.parse(uriString)
                    val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = 2 // downsample for performance
                    }
                    bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                    inputStream?.close()
                } catch (e: Exception) {
                    bitmap = null
                }
            }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        // Fallback default
        Image(
            painter = painterResource(id = R.drawable.img_wallpaper_glass_aurora),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}
