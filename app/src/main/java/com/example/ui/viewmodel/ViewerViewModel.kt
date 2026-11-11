package com.example.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.kiraengine.DocumentEngine
import com.example.kiraengine.DocumentEngineFactory
import com.example.kiraengine.DocumentMetadata
import com.example.kiraengine.EditCommand
import com.example.kiraengine.PdfAnnotation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

enum class ReadingMode {
    NORMAL,
    NIGHT,
    SEPIA
}

enum class AnnotationTool {
    NONE,
    PEN,
    HIGHLIGHT,
    NOTE,
    ERASER
}

class ViewerViewModel(private val database: AppDatabase) : ViewModel() {
    private var engine: DocumentEngine? = null

    private val _metadata = MutableStateFlow<DocumentMetadata?>(null)
    val metadata = _metadata.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _currentPage = MutableStateFlow(0)
    val currentPage = _currentPage.asStateFlow()

    private val _pageCount = MutableStateFlow(1)
    val pageCount = _pageCount.asStateFlow()

    private val _readingMode = MutableStateFlow(ReadingMode.NORMAL)
    val readingMode = _readingMode.asStateFlow()

    private val _zoomScale = MutableStateFlow(1f)
    val zoomScale = _zoomScale.asStateFlow()

    // Annotation state
    private val _annotationTool = MutableStateFlow(AnnotationTool.NONE)
    val annotationTool = _annotationTool.asStateFlow()

    val annotationColor = MutableStateFlow(android.graphics.Color.parseColor("#007AFF"))
    val annotationStrokeWidth = MutableStateFlow(5f)

    // Edit state
    private val _canUndo = MutableStateFlow(false)
    val canUndo = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo = _canRedo.asStateFlow()

    // Search state
    val searchKeyword = MutableStateFlow("")
    val isSearchActive = MutableStateFlow(false)

    // Current page rendered bitmap for PDF/Slide
    private val _currentPageBitmap = MutableStateFlow<Bitmap?>(null)
    val currentPageBitmap = _currentPageBitmap.asStateFlow()

    fun openDocument(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                engine?.close()
                val newEngine = DocumentEngineFactory.createEngine(context, uri)
                engine = newEngine

                val result = newEngine.open(context, uri)
                if (result.isSuccess) {
                    val meta = result.getOrThrow()
                    _metadata.value = meta
                    _pageCount.value = newEngine.pageCount().coerceAtLeast(1)
                    _currentPage.value = 0
                    updateUndoRedo()
                    renderCurrentPage()

                    // Record reading session in DB
                    database.documentDao().insertOrUpdate(
                        com.example.data.RecentDocumentEntity(
                            uri = uri.toString(),
                            name = meta.name,
                            type = meta.type.name,
                            sizeBytes = meta.sizeBytes,
                            lastOpenedTimestamp = System.currentTimeMillis()
                        )
                    )
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Không thể mở tài liệu"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Lỗi khi mở tài liệu"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getActiveEngine(): DocumentEngine? = engine

    fun setPage(index: Int) {
        val count = _pageCount.value
        val clamped = index.coerceIn(0, (count - 1).coerceAtLeast(0))
        if (_currentPage.value != clamped) {
            _currentPage.value = clamped
            renderCurrentPage()
            persistReadingPosition(clamped)
        }
    }

    private fun persistReadingPosition(page: Int) {
        val uri = _metadata.value?.uri?.toString() ?: return
        viewModelScope.launch {
            val progress = (page + 1).toFloat() / _pageCount.value.coerceAtLeast(1).toFloat()
            database.documentDao().updateReadingProgress(uri, page, progress)
        }
    }

    fun setReadingMode(mode: ReadingMode) {
        _readingMode.value = mode
    }

    fun setZoomScale(scale: Float) {
        _zoomScale.value = scale.coerceIn(0.75f, 4.0f)
    }

    fun setAnnotationTool(tool: AnnotationTool) {
        _annotationTool.value = tool
    }

    fun renderCurrentPage(targetWidth: Int = 1080, targetHeight: Int = 1920) {
        val eng = engine ?: return
        viewModelScope.launch {
            val bmp = eng.renderPage(_currentPage.value, targetWidth, targetHeight)
            _currentPageBitmap.value = bmp
        }
    }

    fun applyEdit(cmd: EditCommand): Boolean {
        val eng = engine ?: return false
        val success = eng.applyEdit(cmd)
        if (success) {
            updateUndoRedo()
            renderCurrentPage()
        }
        return success
    }

    fun undo() {
        val eng = engine ?: return
        if (eng.undo()) {
            updateUndoRedo()
            renderCurrentPage()
        }
    }

    fun redo() {
        val eng = engine ?: return
        if (eng.redo()) {
            updateUndoRedo()
            renderCurrentPage()
        }
    }

    private fun updateUndoRedo() {
        _canUndo.value = engine?.canUndo() ?: false
        _canRedo.value = engine?.canRedo() ?: false
    }

    fun saveDocument(context: Context, onComplete: (Boolean, String?) -> Unit) {
        val eng = engine ?: return
        viewModelScope.launch {
            val res = eng.save(context)
            if (res.isSuccess) {
                onComplete(true, "Đã lưu tài liệu thành công")
            } else {
                onComplete(false, res.exceptionOrNull()?.localizedMessage ?: "Lỗi khi lưu tài liệu")
            }
        }
    }

    fun exportToPdf(context: Context, onComplete: (Boolean, Uri?) -> Unit) {
        val eng = engine ?: return
        viewModelScope.launch {
            val exportFile = File(context.cacheDir, "Export_${System.currentTimeMillis()}.pdf")
            val res = eng.exportToPdf(context, exportFile)
            if (res.isSuccess) {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    res.getOrThrow()
                )
                onComplete(true, uri)
            } else {
                onComplete(false, null)
            }
        }
    }

    fun shareDocument(context: Context) {
        val meta = _metadata.value ?: return
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_STREAM, meta.uri)
                putExtra(Intent.EXTRA_TITLE, meta.name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Chia sẻ ${meta.name}"))
        } catch (_: Exception) {}
    }

    override fun onCleared() {
        super.onCleared()
        engine?.close()
        engine = null
    }
}
