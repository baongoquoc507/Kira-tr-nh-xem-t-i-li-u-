package com.example.kiraengine

import android.content.Context
import android.net.Uri

object DocumentEngineFactory {
    fun createEngine(context: Context, uri: Uri): DocumentEngine {
        val type = detectType(context, uri)
        return when (type) {
            DocumentType.PDF -> PdfDocumentEngine()
            DocumentType.WORD -> WordDocumentEngine()
            DocumentType.EXCEL -> ExcelDocumentEngine()
            DocumentType.POWERPOINT -> PptDocumentEngine()
            DocumentType.TEXT -> TextDocumentEngine()
        }
    }

    fun detectType(context: Context, uri: Uri): DocumentType {
        var fileName = uri.lastPathSegment?.lowercase() ?: ""

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex != -1) {
                fileName = cursor.getString(nameIndex)?.lowercase() ?: fileName
            }
        }

        val mimeType = context.contentResolver.getType(uri)?.lowercase() ?: ""

        return when {
            fileName.endsWith(".pdf") || mimeType.contains("pdf") -> DocumentType.PDF
            fileName.endsWith(".docx") || fileName.endsWith(".doc") || mimeType.contains("word") -> DocumentType.WORD
            fileName.endsWith(".xlsx") || fileName.endsWith(".xls") || fileName.endsWith(".csv") ||
                mimeType.contains("spreadsheet") || mimeType.contains("excel") || mimeType.contains("csv") -> DocumentType.EXCEL
            fileName.endsWith(".pptx") || fileName.endsWith(".ppt") || mimeType.contains("presentation") || mimeType.contains("powerpoint") -> DocumentType.POWERPOINT
            else -> DocumentType.TEXT
        }
    }
}
