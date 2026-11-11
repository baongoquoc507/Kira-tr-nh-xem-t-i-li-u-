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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kiraengine.EditCommand
import com.example.kiraengine.ExcelDocumentEngine
import com.example.kiraengine.ExcelFormulaEvaluator
import com.example.ui.components.GlassSurface
import com.example.ui.theme.ExcelGreen
import com.example.ui.theme.InterFontFamily
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
    var showFillColorMenu by remember { mutableStateOf(false) }
    var showTextColorMenu by remember { mutableStateOf(false) }

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
    val defaultTextColor = if (readingMode == ReadingMode.NIGHT) Color(0xFFF9FAFB) else Color(0xFF111827)

    Column(modifier = modifier.fillMaxSize()) {
        // Authentic Excel Formula Bar
        GlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
            shape = RoundedCornerShape(14.dp),
            elevation = 3.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cell Name Box (e.g. A1, B4)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ExcelGreen.copy(alpha = 0.15f))
                            .border(1.dp, ExcelGreen.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
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
                        fontSize = 15.sp,
                        color = ExcelGreen
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
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = "Đồng ý", tint = ExcelGreen, modifier = Modifier.size(18.dp))
                        }

                        IconButton(
                            onClick = {
                                isEditingCell = false
                                formulaBarText = activeCell.formula ?: activeCell.rawValue
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Hủy", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // Quick Formula Palette Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val formulas = listOf("=SUM(", "=AVERAGE(", "=MIN(", "=MAX(", "=COUNT(", "=IF(", "+", "-", "*", "/")
                    formulas.forEach { f ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(glass.borderColor.copy(alpha = 0.25f))
                                .clickable {
                                    isEditingCell = true
                                    formulaBarText = if (formulaBarText.startsWith("=")) formulaBarText + f.removePrefix("=") else f
                                }
                                .padding(horizontal = 7.dp, vertical = 2.dp)
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
                .padding(horizontal = 14.dp, vertical = 4.dp)
                .shadow(elevation = 6.dp, shape = RoundedCornerShape(10.dp))
                .clip(RoundedCornerShape(10.dp))
                .background(paperColor)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Frozen Header Row (A, B, C...)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .background(headerBgColor)
                        .horizontalScroll(horizontalScrollState)
                ) {
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
                        val colWidth = currentSheet.getColWidth(c).dp

                        Box(
                            modifier = Modifier
                                .width(colWidth)
                                .fillMaxHeight()
                                .border(0.5.dp, gridBorderColor)
                                .background(if (isColSel) ExcelGreen.copy(alpha = 0.2f) else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = colLetter,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isColSel) ExcelGreen else defaultTextColor
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
                                .height(36.dp)
                                .horizontalScroll(horizontalScrollState)
                        ) {
                            // Row Number Header
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
                                    color = if (isRowSel) ExcelGreen else defaultTextColor
                                )
                            }

                            // Data Cells with Real Fill Colors & Formatting
                            for (c in 0 until currentSheet.maxCol) {
                                val isSelected = (r == selectedRow && c == selectedCol)
                                val cell = currentSheet.getCell(r, c)
                                val displayValue = cell.getDisplayValue(currentSheet.cells)
                                val colWidth = currentSheet.getColWidth(c).dp

                                val customBg = cell.style.bgColorHex?.let {
                                    try { Color(android.graphics.Color.parseColor(it)) } catch (_: Exception) { null }
                                } ?: if (cell.bgColor != null) Color(cell.bgColor!!) else Color.Transparent

                                val customTextC = cell.style.textColorHex?.let {
                                    try { Color(android.graphics.Color.parseColor(it)) } catch (_: Exception) { null }
                                } ?: defaultTextColor

                                Box(
                                    modifier = Modifier
                                        .width(colWidth)
                                        .fillMaxHeight()
                                        .background(customBg)
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
                                        fontWeight = if (cell.style.isBold || cell.isBold || r == 0) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        color = customTextC
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Office Excel Formatting Bar (Bold, Fill Color, Font Color, Format)
        GlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 2.dp),
            shape = RoundedCornerShape(14.dp),
            elevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bold Toggle
                IconButton(
                    onClick = {
                        val currentStyle = activeCell.style
                        activeCell.style = currentStyle.copy(isBold = !currentStyle.isBold)
                        activeCell.isBold = activeCell.style.isBold
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatBold,
                        contentDescription = "In đậm ô",
                        tint = if (activeCell.style.isBold || activeCell.isBold) ExcelGreen else glass.contentSecondary
                    )
                }

                // Cell Fill Color
                Box {
                    IconButton(
                        onClick = { showFillColorMenu = true },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(imageVector = Icons.Default.FormatColorFill, contentDescription = "Tô màu ô", tint = ExcelGreen)
                    }

                    DropdownMenu(expanded = showFillColorMenu, onDismissRequest = { showFillColorMenu = false }) {
                        val fills = listOf(
                            Pair("Mặc định", null),
                            Pair("Xanh lá Excel", "#D9EAD3"),
                            Pair("Vàng nhạt", "#FFF2CC"),
                            Pair("Xanh dương nhạt", "#C9DAF8"),
                            Pair("Xám viền", "#EFEFEF"),
                            Pair("Cam nhạt", "#FCE5CD")
                        )
                        fills.forEach { (name, hex) ->
                            DropdownMenuItem(
                                text = { Text(name, fontFamily = InterFontFamily) },
                                onClick = {
                                    activeCell.style = activeCell.style.copy(bgColorHex = hex)
                                    showFillColorMenu = false
                                }
                            )
                        }
                    }
                }

                // Text Color
                Box {
                    IconButton(
                        onClick = { showTextColorMenu = true },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(imageVector = Icons.Default.FormatColorText, contentDescription = "Màu chữ", tint = glass.contentSecondary)
                    }

                    DropdownMenu(expanded = showTextColorMenu, onDismissRequest = { showTextColorMenu = false }) {
                        val colors = listOf(
                            Pair("Tự động", null),
                            Pair("Đỏ", "#DC2626"),
                            Pair("Xanh lá", "#16A34A"),
                            Pair("Xanh dương", "#2563EB"),
                            Pair("Trắng", "#FFFFFF")
                        )
                        colors.forEach { (name, hex) ->
                            DropdownMenuItem(
                                text = { Text(name, fontFamily = InterFontFamily) },
                                onClick = {
                                    activeCell.style = activeCell.style.copy(textColorHex = hex)
                                    showTextColorMenu = false
                                }
                            )
                        }
                    }
                }

                // Currency format ₫
                IconButton(
                    onClick = {
                        val isCurr = activeCell.style.numFmtId == 4
                        activeCell.style = activeCell.style.copy(numFmtId = if (isCurr) 0 else 4)
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Text("₫", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (activeCell.style.numFmtId == 4) ExcelGreen else glass.contentSecondary)
                }

                // Percent format %
                IconButton(
                    onClick = {
                        val isPct = activeCell.style.numFmtId == 9
                        activeCell.style = activeCell.style.copy(numFmtId = if (isPct) 0 else 9)
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Text("%", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (activeCell.style.numFmtId == 9) ExcelGreen else glass.contentSecondary)
                }
            }
        }

        // Multi-Sheet Tabs Bar
        GlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
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
                                .clip(RoundedCornerShape(8.dp))
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

                IconButton(
                    onClick = { showAddSheetDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Thêm trang tính", tint = ExcelGreen, modifier = Modifier.size(18.dp))
                }

                IconButton(
                    onClick = { showRenameSheetDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Đổi tên", tint = glass.contentSecondary, modifier = Modifier.size(16.dp))
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
