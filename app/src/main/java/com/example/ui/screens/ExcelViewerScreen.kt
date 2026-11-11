package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kiraengine.EditCommand
import com.example.kiraengine.ExcelDocumentEngine
import com.example.kiraengine.ExcelFormulaEvaluator
import com.example.ui.components.GlassSurface
import com.example.ui.theme.ExcelGreen
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.IosBlue
import com.example.ui.theme.LocalLiquidGlass
import com.example.ui.viewmodel.ReadingMode
import com.example.ui.viewmodel.ViewerViewModel

@Composable
fun ExcelViewerScreen(
    engine: ExcelDocumentEngine,
    viewModel: ViewerViewModel,
    readingMode: ReadingMode,
    modifier: Modifier = Modifier
) {
    val glass = LocalLiquidGlass.current

    var selectedRow by remember { mutableIntStateOf(0) }
    var selectedCol by remember { mutableIntStateOf(0) }
    var formulaBarText by remember { mutableStateOf("") }
    var isEditingCell by remember { mutableStateOf(false) }

    var showAddSheetDialog by remember { mutableStateOf(false) }
    var showRenameSheetDialog by remember { mutableStateOf(false) }

    val currentSheet = engine.currentSheet
    val activeCell = currentSheet.getCell(selectedRow, selectedCol)

    val coordName = remember(selectedRow, selectedCol) {
        val colLetter = ExcelFormulaEvaluator.formatColName(selectedCol)
        "$colLetter${selectedRow + 1}"
    }

    val paperColor = when (readingMode) {
        ReadingMode.NORMAL -> Color.White
        ReadingMode.NIGHT -> Color(0xFF1E2430)
        ReadingMode.SEPIA -> Color(0xFFFAF4E8)
    }

    val gridBorderColor = if (readingMode == ReadingMode.NIGHT) Color(0xFF374151) else Color(0xFFE5E7EB)
    val headerBgColor = if (readingMode == ReadingMode.NIGHT) Color(0xFF111827) else Color(0xFFF3F4F6)
    val textColor = if (readingMode == ReadingMode.NIGHT) Color(0xFFF9FAFB) else Color(0xFF111827)

    Column(modifier = modifier.fillMaxSize()) {
        // Formula Bar
        GlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(14.dp),
            elevation = 3.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cell coordinate badge e.g. A1
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ExcelGreen.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = coordName,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = ExcelGreen
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "fx",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = glass.contentSecondary
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    BasicTextField(
                        value = if (isEditingCell) formulaBarText else (activeCell.formula ?: activeCell.rawValue),
                        onValueChange = {
                            isEditingCell = true
                            formulaBarText = it
                        },
                        textStyle = TextStyle(
                            fontFamily = InterFontFamily,
                            fontSize = 14.sp,
                            color = glass.contentPrimary
                        ),
                        cursorBrush = SolidColor(ExcelGreen),
                        modifier = Modifier.weight(1f)
                    )

                    if (isEditingCell) {
                        IconButton(
                            onClick = {
                                val oldVal = activeCell.rawValue
                                currentSheet.setCell(selectedRow, selectedCol, formulaBarText)
                                viewModel.applyEdit(
                                    EditCommand.ExcelSetCell(
                                        engine.currentSheetIndex,
                                        selectedRow,
                                        selectedCol,
                                        oldVal,
                                        formulaBarText
                                    )
                                )
                                isEditingCell = false
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Áp dụng",
                                tint = ExcelGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Quick Formula Suggestions Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val formulas = listOf("=SUM(", "=AVERAGE(", "=MIN(", "=MAX(", "=COUNT(", "+", "*")
                    formulas.forEach { f ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(glass.borderColor.copy(alpha = 0.2f))
                                .clickable {
                                    isEditingCell = true
                                    formulaBarText = if (formulaBarText.startsWith("=")) formulaBarText + f.removePrefix("=") else f
                                }
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = f,
                                fontFamily = InterFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = glass.contentPrimary
                            )
                        }
                    }
                }
            }
        }

        // Spreadsheet Grid Container
        val horizontalScrollState = rememberScrollState()

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .shadow(elevation = 4.dp, shape = RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(paperColor)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header Row (A, B, C...)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .background(headerBgColor)
                        .horizontalScroll(horizontalScrollState)
                ) {
                    // Corner blank cell (Row header intersection)
                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .fillMaxHeight()
                            .border(0.5.dp, gridBorderColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("◢", fontSize = 10.sp, color = Color.Gray)
                    }

                    for (c in 0 until currentSheet.maxCol) {
                        val colLetter = ExcelFormulaEvaluator.formatColName(c)
                        val isColSel = c == selectedCol
                        Box(
                            modifier = Modifier
                                .width(110.dp)
                                .fillMaxHeight()
                                .border(0.5.dp, gridBorderColor)
                                .background(if (isColSel) ExcelGreen.copy(alpha = 0.15f) else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = colLetter,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isColSel) ExcelGreen else textColor
                            )
                        }
                    }
                }

                // Grid Body
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(currentSheet.maxRow) { r ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .horizontalScroll(horizontalScrollState)
                        ) {
                            // Row Number Header (1, 2, 3...)
                            val isRowSel = r == selectedRow
                            Box(
                                modifier = Modifier
                                    .width(44.dp)
                                    .fillMaxHeight()
                                    .background(headerBgColor)
                                    .border(0.5.dp, gridBorderColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (r + 1).toString(),
                                    fontFamily = InterFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = if (isRowSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isRowSel) ExcelGreen else textColor
                                )
                            }

                            // Row Cells
                            for (c in 0 until currentSheet.maxCol) {
                                val isSelected = (r == selectedRow && c == selectedCol)
                                val cell = currentSheet.getCell(r, c)
                                val displayValue = cell.getDisplayValue(currentSheet.cells)

                                Box(
                                    modifier = Modifier
                                        .width(110.dp)
                                        .fillMaxHeight()
                                        .border(
                                            width = if (isSelected) 2.dp else 0.5.dp,
                                            color = if (isSelected) ExcelGreen else gridBorderColor
                                        )
                                        .clickable {
                                            selectedRow = r
                                            selectedCol = c
                                            formulaBarText = cell.formula ?: cell.rawValue
                                            isEditingCell = false
                                        }
                                        .padding(horizontal = 6.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = displayValue,
                                        fontFamily = InterFontFamily,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        color = textColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Sheet Tabs Bar
        GlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    itemsIndexed(engine.sheets) { idx, sheet ->
                        val isSheetSelected = idx == engine.currentSheetIndex
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSheetSelected) ExcelGreen else Color.Transparent)
                                .clickable {
                                    engine.currentSheetIndex = idx
                                    selectedRow = 0
                                    selectedCol = 0
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = sheet.name,
                                fontFamily = InterFontFamily,
                                fontSize = 12.sp,
                                fontWeight = if (isSheetSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSheetSelected) Color.White else glass.contentPrimary
                            )
                        }
                    }
                }

                // Add Sheet
                IconButton(
                    onClick = { showAddSheetDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Thêm trang tính",
                        tint = ExcelGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Rename Sheet
                IconButton(
                    onClick = { showRenameSheetDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Đổi tên trang tính",
                        tint = glass.contentSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Add Sheet Dialog
        if (showAddSheetDialog) {
            var newSheetName by remember { mutableStateOf("Trang tính ${engine.sheets.size + 1}") }
            AlertDialog(
                onDismissRequest = { showAddSheetDialog = false },
                title = { Text("Thêm trang tính mới", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = newSheetName,
                        onValueChange = { newSheetName = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newSheetName.isNotBlank()) {
                                viewModel.applyEdit(EditCommand.ExcelAddSheet(newSheetName.trim()))
                                engine.currentSheetIndex = engine.sheets.lastIndex
                            }
                            showAddSheetDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ExcelGreen)
                    ) {
                        Text("Thêm", fontFamily = InterFontFamily)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddSheetDialog = false }) {
                        Text("Hủy", fontFamily = InterFontFamily)
                    }
                }
            )
        }

        // Rename Sheet Dialog
        if (showRenameSheetDialog) {
            var renameName by remember { mutableStateOf(currentSheet.name) }
            AlertDialog(
                onDismissRequest = { showRenameSheetDialog = false },
                title = { Text("Đổi tên trang tính", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = renameName,
                        onValueChange = { renameName = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (renameName.isNotBlank()) {
                                viewModel.applyEdit(
                                    EditCommand.ExcelRenameSheet(
                                        engine.currentSheetIndex,
                                        currentSheet.name,
                                        renameName.trim()
                                    )
                                )
                            }
                            showRenameSheetDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ExcelGreen)
                    ) {
                        Text("Lưu", fontFamily = InterFontFamily)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameSheetDialog = false }) {
                        Text("Hủy", fontFamily = InterFontFamily)
                    }
                }
            )
        }
    }
}
