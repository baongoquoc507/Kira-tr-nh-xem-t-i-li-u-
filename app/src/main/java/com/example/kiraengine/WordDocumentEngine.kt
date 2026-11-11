package com.example.kiraengine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
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

data class WordBlock(
    val id: String = java.util.UUID.randomUUID().toString(),
    var text: String = "",
    var isBold: Boolean = false,
    var isItalic: Boolean = false,
    var isUnderline: Boolean = false,
    var headingLevel: Int = 0, // 0 = normal, 1 = H1, 2 = H2, 3 = H3
    var isBullet: Boolean = false,
    var isNumbered: Boolean = false,
    var alignment: String = "left", // left, center, right
    var isTable: Boolean = false,
    var tableData: MutableList<MutableList<String>> = mutableListOf()
)

class WordDocumentEngine : DocumentEngine {
    private var _metadata: DocumentMetadata? = null
    override val metadata: DocumentMetadata? get() = _metadata

    val blocks = mutableListOf<WordBlock>()
    private val undoStack = mutableListOf<EditCommand>()
    private val redoStack = mutableListOf<EditCommand>()

    private var rawZipBytes: ByteArray? = null

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
                ?: return@withContext Result.failure(IllegalStateException("Cannot read file"))
            rawZipBytes = bytes
            if (fileSize == 0L) fileSize = bytes.size.toLong()

            val isLegacyDoc = fileName.endsWith(".doc", ignoreCase = true)
            if (isLegacyDoc) {
                parseLegacyDoc(bytes)
            } else {
                parseDocx(bytes)
            }

            if (blocks.isEmpty()) {
                blocks.add(WordBlock(text = "Tài liệu trống", headingLevel = 1))
                blocks.add(WordBlock(text = "Bắt đầu soạn thảo tài liệu của bạn tại đây..."))
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

    private fun parseDocx(bytes: ByteArray) {
        try {
            val zis = ZipInputStream(ByteArrayInputStream(bytes))
            var entry: ZipEntry? = zis.nextEntry
            var documentXmlContent = ""

            while (entry != null) {
                if (entry.name == "word/document.xml") {
                    val baos = ByteArrayOutputStream()
                    val buffer = ByteArray(1024)
                    var len: Int
                    while (zis.read(buffer).also { len = it } > 0) {
                        baos.write(buffer, 0, len)
                    }
                    documentXmlContent = baos.toString("UTF-8")
                    break
                }
                entry = zis.nextEntry
            }
            zis.close()

            if (documentXmlContent.isNotEmpty()) {
                parseDocumentXml(documentXmlContent)
            }
        } catch (_: Exception) {
            // Fallback plain text scan
            parsePlainTextFallback(bytes)
        }
    }

    private fun parseDocumentXml(xml: String) {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))

        var eventType = parser.eventType
        var currentBlock: WordBlock? = null
        var inParagraph = false
        var inRun = false
        var isBold = false
        var isItalic = false
        var isUnderline = false
        var inTable = false
        var currentTableRow = mutableListOf<String>()
        var currentTableCell = ""

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val name = parser.name?.lowercase() ?: ""
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (name) {
                        "tbl" -> {
                            inTable = true
                            currentBlock = WordBlock(isTable = true)
                        }
                        "tr" -> {
                            currentTableRow = mutableListOf()
                        }
                        "tc" -> {
                            currentTableCell = ""
                        }
                        "p" -> {
                            if (!inTable) {
                                currentBlock = WordBlock()
                                inParagraph = true
                            }
                        }
                        "b" -> isBold = true
                        "i" -> isItalic = true
                        "u" -> isUnderline = true
                        "t" -> {
                            // Text tag
                            val text = parser.nextText()
                            if (inTable) {
                                currentTableCell += text
                            } else {
                                currentBlock?.let { block ->
                                    block.text += text
                                    if (isBold) block.isBold = true
                                    if (isItalic) block.isItalic = true
                                    if (isUnderline) block.isUnderline = true
                                }
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (name) {
                        "p" -> {
                            if (!inTable && currentBlock != null && currentBlock.text.isNotBlank()) {
                                if (currentBlock.text.startsWith("# ")) {
                                    currentBlock.headingLevel = 1
                                    currentBlock.text = currentBlock.text.removePrefix("# ")
                                } else if (currentBlock.text.startsWith("## ")) {
                                    currentBlock.headingLevel = 2
                                    currentBlock.text = currentBlock.text.removePrefix("## ")
                                }
                                blocks.add(currentBlock)
                                currentBlock = null
                            }
                            inParagraph = false
                            isBold = false
                            isItalic = false
                            isUnderline = false
                        }
                        "tc" -> {
                            currentTableRow.add(currentTableCell.trim())
                        }
                        "tr" -> {
                            if (currentTableRow.isNotEmpty()) {
                                currentBlock?.tableData?.add(currentTableRow)
                            }
                        }
                        "tbl" -> {
                            if (currentBlock != null && currentBlock.tableData.isNotEmpty()) {
                                blocks.add(currentBlock)
                            }
                            currentBlock = null
                            inTable = false
                        }
                    }
                }
            }
            eventType = parser.next()
        }
    }

    private fun parseLegacyDoc(bytes: ByteArray) {
        // Extract visible Unicode/ASCII characters cleanly
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
        for ((idx, line) in lines.take(150).withIndex()) {
            blocks.add(
                WordBlock(
                    text = line.trim(),
                    headingLevel = if (idx == 0) 1 else 0
                )
            )
        }
    }

    private fun parsePlainTextFallback(bytes: ByteArray) {
        val text = String(bytes, Charsets.UTF_8).filter { it.code in 32..126 || it == '\n' || it == '\r' }
        val lines = text.split("\n").filter { it.trim().length > 2 }
        for ((idx, line) in lines.take(100).withIndex()) {
            blocks.add(WordBlock(text = line.trim(), headingLevel = if (idx == 0) 1 else 0))
        }
    }

    override fun pageCount(): Int {
        val totalChars = blocks.sumOf { it.text.length + (if (it.isTable) 300 else 0) }
        return (totalChars / 1200 + 1).coerceAtLeast(1)
    }

    override suspend fun renderPage(index: Int, targetWidth: Int, targetHeight: Int): Bitmap? = withContext(Dispatchers.Default) {
        val width = if (targetWidth > 0) targetWidth else 1080
        val height = if (targetHeight > 0) targetHeight else 1920

        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(AndroidColor.WHITE)

        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.BLACK
            textSize = 34f
        }

        var yOffset = 100f
        val xMargin = 80f
        val contentWidth = width - (xMargin * 2)

        val blocksPerPage = (blocks.size / pageCount().coerceAtLeast(1)).coerceAtLeast(1)
        val startIdx = (index * blocksPerPage).coerceIn(0, blocks.size)
        val endIdx = ((index + 1) * blocksPerPage).coerceIn(startIdx, blocks.size)

        for (i in startIdx until endIdx) {
            val block = blocks[i]
            if (block.isTable) {
                // Draw simplified table
                val rowHeight = 60f
                var tableY = yOffset
                val colWidth = contentWidth / (block.tableData.firstOrNull()?.size ?: 1).coerceAtLeast(1)

                val borderPaint = Paint().apply {
                    color = AndroidColor.LTGRAY
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                }

                for (row in block.tableData.take(8)) {
                    for ((cIdx, cell) in row.withIndex()) {
                        val cellRect = android.graphics.RectF(
                            xMargin + cIdx * colWidth,
                            tableY,
                            xMargin + (cIdx + 1) * colWidth,
                            tableY + rowHeight
                        )
                        canvas.drawRect(cellRect, borderPaint)
                        paint.textSize = 28f
                        paint.typeface = Typeface.DEFAULT
                        canvas.drawText(cell.take(12), cellRect.left + 10, cellRect.centerY() + 8, paint)
                    }
                    tableY += rowHeight
                }
                yOffset = tableY + 40f
            } else {
                paint.textSize = when (block.headingLevel) {
                    1 -> 52f
                    2 -> 42f
                    3 -> 36f
                    else -> 32f
                }
                paint.typeface = when {
                    block.isBold && block.isItalic -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
                    block.isBold || block.headingLevel > 0 -> Typeface.DEFAULT_BOLD
                    block.isItalic -> Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                    else -> Typeface.DEFAULT
                }

                val prefix = if (block.isBullet) "• " else ""
                val textToDraw = prefix + block.text

                val staticLayout = StaticLayout.Builder.obtain(
                    textToDraw,
                    0,
                    textToDraw.length,
                    paint,
                    contentWidth.toInt()
                ).setAlignment(
                    when (block.alignment) {
                        "center" -> Layout.Alignment.ALIGN_CENTER
                        "right" -> Layout.Alignment.ALIGN_OPPOSITE
                        else -> Layout.Alignment.ALIGN_NORMAL
                    }
                ).build()

                canvas.save()
                canvas.translate(xMargin, yOffset)
                staticLayout.draw(canvas)
                canvas.restore()

                yOffset += staticLayout.height + 30f
            }
            if (yOffset > height - 100f) break
        }

        bmp
    }

    override fun applyEdit(command: EditCommand): Boolean {
        return when (command) {
            is EditCommand.WordUpdateText -> {
                if (command.paragraphIndex in blocks.indices) {
                    blocks[command.paragraphIndex].text = command.newText
                    undoStack.add(command)
                    redoStack.clear()
                    true
                } else false
            }
            is EditCommand.WordToggleFormat -> {
                if (command.paragraphIndex in blocks.indices) {
                    val b = blocks[command.paragraphIndex]
                    command.bold?.let { b.isBold = it }
                    command.italic?.let { b.isItalic = it }
                    command.underline?.let { b.isUnderline = it }
                    undoStack.add(command)
                    redoStack.clear()
                    true
                } else false
            }
            is EditCommand.WordAddParagraph -> {
                val newBlock = WordBlock(
                    text = command.text,
                    headingLevel = if (command.isHeading) 1 else 0
                )
                val idx = command.index.coerceIn(0, blocks.size)
                blocks.add(idx, newBlock)
                undoStack.add(command)
                redoStack.clear()
                true
            }
            is EditCommand.WordInsertTable -> {
                val table = mutableListOf<MutableList<String>>()
                for (r in 0 until command.rows) {
                    val row = mutableListOf<String>()
                    for (c in 0 until command.cols) {
                        row.add(if (r == 0) "Cột ${c + 1}" else "Dữ liệu ${r}-${c + 1}")
                    }
                    table.add(row)
                }
                val block = WordBlock(isTable = true, tableData = table)
                val idx = command.index.coerceIn(0, blocks.size)
                blocks.add(idx, block)
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
                if (cmd.paragraphIndex in blocks.indices) {
                    blocks[cmd.paragraphIndex].text = cmd.oldText
                    redoStack.add(cmd)
                }
            }
            is EditCommand.WordAddParagraph -> {
                if (cmd.index in blocks.indices) {
                    blocks.removeAt(cmd.index)
                    redoStack.add(cmd)
                }
            }
            is EditCommand.WordInsertTable -> {
                if (cmd.index in blocks.indices) {
                    blocks.removeAt(cmd.index)
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
                if (cmd.paragraphIndex in blocks.indices) {
                    blocks[cmd.paragraphIndex].text = cmd.newText
                    undoStack.add(cmd)
                }
            }
            is EditCommand.WordAddParagraph -> {
                val block = WordBlock(text = cmd.text, headingLevel = if (cmd.isHeading) 1 else 0)
                blocks.add(cmd.index.coerceIn(0, blocks.size), block)
                undoStack.add(cmd)
            }
            is EditCommand.WordInsertTable -> {
                val table = mutableListOf<MutableList<String>>()
                for (r in 0 until cmd.rows) {
                    val row = mutableListOf<String>()
                    for (c in 0 until cmd.cols) {
                        row.add(if (r == 0) "Cột ${c + 1}" else "Dữ liệu ${r}-${c + 1}")
                    }
                    table.add(row)
                }
                blocks.add(cmd.index.coerceIn(0, blocks.size), WordBlock(isTable = true, tableData = table))
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

            // Generate clean DOCX OpenXML Zip Archive
            val zos = ZipOutputStream(FileOutputStream(targetFile))

            // [Content_Types].xml
            zos.putNextEntry(ZipEntry("[Content_Types].xml"))
            zos.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>""".toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // _rels/.rels
            zos.putNextEntry(ZipEntry("_rels/.rels"))
            zos.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>""".toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // word/document.xml
            zos.putNextEntry(ZipEntry("word/document.xml"))
            val sb = StringBuilder()
            sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
<w:body>""")

            for (block in blocks) {
                if (block.isTable) {
                    sb.append("<w:tbl>")
                    for (row in block.tableData) {
                        sb.append("<w:tr>")
                        for (cell in row) {
                            sb.append("<w:tc><w:p><w:r><w:t>${escapeXml(cell)}</w:t></w:r></w:p></w:tc>")
                        }
                        sb.append("</w:tr>")
                    }
                    sb.append("</w:tbl>")
                } else {
                    sb.append("<w:p>")
                    if (block.isBold || block.isItalic || block.isUnderline) {
                        sb.append("<w:pPr><w:rPr>")
                        if (block.isBold) sb.append("<w:b/>")
                        if (block.isItalic) sb.append("<w:i/>")
                        if (block.isUnderline) sb.append("<w:u w:val=\"single\"/>")
                        sb.append("</w:rPr></w:pPr>")
                    }
                    sb.append("<w:r><w:t>${escapeXml(block.text)}</w:t></w:r></w:p>")
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
        blocks.clear()
        undoStack.clear()
        redoStack.clear()
        rawZipBytes = null
    }
}
