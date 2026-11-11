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

data class ExcelCellStyle(
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val textColorHex: String? = null,
    val bgColorHex: String? = null,
    val fontSizePt: Float = 11f,
    val alignment: String = "left", // left, center, right
    val numFmtId: Int = 0
)

data class ExcelCell(
    val row: Int,
    val col: Int,
    var rawValue: String = "",
    var formula: String? = null,
    var isBold: Boolean = false,
    var bgColor: Int? = null,
    var style: ExcelCellStyle = ExcelCellStyle()
) {
    fun getDisplayValue(allCells: Map<String, ExcelCell>): String {
        val form = formula
        if (!form.isNullOrEmpty() && form.startsWith("=")) {
            return ExcelFormulaEvaluator.evaluate(form, allCells)
        }
        return formatNumberIfNeeded(rawValue, style.numFmtId)
    }

    private fun formatNumberIfNeeded(raw: String, numFmt: Int): String {
        val d = raw.toDoubleOrNull() ?: return raw
        return when (numFmt) {
            9, 10 -> String.format("%.1f%%", d * 100) // Percentage
            2, 4 -> String.format("%,.2f", d) // Currency / Decimal
            else -> if (d % 1.0 == 0.0 && d < 1e12 && d > -1e12) d.toLong().toString() else raw
        }
    }
}

data class ExcelSheet(
    var name: String,
    val cells: MutableMap<String, ExcelCell> = mutableMapOf(), // Key: "R,C"
    var maxRow: Int = 30,
    var maxCol: Int = 10,
    var frozenRows: Int = 1,
    var frozenCols: Int = 1,
    val colWidths: MutableMap<Int, Float> = mutableMapOf(),
    val mergedRegions: MutableList<Pair<Pair<Int, Int>, Pair<Int, Int>>> = mutableListOf() // ((r1, c1), (r2, c2))
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
        if (r >= maxRow) maxRow = r + 15
        if (c >= maxCol) maxCol = c + 5
    }

    fun getColWidth(col: Int): Float {
        return colWidths[col] ?: 115f
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
                    val s = nums.sum()
                    if (s % 1.0 == 0.0) s.toLong().toString() else String.format("%.2f", s)
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
                    val m = nums.minOrNull() ?: 0.0
                    if (m % 1.0 == 0.0) m.toLong().toString() else String.format("%.2f", m)
                }
                upper.startsWith("=MAX(") -> {
                    val range = upper.substring(5, upper.lastIndexOf(")"))
                    val nums = extractRangeNumbers(range, cells)
                    val m = nums.maxOrNull() ?: 0.0
                    if (m % 1.0 == 0.0) m.toLong().toString() else String.format("%.2f", m)
                }
                upper.startsWith("=COUNT(") || upper.startsWith("=COUNTA(") -> {
                    val range = upper.substring(upper.indexOf("(") + 1, upper.lastIndexOf(")"))
                    extractRangeNumbers(range, cells).size.toString()
                }
                upper.startsWith("=PRODUCT(") -> {
                    val range = upper.substring(9, upper.lastIndexOf(")"))
                    val nums = extractRangeNumbers(range, cells)
                    val p = if (nums.isEmpty()) 0.0 else nums.fold(1.0) { acc, d -> acc * d }
                    if (p % 1.0 == 0.0) p.toLong().toString() else String.format("%.2f", p)
                }
                upper.startsWith("=IF(") -> {
                    // Basic IF(A1>0, trueVal, falseVal)
                    val inner = formula.substring(4, formula.lastIndexOf(")"))
                    val parts = inner.split(",")
                    if (parts.size >= 2) {
                        val cond = parts[0].trim()
                        val isTrue = evaluateCondition(cond, cells)
                        if (isTrue) parts[1].trim().trim('"') else if (parts.size > 2) parts[2].trim().trim('"') else "FALSE"
                    } else formula
                }
                else -> {
                    val expr = upper.removePrefix("=")
                    when {
                        expr.contains("+") -> {
                            val parts = expr.split("+")
                            val sum = parts.sumOf { getCellValueAsDouble(it.trim(), cells) }
                            if (sum % 1.0 == 0.0) sum.toLong().toString() else String.format("%.2f", sum)
                        }
                        expr.contains("-") -> {
                            val parts = expr.split("-")
                            if (parts.isNotEmpty()) {
                                var res = getCellValueAsDouble(parts[0].trim(), cells)
                                for (p in parts.drop(1)) {
                                    res -= getCellValueAsDouble(p.trim(), cells)
                                }
                                if (res % 1.0 == 0.0) res.toLong().toString() else String.format("%.2f", res)
                            } else formula
                        }
                        expr.contains("*") -> {
                            val parts = expr.split("*")
                            val prod = parts.fold(1.0) { acc, s -> acc * getCellValueAsDouble(s.trim(), cells) }
                            if (prod % 1.0 == 0.0) prod.toLong().toString() else String.format("%.2f", prod)
                        }
                        expr.contains("/") -> {
                            val parts = expr.split("/")
                            if (parts.size == 2) {
                                val n1 = getCellValueAsDouble(parts[0].trim(), cells)
                                val n2 = getCellValueAsDouble(parts[1].trim(), cells)
                                if (n2 != 0.0) {
                                    val q = n1 / n2
                                    if (q % 1.0 == 0.0) q.toLong().toString() else String.format("%.2f", q)
                                } else "#DIV/0!"
                            } else formula
                        }
                        else -> {
                            // Single coordinate reference e.g. =A1
                            val d = getCellValueAsDouble(expr, cells)
                            if (d != 0.0) d.toString() else cells[coordToKey(expr)]?.rawValue ?: formula
                        }
                    }
                }
            }
        } catch (_: Exception) {
            "#ERROR!"
        }
    }

    private fun evaluateCondition(cond: String, cells: Map<String, ExcelCell>): Boolean {
        return when {
            cond.contains(">=") -> {
                val p = cond.split(">=")
                getCellValueAsDouble(p[0].trim(), cells) >= getCellValueAsDouble(p[1].trim(), cells)
            }
            cond.contains("<=") -> {
                val p = cond.split("<=")
                getCellValueAsDouble(p[0].trim(), cells) <= getCellValueAsDouble(p[1].trim(), cells)
            }
            cond.contains(">") -> {
                val p = cond.split(">")
                getCellValueAsDouble(p[0].trim(), cells) > getCellValueAsDouble(p[1].trim(), cells)
            }
            cond.contains("<") -> {
                val p = cond.split("<")
                getCellValueAsDouble(p[0].trim(), cells) < getCellValueAsDouble(p[1].trim(), cells)
            }
            cond.contains("=") -> {
                val p = cond.split("=")
                getCellValueAsDouble(p[0].trim(), cells) == getCellValueAsDouble(p[1].trim(), cells)
            }
            else -> false
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
        } else {
            getCellValueAsDouble(rangeStr.trim(), cells).let { list.add(it) }
        }
        return list
    }

    private fun coordToKey(coord: String): String {
        val (c, r) = parseCoord(coord)
        return "$r,$c"
    }

    private fun getCellValueAsDouble(coordStr: String, cells: Map<String, ExcelCell>): Double {
        coordStr.toDoubleOrNull()?.let { return it }
        val (c, r) = parseCoord(coordStr)
        return cells["$r,$c"]?.rawValue?.toDoubleOrNull() ?: 0.0
    }

    fun parseCoord(coord: String): Pair<Int, Int> {
        val colLetters = coord.filter { it.isLetter() }.uppercase()
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
                parseXlsxFull(context, uri)
            }

            if (sheets.isEmpty()) {
                val defaultSheet = ExcelSheet("Sheet1")
                defaultSheet.setCell(0, 0, "Mục")
                defaultSheet.setCell(0, 1, "Số lượng")
                defaultSheet.setCell(0, 2, "Đơn giá")
                defaultSheet.setCell(0, 3, "Thành tiền")
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
                        if (r == 0) {
                            sheet.getCell(r, c).isBold = true
                            sheet.getCell(r, c).style = ExcelCellStyle(isBold = true, bgColorHex = "#F3F4F6")
                        }
                    }
                    r++
                }
                sheet.maxRow = maxOf(r + 10, 30)
                sheet.maxCol = maxOf(sheet.cells.keys.map { it.substringAfter(",").toInt() }.maxOrNull() ?: 8, 8)
            }
        }
        sheets.add(sheet)
    }

    private fun parseLegacyXls(context: Context, uri: Uri) {
        val sheet = ExcelSheet("Trang tính")
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val bytes = stream.readBytes()
            val text = String(bytes, Charsets.ISO_8859_1).filter { it.code in 32..126 || it == '\t' || it == '\n' }
            val lines = text.split("\n").filter { it.isNotBlank() }
            for ((r, line) in lines.take(100).withIndex()) {
                val cols = line.split("\t", ",")
                for ((c, col) in cols.take(15).withIndex()) {
                    sheet.setCell(r, c, col.trim())
                    if (r == 0) sheet.getCell(r, c).isBold = true
                }
            }
        }
        sheets.add(sheet)
    }

    private fun parseXlsxFull(context: Context, uri: Uri) {
        try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return
            val zis = ZipInputStream(ByteArrayInputStream(bytes))
            var entry: ZipEntry? = zis.nextEntry

            val sharedStrings = mutableListOf<String>()
            val stylesList = mutableListOf<ExcelCellStyle>()
            val sheetXmls = mutableMapOf<String, String>()
            val sheetNames = mutableMapOf<String, String>()

            while (entry != null) {
                val entryName = entry.name
                when {
                    entryName == "xl/sharedStrings.xml" -> {
                        sharedStrings.addAll(parseSharedStrings(readEntryToString(zis)))
                    }
                    entryName == "xl/styles.xml" -> {
                        stylesList.addAll(parseStylesXml(readEntryToString(zis)))
                    }
                    entryName == "xl/workbook.xml" -> {
                        sheetNames.putAll(parseWorkbookSheetNames(readEntryToString(zis)))
                    }
                    entryName.startsWith("xl/worksheets/sheet") && entryName.endsWith(".xml") -> {
                        sheetXmls[entryName] = readEntryToString(zis)
                    }
                }
                entry = zis.nextEntry
            }
            zis.close()

            if (sheetXmls.isNotEmpty()) {
                sheetXmls.toSortedMap().forEach { (path, xml) ->
                    val sheetKey = path.substringAfterLast("/")
                    val name = sheetNames[sheetKey] ?: "Trang tính ${sheets.size + 1}"
                    val sheet = parseWorksheetXmlFull(name, xml, sharedStrings, stylesList)
                    sheets.add(sheet)
                }
            }
        } catch (_: Exception) {}
    }

    private fun parseWorkbookSheetNames(xml: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))
            var event = parser.eventType
            var idx = 1
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && parser.name?.equals("sheet", ignoreCase = true) == true) {
                    val sheetName = parser.getAttributeValue(null, "name")
                    if (!sheetName.isNullOrEmpty()) {
                        map["sheet$idx.xml"] = sheetName
                        idx++
                    }
                }
                event = parser.next()
            }
        } catch (_: Exception) {}
        return map
    }

    private fun parseStylesXml(xml: String): List<ExcelCellStyle> {
        val styles = mutableListOf<ExcelCellStyle>()
        val fills = mutableListOf<String?>() // fill colors
        val fonts = mutableListOf<Pair<Boolean, String?>>() // isBold, colorHex

        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))
            var event = parser.eventType

            var inFills = false
            var inFonts = false
            var inCellXfs = false

            var currentFontBold = false
            var currentFontColor: String? = null
            var currentFillColor: String? = null

            while (event != XmlPullParser.END_DOCUMENT) {
                val tag = parser.name?.lowercase() ?: ""
                when (event) {
                    XmlPullParser.START_TAG -> {
                        when (tag) {
                            "fills" -> inFills = true
                            "fonts" -> inFonts = true
                            "cellxfs" -> inCellXfs = true
                            "font" -> {
                                currentFontBold = false
                                currentFontColor = null
                            }
                            "b" -> if (inFonts) currentFontBold = true
                            "color" -> {
                                if (inFonts) {
                                    val rgb = parser.getAttributeValue(null, "rgb")
                                    if (!rgb.isNullOrEmpty()) currentFontColor = "#" + rgb.takeLast(6)
                                }
                            }
                            "fgcolor" -> {
                                if (inFills) {
                                    val rgb = parser.getAttributeValue(null, "rgb")
                                    if (!rgb.isNullOrEmpty()) currentFillColor = "#" + rgb.takeLast(6)
                                }
                            }
                            "xf" -> {
                                if (inCellXfs) {
                                    val fontId = parser.getAttributeValue(null, "fontId")?.toIntOrNull() ?: 0
                                    val fillId = parser.getAttributeValue(null, "fillId")?.toIntOrNull() ?: 0
                                    val numFmtId = parser.getAttributeValue(null, "numFmtId")?.toIntOrNull() ?: 0

                                    val fontInfo = fonts.getOrNull(fontId)
                                    val fillCol = fills.getOrNull(fillId)

                                    styles.add(
                                        ExcelCellStyle(
                                            isBold = fontInfo?.first ?: false,
                                            textColorHex = fontInfo?.second,
                                            bgColorHex = fillCol,
                                            numFmtId = numFmtId
                                        )
                                    )
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        when (tag) {
                            "fills" -> inFills = false
                            "fonts" -> inFonts = false
                            "cellxfs" -> inCellXfs = false
                            "font" -> if (inFonts) fonts.add(Pair(currentFontBold, currentFontColor))
                            "fill" -> {
                                if (inFills) {
                                    fills.add(currentFillColor)
                                    currentFillColor = null
                                }
                            }
                        }
                    }
                }
                event = parser.next()
            }
        } catch (_: Exception) {}
        return styles
    }

    private fun readEntryToString(zis: ZipInputStream): String {
        val baos = ByteArrayOutputStream()
        val buf = ByteArray(2048)
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
            if (event == XmlPullParser.START_TAG && parser.name?.equals("t", ignoreCase = true) == true) {
                list.add(parser.nextText())
            }
            event = parser.next()
        }
        return list
    }

    private fun parseWorksheetXmlFull(
        name: String,
        xml: String,
        sharedStrings: List<String>,
        stylesList: List<ExcelCellStyle>
    ): ExcelSheet {
        val sheet = ExcelSheet(name)
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))

        var event = parser.eventType
        var currentCellRef = ""
        var isSharedString = false
        var isInlineString = false
        var currentFormula: String? = null
        var currentStyleIdx = 0

        while (event != XmlPullParser.END_DOCUMENT) {
            val tag = parser.name?.lowercase() ?: ""
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (tag) {
                        "col" -> {
                            val minCol = (parser.getAttributeValue(null, "min")?.toIntOrNull() ?: 1) - 1
                            val maxCol = (parser.getAttributeValue(null, "max")?.toIntOrNull() ?: 1) - 1
                            val width = parser.getAttributeValue(null, "width")?.toFloatOrNull() ?: 10f
                            val pxW = (width * 8.5f).coerceIn(60f, 320f)
                            for (c in minCol..maxCol) {
                                sheet.colWidths[c] = pxW
                            }
                        }
                        "c" -> {
                            currentCellRef = parser.getAttributeValue(null, "r") ?: ""
                            val type = parser.getAttributeValue(null, "t")
                            isSharedString = (type == "s")
                            isInlineString = (type == "inlineStr")
                            currentStyleIdx = parser.getAttributeValue(null, "s")?.toIntOrNull() ?: 0
                            currentFormula = null
                        }
                        "f" -> {
                            currentFormula = "=" + parser.nextText()
                        }
                        "v", "t" -> {
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
                                val cell = sheet.getCell(r, c)
                                if (currentFormula != null) {
                                    cell.formula = currentFormula
                                }
                                if (currentStyleIdx in stylesList.indices) {
                                    val s = stylesList[currentStyleIdx]
                                    cell.style = s
                                    cell.isBold = s.isBold
                                    s.bgColorHex?.let { h ->
                                        try { cell.bgColor = AndroidColor.parseColor(h) } catch (_: Exception) {}
                                    }
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
            color = AndroidColor.parseColor("#E5E7EB")
            strokeWidth = 1.5f
            style = Paint.Style.STROKE
        }
        val headerPaint = Paint().apply {
            color = AndroidColor.parseColor("#F3F4F6")
            style = Paint.Style.FILL
        }

        val rowH = 65f
        val startY = 80f
        val startX = 80f

        for (r in 0 until minOf(targetSheet.maxRow, 25)) {
            val y = startY + r * rowH
            var currentX = startX
            for (c in 0 until minOf(targetSheet.maxCol, 6)) {
                val colW = targetSheet.getColWidth(c).coerceIn(90f, 220f)
                val cell = targetSheet.getCell(r, c)

                // Background fill
                if (r == 0) {
                    canvas.drawRect(currentX, y, currentX + colW, y + rowH, headerPaint)
                } else if (cell.style.bgColorHex != null) {
                    try {
                        val bgP = Paint().apply {
                            color = AndroidColor.parseColor(cell.style.bgColorHex)
                            style = Paint.Style.FILL
                        }
                        canvas.drawRect(currentX, y, currentX + colW, y + rowH, bgP)
                    } catch (_: Exception) {}
                }

                canvas.drawRect(currentX, y, currentX + colW, y + rowH, gridPaint)
                val cellVal = cell.getDisplayValue(targetSheet.cells)
                if (cellVal.isNotEmpty()) {
                    paint.isFakeBoldText = cell.style.isBold || r == 0
                    canvas.drawText(cellVal.take(12), currentX + 12f, y + rowH * 0.65f, paint)
                }
                currentX += colW
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
