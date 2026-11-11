package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.RecentDocumentEntity
import com.example.data.SampleFilesGenerator
import com.example.kiraengine.DocumentEngineFactory
import com.example.kiraengine.DocumentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

enum class SortOption {
    DATE_DESC,
    DATE_ASC,
    NAME_ASC,
    NAME_DESC,
    SIZE_DESC,
    SIZE_ASC
}

enum class HomeTab {
    RECENT,
    FAVORITES,
    ALL,
    SETTINGS
}

class HomeViewModel(private val database: AppDatabase) : ViewModel() {
    val currentTab = MutableStateFlow(HomeTab.RECENT)
    val searchQuery = MutableStateFlow("")
    val selectedFilterType = MutableStateFlow<DocumentType?>(null)
    val sortOption = MutableStateFlow(SortOption.DATE_DESC)

    private val allDocumentsFlow = database.documentDao().getRecentDocuments()
    private val favoriteDocumentsFlow = database.documentDao().getFavoriteDocuments()

    private data class FilterParams(
        val tab: HomeTab,
        val query: String,
        val filterType: DocumentType?,
        val sort: SortOption
    )

    private val filterParams = combine(
        currentTab,
        searchQuery,
        selectedFilterType,
        sortOption
    ) { tab, query, filterType, sort ->
        FilterParams(tab, query, filterType, sort)
    }

    val filteredDocuments: StateFlow<List<RecentDocumentEntity>> = combine(
        allDocumentsFlow,
        favoriteDocumentsFlow,
        filterParams
    ) { allDocs, favDocs, params ->
        val baseList = when (params.tab) {
            HomeTab.RECENT -> allDocs
            HomeTab.FAVORITES -> favDocs
            HomeTab.ALL -> allDocs
            HomeTab.SETTINGS -> emptyList()
        }

        var result = baseList

        // Search filter
        if (params.query.isNotBlank()) {
            result = result.filter { it.name.contains(params.query, ignoreCase = true) }
        }

        // Type filter
        if (params.filterType != null) {
            result = result.filter { it.type == params.filterType.name }
        }

        // Sort
        when (params.sort) {
            SortOption.DATE_DESC -> result.sortedByDescending { it.lastOpenedTimestamp }
            SortOption.DATE_ASC -> result.sortedBy { it.lastOpenedTimestamp }
            SortOption.NAME_ASC -> result.sortedBy { it.name.lowercase() }
            SortOption.NAME_DESC -> result.sortedByDescending { it.name.lowercase() }
            SortOption.SIZE_DESC -> result.sortedByDescending { it.sizeBytes }
            SortOption.SIZE_ASC -> result.sortedBy { it.sizeBytes }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun initializeSamples(context: Context) {
        viewModelScope.launch {
            SampleFilesGenerator.generateSamples(context, database)
        }
    }

    fun onDocumentOpened(context: Context, uri: Uri) {
        viewModelScope.launch {
            var fileName = uri.lastPathSegment ?: "Document"
            var fileSize = 0L

            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: fileName
                    if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                }
            }

            val docType = DocumentEngineFactory.detectType(context, uri)
            database.documentDao().insertOrUpdate(
                RecentDocumentEntity(
                    uri = uri.toString(),
                    name = fileName,
                    type = docType.name,
                    sizeBytes = fileSize,
                    lastOpenedTimestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun toggleFavorite(uri: String, currentStatus: Boolean) {
        viewModelScope.launch {
            database.documentDao().toggleFavorite(uri, !currentStatus)
        }
    }

    fun renameDocument(uri: String, newName: String) {
        viewModelScope.launch {
            database.documentDao().renameDocument(uri, newName)
        }
    }

    fun deleteDocument(uri: String) {
        viewModelScope.launch {
            database.documentDao().deleteDocument(uri)
        }
    }

    fun createNewDocument(context: Context, type: DocumentType, title: String): Uri? {
        val dir = File(context.filesDir, "my_documents").apply { mkdirs() }
        val ext = when (type) {
            DocumentType.WORD -> ".docx"
            DocumentType.EXCEL -> ".xlsx"
            DocumentType.POWERPOINT -> ".pptx"
            DocumentType.PDF -> ".pdf"
            DocumentType.TEXT -> ".txt"
        }
        val safeName = title.trim().replace("[^a-zA-Z0-9_.-]".toRegex(), "_") + ext
        val file = File(dir, safeName)

        when (type) {
            DocumentType.TEXT -> file.writeText("Văn bản mới được tạo bởi Kira Document Viewer.\n", Charsets.UTF_8)
            DocumentType.EXCEL -> file.writeText("Mục,Số lượng,Đơn giá,Thành tiền\nSản phẩm 1,10,50000,500000\n", Charsets.UTF_8)
            else -> file.writeText("", Charsets.UTF_8)
        }

        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        onDocumentOpened(context, uri)
        return uri
    }
}
