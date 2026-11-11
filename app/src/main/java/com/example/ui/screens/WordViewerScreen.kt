package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Undo
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
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kiraengine.EditCommand
import com.example.kiraengine.WordBlock
import com.example.kiraengine.WordDocumentEngine
import com.example.ui.components.GlassSurface
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.IosBlue
import com.example.ui.theme.LocalLiquidGlass
import com.example.ui.viewmodel.ReadingMode
import com.example.ui.viewmodel.ViewerViewModel

@Composable
fun WordViewerScreen(
    engine: WordDocumentEngine,
    viewModel: ViewerViewModel,
    readingMode: ReadingMode,
    modifier: Modifier = Modifier
) {
    val glass = LocalLiquidGlass.current
    var selectedIndex by remember { mutableIntStateOf(0) }
    var showInsertTableDialog by remember { mutableStateOf(false) }

    // Paper background color according to reading mode
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

    Column(modifier = modifier.fillMaxSize()) {
        // Document Canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .shadow(elevation = 6.dp, shape = RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(paperColor)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                itemsIndexed(engine.blocks, key = { index, block -> block.id }) { index, block ->
                    val isSelected = index == selectedIndex

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .then(
                                if (isSelected) Modifier.background(IosBlue.copy(alpha = 0.08f))
                                else Modifier
                            )
                            .clickable { selectedIndex = index }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        if (block.isTable) {
                            WordTableView(
                                block = block,
                                textColor = textColor,
                                onCellEdit = { r, c, valStr ->
                                    if (r in block.tableData.indices && c in block.tableData[r].indices) {
                                        block.tableData[r][c] = valStr
                                    }
                                }
                            )
                        } else {
                            val fontStyle = if (block.isItalic) FontStyle.Italic else FontStyle.Normal
                            val fontWeight = when {
                                block.isBold || block.headingLevel > 0 -> FontWeight.Bold
                                else -> FontWeight.Normal
                            }
                            val textDecoration = if (block.isUnderline) TextDecoration.Underline else TextDecoration.None
                            val fontSize = when (block.headingLevel) {
                                1 -> 24.sp
                                2 -> 20.sp
                                3 -> 17.sp
                                else -> 15.sp
                            }
                            val textAlign = when (block.alignment) {
                                "center" -> TextAlign.Center
                                "right" -> TextAlign.Right
                                else -> TextAlign.Start
                            }

                            Row(verticalAlignment = Alignment.Top) {
                                if (block.isBullet) {
                                    Text(
                                        text = "•  ",
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor,
                                        fontSize = fontSize
                                    )
                                }
                                BasicTextField(
                                    value = block.text,
                                    onValueChange = { newText ->
                                        val oldText = block.text
                                        block.text = newText
                                        viewModel.applyEdit(EditCommand.WordUpdateText(index, oldText, newText))
                                    },
                                    textStyle = TextStyle(
                                        fontFamily = InterFontFamily,
                                        fontSize = fontSize,
                                        fontWeight = fontWeight,
                                        fontStyle = fontStyle,
                                        textDecoration = textDecoration,
                                        textAlign = textAlign,
                                        color = textColor,
                                        lineHeight = (fontSize.value * 1.45f).sp
                                    ),
                                    cursorBrush = SolidColor(IosBlue),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating Word Format Toolbar
        GlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = 12.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val currentBlock = engine.blocks.getOrNull(selectedIndex)

                // Bold
                IconButton(
                    onClick = {
                        if (currentBlock != null) {
                            val newB = !currentBlock.isBold
                            currentBlock.isBold = newB
                            viewModel.applyEdit(EditCommand.WordToggleFormat(selectedIndex, bold = newB))
                        }
                    },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatBold,
                        contentDescription = "In đậm",
                        tint = if (currentBlock?.isBold == true) IosBlue else glass.contentSecondary
                    )
                }

                // Italic
                IconButton(
                    onClick = {
                        if (currentBlock != null) {
                            val newI = !currentBlock.isItalic
                            currentBlock.isItalic = newI
                            viewModel.applyEdit(EditCommand.WordToggleFormat(selectedIndex, italic = newI))
                        }
                    },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatItalic,
                        contentDescription = "In nghiêng",
                        tint = if (currentBlock?.isItalic == true) IosBlue else glass.contentSecondary
                    )
                }

                // Underline
                IconButton(
                    onClick = {
                        if (currentBlock != null) {
                            val newU = !currentBlock.isUnderline
                            currentBlock.isUnderline = newU
                            viewModel.applyEdit(EditCommand.WordToggleFormat(selectedIndex, underline = newU))
                        }
                    },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatUnderlined,
                        contentDescription = "Gạch chân",
                        tint = if (currentBlock?.isUnderline == true) IosBlue else glass.contentSecondary
                    )
                }

                // Heading / Title Toggle
                IconButton(
                    onClick = {
                        if (currentBlock != null) {
                            currentBlock.headingLevel = if (currentBlock.headingLevel == 0) 1 else 0
                        }
                    },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Title,
                        contentDescription = "Tiêu đề",
                        tint = if ((currentBlock?.headingLevel ?: 0) > 0) IosBlue else glass.contentSecondary
                    )
                }

                // Bullet List Toggle
                IconButton(
                    onClick = {
                        if (currentBlock != null) {
                            currentBlock.isBullet = !currentBlock.isBullet
                        }
                    },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatListBulleted,
                        contentDescription = "Danh sách",
                        tint = if (currentBlock?.isBullet == true) IosBlue else glass.contentSecondary
                    )
                }

                // Insert Table
                IconButton(
                    onClick = { showInsertTableDialog = true },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TableChart,
                        contentDescription = "Chèn bảng",
                        tint = glass.contentSecondary
                    )
                }

                // Add Paragraph
                IconButton(
                    onClick = {
                        viewModel.applyEdit(
                            EditCommand.WordAddParagraph(selectedIndex + 1, "Đoạn văn mới...")
                        )
                        selectedIndex += 1
                    },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Thêm đoạn",
                        tint = IosBlue
                    )
                }
            }
        }

        // Insert Table Dialog
        if (showInsertTableDialog) {
            var rowsStr by remember { mutableStateOf("3") }
            var colsStr by remember { mutableStateOf("3") }

            AlertDialog(
                onDismissRequest = { showInsertTableDialog = false },
                title = { Text("Chèn bảng mới", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold) },
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
                            viewModel.applyEdit(EditCommand.WordInsertTable(selectedIndex + 1, r, c))
                            showInsertTableDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IosBlue)
                    ) {
                        Text("Chèn", fontFamily = InterFontFamily)
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
fun WordTableView(
    block: WordBlock,
    textColor: Color,
    onCellEdit: (Int, Int, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.LightGray, RoundedCornerShape(6.dp))
            .clip(RoundedCornerShape(6.dp))
    ) {
        block.tableData.forEachIndexed { r, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (r == 0) Color.LightGray.copy(alpha = 0.25f) else Color.Transparent)
            ) {
                row.forEachIndexed { c, cell ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .border(0.5.dp, Color.LightGray.copy(alpha = 0.6f))
                            .padding(6.dp)
                    ) {
                        BasicTextField(
                            value = cell,
                            onValueChange = { onCellEdit(r, c, it) },
                            textStyle = TextStyle(
                                fontFamily = InterFontFamily,
                                fontSize = 13.sp,
                                fontWeight = if (r == 0) FontWeight.Bold else FontWeight.Normal,
                                color = textColor
                            ),
                            cursorBrush = SolidColor(IosBlue),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
