package com.example.kiraengine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.StringReader
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Rich Text Run within a Word paragraph.
 */
data class WordRun(
    var text: String = "",
    var isBold: Boolean = false,
    var isItalic: Boolean = false,
    var isUnderline: Boolean = false,
    var isStrike: Boolean = false,
    var colorHex: String? = null,
    var highlightHex: String? = null,
    var fontSizePt: Float = 12f,
    var fontFamily: String? = null
)

/**
 * Document Element (Paragraph, Table, Image, or Page Break).
 */
data class WordParagraph(
    val id: String = java.util.UUID.randomUUID().toString(),
    val runs: MutableList<WordRun> = mutableListOf(),
    var alignment: String = "left", // left, center, right, justify
    var headingLevel: Int = 0, // 0 = Body, 1 = H1, 2 = H2, 3 = H3
    var isBullet: Boolean = false,
    var isNumbered: Boolean = false,
    var bulletNumber: Int = 1,
    var spaceBeforePt: Float = 0f,
    var spaceAfterPt: Float = 4f,
    var lineSpacingMultiplier: Float = 1.2f,
    var indentPt: Float = 0f
) {
    val fullText: String get() = runs.joinToString("") { it.text }

    val isBold: Boolean get() = runs.any { it.isBold }
    val isItalic: Boolean get() = runs.any { it.isItalic }
    val isUnderline: Boolean get() = runs.any { it.isUnderline }
}

data class WordTableCell(
    val paragraphs: MutableList<WordParagraph> = mutableListOf(),
    var bgHex: String? = null,
    var colSpan: Int = 1,
    var widthPercent: Float = 1f
) {
    val cellText: String get() = paragraphs.joinToString("\n") { it.fullText }
}

data class WordTable(
    val id: String = java.util.UUID.randomUUID().toString(),
    val rows: MutableList<MutableList<WordTableCell>> = mutableListOf(),
    var colCount: Int = 1
)

data class WordEmbeddedImage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val bitmap: Bitmap?,
    val name: String = "Image",
    var widthPx: Int = 600,
    var heightPx: Int = 400
)

sealed class WordItem {
    data class Paragraph(val p: WordParagraph) : WordItem()
    data class Table(val t: WordTable) : WordItem()
    data class Image(val img: WordEmbeddedImage) : WordItem()
}

data class WordBlock(
    val id: String = java.util.UUID.randomUUID().toString(),
    var text: String = "",
    var isBold: Boolean = false,
    var isItalic: Boolean = false,
    var isUnderline: Boolean = false,
    var headingLevel: Int = 0,
    var isBullet: Boolean = false,
    var isNumbered: Boolean = false,
    var alignment: String = "left",
    var isTable: Boolean = false,
    var tableData: MutableList<MutableList<String>> = mutableListOf()
)

class WordDocumentEngine : DocumentEngine {
    private var _metadata: DocumentMetadata? = null
    override val metadata: DocumentMetadata? get() = _metadata

    val items = mutableListOf<WordItem>()
    val mediaImages = mutableMapOf<String, Bitmap>() // rId/name -> Bitmap

    private val undoStack = mutableListOf<EditCommand>()
    private val redoStack = mutableListOf<EditCommand>()
    private var rawZipBytes: ByteArray? = null

    // For backwards compatibility with basic UI helpers
    val blocks: List<WordBlock>
        get() = items.map { item ->
            when (item) {
                is WordItem.Paragraph -> WordBlock(
                    id = item.p.id,
                    text = item.p.fullText,
                    isBold = item.p.isBold,
                    isItalic = item.p.isItalic,
                    isUnderline = item.p.isUnderline,
                    headingLevel = item.p.headingLevel,
                    isBullet = item.p.isBullet,
                    isNumbered = item.p.isNumbered,
                    alignment = item.p.alignment
                )
                is WordItem.Table -> WordBlock(
                    id = item.t.id,
                    isTable = true,
                    tableData = item.t.rows.map { r -> r.map { it.cellText }.toMutableList() }.toMutableList()
                )
                is WordItem.Image -> WordBlock(
                    id = item.img.id,
                    text = "[Hình ảnh: ${item.img.name}]"
                )
            }
        }

    override suspend fun open(context: Context, uri: Uri): Result<DocumentMetadata> = withContext(Dispatchers.IO) {
        try {
            close()
            var fileName = "Document.docx"
            var fileSize = 0L

            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: fileName
                    if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                }
            }

            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext Result.failure(IllegalStateException("Không thể đọc tệp"))
            rawZipBytes = bytes
            if (fileSize == 0L) fileSize = bytes.size.toLong()

            val isLegacyDoc = fileName.endsWith(".doc", ignoreCase = true)
            if (isLegacyDoc) {
                parseLegacyDoc(bytes)
            } else {
                parseDocxArchive(bytes)
            }

            if (items.isEmpty()) {
                val p1 = WordParagraph(headingLevel = 1)
                p1.runs.add(WordRun(text = "Tài liệu mới", isBold = true, fontSizePt = 22f))
                items.add(WordItem.Paragraph(p1))

                val p2 = WordParagraph()
                p2.runs.add(WordRun(text = "Bắt đầu soạn thảo văn bản tại đây...", fontSizePt = 12f))
                items.add(WordItem.Paragraph(p2))
            }

            val meta = DocumentMetadata(
                uri = uri,
                name = fileName,
                type = DocumentType.WORD,
                sizeBytes = fileSize,
                pageCount = pageCount(),
                isReadOnly = isLegacyDoc
            )
            _metadata = meta
            Result.success(meta)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseDocxArchive(bytes: ByteArray) {
        try {
            val zis = ZipInputStream(ByteArrayInputStream(bytes))
            var entry: ZipEntry? = zis.nextEntry
            var documentXml: String? = null
            var relsXml: String? = null
            val relsMap = mutableMapOf<String, String>() // rId -> target

            while (entry != null) {
                val name = entry.name
                when {
                    name == "word/document.xml" -> {
                        documentXml = readZipStreamToString(zis)
                    }
                    name == "word/_rels/document.xml.rels" -> {
                        relsXml = readZipStreamToString(zis)
                    }
                    name.startsWith("word/media/") -> {
                        val imgBytes = readZipStreamToBytes(zis)
                        val bmp = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.size)
                        if (bmp != null) {
                            val imgFileName = name.substringAfterLast("/")
                            mediaImages[imgFileName] = bmp
                            mediaImages[name] = bmp
                        }
                    }
                }
                entry = zis.nextEntry
            }
            zis.close()

            // Parse relationships if present
            if (relsXml != null) {
                parseRels(relsXml, relsMap)
            }

            if (!documentXml.isNullOrEmpty()) {
                parseDocumentXmlStructure(documentXml, relsMap)
            } else {
                parsePlainTextFallback(bytes)
            }
        } catch (_: Exception) {
            parsePlainTextFallback(bytes)
        }
    }

    private fun parseRels(xml: String, relsMap: MutableMap<String, String>) {
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && parser.name?.equals("Relationship", ignoreCase = true) == true) {
                    val id = parser.getAttributeValue(null, "Id")
                    val target = parser.getAttributeValue(null, "Target")
                    if (!id.isNullOrEmpty() && !target.isNullOrEmpty()) {
                        relsMap[id] = target.substringAfterLast("/")
                    }
                }
                event = parser.next()
            }
        } catch (_: Exception) {}
    }

    private fun parseDocumentXmlStructure(xml: String, relsMap: Map<String, String>) {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))

        var eventType = parser.eventType
        var currentParagraph: WordParagraph? = null
        var currentRun: WordRun? = null

        var inTable = false
        var currentTable: WordTable? = null
        var currentRow: MutableList<WordTableCell>? = null
        var currentCell: WordTableCell? = null

        var inRPr = false
        var inPPr = false
        var currentHeadingLevel = 0
        var currentAlignment = "left"
        var currentBullet = false
        var currentNumbered = false
        var bulletCount = 0

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tag = parser.name?.lowercase() ?: ""
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (tag) {
                        "tbl" -> {
                            inTable = true
                            currentTable = WordTable()
                        }
                        "tr" -> {
                            currentRow = mutableListOf()
                        }
                        "tc" -> {
                            currentCell = WordTableCell()
                        }
                        "shd" -> {
                            val fill = parser.getAttributeValue(null, "fill")
                            if (!fill.isNullOrEmpty() && fill != "auto" && fill != "none") {
                                currentCell?.bgHex = if (fill.startsWith("#")) fill else "#$fill"
                            }
                        }
                        "gridspan" -> {
                            val span = parser.getAttributeValue(null, "val")?.toIntOrNull() ?: 1
                            currentCell?.colSpan = span
                        }
                        "p" -> {
                            currentParagraph = WordParagraph()
                            inPPr = false
                            currentHeadingLevel = 0
                            currentAlignment = "left"
                            currentBullet = false
                            currentNumbered = false
                        }
                        "ppr" -> inPPr = true
                        "jc" -> {
                            if (inPPr) {
                                val v = parser.getAttributeValue(null, "val") ?: "left"
                                currentAlignment = when (v) {
                                    "center" -> "center"
                                    "right" -> "right"
                                    "both" -> "justify"
                                    else -> "left"
                                }
                            }
                        }
                        "pstyle" -> {
                            if (inPPr) {
                                val s = parser.getAttributeValue(null, "val")?.lowercase() ?: ""
                                if (s.contains("heading1") || s.contains("heading 1") || s.contains("title")) currentHeadingLevel = 1
                                else if (s.contains("heading2") || s.contains("heading 2")) currentHeadingLevel = 2
                                else if (s.contains("heading3") || s.contains("heading 3")) currentHeadingLevel = 3
                            }
                        }
                        "numpr" -> {
                            if (inPPr) {
                                currentBullet = true
                            }
                        }
                        "r" -> {
                            currentRun = WordRun(
                                fontSizePt = when (currentHeadingLevel) {
                                    1 -> 20f
                                    2 -> 16f
                                    3 -> 14f
                                    else -> 12f
                                },
                                isBold = currentHeadingLevel > 0
                            )
                            inRPr = false
                        }
                        "rpr" -> inRPr = true
                        "b", "bcs" -> if (inRPr) currentRun?.isBold = true
                        "i", "ics" -> if (inRPr) currentRun?.isItalic = true
                        "u" -> if (inRPr) currentRun?.isUnderline = true
                        "strike" -> if (inRPr) currentRun?.isStrike = true
                        "color" -> {
                            if (inRPr) {
                                val c = parser.getAttributeValue(null, "val")
                                if (!c.isNullOrEmpty() && c != "auto") {
                                    currentRun?.colorHex = if (c.startsWith("#")) c else "#$c"
                                }
                            }
                        }
                        "highlight" -> {
                            if (inRPr) {
                                val hl = parser.getAttributeValue(null, "val")
                                currentRun?.highlightHex = when (hl?.lowercase()) {
                                    "yellow" -> "#FFF59D"
                                    "green" -> "#A5D6A7"
                                    "cyan" -> "#80DEEA"
                                    "magenta" -> "#F48FB1"
                                    else -> null
                                }
                            }
                        }
                        "sz" -> {
                            if (inRPr) {
                                val szVal = parser.getAttributeValue(null, "val")?.toFloatOrNull()
                                if (szVal != null && szVal > 0) {
                                    currentRun?.fontSizePt = (szVal / 2f).coerceIn(8f, 72f)
                                }
                            }
                        }
                        "rfonts" -> {
                            if (inRPr) {
                                val font = parser.getAttributeValue(null, "ascii")
                                    ?: parser.getAttributeValue(null, "hAnsi")
                                currentRun?.fontFamily = font
                            }
                        }
                        "blip" -> {
                            // Embedded image reference
                            val embedId = parser.getAttributeValue("http://schemas.openxmlformats.org/officeDocument/2006/relationships", "embed")
                                ?: parser.getAttributeValue(null, "embed")
                            if (!embedId.isNullOrEmpty()) {
                                val imgName = relsMap[embedId] ?: embedId
                                val bmp = mediaImages[imgName] ?: mediaImages.values.firstOrNull()
                                if (bmp != null) {
                                    items.add(WordItem.Image(WordEmbeddedImage(bitmap = bmp, name = imgName)))
                                }
                            }
                        }
                        "t" -> {
                            val text = parser.nextText()
                            if (currentRun != null) {
                                currentRun.text += text
                            } else if (currentParagraph != null) {
                                currentParagraph.runs.add(WordRun(text = text))
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (tag) {
                        "rpr" -> inRPr = false
                        "ppr" -> inPPr = false
                        "r" -> {
                            if (currentRun != null && currentRun.text.isNotEmpty()) {
                                currentParagraph?.runs?.add(currentRun)
                            }
                            currentRun = null
                        }
                        "p" -> {
                            if (currentParagraph != null) {
                                currentParagraph.headingLevel = currentHeadingLevel
                                currentParagraph.alignment = currentAlignment
                                currentParagraph.isBullet = currentBullet
                                currentParagraph.isNumbered = currentNumbered

                                if (inTable && currentCell != null) {
                                    currentCell.paragraphs.add(currentParagraph)
                                } else if (currentParagraph.runs.isNotEmpty() || currentParagraph.headingLevel > 0) {
                                    items.add(WordItem.Paragraph(currentParagraph))
                                }
                            }
                            currentParagraph = null
                        }
                        "tc" -> {
                            if (currentCell != null && currentRow != null) {
                                currentRow.add(currentCell)
                            }
                            currentCell = null
                        }
                        "tr" -> {
                            if (currentRow != null && currentTable != null) {
                                currentTable.rows.add(currentRow)
                                currentTable.colCount = maxOf(currentTable.colCount, currentRow.size)
                            }
                            currentRow = null
                        }
                        "tbl" -> {
                            if (currentTable != null && currentTable.rows.isNotEmpty()) {
                                items.add(WordItem.Table(currentTable))
                            }
                            currentTable = null
                            inTable = false
                        }
                    }
                }
            }
            eventType = parser.next()
        }
    }

    private fun parseLegacyDoc(bytes: ByteArray) {
        // High quality string decoding for legacy binary Word documents (.doc)
        val sb = StringBuilder()
        var i = 0
        while (i < bytes.size - 1) {
            val b1 = bytes[i].toInt() and 0xFF
            val b2 = bytes[i + 1].toInt() and 0xFF
            val ch = (b2 shl 8) or b1
            if (ch in 32..126 || ch in 160..600 || ch == 10 || ch == 13) {
                sb.append(ch.toChar())
            }
            i += 2
        }
        val lines = sb.toString().split("\n", "\r").filter { it.trim().length > 3 }
        for ((idx, line) in lines.take(200).withIndex()) {
            val p = WordParagraph(
                headingLevel = if (idx == 0) 1 else 0
            )
            p.runs.add(
                WordRun(
                    text = line.trim(),
                    isBold = idx == 0,
                    fontSizePt = if (idx == 0) 18f else 12f
                )
            )
            items.add(WordItem.Paragraph(p))
        }
    }

    private fun parsePlainTextFallback(bytes: ByteArray) {
        val text = String(bytes, Charsets.UTF_8).filter { it.code in 32..126 || it == '\n' || it == '\r' || it.code in 160..700 }
        val lines = text.split("\n").filter { it.trim().length > 2 }
        for ((idx, line) in lines.take(150).withIndex()) {
            val p = WordParagraph(headingLevel = if (idx == 0) 1 else 0)
            p.runs.add(WordRun(text = line.trim(), isBold = idx == 0, fontSizePt = if (idx == 0) 18f else 12f))
            items.add(WordItem.Paragraph(p))
        }
    }

    private fun readZipStreamToString(zis: ZipInputStream): String {
        val baos = ByteArrayOutputStream()
        val buffer = ByteArray(2048)
        var len: Int
        while (zis.read(buffer).also { len = it } > 0) {
            baos.write(buffer, 0, len)
        }
        return baos.toString("UTF-8")
    }

    private fun readZipStreamToBytes(zis: ZipInputStream): ByteArray {
        val baos = ByteArrayOutputStream()
        val buffer = ByteArray(2048)
        var len: Int
        while (zis.read(buffer).also { len = it } > 0) {
            baos.write(buffer, 0, len)
        }
        return baos.toByteArray()
    }

    override fun pageCount(): Int {
        val totalLength = items.sumOf {
            when (it) {
                is WordItem.Paragraph -> it.p.fullText.length + 50
                is WordItem.Table -> it.t.rows.sumOf { r -> r.sumOf { c -> c.cellText.length + 30 } } + 200
                is WordItem.Image -> 400
            }
        }
        return (totalLength / 1400 + 1).coerceAtLeast(1)
    }

    override suspend fun renderPage(index: Int, targetWidth: Int, targetHeight: Int): Bitmap? = withContext(Dispatchers.Default) {
        // Render authentic A4 page bitmap (210mm x 297mm aspect ratio = 1 : 1.414)
        val width = if (targetWidth > 0) targetWidth else 1080
        val height = if (targetHeight > 0) targetHeight else (width * 1.414f).toInt()

        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(AndroidColor.WHITE)

        val marginX = (width * 0.08f).coerceAtLeast(40f)
        val marginY = (height * 0.06f).coerceAtLeast(50f)
        val contentWidth = width - (marginX * 2)

        var currentY = marginY

        val itemsPerPage = (items.size / pageCount().coerceAtLeast(1)).coerceAtLeast(1)
        val startIdx = (index * itemsPerPage).coerceIn(0, items.size)
        val endIdx = ((index + 1) * itemsPerPage).coerceIn(startIdx, items.size)

        val borderPaint = Paint().apply {
            color = AndroidColor.parseColor("#D1D5DB")
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }

        for (i in startIdx until endIdx) {
            val item = items[i]
            when (item) {
                is WordItem.Paragraph -> {
                    val p = item.p
                    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = AndroidColor.parseColor("#111827")
                        textSize = when (p.headingLevel) {
                            1 -> 48f
                            2 -> 38f
                            3 -> 32f
                            else -> 28f
                        }
                        typeface = when {
                            p.isBold -> Typeface.DEFAULT_BOLD
                            p.isItalic -> Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                            else -> Typeface.DEFAULT
                        }
                    }

                    val prefix = if (p.isBullet) "•  " else ""
                    val textToDraw = prefix + p.fullText

                    val alignment = when (p.alignment) {
                        "center" -> Layout.Alignment.ALIGN_CENTER
                        "right" -> Layout.Alignment.ALIGN_OPPOSITE
                        else -> Layout.Alignment.ALIGN_NORMAL
                    }

                    val layout = StaticLayout.Builder.obtain(
                        textToDraw,
                        0,
                        textToDraw.length,
                        paint,
                        contentWidth.toInt()
                    ).setAlignment(alignment).build()

                    canvas.save()
                    canvas.translate(marginX, currentY)
                    layout.draw(canvas)
                    canvas.restore()

                    currentY += layout.height + 20f
                }
                is WordItem.Table -> {
                    val table = item.t
                    val rowHeight = 55f
                    val colW = contentWidth / table.colCount.coerceAtLeast(1)
                    var tableY = currentY

                    for (row in table.rows.take(10)) {
                        for ((cIdx, cell) in row.withIndex()) {
                            val rect = RectF(
                                marginX + cIdx * colW,
                                tableY,
                                marginX + (cIdx + 1) * colW,
                                tableY + rowHeight
                            )

                            cell.bgHex?.let { hex ->
                                try {
                                    val bgP = Paint().apply {
                                        color = AndroidColor.parseColor(hex)
                                        style = Paint.Style.FILL
                                    }
                                    canvas.drawRect(rect, bgP)
                                } catch (_: Exception) {}
                            }

                            canvas.drawRect(rect, borderPaint)

                            val cellP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                textSize = 24f
                                color = AndroidColor.parseColor("#1F2937")
                            }
                            canvas.drawText(cell.cellText.take(16), rect.left + 12f, rect.centerY() + 8f, cellP)
                        }
                        tableY += rowHeight
                    }
                    currentY = tableY + 30f
                }
                is WordItem.Image -> {
                    val img = item.img
                    if (img.bitmap != null) {
                        val maxImgH = 300f
                        val scale = minOf(contentWidth / img.bitmap.width.toFloat(), maxImgH / img.bitmap.height.toFloat())
                        val destW = img.bitmap.width * scale
                        val destH = img.bitmap.height * scale
                        val destRect = RectF(marginX, currentY, marginX + destW, currentY + destH)
                        canvas.drawBitmap(img.bitmap, null, destRect, null)
                        currentY += destH + 24f
                    }
                }
            }
            if (currentY > height - marginY) break
        }

        // Draw page footer (Trang X / Y)
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#9CA3AF")
            textSize = 22f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("${index + 1} / ${pageCount()}", width / 2f, height - 30f, footerPaint)

        bmp
    }

    override fun applyEdit(command: EditCommand): Boolean {
        return when (command) {
            is EditCommand.WordUpdateText -> {
                val p = items.filterIsInstance<WordItem.Paragraph>().getOrNull(command.paragraphIndex)?.p
                if (p != null) {
                    p.runs.clear()
                    p.runs.add(WordRun(text = command.newText, fontSizePt = if (p.headingLevel > 0) 18f else 12f, isBold = p.headingLevel > 0))
                    undoStack.add(command)
                    redoStack.clear()
                    true
                } else false
            }
            is EditCommand.WordToggleFormat -> {
                val p = items.filterIsInstance<WordItem.Paragraph>().getOrNull(command.paragraphIndex)?.p
                if (p != null) {
                    p.runs.forEach { r ->
                        command.bold?.let { r.isBold = it }
                        command.italic?.let { r.isItalic = it }
                        command.underline?.let { r.isUnderline = it }
                    }
                    undoStack.add(command)
                    redoStack.clear()
                    true
                } else false
            }
            is EditCommand.WordAddParagraph -> {
                val p = WordParagraph(headingLevel = if (command.isHeading) 1 else 0)
                p.runs.add(WordRun(text = command.text, fontSizePt = if (command.isHeading) 20f else 12f, isBold = command.isHeading))
                val idx = command.index.coerceIn(0, items.size)
                items.add(idx, WordItem.Paragraph(p))
                undoStack.add(command)
                redoStack.clear()
                true
            }
            is EditCommand.WordInsertTable -> {
                val table = WordTable(colCount = command.cols)
                for (r in 0 until command.rows) {
                    val row = mutableListOf<WordTableCell>()
                    for (c in 0 until command.cols) {
                        val cell = WordTableCell(
                            bgHex = if (r == 0) "#F3F4F6" else null
                        )
                        val p = WordParagraph()
                        p.runs.add(WordRun(text = if (r == 0) "Cột ${c + 1}" else "Dữ liệu ${r}-${c + 1}", isBold = r == 0))
                        cell.paragraphs.add(p)
                        row.add(cell)
                    }
                    table.rows.add(row)
                }
                val idx = command.index.coerceIn(0, items.size)
                items.add(idx, WordItem.Table(table))
                undoStack.add(command)
                redoStack.clear()
                true
            }
            else -> false
        }
    }

    override fun undo(): Boolean {
        if (undoStack.isEmpty()) return false
        val cmd = undoStack.removeAt(undoStack.lastIndex)
        when (cmd) {
            is EditCommand.WordUpdateText -> {
                val p = items.filterIsInstance<WordItem.Paragraph>().getOrNull(cmd.paragraphIndex)?.p
                if (p != null) {
                    p.runs.clear()
                    p.runs.add(WordRun(text = cmd.oldText))
                    redoStack.add(cmd)
                }
            }
            is EditCommand.WordAddParagraph -> {
                if (cmd.index in items.indices) {
                    items.removeAt(cmd.index)
                    redoStack.add(cmd)
                }
            }
            is EditCommand.WordInsertTable -> {
                if (cmd.index in items.indices) {
                    items.removeAt(cmd.index)
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
            is EditCommand.WordUpdateText -> {
                val p = items.filterIsInstance<WordItem.Paragraph>().getOrNull(cmd.paragraphIndex)?.p
                if (p != null) {
                    p.runs.clear()
                    p.runs.add(WordRun(text = cmd.newText))
                    undoStack.add(cmd)
                }
            }
            is EditCommand.WordAddParagraph -> {
                val p = WordParagraph(headingLevel = if (cmd.isHeading) 1 else 0)
                p.runs.add(WordRun(text = cmd.text, isBold = cmd.isHeading))
                items.add(cmd.index.coerceIn(0, items.size), WordItem.Paragraph(p))
                undoStack.add(cmd)
            }
            is EditCommand.WordInsertTable -> {
                val table = WordTable(colCount = cmd.cols)
                for (r in 0 until cmd.rows) {
                    val row = mutableListOf<WordTableCell>()
                    for (c in 0 until cmd.cols) {
                        val cell = WordTableCell(bgHex = if (r == 0) "#F3F4F6" else null)
                        val p = WordParagraph()
                        p.runs.add(WordRun(text = if (r == 0) "Cột ${c + 1}" else "Dữ liệu ${r}-${c + 1}", isBold = r == 0))
                        cell.paragraphs.add(p)
                        row.add(cell)
                    }
                    table.rows.add(row)
                }
                items.add(cmd.index.coerceIn(0, items.size), WordItem.Table(table))
                undoStack.add(cmd)
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
            val targetFile = File(context.cacheDir, "saved_${System.currentTimeMillis()}.docx")
            val zos = ZipOutputStream(FileOutputStream(targetFile))

            zos.putNextEntry(ZipEntry("[Content_Types].xml"))
            zos.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>""".toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("_rels/.rels"))
            zos.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>""".toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("word/document.xml"))
            val sb = StringBuilder()
            sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
<w:body>""")

            for (item in items) {
                when (item) {
                    is WordItem.Paragraph -> {
                        val p = item.p
                        sb.append("<w:p>")
                        if (p.alignment != "left") {
                            sb.append("<w:pPr><w:jc w:val=\"${p.alignment}\"/></w:pPr>")
                        }
                        for (run in p.runs) {
                            sb.append("<w:r>")
                            if (run.isBold || run.isItalic || run.isUnderline || run.colorHex != null) {
                                sb.append("<w:rPr>")
                                if (run.isBold) sb.append("<w:b/>")
                                if (run.isItalic) sb.append("<w:i/>")
                                if (run.isUnderline) sb.append("<w:u w:val=\"single\"/>")
                                if (run.colorHex != null) sb.append("<w:color w:val=\"${run.colorHex!!.removePrefix("#")}\"/>")
                                sb.append("</w:rPr>")
                            }
                            sb.append("<w:t>${escapeXml(run.text)}</w:t></w:r>")
                        }
                        sb.append("</w:p>")
                    }
                    is WordItem.Table -> {
                        sb.append("<w:tbl>")
                        for (row in item.t.rows) {
                            sb.append("<w:tr>")
                            for (cell in row) {
                                sb.append("<w:tc>")
                                if (cell.bgHex != null) {
                                    sb.append("<w:tcPr><w:shd w:fill=\"${cell.bgHex!!.removePrefix("#")}\"/></w:tcPr>")
                                }
                                for (cp in cell.paragraphs) {
                                    sb.append("<w:p><w:r><w:t>${escapeXml(cp.fullText)}</w:t></w:r></w:p>")
                                }
                                sb.append("</w:tc>")
                            }
                            sb.append("</w:tr>")
                        }
                        sb.append("</w:tbl>")
                    }
                    is WordItem.Image -> {}
                }
            }
            sb.append("</w:body></w:document>")
            zos.write(sb.toString().toByteArray(Charsets.UTF_8))
            zos.closeEntry()
            zos.close()

            context.contentResolver.openOutputStream(uri)?.use { out ->
                targetFile.inputStream().use { it.copyTo(out) }
            }
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun escapeXml(input: String): String {
        return input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    override suspend fun exportToPdf(context: Context, targetFile: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            val doc = PdfDocument()
            val totalPages = pageCount()
            for (i in 0 until totalPages) {
                val pageInfo = PdfDocument.PageInfo.Builder(1080, 1528, i + 1).create() // A4 ratio
                val page = doc.startPage(pageInfo)
                val bmp = renderPage(i, 1080, 1528)
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
        items.clear()
        mediaImages.clear()
        undoStack.clear()
        redoStack.clear()
        rawZipBytes = null
    }
}
