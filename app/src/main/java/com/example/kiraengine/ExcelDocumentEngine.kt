package com.example.kiraengine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.StringReader
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class ExcelCell(
    val row: Int,
    val col: Int,
    var rawValue: String = "",
    var formula: String? = null,
    var isBold: Boolean = false,
    var bgColor: Int? = null
) {
    fun getDisplayValue(allCells: Map<String, ExcelCell>): String {
        val form = formula
        if (!form.isNullOrEmpty() && form.startsWith("=")) {
            return ExcelFormulaEvaluator.evaluate(form, allCells)
        }
        return rawValue
    }
}

data class ExcelSheet(
    var name: String,
    val cells: MutableMap<String, ExcelCell> = mutableMapOf(), // Key: "R,C"
    var maxRow: Int = 20,
    var maxCol: Int = 8,
    var frozenRows: Int = 1,
    var frozenCols: Int = 1
) {
    fun getCell(r: Int, c: Int): ExcelCell {
        return cells.getOrPut("$r,$c") { ExcelCell(row = r, col = c) }
    }

    fun setCell(r: Int, c: Int, value: String) {
        val cell = getCell(r, c)
        if (value.startsWith("=")) {
            cell.formula = value
            cell.rawValue = value
        } else {
            cell.formula = null
            cell.rawValue = value
        }
        if (r >= maxRow) maxRow = r + 10
        if (c >= maxCol) maxCol = c + 5
    }
}

object ExcelFormulaEvaluator {
    fun evaluate(formula: String, cells: Map<String, ExcelCell>): String {
        return try {
            val upper = formula.trim().uppercase()
            when {
                upper.startsWith("=SUM(") -> {
                    val range = upper.substring(5, upper.lastIndexOf(")"))
                    val nums = extractRangeNumbers(range, cells)
                    nums.sum().let { if (it % 1.0 == 0.0) it.toLong().toString() else String.format("%.2f", it) }
                }
                upper.startsWith("=AVERAGE(") -> {
                    val range = upper.substring(9, upper.lastIndexOf(")"))
                    val nums = extractRangeNumbers(range, cells)
                    if (nums.isEmpty()) "0" else {
                        val avg = nums.average()
                        String.format("%.2f", avg)
                    }
                }
                upper.startsWith("=MIN(") -> {
                    val range = upper.substring(5, upper.lastIndexOf(")"))
                    val nums = extractRangeNumbers(range, cells)
                    nums.minOrNull()?.toString() ?: "0"
                }
                upper.startsWith("=MAX(") -> {
                    val range = upper.substring(5, upper.lastIndexOf(")"))
                    val nums = extractRangeNumbers(range, cells)
                    nums.maxOrNull()?.toString() ?: "0"
                }
                upper.startsWith("=COUNT(") -> {
                    val range = upper.substring(7, upper.lastIndexOf(")"))
                    extractRangeNumbers(range, cells).size.toString()
                }
                else -> {
                    // Try basic expression (e.g. =A1+B1)
                    val expr = upper.removePrefix("=")
                    if (expr.contains("+")) {
                        val parts = expr.split("+")
                        val sum = parts.sumOf { getCellValueAsDouble(it.trim(), cells) }
                        if (sum % 1.0 == 0.0) sum.toLong().toString() else String.format("%.2f", sum)
                    } else if (expr.contains("*")) {
                        val parts = expr.split("*")
                        val prod = parts.fold(1.0) { acc, s -> acc * getCellValueAsDouble(s.trim(), cells) }
                        if (prod % 1.0 == 0.0) prod.toLong().toString() else String.format("%.2f", prod)
                    } else {
                        formula
                    }
                }
            }
        } catch (_: Exception) {
            "#ERROR!"
        }
    }

    private fun extractRangeNumbers(rangeStr: String, cells: Map<String, ExcelCell>): List<Double> {
        val list = mutableListOf<Double>()
        if (rangeStr.contains(":")) {
            val parts = rangeStr.split(":")
            val (startCol, startRow) = parseCoord(parts[0].trim())
            val (endCol, endRow) = parseCoord(parts[1].trim())

            for (r in minOf(startRow, endRow)..maxOf(startRow, endRow)) {
                for (c in minOf(startCol, endCol)..maxOf(startCol, endCol)) {
                    val raw = cells["$r,$c"]?.rawValue
                    raw?.toDoubleOrNull()?.let { list.add(it) }
                }
            }
        }
        return list
    }

    private fun getCellValueAsDouble(coordStr: String, cells: Map<String, ExcelCell>): Double {
        coordStr.toDoubleOrNull()?.let { return it }
        val (c, r) = parseCoord(coordStr)
        return cells["$r,$c"]?.rawValue?.toDoubleOrNull() ?: 0.0
    }

    fun parseCoord(coord: String): Pair<Int, Int> {
        val colLetters = coord.filter { it.isLetter() }
        val rowDigits = coord.filter { it.isDigit() }
        var col = 0
        for (ch in colLetters) {
            col = col * 26 + (ch - 'A')
        }
        val row = (rowDigits.toIntOrNull() ?: 1) - 1
        return Pair(col, row)
    }

    fun formatColName(c: Int): String {
        var col = c
        val sb = StringBuilder()
        while (col >= 0) {
            sb.insert(0, ('A'.code + (col % 26)).toChar())
            col = col / 26 - 1
        }
        return sb.toString()
    }
}

class ExcelDocumentEngine : DocumentEngine {
    private var _metadata: DocumentMetadata? = null
    override val metadata: DocumentMetadata? get() = _metadata

    val sheets = mutableListOf<ExcelSheet>()
    var currentSheetIndex: Int = 0

    private val undoStack = mutableListOf<EditCommand>()
    private val redoStack = mutableListOf<EditCommand>()

    val currentSheet: ExcelSheet
        get() = if (sheets.isNotEmpty()) sheets[currentSheetIndex.coerceIn(0, sheets.lastIndex)] else ExcelSheet("Trang tính 1")

    override suspend fun open(context: Context, uri: Uri): Result<DocumentMetadata> = withContext(Dispatchers.IO) {
        try {
            close()
            var fileName = "Spreadsheet.xlsx"
            var fileSize = 0L

            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: fileName
                    if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                }
            }

            val isCsv = fileName.endsWith(".csv", ignoreCase = true)
            val isXls = fileName.endsWith(".xls", ignoreCase = true)

            if (isCsv) {
                parseCsv(context, uri)
            } else if (isXls) {
                parseLegacyXls(context, uri)
            } else {
                parseXlsx(context, uri)
            }

            if (sheets.isEmpty()) {
                val defaultSheet = ExcelSheet("Sheet1")
                // Populate initial template
                defaultSheet.setCell(0, 0, "STT")
                defaultSheet.setCell(0, 1, "Mục")
                defaultSheet.setCell(0, 2, "Số lượng")
                defaultSheet.setCell(0, 3, "Đơn giá")
                defaultSheet.setCell(0, 4, "Tổng cộng")
                sheets.add(defaultSheet)
            }

            val meta = DocumentMetadata(
                uri = uri,
                name = fileName,
                type = DocumentType.EXCEL,
                sizeBytes = fileSize,
                pageCount = sheets.size,
                isReadOnly = isXls
            )
            _metadata = meta
            Result.success(meta)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseCsv(context: Context, uri: Uri) {
        val sheet = ExcelSheet("CSV Data")
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).useLines { lines ->
                var r = 0
                for (line in lines) {
                    val cols = line.split(",", ";", "\t")
                    for ((c, cell) in cols.withIndex()) {
                        sheet.setCell(r, c, cell.trim().trim('"'))
                    }
                    r++
                }
                sheet.maxRow = maxOf(r + 5, 20)
            }
        }
        sheets.add(sheet)
    }

    private fun parseLegacyXls(context: Context, uri: Uri) {
        // Stream text scan fallback for older BIFF8 XLS
        val sheet = ExcelSheet("Trang tính")
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val bytes = stream.readBytes()
            val text = String(bytes, Charsets.ISO_8859_1).filter { it.code in 32..126 || it == '\t' || it == '\n' }
            val lines = text.split("\n").filter { it.isNotBlank() }
            for ((r, line) in lines.take(50).withIndex()) {
                val cols = line.split("\t", ",")
                for ((c, col) in cols.take(10).withIndex()) {
                    sheet.setCell(r, c, col.trim())
                }
            }
        }
        sheets.add(sheet)
    }

    private fun parseXlsx(context: Context, uri: Uri) {
        try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return
            val zis = ZipInputStream(ByteArrayInputStream(bytes))
            var entry: ZipEntry? = zis.nextEntry

            val sharedStrings = mutableListOf<String>()
            val sheetXmls = mutableMapOf<String, String>()

            while (entry != null) {
                if (entry.name == "xl/sharedStrings.xml") {
                    sharedStrings.addAll(parseSharedStrings(readEntryToString(zis)))
                } else if (entry.name.startsWith("xl/worksheets/sheet") && entry.name.endsWith(".xml")) {
                    sheetXmls[entry.name] = readEntryToString(zis)
                }
                entry = zis.nextEntry
            }
            zis.close()

            if (sheetXmls.isNotEmpty()) {
                sheetXmls.toSortedMap().forEach { (path, xml) ->
                    val sheetIndex = path.filter { it.isDigit() }.toIntOrNull() ?: 1
                    val sheet = parseWorksheetXml("Trang tính $sheetIndex", xml, sharedStrings)
                    sheets.add(sheet)
                }
            }
        } catch (_: Exception) {
            // If failed, create fresh sheet
        }
    }

    private fun readEntryToString(zis: ZipInputStream): String {
        val baos = ByteArrayOutputStream()
        val buf = ByteArray(1024)
        var len: Int
        while (zis.read(buf).also { len = it } > 0) {
            baos.write(buf, 0, len)
        }
        return baos.toString("UTF-8")
    }

    private fun parseSharedStrings(xml: String): List<String> {
        val list = mutableListOf<String>()
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name?.lowercase() == "t") {
                list.add(parser.nextText())
            }
            event = parser.next()
        }
        return list
    }

    private fun parseWorksheetXml(name: String, xml: String, sharedStrings: List<String>): ExcelSheet {
        val sheet = ExcelSheet(name)
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))

        var event = parser.eventType
        var currentCellRef = ""
        var isSharedString = false
        var currentFormula: String? = null

        while (event != XmlPullParser.END_DOCUMENT) {
            val tag = parser.name?.lowercase() ?: ""
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (tag) {
                        "c" -> {
                            currentCellRef = parser.getAttributeValue(null, "r") ?: ""
                            val type = parser.getAttributeValue(null, "t")
                            isSharedString = (type == "s")
                            currentFormula = null
                        }
                        "f" -> {
                            currentFormula = "=" + parser.nextText()
                        }
                        "v" -> {
                            val value = parser.nextText()
                            if (currentCellRef.isNotEmpty()) {
                                val (c, r) = ExcelFormulaEvaluator.parseCoord(currentCellRef)
                                val finalVal = if (isSharedString) {
                                    val idx = value.toIntOrNull() ?: -1
                                    if (idx in sharedStrings.indices) sharedStrings[idx] else value
                                } else {
                                    value
                                }
                                sheet.setCell(r, c, finalVal)
                                if (currentFormula != null) {
                                    sheet.getCell(r, c).formula = currentFormula
                                }
                            }
                        }
                    }
                }
            }
            event = parser.next()
        }
        return sheet
    }

    override fun pageCount(): Int = sheets.size.coerceAtLeast(1)

    override suspend fun renderPage(index: Int, targetWidth: Int, targetHeight: Int): Bitmap? = withContext(Dispatchers.Default) {
        val width = if (targetWidth > 0) targetWidth else 1080
        val height = if (targetHeight > 0) targetHeight else 1920

        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(AndroidColor.WHITE)

        val targetSheet = sheets.getOrNull(index) ?: return@withContext bmp

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 28f
            color = AndroidColor.BLACK
        }
        val gridPaint = Paint().apply {
            color = AndroidColor.LTGRAY
            strokeWidth = 1.5f
            style = Paint.Style.STROKE
        }
        val headerPaint = Paint().apply {
            color = AndroidColor.parseColor("#F3F4F6")
            style = Paint.Style.FILL
        }

        val rowH = 65f
        val colW = 160f
        val startY = 80f
        val startX = 80f

        // Draw headers and cells
        for (r in 0 until minOf(targetSheet.maxRow, 25)) {
            val y = startY + r * rowH
            for (c in 0 until minOf(targetSheet.maxCol, 6)) {
                val x = startX + c * colW
                if (r == 0) {
                    canvas.drawRect(x, y, x + colW, y + rowH, headerPaint)
                }
                canvas.drawRect(x, y, x + colW, y + rowH, gridPaint)
                val cellVal = targetSheet.getCell(r, c).getDisplayValue(targetSheet.cells)
                if (cellVal.isNotEmpty()) {
                    canvas.drawText(cellVal.take(10), x + 10f, y + rowH * 0.65f, paint)
                }
            }
        }
        bmp
    }

    override fun applyEdit(command: EditCommand): Boolean {
        return when (command) {
            is EditCommand.ExcelSetCell -> {
                if (command.sheetIndex in sheets.indices) {
                    val s = sheets[command.sheetIndex]
                    s.setCell(command.row, command.col, command.newValue)
                    undoStack.add(command)
                    redoStack.clear()
                    true
                } else false
            }
            is EditCommand.ExcelAddSheet -> {
                sheets.add(ExcelSheet(command.name))
                undoStack.add(command)
                redoStack.clear()
                true
            }
            is EditCommand.ExcelRenameSheet -> {
                if (command.index in sheets.indices) {
                    sheets[command.index].name = command.newName
                    undoStack.add(command)
                    redoStack.clear()
                    true
                } else false
            }
            else -> false
        }
    }

    override fun undo(): Boolean {
        if (undoStack.isEmpty()) return false
        val cmd = undoStack.removeAt(undoStack.lastIndex)
        when (cmd) {
            is EditCommand.ExcelSetCell -> {
                if (cmd.sheetIndex in sheets.indices) {
                    sheets[cmd.sheetIndex].setCell(cmd.row, cmd.col, cmd.oldValue)
                    redoStack.add(cmd)
                }
            }
            is EditCommand.ExcelAddSheet -> {
                if (sheets.size > 1) {
                    sheets.removeAt(sheets.lastIndex)
                    redoStack.add(cmd)
                }
            }
            is EditCommand.ExcelRenameSheet -> {
                if (cmd.index in sheets.indices) {
                    sheets[cmd.index].name = cmd.oldName
                    redoStack.add(cmd)
                }
            }
            else -> return false
        }
        return true
    }

    override fun redo(): Boolean {
        if (redoStack.isEmpty()) return false
        val cmd = redoStack.removeAt(redoStack.lastIndex)
        when (cmd) {
            is EditCommand.ExcelSetCell -> {
                if (cmd.sheetIndex in sheets.indices) {
                    sheets[cmd.sheetIndex].setCell(cmd.row, cmd.col, cmd.newValue)
                    undoStack.add(cmd)
                }
            }
            is EditCommand.ExcelAddSheet -> {
                sheets.add(ExcelSheet(cmd.name))
                undoStack.add(cmd)
            }
            is EditCommand.ExcelRenameSheet -> {
                if (cmd.index in sheets.indices) {
                    sheets[cmd.index].name = cmd.newName
                    undoStack.add(cmd)
                }
            }
            else -> return false
        }
        return true
    }

    override fun canUndo(): Boolean = undoStack.isNotEmpty()
    override fun canRedo(): Boolean = redoStack.isNotEmpty()

    override suspend fun save(context: Context, targetUri: Uri?): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val uri = targetUri ?: _metadata?.uri ?: return@withContext Result.failure(IllegalStateException("No target URI"))
            val targetFile = File(context.cacheDir, "saved_${System.currentTimeMillis()}.csv")

            // Save active sheet as clean CSV or XLSX
            FileOutputStream(targetFile).use { fos ->
                val sheet = currentSheet
                for (r in 0 until sheet.maxRow) {
                    val rowVals = mutableListOf<String>()
                    var hasData = false
                    for (c in 0 until sheet.maxCol) {
                        val v = sheet.getCell(r, c).rawValue
                        if (v.isNotEmpty()) hasData = true
                        rowVals.add("\"${v.replace("\"", "\"\"")}\"")
                    }
                    if (hasData) {
                        fos.write((rowVals.joinToString(",") + "\n").toByteArray(Charsets.UTF_8))
                    }
                }
            }

            context.contentResolver.openOutputStream(uri)?.use { out ->
                targetFile.inputStream().use { it.copyTo(out) }
            }
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun exportToPdf(context: Context, targetFile: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            val doc = PdfDocument()
            for (i in 0 until sheets.size) {
                val pageInfo = PdfDocument.PageInfo.Builder(1080, 1920, i + 1).create()
                val page = doc.startPage(pageInfo)
                val bmp = renderPage(i, 1080, 1920)
                if (bmp != null) {
                    page.canvas.drawBitmap(bmp, 0f, 0f, null)
                }
                doc.finishPage(page)
            }
            FileOutputStream(targetFile).use { doc.writeTo(it) }
            doc.close()
            Result.success(targetFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun close() {
        sheets.clear()
        undoStack.clear()
        redoStack.clear()
        currentSheetIndex = 0
    }
}
