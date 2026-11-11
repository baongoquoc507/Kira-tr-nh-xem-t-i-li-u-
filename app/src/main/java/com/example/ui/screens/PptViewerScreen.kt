package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kiraengine.EditCommand
import com.example.kiraengine.PptDocumentEngine
import com.example.ui.components.GlassSurface
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.LocalLiquidGlass
import com.example.ui.theme.PptOrange
import com.example.ui.viewmodel.ViewerViewModel

@Composable
fun PptViewerScreen(
    engine: PptDocumentEngine,
    viewModel: ViewerViewModel,
    modifier: Modifier = Modifier
) {
    val glass = LocalLiquidGlass.current
    var selectedSlideIdx by remember { mutableIntStateOf(0) }
    var isSlideshowActive by remember { mutableStateOf(false) }
    var showEditSlideDialog by remember { mutableStateOf(false) }
    var showAddSlideDialog by remember { mutableStateOf(false) }

    val currentSlide = engine.slides.getOrNull(selectedSlideIdx)
    val pageBitmap by viewModel.currentPageBitmap.collectAsState()

    LaunchedEffect(selectedSlideIdx) {
        viewModel.setPage(selectedSlideIdx)
    }

    if (isSlideshowActive) {
        // Fullscreen Slideshow Presentation Mode
        FullScreenSlideshowView(
            engine = engine,
            startIndex = selectedSlideIdx,
            onExit = { isSlideshowActive = false }
        )
    } else {
        Column(modifier = modifier.fillMaxSize()) {
            // Main Slide Canvas Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                GlassSurface(
                    shape = RoundedCornerShape(16.dp),
                    elevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                ) {
                    Crossfade(targetState = selectedSlideIdx, label = "slide_crossfade") { idx ->
                        val slide = engine.slides.getOrNull(idx)
                        if (slide != null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White)
                                    .padding(24.dp)
                            ) {
                                // Slide top accent line
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .background(PptOrange)
                                )
                                Spacer(modifier = Modifier.height(14.dp))

                                // Title
                                Text(
                                    text = slide.title,
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = Color(0xFF111827)
                                )

                                slide.subtitle?.let { sub ->
                                    Text(
                                        text = sub,
                                        fontFamily = InterFontFamily,
                                        fontSize = 13.sp,
                                        color = Color(0xFF6B7280),
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Bullet Points
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    slide.bulletPoints.forEach { pt ->
                                        Row(verticalAlignment = Alignment.Top) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(top = 6.dp, end = 8.dp)
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(PptOrange)
                                            )
                                            Text(
                                                text = pt,
                                                fontFamily = InterFontFamily,
                                                fontSize = 14.sp,
                                                color = Color(0xFF1F2937),
                                                lineHeight = 18.sp
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                // Slide index footer
                                Text(
                                    text = "Slide ${idx + 1} / ${engine.slides.size}",
                                    fontFamily = InterFontFamily,
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.align(Alignment.End)
                                )
                            }
                        }
                    }
                }
            }

            // Slide Control Toolbar (Play Slideshow, Edit Slide, Add Slide)
            GlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Play Slideshow
                    Button(
                        onClick = { isSlideshowActive = true },
                        colors = ButtonDefaults.buttonColors(containerColor = PptOrange),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Trình chiếu", fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }

                    Row {
                        // Edit Slide
                        IconButton(onClick = { showEditSlideDialog = true }) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Sửa slide", tint = glass.contentSecondary)
                        }

                        // Add Slide
                        IconButton(onClick = { showAddSlideDialog = true }) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Thêm slide", tint = PptOrange)
                        }
                    }
                }
            }

            // Bottom Slide Thumbnail Strip
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(engine.slides) { idx, slide ->
                    val isSelected = idx == selectedSlideIdx
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) PptOrange else Color.LightGray.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedSlideIdx = idx }
                            .padding(6.dp)
                    ) {
                        Column {
                            Text(
                                text = "${idx + 1}. ${slide.title}",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                maxLines = 1,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = slide.bulletPoints.firstOrNull() ?: "",
                                fontFamily = InterFontFamily,
                                fontSize = 8.sp,
                                maxLines = 1,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }

        // Edit Slide Dialog
        if (showEditSlideDialog && currentSlide != null) {
            var title by remember { mutableStateOf(currentSlide.title) }
            var bulletsText by remember { mutableStateOf(currentSlide.bulletPoints.joinToString("\n")) }

            AlertDialog(
                onDismissRequest = { showEditSlideDialog = false },
                title = { Text("Chỉnh sửa Slide ${selectedSlideIdx + 1}", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Tiêu đề slide") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = bulletsText,
                            onValueChange = { bulletsText = it },
                            label = { Text("Các ý chính (mỗi dòng một ý)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val bulletsList = bulletsText.split("\n").filter { it.isNotBlank() }
                            viewModel.applyEdit(
                                EditCommand.PptUpdateSlide(
                                    selectedSlideIdx,
                                    currentSlide.title,
                                    title,
                                    currentSlide.bulletPoints.toList(),
                                    bulletsList
                                )
                            )
                            showEditSlideDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PptOrange)
                    ) {
                        Text("Lưu", fontFamily = InterFontFamily)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditSlideDialog = false }) {
                        Text("Hủy", fontFamily = InterFontFamily)
                    }
                }
            )
        }

        // Add Slide Dialog
        if (showAddSlideDialog) {
            var newTitle by remember { mutableStateOf("Slide mới") }
            var newBulletsText by remember { mutableStateOf("Điểm chính 1\nĐiểm chính 2") }

            AlertDialog(
                onDismissRequest = { showAddSlideDialog = false },
                title = { Text("Thêm slide mới", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newTitle,
                            onValueChange = { newTitle = it },
                            label = { Text("Tiêu đề") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = newBulletsText,
                            onValueChange = { newBulletsText = it },
                            label = { Text("Nội dung (mỗi dòng 1 ý)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val bullets = newBulletsText.split("\n").filter { it.isNotBlank() }
                            val newIdx = selectedSlideIdx + 1
                            viewModel.applyEdit(EditCommand.PptAddSlide(newIdx, newTitle, bullets))
                            selectedSlideIdx = newIdx
                            showAddSlideDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PptOrange)
                    ) {
                        Text("Thêm", fontFamily = InterFontFamily)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddSlideDialog = false }) {
                        Text("Hủy", fontFamily = InterFontFamily)
                    }
                }
            )
        }
    }
}

/**
 * Fullscreen Slideshow Presentation View
 */
@Composable
fun FullScreenSlideshowView(
    engine: PptDocumentEngine,
    startIndex: Int,
    onExit: () -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(startIndex) }
    var laserPoint by remember { mutableStateOf<Offset?>(null) }

    val currentSlide = engine.slides.getOrNull(currentIndex)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        // If tap right half -> next slide, left half -> prev slide
                        if (offset.x > size.width * 0.7f) {
                            if (currentIndex < engine.slides.size - 1) currentIndex++
                        } else if (offset.x < size.width * 0.3f) {
                            if (currentIndex > 0) currentIndex--
                        } else {
                            laserPoint = offset
                        }
                    }
                )
            }
    ) {
        // Slide Content centered
        if (currentSlide != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .aspectRatio(16f / 9f)
                    .align(Alignment.Center)
                    .shadow(16.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(32.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(PptOrange))
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = currentSlide.title,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        color = Color(0xFF111827)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    currentSlide.bulletPoints.forEach { pt ->
                        Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                            Box(modifier = Modifier.padding(top = 8.dp, end = 12.dp).size(8.dp).clip(CircleShape).background(PptOrange))
                            Text(text = pt, fontFamily = InterFontFamily, fontSize = 18.sp, color = Color(0xFF1F2937))
                        }
                    }
                }
            }
        }

        // Virtual Laser Pointer Dot
        laserPoint?.let { pos ->
            Box(
                modifier = Modifier
                    .offset { IntOffset(pos.x.toInt() - 10, pos.y.toInt() - 10) }
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.Red.copy(alpha = 0.85f))
                    .border(2.dp, Color.White, CircleShape)
            )
        }

        // Top presentation toolbar (Slide Counter & Close)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${currentIndex + 1} / ${engine.slides.size}",
                    fontFamily = InterFontFamily,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }

            IconButton(
                onClick = onExit,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
            ) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Thoát trình chiếu", tint = Color.White)
            }
        }
    }
}
