package com.example.kiraengine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PdfDocumentEngine : DocumentEngine {
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null
    private var tempPdfFile: File? = null
    private var _metadata: DocumentMetadata? = null
    override val metadata: DocumentMetadata? get() = _metadata

    // LRU Cache for rendered page bitmaps (visible + pre-rendered neighbors)
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8 // 1/8th of available memory
    private val bitmapCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    // Annotation lists
    private val annotations = mutableListOf<PdfAnnotation>()
    private val undoStack = mutableListOf<EditCommand>()
    private val redoStack = mutableListOf<EditCommand>()

    override suspend fun open(context: Context, uri: Uri): Result<DocumentMetadata> = withContext(Dispatchers.IO) {
        try {
            close()
            var fileName = "Document.pdf"
            var fileSize = 0L

            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: fileName
                    if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                }
            }

            // Copy to private cache file for safe random-access ParcelFileDescriptor
            val tempFile = File(context.cacheDir, "kira_pdf_${System.currentTimeMillis()}.pdf")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (fileSize == 0L) fileSize = tempFile.length()
            tempPdfFile = tempFile

            fileDescriptor = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fileDescriptor!!)
            pdfRenderer = renderer

            val meta = DocumentMetadata(
                uri = uri,
                name = fileName,
                type = DocumentType.PDF,
                sizeBytes = fileSize,
                pageCount = renderer.pageCount,
                isReadOnly = false
            )
            _metadata = meta
            Result.success(meta)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun pageCount(): Int = pdfRenderer?.pageCount ?: 0

    override suspend fun renderPage(index: Int, targetWidth: Int, targetHeight: Int): Bitmap? = withContext(Dispatchers.Default) {
        val renderer = pdfRenderer ?: return@withContext null
        if (index < 0 || index >= renderer.pageCount) return@withContext null

        val cacheKey = "$index-${targetWidth}x$targetHeight"
        bitmapCache.get(cacheKey)?.let { return@withContext it }

        var pageBitmap: Bitmap? = null
        synchronized(renderer) {
            val page = renderer.openPage(index)
            val width = if (targetWidth > 0) targetWidth else page.width
            val height = if (targetHeight > 0) targetHeight else page.height

            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            canvas.drawColor(AndroidColor.WHITE)
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()

            // Paint user annotations on top of page bitmap
            drawAnnotations(canvas, index, width.toFloat() / page.width.toFloat())

            bitmapCache.put(cacheKey, bmp)
            pageBitmap = bmp
        }
        pageBitmap
    }

    private fun drawAnnotations(canvas: Canvas, pageIndex: Int, scale: Float) {
        val pageAnnotations = annotations.filter { it.pageIndex == pageIndex }
        for (ann in pageAnnotations) {
            when (ann) {
                is PdfAnnotation.Highlight -> {
                    val paint = Paint().apply {
                        color = ann.colorArgb
                        style = Paint.Style.FILL
                        alpha = 110 // Semi-transparent highlight
                    }
                    canvas.drawRect(
                        ann.left * scale,
                        ann.top * scale,
                        ann.right * scale,
                        ann.bottom * scale,
                        paint
                    )
                }
                is PdfAnnotation.InkDrawing -> {
                    if (ann.points.size > 1) {
                        val paint = Paint().apply {
                            color = ann.colorArgb
                            style = Paint.Style.STROKE
                            strokeWidth = ann.strokeWidth * scale
                            strokeCap = Paint.Cap.ROUND
                            strokeJoin = Paint.Join.ROUND
                            isAntiAlias = true
                        }
                        val path = Path()
                        path.moveTo(ann.points[0].first * scale, ann.points[0].second * scale)
                        for (i in 1 until ann.points.size) {
                            path.lineTo(ann.points[i].first * scale, ann.points[i].second * scale)
                        }
                        canvas.drawPath(path, paint)
                    }
                }
                is PdfAnnotation.TextNote -> {
                    val paint = Paint().apply {
                        color = ann.colorArgb
                        textSize = 28f * scale
                        isAntiAlias = true
                    }
                    canvas.drawText(ann.text, ann.x * scale, ann.y * scale, paint)
                }
            }
        }
    }

    fun getAnnotationsForPage(pageIndex: Int): List<PdfAnnotation> {
        return annotations.filter { it.pageIndex == pageIndex }
    }

    override fun applyEdit(command: EditCommand): Boolean {
        return when (command) {
            is EditCommand.PdfAddAnnotation -> {
                annotations.add(command.annotation)
                undoStack.add(command)
                redoStack.clear()
                bitmapCache.evictAll()
                true
            }
            is EditCommand.PdfRemoveAnnotation -> {
                annotations.remove(command.annotation)
                undoStack.add(command)
                redoStack.clear()
                bitmapCache.evictAll()
                true
            }
            else -> false
        }
    }

    override fun undo(): Boolean {
        if (undoStack.isEmpty()) return false
        val cmd = undoStack.removeAt(undoStack.lastIndex)
        when (cmd) {
            is EditCommand.PdfAddAnnotation -> {
                annotations.remove(cmd.annotation)
                redoStack.add(cmd)
            }
            is EditCommand.PdfRemoveAnnotation -> {
                annotations.add(cmd.annotation)
                redoStack.add(cmd)
            }
            else -> return false
        }
        bitmapCache.evictAll()
        return true
    }

    override fun redo(): Boolean {
        if (redoStack.isEmpty()) return false
        val cmd = redoStack.removeAt(redoStack.lastIndex)
        when (cmd) {
            is EditCommand.PdfAddAnnotation -> {
                annotations.add(cmd.annotation)
                undoStack.add(cmd)
            }
            is EditCommand.PdfRemoveAnnotation -> {
                annotations.remove(cmd.annotation)
                undoStack.add(cmd)
            }
            else -> return false
        }
        bitmapCache.evictAll()
        return true
    }

    override fun canUndo(): Boolean = undoStack.isNotEmpty()
    override fun canRedo(): Boolean = redoStack.isNotEmpty()

    override suspend fun save(context: Context, targetUri: Uri?): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val uri = targetUri ?: _metadata?.uri ?: return@withContext Result.failure(IllegalStateException("No target URI"))
            val targetFile = File(context.cacheDir, "saved_${System.currentTimeMillis()}.pdf")
            exportToPdf(context, targetFile).getOrThrow()

            context.contentResolver.openOutputStream(uri)?.use { out ->
                targetFile.inputStream().use { it.copyTo(out) }
            }
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun exportToPdf(context: Context, targetFile: File): Result<File> = withContext(Dispatchers.IO) {
        val renderer = pdfRenderer ?: return@withContext Result.failure(IllegalStateException("Renderer not open"))
        try {
            val document = PdfDocument()
            for (i in 0 until renderer.pageCount) {
                val pageInfo = PdfDocument.PageInfo.Builder(1080, 1920, i + 1).create()
                val page = document.startPage(pageInfo)
                val bmp = renderPage(i, 1080, 1920)
                if (bmp != null) {
                    page.canvas.drawBitmap(bmp, 0f, 0f, null)
                }
                document.finishPage(page)
            }
            FileOutputStream(targetFile).use { fos ->
                document.writeTo(fos)
            }
            document.close()
            Result.success(targetFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun close() {
        try {
            bitmapCache.evictAll()
            pdfRenderer?.close()
            fileDescriptor?.close()
            tempPdfFile?.delete()
        } catch (_: Exception) {}
        pdfRenderer = null
        fileDescriptor = null
        tempPdfFile = null
        annotations.clear()
        undoStack.clear()
        redoStack.clear()
    }
}
