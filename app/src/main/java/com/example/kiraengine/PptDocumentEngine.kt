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

data class PptTextRun(
    var text: String = "",
    var isBold: Boolean = false,
    var isItalic: Boolean = false,
    var colorHex: String? = null,
    var fontSizePt: Float = 16f
)

data class PptParagraph(
    val runs: MutableList<PptTextRun> = mutableListOf(),
    var isBullet: Boolean = false,
    var alignment: String = "left"
) {
    val fullText: String get() = runs.joinToString("") { it.text }
}

data class PptShape(
    var id: String = java.util.UUID.randomUUID().toString(),
    var xRatio: Float = 0.05f,
    var yRatio: Float = 0.1f,
    var wRatio: Float = 0.9f,
    var hRatio: Float = 0.2f,
    var bgHex: String? = null,
    val paragraphs: MutableList<PptParagraph> = mutableListOf(),
    var isTitle: Boolean = false,
    var imageBitmap: Bitmap? = null
)

data class PptSlide(
    val id: String = java.util.UUID.randomUUID().toString(),
    var slideNumber: Int,
    var title: String = "",
    val bulletPoints: MutableList<String> = mutableListOf(),
    var bgHexColor: String = "#FFFFFF",
    var subtitle: String? = null,
    val shapes: MutableList<PptShape> = mutableListOf(),
    var notes: String = ""
)

class PptDocumentEngine : DocumentEngine {
    private var _metadata: DocumentMetadata? = null
    override val metadata: DocumentMetadata? get() = _metadata

    val slides = mutableListOf<PptSlide>()
    val mediaImages = mutableMapOf<String, Bitmap>()
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
                ?: return@withContext Result.failure(IllegalStateException("Không thể đọc tệp trình chiếu"))
            if (fileSize == 0L) fileSize = bytes.size.toLong()

            if (isLegacyPpt) {
                parseLegacyPpt(bytes)
            } else {
                parsePptxArchive(bytes)
            }

            if (slides.isEmpty()) {
                val s1 = PptSlide(
                    slideNumber = 1,
                    title = "Trình chiếu Kira",
                    subtitle = "Xem và trình chiếu như Microsoft PowerPoint",
                    bulletPoints = mutableListOf(
                        "Bố cục tỷ lệ 16:9 chuẩn văn phòng",
                        "Chế độ trình chiếu toàn màn hình mượt mà",
                        "100% Hoàn toàn ngoại tuyến"
                    ),
                    bgHexColor = "#FFFFFF"
                )
                slides.add(s1)
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

    private fun parsePptxArchive(bytes: ByteArray) {
        try {
            val zis = ZipInputStream(ByteArrayInputStream(bytes))
            var entry: ZipEntry? = zis.nextEntry
            val slideEntries = mutableMapOf<String, String>()

            while (entry != null) {
                val name = entry.name
                when {
                    name.startsWith("ppt/slides/slide") && name.endsWith(".xml") -> {
                        slideEntries[name] = readZipStreamToString(zis)
                    }
                    name.startsWith("ppt/media/") -> {
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

            if (slideEntries.isNotEmpty()) {
                slideEntries.toSortedMap().forEach { (_, xml) ->
                    val slide = parseSlideXmlDetailed(xml)
                    slide.slideNumber = slides.size + 1
                    slides.add(slide)
                }
            }
        } catch (_: Exception) {}
    }

    private fun parseSlideXmlDetailed(xml: String): PptSlide {
        val slide = PptSlide(slideNumber = 1)
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))

        var event = parser.eventType
        var currentShape: PptShape? = null
        var currentParagraph: PptParagraph? = null
        var currentRun: PptTextRun? = null

        var inBg = false
        var slideBgColor: String? = null

        // Standard 16:9 PPTX canvas: 12192000 x 6858000 EMUs
        val slideW = 12192000f
        val slideH = 6858000f

        while (event != XmlPullParser.END_DOCUMENT) {
            val tag = parser.name?.lowercase() ?: ""
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (tag) {
                        "bg" -> inBg = true
                        "srgbclr" -> {
                            val hex = parser.getAttributeValue(null, "val")
                            if (!hex.isNullOrEmpty()) {
                                val fullHex = if (hex.startsWith("#")) hex else "#$hex"
                                if (inBg) slideBgColor = fullHex
                                else currentRun?.colorHex = fullHex
                            }
                        }
                        "sp" -> {
                            currentShape = PptShape()
                        }
                        "off" -> {
                            val x = parser.getAttributeValue(null, "x")?.toFloatOrNull() ?: 0f
                            val y = parser.getAttributeValue(null, "y")?.toFloatOrNull() ?: 0f
                            currentShape?.xRatio = (x / slideW).coerceIn(0f, 0.95f)
                            currentShape?.yRatio = (y / slideH).coerceIn(0f, 0.95f)
                        }
                        "ext" -> {
                            val cx = parser.getAttributeValue(null, "cx")?.toFloatOrNull() ?: 0f
                            val cy = parser.getAttributeValue(null, "cy")?.toFloatOrNull() ?: 0f
                            if (cx > 0) currentShape?.wRatio = (cx / slideW).coerceIn(0.05f, 1f)
                            if (cy > 0) currentShape?.hRatio = (cy / slideH).coerceIn(0.05f, 1f)
                        }
                        "p" -> {
                            currentParagraph = PptParagraph()
                        }
                        "buauto", "buchar", "bufont" -> {
                            currentParagraph?.isBullet = true
                        }
                        "r" -> {
                            currentRun = PptTextRun()
                        }
                        "rpr" -> {
                            val sz = parser.getAttributeValue(null, "sz")?.toFloatOrNull()
                            if (sz != null && sz > 0) {
                                currentRun?.fontSizePt = (sz / 100f).coerceIn(10f, 60f)
                            }
                            val b = parser.getAttributeValue(null, "b")
                            if (b == "1" || b == "true") currentRun?.isBold = true
                            val i = parser.getAttributeValue(null, "i")
                            if (i == "1" || i == "true") currentRun?.isItalic = true
                        }
                        "t" -> {
                            val text = parser.nextText()
                            if (currentRun != null) {
                                currentRun.text += text
                            } else if (currentParagraph != null) {
                                currentParagraph.runs.add(PptTextRun(text = text))
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (tag) {
                        "bg" -> inBg = false
                        "r" -> {
                            if (currentRun != null && currentRun.text.isNotBlank()) {
                                currentParagraph?.runs?.add(currentRun)
                            }
                            currentRun = null
                        }
                        "p" -> {
                            if (currentParagraph != null && currentParagraph.runs.isNotEmpty()) {
                                currentShape?.paragraphs?.add(currentParagraph)
                                val text = currentParagraph.fullText
                                if (slide.title.isBlank() && text.length < 100 && (currentShape?.yRatio ?: 1f) < 0.4f) {
                                    slide.title = text
                                    currentShape?.isTitle = true
                                } else {
                                    slide.bulletPoints.add(text)
                                }
                            }
                            currentParagraph = null
                        }
                        "sp" -> {
                            if (currentShape != null && currentShape.paragraphs.isNotEmpty()) {
                                slide.shapes.add(currentShape)
                            }
                            currentShape = null
                        }
                    }
                }
            }
            event = parser.next()
        }

        if (slideBgColor != null) slide.bgHexColor = slideBgColor
        if (slide.title.isBlank()) slide.title = "Slide ${slide.slideNumber}"
        return slide
    }

    private fun parseLegacyPpt(bytes: ByteArray) {
        val text = String(bytes, Charsets.ISO_8859_1).filter { it.code in 32..126 || it == '\n' }
        val lines = text.split("\n").filter { it.trim().length > 4 }
        var currentSlide = PptSlide(slideNumber = 1, title = lines.firstOrNull() ?: "Slide 1")
        for ((idx, line) in lines.drop(1).take(50).withIndex()) {
            if (idx % 4 == 0 && idx != 0) {
                slides.add(currentSlide)
                currentSlide = PptSlide(slideNumber = slides.size + 1, title = line.trim())
            } else {
                currentSlide.bulletPoints.add(line.trim())
            }
        }
        slides.add(currentSlide)
    }

    private fun readZipStreamToString(zis: ZipInputStream): String {
        val baos = ByteArrayOutputStream()
        val buf = ByteArray(2048)
        var len: Int
        while (zis.read(buf).also { len = it } > 0) {
            baos.write(buf, 0, len)
        }
        return baos.toString("UTF-8")
    }

    private fun readZipStreamToBytes(zis: ZipInputStream): ByteArray {
        val baos = ByteArrayOutputStream()
        val buf = ByteArray(2048)
        var len: Int
        while (zis.read(buf).also { len = it } > 0) {
            baos.write(buf, 0, len)
        }
        return baos.toByteArray()
    }

    override fun pageCount(): Int = slides.size.coerceAtLeast(1)

    override suspend fun renderPage(index: Int, targetWidth: Int, targetHeight: Int): Bitmap? = withContext(Dispatchers.Default) {
        // Standard 16:9 widescreen PowerPoint aspect ratio
        val width = if (targetWidth > 0) targetWidth else 1920
        val height = if (targetHeight > 0) targetHeight else (width * 9f / 16f).toInt()

        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val slide = slides.getOrNull(index) ?: return@withContext bmp

        // Background
        val bgPaint = Paint().apply {
            color = try { AndroidColor.parseColor(slide.bgHexColor) } catch (_: Exception) { AndroidColor.WHITE }
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Top PowerPoint Brand Stripe
        val accentPaint = Paint().apply {
            color = AndroidColor.parseColor("#C43E1C") // PowerPoint Orange-Red
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), 14f, accentPaint)

        // Title
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#111827")
            textSize = 54f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText(slide.title, 100f, 180f, titlePaint)

        // Subtitle
        slide.subtitle?.let { sub ->
            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.parseColor("#6B7280")
                textSize = 30f
            }
            canvas.drawText(sub, 100f, 235f, subPaint)
        }

        // Bullets
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#1F2937")
            textSize = 36f
        }
        val bulletIconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#C43E1C")
            style = Paint.Style.FILL
        }

        var yOffset = if (slide.subtitle != null) 330f else 280f
        for (bullet in slide.bulletPoints.take(7)) {
            canvas.drawCircle(110f, yOffset - 12f, 7f, bulletIconPaint)
            canvas.drawText(bullet, 140f, yOffset, bodyPaint)
            yOffset += 72f
        }

        // Footer
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#9CA3AF")
            textSize = 24f
        }
        canvas.drawText("${index + 1} / ${slides.size}", width - 160f, height - 50f, footerPaint)

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
        mediaImages.clear()
        undoStack.clear()
        redoStack.clear()
        currentSlideIndex = 0
    }
}
