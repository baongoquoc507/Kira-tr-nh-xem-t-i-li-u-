package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kiraengine.EditCommand
import com.example.kiraengine.PdfAnnotation
import com.example.kiraengine.PdfDocumentEngine
import com.example.ui.components.GlassSurface
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.IosBlue
import com.example.ui.theme.LocalLiquidGlass
import com.example.ui.viewmodel.AnnotationTool
import com.example.ui.viewmodel.ReadingMode
import com.example.ui.viewmodel.ViewerViewModel

@Composable
fun PdfViewerScreen(
    engine: PdfDocumentEngine,
    viewModel: ViewerViewModel,
    readingMode: ReadingMode,
    modifier: Modifier = Modifier
) {
    val glass = LocalLiquidGlass.current

    val currentPage by viewModel.currentPage.collectAsState()
    val totalPages by viewModel.pageCount.collectAsState()
    val currentBitmap by viewModel.currentPageBitmap.collectAsState()
    val annotationTool by viewModel.annotationTool.collectAsState()

    var scale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    val currentStrokePoints = remember { mutableStateListOf<Pair<Float, Float>>() }
    var selectedColor by remember { mutableStateOf(Color(0xFF007AFF)) }
    var selectedStrokeWidth by remember { mutableFloatStateOf(6f) }

    var showNoteDialog by remember { mutableStateOf(false) }
    var pendingNotePosition by remember { mutableStateOf<Offset?>(null) }
    var noteText by remember { mutableStateOf("") }

    LaunchedEffect(currentPage) {
        scale = 1f
        panOffset = Offset.Zero
        viewModel.renderCurrentPage()
    }

    val paperFilterColor = when (readingMode) {
        ReadingMode.NORMAL -> Color.Transparent
        ReadingMode.NIGHT -> Color(0xFF1E2430).copy(alpha = 0.85f)
        ReadingMode.SEPIA -> Color(0xFFFAF4E8).copy(alpha = 0.35f)
    }

    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFE5E7EB))
                .pointerInput(annotationTool) {
                    if (annotationTool == AnnotationTool.NONE) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.8f, 4.0f)
                            panOffset += pan
                        }
                    } else if (annotationTool == AnnotationTool.PEN || annotationTool == AnnotationTool.HIGHLIGHT) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                currentStrokePoints.clear()
                                currentStrokePoints.add(Pair(offset.x, offset.y))
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                currentStrokePoints.add(Pair(change.position.x, change.position.y))
                            },
                            onDragEnd = {
                                if (currentStrokePoints.size > 1) {
                                    val isHighlight = annotationTool == AnnotationTool.HIGHLIGHT
                                    val ann = PdfAnnotation.InkDrawing(
                                        page = currentPage,
                                        points = currentStrokePoints.toList(),
                                        colorArgb = if (isHighlight) selectedColor.copy(alpha = 0.45f).toArgb() else selectedColor.toArgb(),
                                        strokeWidth = if (isHighlight) selectedStrokeWidth * 3 else selectedStrokeWidth
                                    )
                                    viewModel.applyEdit(EditCommand.PdfAddAnnotation(currentPage, ann))
                                }
                                currentStrokePoints.clear()
                            }
                        )
                    } else if (annotationTool == AnnotationTool.NOTE) {
                        detectTapGestures { offset ->
                            pendingNotePosition = offset
                            noteText = ""
                            showNoteDialog = true
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (currentBitmap != null) {
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = panOffset.x
                            translationY = panOffset.y
                        }
                        .shadow(elevation = 8.dp, shape = RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    Image(
                        bitmap = currentBitmap!!.asImageBitmap(),
                        contentDescription = "Trang ${currentPage + 1}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )

                    if (paperFilterColor != Color.Transparent) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(paperFilterColor)
                        )
                    }

                    if (currentStrokePoints.isNotEmpty()) {
                        Canvas(modifier = Modifier.matchParentSize()) {
                            val path = Path()
                            path.moveTo(currentStrokePoints[0].first, currentStrokePoints[0].second)
                            for (i in 1 until currentStrokePoints.size) {
                                path.lineTo(currentStrokePoints[i].first, currentStrokePoints[i].second)
                            }
                            drawPath(
                                path = path,
                                color = if (annotationTool == AnnotationTool.HIGHLIGHT) selectedColor.copy(alpha = 0.45f) else selectedColor,
                                style = Stroke(
                                    width = if (annotationTool == AnnotationTool.HIGHLIGHT) selectedStrokeWidth * 3 else selectedStrokeWidth,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }
                }
            } else {
                CircularProgressIndicator(color = IosBlue, modifier = Modifier.size(36.dp))
            }

            if (annotationTool != AnnotationTool.NONE) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = when (annotationTool) {
                            AnnotationTool.PEN -> "Chế độ vẽ tay (Pen)"
                            AnnotationTool.HIGHLIGHT -> "Chế độ tô sáng (Highlight)"
                            AnnotationTool.NOTE -> "Chạm vào trang để ghim ghi chú"
                            else -> "Chú thích"
                        },
                        fontFamily = InterFontFamily,
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Bottom Controls
        GlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(18.dp),
            elevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (currentPage > 0) viewModel.setPage(currentPage - 1) },
                        enabled = currentPage > 0,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Trang trước", tint = glass.contentPrimary)
                    }

                    Slider(
                        value = currentPage.toFloat(),
                        onValueChange = { viewModel.setPage(it.toInt()) },
                        valueRange = 0f..(totalPages - 1).coerceAtLeast(0).toFloat(),
                        steps = (totalPages - 2).coerceAtLeast(0),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = IosBlue,
                            activeTrackColor = IosBlue
                        )
                    )

                    IconButton(
                        onClick = { if (currentPage < totalPages - 1) viewModel.setPage(currentPage + 1) },
                        enabled = currentPage < totalPages - 1,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Trang sau", tint = glass.contentPrimary)
                    }

                    Text(
                        text = "${currentPage + 1}/$totalPages",
                        fontFamily = InterFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = glass.contentPrimary,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            viewModel.setAnnotationTool(
                                if (annotationTool == AnnotationTool.PEN) AnnotationTool.NONE else AnnotationTool.PEN
                            )
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .then(if (annotationTool == AnnotationTool.PEN) Modifier.background(IosBlue.copy(alpha = 0.2f)) else Modifier)
                    ) {
                        Icon(imageVector = Icons.Default.Create, contentDescription = "Vẽ tay", tint = if (annotationTool == AnnotationTool.PEN) IosBlue else glass.contentSecondary)
                    }

                    IconButton(
                        onClick = {
                            viewModel.setAnnotationTool(
                                if (annotationTool == AnnotationTool.HIGHLIGHT) AnnotationTool.NONE else AnnotationTool.HIGHLIGHT
                            )
                            selectedColor = Color(0xFFFFCC00)
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .then(if (annotationTool == AnnotationTool.HIGHLIGHT) Modifier.background(Color(0xFFFFCC00).copy(alpha = 0.2f)) else Modifier)
                    ) {
                        Icon(imageVector = Icons.Default.Highlight, contentDescription = "Tô sáng", tint = if (annotationTool == AnnotationTool.HIGHLIGHT) Color(0xFFFF9500) else glass.contentSecondary)
                    }

                    IconButton(
                        onClick = {
                            viewModel.setAnnotationTool(
                                if (annotationTool == AnnotationTool.NOTE) AnnotationTool.NONE else AnnotationTool.NOTE
                            )
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .then(if (annotationTool == AnnotationTool.NOTE) Modifier.background(IosBlue.copy(alpha = 0.2f)) else Modifier)
                    ) {
                        Icon(imageVector = Icons.Default.Comment, contentDescription = "Ghi chú", tint = if (annotationTool == AnnotationTool.NOTE) IosBlue else glass.contentSecondary)
                    }

                    if (annotationTool == AnnotationTool.PEN || annotationTool == AnnotationTool.HIGHLIGHT) {
                        val colors = listOf(Color(0xFF007AFF), Color(0xFFFF3B30), Color(0xFF34C759), Color(0xFFFFCC00), Color(0xFF111827))
                        colors.forEach { c ->
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(
                                        width = if (selectedColor == c) 2.dp else 0.dp,
                                        color = Color.White,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = c }
                            )
                        }
                    }

                    if (annotationTool != AnnotationTool.NONE) {
                        IconButton(
                            onClick = { viewModel.setAnnotationTool(AnnotationTool.NONE) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Đóng", tint = glass.contentSecondary)
                        }
                    }
                }
            }
        }

        if (showNoteDialog) {
            AlertDialog(
                onDismissRequest = { showNoteDialog = false },
                title = { Text("Thêm ghi chú vào trang", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Nội dung ghi chú") },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (noteText.isNotBlank()) {
                                val pos = pendingNotePosition ?: Offset(100f, 200f)
                                val ann = PdfAnnotation.TextNote(
                                    page = currentPage,
                                    x = pos.x,
                                    y = pos.y,
                                    text = noteText.trim(),
                                    colorArgb = android.graphics.Color.BLUE
                                )
                                viewModel.applyEdit(EditCommand.PdfAddAnnotation(currentPage, ann))
                            }
                            showNoteDialog = false
                            viewModel.setAnnotationTool(AnnotationTool.NONE)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IosBlue)
                    ) {
                        Text("Lưu", fontFamily = InterFontFamily)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNoteDialog = false }) {
                        Text("Hủy", fontFamily = InterFontFamily)
                    }
                }
            )
        }
    }
}
