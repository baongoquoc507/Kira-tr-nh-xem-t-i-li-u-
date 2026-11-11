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
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader

class TextDocumentEngine : DocumentEngine {
    private var _metadata: DocumentMetadata? = null
    override val metadata: DocumentMetadata? get() = _metadata

    var content: String = ""
    val lines = mutableListOf<String>()

    private val undoStack = mutableListOf<EditCommand>()
    private val redoStack = mutableListOf<EditCommand>()

    override suspend fun open(context: Context, uri: Uri): Result<DocumentMetadata> = withContext(Dispatchers.IO) {
        try {
            close()
            var fileName = "Document.txt"
            var fileSize = 0L

            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: fileName
                    if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                }
            }

            val sb = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).useLines { lineSeq ->
                    for (line in lineSeq) {
                        lines.add(line)
                        sb.append(line).append("\n")
                    }
                }
            }
            content = sb.toString()
            if (fileSize == 0L) fileSize = content.toByteArray(Charsets.UTF_8).size.toLong()

            val meta = DocumentMetadata(
                uri = uri,
                name = fileName,
                type = DocumentType.TEXT,
                sizeBytes = fileSize,
                pageCount = pageCount(),
                isReadOnly = false
            )
            _metadata = meta
            Result.success(meta)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun pageCount(): Int = (lines.size / 45 + 1).coerceAtLeast(1)

    override suspend fun renderPage(index: Int, targetWidth: Int, targetHeight: Int): Bitmap? = withContext(Dispatchers.Default) {
        val width = if (targetWidth > 0) targetWidth else 1080
        val height = if (targetHeight > 0) targetHeight else 1920

        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(AndroidColor.WHITE)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.BLACK
            textSize = 28f
            typeface = Typeface.MONOSPACE
        }
        val lineNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.LTGRAY
            textSize = 24f
            typeface = Typeface.MONOSPACE
        }

        val linesPerPage = 45
        val startLine = index * linesPerPage
        val endLine = minOf(startLine + linesPerPage, lines.size)

        var y = 80f
        for (i in startLine until endLine) {
            val lineNum = (i + 1).toString().padStart(4, ' ')
            canvas.drawText(lineNum, 30f, y, lineNumPaint)
            canvas.drawText(lines[i], 120f, y, paint)
            y += 38f
        }
        bmp
    }

    override fun applyEdit(command: EditCommand): Boolean {
        return when (command) {
            is EditCommand.TextUpdate -> {
                content = command.newText
                lines.clear()
                lines.addAll(content.split("\n"))
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
            is EditCommand.TextUpdate -> {
                content = cmd.oldText
                lines.clear()
                lines.addAll(content.split("\n"))
                redoStack.add(cmd)
            }
            else -> return false
        }
        return true
    }

    override fun redo(): Boolean {
        if (redoStack.isEmpty()) return false
        val cmd = redoStack.removeAt(redoStack.lastIndex)
        when (cmd) {
            is EditCommand.TextUpdate -> {
                content = cmd.newText
                lines.clear()
                lines.addAll(content.split("\n"))
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
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(content.toByteArray(Charsets.UTF_8))
            }
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun exportToPdf(context: Context, targetFile: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            val doc = PdfDocument()
            val total = pageCount()
            for (i in 0 until total) {
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
        content = ""
        lines.clear()
        undoStack.clear()
        redoStack.clear()
    }
}
