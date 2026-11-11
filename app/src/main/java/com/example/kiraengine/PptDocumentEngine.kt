package com.example.kiraengine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.StringReader
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

data class PptSlide(
    val id: String = java.util.UUID.randomUUID().toString(),
    var slideNumber: Int,
    var title: String,
    val bulletPoints: MutableList<String> = mutableListOf(),
    var bgHexColor: String = "#FFFFFF",
    var subtitle: String? = null
)

class PptDocumentEngine : DocumentEngine {
    private var _metadata: DocumentMetadata? = null
    override val metadata: DocumentMetadata? get() = _metadata

    val slides = mutableListOf<PptSlide>()
    var currentSlideIndex: Int = 0

    private val undoStack = mutableListOf<EditCommand>()
    private val redoStack = mutableListOf<EditCommand>()

    override suspend fun open(context: Context, uri: Uri): Result<DocumentMetadata> = withContext(Dispatchers.IO) {
        try {
            close()
            var fileName = "Presentation.pptx"
            var fileSize = 0L

            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: fileName
                    if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                }
            }

            val isLegacyPpt = fileName.endsWith(".ppt", ignoreCase = true)
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext Result.failure(IllegalStateException("Cannot read presentation"))
            if (fileSize == 0L) fileSize = bytes.size.toLong()

            if (isLegacyPpt) {
                parseLegacyPpt(bytes)
            } else {
                parsePptx(bytes)
            }

            if (slides.isEmpty()) {
                slides.add(
                    PptSlide(
                        slideNumber = 1,
                        title = "Trình chiếu Kira",
                        subtitle = "Trình bày chuyên nghiệp và mượt mà",
                        bulletPoints = mutableListOf("Xem slide toàn màn hình", "Chỉnh sửa nội dung nhanh chóng", "Hỗ trợ tệp PPTX và PPT")
                    )
                )
            }

            val meta = DocumentMetadata(
                uri = uri,
                name = fileName,
                type = DocumentType.POWERPOINT,
                sizeBytes = fileSize,
                pageCount = slides.size,
                isReadOnly = isLegacyPpt
            )
            _metadata = meta
            Result.success(meta)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parsePptx(bytes: ByteArray) {
        try {
            val zis = ZipInputStream(ByteArrayInputStream(bytes))
            var entry: ZipEntry? = zis.nextEntry
            val slideEntries = mutableMapOf<String, String>()

            while (entry != null) {
                if (entry.name.startsWith("ppt/slides/slide") && entry.name.endsWith(".xml")) {
                    val content = zis.readBytes().toString(Charsets.UTF_8)
                    slideEntries[entry.name] = content
                }
                entry = zis.nextEntry
            }
            zis.close()

            if (slideEntries.isNotEmpty()) {
                slideEntries.toSortedMap().forEach { (_, xml) ->
                    val (title, bullets) = parseSlideXml(xml)
                    slides.add(
                        PptSlide(
                            slideNumber = slides.size + 1,
                            title = title.ifBlank { "Slide ${slides.size + 1}" },
                            bulletPoints = bullets.toMutableList()
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // Fallback
        }
    }

    private fun parseSlideXml(xml: String): Pair<String, List<String>> {
        var title = ""
        val bullets = mutableListOf<String>()
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))

        var event = parser.eventType
        var currentText = ""

        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name?.lowercase() == "t") {
                val t = parser.nextText().trim()
                if (t.isNotBlank()) {
                    if (title.isBlank()) {
                        title = t
                    } else {
                        bullets.add(t)
                    }
                }
            }
            event = parser.next()
        }
        return Pair(title, bullets)
    }

    private fun parseLegacyPpt(bytes: ByteArray) {
        val text = String(bytes, Charsets.ISO_8859_1).filter { it.code in 32..126 || it == '\n' }
        val lines = text.split("\n").filter { it.trim().length > 4 }
        var currentSlide = PptSlide(slideNumber = 1, title = lines.firstOrNull() ?: "Slide 1")
        for ((idx, line) in lines.drop(1).take(40).withIndex()) {
            if (idx % 4 == 0 && idx != 0) {
                slides.add(currentSlide)
                currentSlide = PptSlide(slideNumber = slides.size + 1, title = line.trim())
            } else {
                currentSlide.bulletPoints.add(line.trim())
            }
        }
        slides.add(currentSlide)
    }

    override fun pageCount(): Int = slides.size.coerceAtLeast(1)

    override suspend fun renderPage(index: Int, targetWidth: Int, targetHeight: Int): Bitmap? = withContext(Dispatchers.Default) {
        val width = if (targetWidth > 0) targetWidth else 1920
        val height = if (targetHeight > 0) targetHeight else 1080

        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val slide = slides.getOrNull(index) ?: return@withContext bmp

        // Background
        val bgPaint = Paint().apply {
            color = try {
                AndroidColor.parseColor(slide.bgHexColor)
            } catch (_: Exception) {
                AndroidColor.WHITE
            }
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Slide frame shadow & top accent bar
        val accentPaint = Paint().apply {
            color = AndroidColor.parseColor("#007AFF")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), 16f, accentPaint)

        // Title
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#111827")
            textSize = 58f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText(slide.title, 120f, 200f, titlePaint)

        // Subtitle if any
        slide.subtitle?.let { sub ->
            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.parseColor("#4B5563")
                textSize = 34f
                typeface = Typeface.DEFAULT
            }
            canvas.drawText(sub, 120f, 260f, subPaint)
        }

        // Bullets
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#1F2937")
            textSize = 38f
            typeface = Typeface.DEFAULT
        }
        val bulletIconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#007AFF")
            style = Paint.Style.FILL
        }

        var yOffset = if (slide.subtitle != null) 360f else 320f
        for (bullet in slide.bulletPoints.take(7)) {
            canvas.drawCircle(130f, yOffset - 12f, 8f, bulletIconPaint)
            canvas.drawText(bullet, 160f, yOffset, bodyPaint)
            yOffset += 75f
        }

        // Slide number footer
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#9CA3AF")
            textSize = 28f
        }
        canvas.drawText("${index + 1} / ${slides.size}", width - 200f, height - 60f, footerPaint)

        bmp
    }

    override fun applyEdit(command: EditCommand): Boolean {
        return when (command) {
            is EditCommand.PptUpdateSlide -> {
                if (command.slideIndex in slides.indices) {
                    val s = slides[command.slideIndex]
                    s.title = command.newTitle
                    s.bulletPoints.clear()
                    s.bulletPoints.addAll(command.newBullets)
                    undoStack.add(command)
                    redoStack.clear()
                    true
                } else false
            }
            is EditCommand.PptAddSlide -> {
                val newSlide = PptSlide(
                    slideNumber = command.index + 1,
                    title = command.title,
                    bulletPoints = command.bullets.toMutableList()
                )
                val idx = command.index.coerceIn(0, slides.size)
                slides.add(idx, newSlide)
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
            is EditCommand.PptUpdateSlide -> {
                if (cmd.slideIndex in slides.indices) {
                    val s = slides[cmd.slideIndex]
                    s.title = cmd.oldTitle
                    s.bulletPoints.clear()
                    s.bulletPoints.addAll(cmd.oldBullets)
                    redoStack.add(cmd)
                }
            }
            is EditCommand.PptAddSlide -> {
                if (cmd.index in slides.indices) {
                    slides.removeAt(cmd.index)
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
            is EditCommand.PptUpdateSlide -> {
                if (cmd.slideIndex in slides.indices) {
                    val s = slides[cmd.slideIndex]
                    s.title = cmd.newTitle
                    s.bulletPoints.clear()
                    s.bulletPoints.addAll(cmd.newBullets)
                    undoStack.add(cmd)
                }
            }
            is EditCommand.PptAddSlide -> {
                val newSlide = PptSlide(slideNumber = cmd.index + 1, title = cmd.title, bulletPoints = cmd.bullets.toMutableList())
                slides.add(cmd.index.coerceIn(0, slides.size), newSlide)
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
            // Write slide text summary
            val sb = StringBuilder()
            for (slide in slides) {
                sb.append("=== Slide ${slide.slideNumber}: ${slide.title} ===\n")
                slide.subtitle?.let { sb.append("Subtitle: $it\n") }
                for (b in slide.bulletPoints) {
                    sb.append("- $b\n")
                }
                sb.append("\n")
            }
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(sb.toString().toByteArray(Charsets.UTF_8))
            }
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun exportToPdf(context: Context, targetFile: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            val doc = PdfDocument()
            for (i in 0 until slides.size) {
                val pageInfo = PdfDocument.PageInfo.Builder(1920, 1080, i + 1).create()
                val page = doc.startPage(pageInfo)
                val bmp = renderPage(i, 1920, 1080)
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
        slides.clear()
        undoStack.clear()
        redoStack.clear()
        currentSlideIndex = 0
    }
}
