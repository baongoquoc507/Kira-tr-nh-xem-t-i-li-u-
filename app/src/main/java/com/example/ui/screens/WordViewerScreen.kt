package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignJustify
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Pageview
import androidx.compose.material.icons.filled.StrikethroughS
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kiraengine.EditCommand
import com.example.kiraengine.WordDocumentEngine
import com.example.kiraengine.WordItem
import com.example.kiraengine.WordParagraph
import com.example.kiraengine.WordRun
import com.example.kiraengine.WordTable
import com.example.ui.components.GlassSurface
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.IosBlue
import com.example.ui.theme.LocalLiquidGlass
import com.example.ui.theme.WordBlue
import com.example.ui.viewmodel.ReadingMode
import com.example.ui.viewmodel.ViewerViewModel

enum class WordViewMode {
    PRINT_LAYOUT, // A4 Sheets with margins and page breaks like MS Word
    MOBILE_FLOW   // Continuous mobile friendly flow
}

@Composable
fun WordViewerScreen(
    engine: WordDocumentEngine,
    viewModel: ViewerViewModel,
    readingMode: ReadingMode,
    modifier: Modifier = Modifier
) {
    val glass = LocalLiquidGlass.current

    var viewMode by remember { mutableStateOf(WordViewMode.PRINT_LAYOUT) }
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var selectedItemIndex by remember { mutableIntStateOf(0) }

    var showInsertTableDialog by remember { mutableStateOf(false) }
    var showColorMenu by remember { mutableStateOf(false) }
    var showHighlightMenu by remember { mutableStateOf(false) }

    val paperColor = when (readingMode) {
        ReadingMode.NORMAL -> Color.White
        ReadingMode.NIGHT -> Color(0xFF1E2430)
        ReadingMode.SEPIA -> Color(0xFFFAF4E8)
    }

    val textColor = when (readingMode) {
        ReadingMode.NORMAL -> Color(0xFF111827)
        ReadingMode.NIGHT -> Color(0xFFF3F4F6)
        ReadingMode.SEPIA -> Color(0xFF3E2723)
    }

    val pageCount = remember(engine.items.size) { engine.pageCount() }

    Column(modifier = modifier.fillMaxSize()) {
        // Mode & Zoom Bar
        GlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // View Mode Toggle (A4 Print Layout vs Continuous Flow)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(glass.borderColor.copy(alpha = 0.2f))
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (viewMode == WordViewMode.PRINT_LAYOUT) WordBlue else Color.Transparent)
                            .clickable { viewMode = WordViewMode.PRINT_LAYOUT }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Trang A4 (In ấn)",
                            fontFamily = InterFontFamily,
                            fontSize = 11.sp,
                            fontWeight = if (viewMode == WordViewMode.PRINT_LAYOUT) FontWeight.Bold else FontWeight.Normal,
                            color = if (viewMode == WordViewMode.PRINT_LAYOUT) Color.White else glass.contentPrimary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (viewMode == WordViewMode.MOBILE_FLOW) WordBlue else Color.Transparent)
                            .clickable { viewMode = WordViewMode.MOBILE_FLOW }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Cuộn liên tục",
                            fontFamily = InterFontFamily,
                            fontSize = 11.sp,
                            fontWeight = if (viewMode == WordViewMode.MOBILE_FLOW) FontWeight.Bold else FontWeight.Normal,
                            color = if (viewMode == WordViewMode.MOBILE_FLOW) Color.White else glass.contentPrimary
                        )
                    }
                }

                // Zoom Level Controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { zoomScale = (zoomScale - 0.15f).coerceIn(0.6f, 2.0f) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ZoomOut, contentDescription = "Thu nhỏ", tint = glass.contentSecondary, modifier = Modifier.size(16.dp))
                    }
                    Text(
                        text = "${(zoomScale * 100).toInt()}%",
                        fontFamily = InterFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = glass.contentPrimary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    IconButton(
                        onClick = { zoomScale = (zoomScale + 0.15f).coerceIn(0.6f, 2.0f) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ZoomIn, contentDescription = "Phóng to", tint = glass.contentSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Main Document Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(if (viewMode == WordViewMode.PRINT_LAYOUT) Color(0xFFE5E7EB) else paperColor)
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        zoomScale = (zoomScale * zoom).coerceIn(0.6f, 2.5f)
                    }
                }
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = zoomScale
                        scaleY = zoomScale
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (viewMode == WordViewMode.PRINT_LAYOUT) {
                    // Authentic A4 Pages Layout (210mm x 297mm proportion = 1 : 1.414)
                    val itemsPerPage = (engine.items.size / pageCount.coerceAtLeast(1)).coerceAtLeast(1)

                    items(pageCount) { pageIdx ->
                        val startIdx = (pageIdx * itemsPerPage).coerceIn(0, engine.items.size)
                        val endIdx = ((pageIdx + 1) * itemsPerPage).coerceIn(startIdx, engine.items.size)
                        val pageItems = engine.items.subList(startIdx, endIdx)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .aspectRatio(1f / 1.414f)
                                .shadow(elevation = 8.dp, shape = RoundedCornerShape(4.dp))
                                .clip(RoundedCornerShape(4.dp))
                                .background(paperColor)
                                .padding(horizontal = 24.dp, vertical = 28.dp)
                        ) {
                            // Page Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = engine.metadata?.name ?: "Tài liệu",
                                    fontFamily = InterFontFamily,
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = "Trang ${pageIdx + 1}",
                                    fontFamily = InterFontFamily,
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }

                            // Items on this page
                            Column(modifier = Modifier.weight(1f)) {
                                pageItems.forEachIndexed { subIdx, item ->
                                    val globalIdx = startIdx + subIdx
                                    RenderWordItem(
                                        item = item,
                                        isSelected = globalIdx == selectedItemIndex,
                                        textColor = textColor,
                                        onSelect = { selectedItemIndex = globalIdx },
                                        onTextChange = { newT ->
                                            if (item is WordItem.Paragraph) {
                                                val oldT = item.p.fullText
                                                item.p.runs.clear()
                                                item.p.runs.add(WordRun(text = newT, fontSizePt = if (item.p.headingLevel > 0) 18f else 12f, isBold = item.p.headingLevel > 0))
                                                viewModel.applyEdit(EditCommand.WordUpdateText(globalIdx, oldT, newT))
                                            }
                                        }
                                    )
                                }
                            }

                            // Page Footer
                            Text(
                                text = "— ${pageIdx + 1} —",
                                fontFamily = InterFontFamily,
                                fontSize = 10.sp,
                                color = Color.LightGray,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                } else {
                    // Mobile Flow Layout
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp)
                        ) {
                            engine.items.forEachIndexed { idx, item ->
                                RenderWordItem(
                                    item = item,
                                    isSelected = idx == selectedItemIndex,
                                    textColor = textColor,
                                    onSelect = { selectedItemIndex = idx },
                                    onTextChange = { newT ->
                                        if (item is WordItem.Paragraph) {
                                            val oldT = item.p.fullText
                                            item.p.runs.clear()
                                            item.p.runs.add(WordRun(text = newT, fontSizePt = if (item.p.headingLevel > 0) 18f else 12f, isBold = item.p.headingLevel > 0))
                                            viewModel.applyEdit(EditCommand.WordUpdateText(idx, oldT, newT))
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Office Formatting Toolbar (Like Word Ribbon)
        GlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
            shape = RoundedCornerShape(18.dp),
            elevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                val currentItem = engine.items.getOrNull(selectedItemIndex)
                val currentP = (currentItem as? WordItem.Paragraph)?.p

                // Row 1: Formatting Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Bold
                    IconButton(
                        onClick = {
                            if (currentP != null) {
                                val newB = !currentP.isBold
                                currentP.runs.forEach { it.isBold = newB }
                                viewModel.applyEdit(EditCommand.WordToggleFormat(selectedItemIndex, bold = newB))
                            }
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatBold,
                            contentDescription = "In đậm",
                            tint = if (currentP?.isBold == true) WordBlue else glass.contentSecondary
                        )
                    }

                    // Italic
                    IconButton(
                        onClick = {
                            if (currentP != null) {
                                val newI = !currentP.isItalic
                                currentP.runs.forEach { it.isItalic = newI }
                                viewModel.applyEdit(EditCommand.WordToggleFormat(selectedItemIndex, italic = newI))
                            }
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatItalic,
                            contentDescription = "In nghiêng",
                            tint = if (currentP?.isItalic == true) WordBlue else glass.contentSecondary
                        )
                    }

                    // Underline
                    IconButton(
                        onClick = {
                            if (currentP != null) {
                                val newU = !currentP.isUnderline
                                currentP.runs.forEach { it.isUnderline = newU }
                                viewModel.applyEdit(EditCommand.WordToggleFormat(selectedItemIndex, underline = newU))
                            }
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatUnderlined,
                            contentDescription = "Gạch chân",
                            tint = if (currentP?.isUnderline == true) WordBlue else glass.contentSecondary
                        )
                    }

                    // Strikethrough
                    IconButton(
                        onClick = {
                            if (currentP != null) {
                                val newS = !(currentP.runs.firstOrNull()?.isStrike ?: false)
                                currentP.runs.forEach { it.isStrike = newS }
                            }
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.StrikethroughS,
                            contentDescription = "Gạch ngang",
                            tint = if (currentP?.runs?.any { it.isStrike } == true) WordBlue else glass.contentSecondary
                        )
                    }

                    // Heading Level Toggle (Body -> H1 -> H2 -> H3)
                    IconButton(
                        onClick = {
                            if (currentP != null) {
                                currentP.headingLevel = (currentP.headingLevel + 1) % 4
                            }
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Title,
                            contentDescription = "Tiêu đề",
                            tint = if ((currentP?.headingLevel ?: 0) > 0) WordBlue else glass.contentSecondary
                        )
                    }

                    // Text Color Picker
                    Box {
                        IconButton(
                            onClick = { showColorMenu = true },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(imageVector = Icons.Default.FormatColorText, contentDescription = "Màu chữ", tint = WordBlue)
                        }

                        DropdownMenu(expanded = showColorMenu, onDismissRequest = { showColorMenu = false }) {
                            val colors = listOf(
                                Pair("Mặc định", null),
                                Pair("Đỏ", "#DC2626"),
                                Pair("Xanh dương", "#2563EB"),
                                Pair("Xanh lá", "#16A34A"),
                                Pair("Cam", "#EA580C"),
                                Pair("Tím", "#9333EA")
                            )
                            colors.forEach { (name, hex) ->
                                DropdownMenuItem(
                                    text = { Text(name, fontFamily = InterFontFamily) },
                                    onClick = {
                                        currentP?.runs?.forEach { it.colorHex = hex }
                                        showColorMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Highlight Color Picker
                    Box {
                        IconButton(
                            onClick = { showHighlightMenu = true },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Highlight, contentDescription = "Tô sáng", tint = Color(0xFFFF9500))
                        }

                        DropdownMenu(expanded = showHighlightMenu, onDismissRequest = { showHighlightMenu = false }) {
                            val highlights = listOf(
                                Pair("Không màu", null),
                                Pair("Vàng", "#FFF59D"),
                                Pair("Xanh lá", "#A5D6A7"),
                                Pair("Xanh lơ", "#80DEEA"),
                                Pair("Hồng", "#F48FB1")
                            )
                            highlights.forEach { (name, hex) ->
                                DropdownMenuItem(
                                    text = { Text(name, fontFamily = InterFontFamily) },
                                    onClick = {
                                        currentP?.runs?.forEach { it.highlightHex = hex }
                                        showHighlightMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Row 2: Alignment, Lists, Table, and Add
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Align Left
                    IconButton(
                        onClick = { currentP?.alignment = "left" },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.FormatAlignLeft, contentDescription = "Căn trái", tint = if (currentP?.alignment == "left") WordBlue else glass.contentSecondary)
                    }

                    // Align Center
                    IconButton(
                        onClick = { currentP?.alignment = "center" },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.FormatAlignCenter, contentDescription = "Căn giữa", tint = if (currentP?.alignment == "center") WordBlue else glass.contentSecondary)
                    }

                    // Align Right
                    IconButton(
                        onClick = { currentP?.alignment = "right" },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.FormatAlignRight, contentDescription = "Căn phải", tint = if (currentP?.alignment == "right") WordBlue else glass.contentSecondary)
                    }

                    // Align Justify
                    IconButton(
                        onClick = { currentP?.alignment = "justify" },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.FormatAlignJustify, contentDescription = "Căn đều", tint = if (currentP?.alignment == "justify") WordBlue else glass.contentSecondary)
                    }

                    // Bullet List
                    IconButton(
                        onClick = { currentP?.isBullet = !(currentP?.isBullet ?: false) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.FormatListBulleted, contentDescription = "Danh sách chấm", tint = if (currentP?.isBullet == true) WordBlue else glass.contentSecondary)
                    }

                    // Insert Table
                    IconButton(
                        onClick = { showInsertTableDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.TableChart, contentDescription = "Chèn bảng", tint = WordBlue)
                    }

                    // Add Paragraph
                    IconButton(
                        onClick = {
                            viewModel.applyEdit(EditCommand.WordAddParagraph(selectedItemIndex + 1, "Đoạn văn mới..."))
                            selectedItemIndex += 1
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Thêm đoạn", tint = WordBlue)
                    }
                }
            }
        }

        // Insert Table Dialog
        if (showInsertTableDialog) {
            var rowsStr by remember { mutableStateOf("3") }
            var colsStr by remember { mutableStateOf("3") }

            AlertDialog(
                onDismissRequest = { showInsertTableDialog = false },
                title = { Text("Chèn bảng Word mới", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = rowsStr,
                            onValueChange = { rowsStr = it },
                            label = { Text("Số hàng (1-10)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = colsStr,
                            onValueChange = { colsStr = it },
                            label = { Text("Số cột (1-6)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val r = rowsStr.toIntOrNull()?.coerceIn(1, 10) ?: 3
                            val c = colsStr.toIntOrNull()?.coerceIn(1, 6) ?: 3
                            viewModel.applyEdit(EditCommand.WordInsertTable(selectedItemIndex + 1, r, c))
                            showInsertTableDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WordBlue)
                    ) {
                        Text("Chèn bảng", fontFamily = InterFontFamily)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showInsertTableDialog = false }) {
                        Text("Hủy", fontFamily = InterFontFamily)
                    }
                }
            )
        }
    }
}

@Composable
fun RenderWordItem(
    item: WordItem,
    isSelected: Boolean,
    textColor: Color,
    onSelect: () -> Unit,
    onTextChange: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(4.dp))
            .then(
                if (isSelected) Modifier.background(WordBlue.copy(alpha = 0.08f))
                else Modifier
            )
            .clickable { onSelect() }
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        when (item) {
            is WordItem.Paragraph -> {
                val p = item.p
                val textAlign = when (p.alignment) {
                    "center" -> TextAlign.Center
                    "right" -> TextAlign.Right
                    "justify" -> TextAlign.Justify
                    else -> TextAlign.Start
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    if (p.isBullet) {
                        Text(
                            text = "•  ",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            fontSize = 14.sp
                        )
                    } else if (p.isNumbered) {
                        Text(
                            text = "${p.bulletNumber}.  ",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            fontSize = 14.sp
                        )
                    }

                    // Rich Run-Formatted text display & editing
                    val annotatedString = buildAnnotatedString {
                        if (p.runs.isEmpty()) {
                            append(p.fullText)
                        } else {
                            for (run in p.runs) {
                                val spanStyle = SpanStyle(
                                    fontWeight = if (run.isBold || p.headingLevel > 0) FontWeight.Bold else FontWeight.Normal,
                                    fontStyle = if (run.isItalic) FontStyle.Italic else FontStyle.Normal,
                                    textDecoration = when {
                                        run.isUnderline && run.isStrike -> TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
                                        run.isUnderline -> TextDecoration.Underline
                                        run.isStrike -> TextDecoration.LineThrough
                                        else -> TextDecoration.None
                                    },
                                    color = run.colorHex?.let { try { Color(android.graphics.Color.parseColor(it)) } catch (_: Exception) { null } } ?: textColor,
                                    background = run.highlightHex?.let { try { Color(android.graphics.Color.parseColor(it)) } catch (_: Exception) { Color.Transparent } } ?: Color.Transparent,
                                    fontSize = when (p.headingLevel) {
                                        1 -> 22.sp
                                        2 -> 18.sp
                                        3 -> 15.sp
                                        else -> (run.fontSizePt * 1.15f).sp
                                    }
                                )
                                withStyle(spanStyle) {
                                    append(run.text)
                                }
                            }
                        }
                    }

                    BasicTextField(
                        value = p.fullText,
                        onValueChange = onTextChange,
                        textStyle = TextStyle(
                            fontFamily = InterFontFamily,
                            fontSize = when (p.headingLevel) {
                                1 -> 22.sp
                                2 -> 18.sp
                                3 -> 15.sp
                                else -> 14.sp
                            },
                            fontWeight = if (p.isBold || p.headingLevel > 0) FontWeight.Bold else FontWeight.Normal,
                            textAlign = textAlign,
                            color = textColor,
                            lineHeight = 22.sp
                        ),
                        cursorBrush = SolidColor(WordBlue),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            is WordItem.Table -> {
                TableBlockView(table = item.t, textColor = textColor)
            }
            is WordItem.Image -> {
                if (item.img.bitmap != null) {
                    Image(
                        bitmap = item.img.bitmap.asImageBitmap(),
                        contentDescription = item.img.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )
                }
            }
        }
    }
}

@Composable
fun TableBlockView(
    table: WordTable,
    textColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFD1D5DB), RoundedCornerShape(4.dp))
            .clip(RoundedCornerShape(4.dp))
    ) {
        table.rows.forEachIndexed { r, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (r == 0) Color(0xFFF3F4F6) else Color.Transparent)
            ) {
                row.forEachIndexed { c, cell ->
                    val cellBg = cell.bgHex?.let { try { Color(android.graphics.Color.parseColor(it)) } catch (_: Exception) { null } }
                        ?: if (r == 0) Color(0xFFF3F4F6) else Color.Transparent

                    Box(
                        modifier = Modifier
                            .weight(cell.colSpan.toFloat())
                            .background(cellBg)
                            .border(0.5.dp, Color(0xFFE5E7EB))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = cell.cellText,
                            fontFamily = InterFontFamily,
                            fontSize = 13.sp,
                            fontWeight = if (r == 0) FontWeight.Bold else FontWeight.Normal,
                            color = textColor
                        )
                    }
                }
            }
        }
    }
}
