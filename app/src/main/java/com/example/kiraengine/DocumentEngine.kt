package com.example.kiraengine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import java.io.File

enum class DocumentType(val displayName: String, val extensions: List<String>) {
    WORD("Word", listOf("docx", "doc")),
    EXCEL("Excel", listOf("xlsx", "xls", "csv")),
    POWERPOINT("PowerPoint", listOf("pptx", "ppt")),
    PDF("PDF", listOf("pdf")),
    TEXT("Văn bản", listOf("txt", "log", "json", "xml", "md"))
}

data class DocumentMetadata(
    val uri: Uri,
    val name: String,
    val type: DocumentType,
    val sizeBytes: Long,
    val pageCount: Int,
    val isReadOnly: Boolean,
    val lastModified: Long = System.currentTimeMillis()
)

sealed interface EditCommand {
    val description: String

    // Word Commands
    data class WordUpdateText(val paragraphIndex: Int, val oldText: String, val newText: String) : EditCommand {
        override val description: String = "Sửa văn bản"
    }
    data class WordToggleFormat(val paragraphIndex: Int, val bold: Boolean? = null, val italic: Boolean? = null, val underline: Boolean? = null) : EditCommand {
        override val description: String = "Đổi định dạng"
    }
    data class WordAddParagraph(val index: Int, val text: String, val isHeading: Boolean = false) : EditCommand {
        override val description: String = "Thêm đoạn văn"
    }
    data class WordInsertTable(val index: Int, val rows: Int, val cols: Int) : EditCommand {
        override val description: String = "Chèn bảng"
    }

    // Excel Commands
    data class ExcelSetCell(val sheetIndex: Int, val row: Int, val col: Int, val oldValue: String, val newValue: String) : EditCommand {
        override val description: String = "Sửa ô [$row, $col]"
    }
    data class ExcelAddSheet(val name: String) : EditCommand {
        override val description: String = "Thêm trang tính mới"
    }
    data class ExcelRenameSheet(val index: Int, val oldName: String, val newName: String) : EditCommand {
        override val description: String = "Đổi tên trang tính"
    }

    // PowerPoint Commands
    data class PptUpdateSlide(val slideIndex: Int, val oldTitle: String, val newTitle: String, val oldBullets: List<String>, val newBullets: List<String>) : EditCommand {
        override val description: String = "Sửa slide $slideIndex"
    }
    data class PptAddSlide(val index: Int, val title: String, val bullets: List<String>) : EditCommand {
        override val description: String = "Thêm slide mới"
    }

    // PDF Annotation Commands
    data class PdfAddAnnotation(val pageIndex: Int, val annotation: PdfAnnotation) : EditCommand {
        override val description: String = "Thêm chú thích PDF"
    }
    data class PdfRemoveAnnotation(val pageIndex: Int, val annotation: PdfAnnotation) : EditCommand {
        override val description: String = "Xóa chú thích PDF"
    }

    // Text Commands
    data class TextUpdate(val oldText: String, val newText: String) : EditCommand {
        override val description: String = "Sửa văn bản"
    }
}

sealed class PdfAnnotation(val id: String = java.util.UUID.randomUUID().toString(), val pageIndex: Int) {
    data class InkDrawing(
        val page: Int,
        val points: List<Pair<Float, Float>>,
        val colorArgb: Int,
        val strokeWidth: Float
    ) : PdfAnnotation(pageIndex = page)

    data class Highlight(
        val page: Int,
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
        val colorArgb: Int
    ) : PdfAnnotation(pageIndex = page)

    data class TextNote(
        val page: Int,
        val x: Float,
        val y: Float,
        val text: String,
        val colorArgb: Int
    ) : PdfAnnotation(pageIndex = page)
}

/**
 * Universal decoupled Document Engine interface.
 * UI interacts exclusively via this interface.
 */
interface DocumentEngine {
    val metadata: DocumentMetadata?
    suspend fun open(context: Context, uri: Uri): Result<DocumentMetadata>
    fun pageCount(): Int
    suspend fun renderPage(index: Int, targetWidth: Int, targetHeight: Int): Bitmap?
    fun applyEdit(command: EditCommand): Boolean
    fun undo(): Boolean
    fun redo(): Boolean
    fun canUndo(): Boolean
    fun canRedo(): Boolean
    suspend fun save(context: Context, targetUri: Uri? = null): Result<Uri>
    suspend fun exportToPdf(context: Context, targetFile: File): Result<File>
    fun close()
}
