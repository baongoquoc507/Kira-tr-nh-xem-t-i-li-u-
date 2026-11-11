package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RecentDocumentEntity
import com.example.kiraengine.DocumentType
import com.example.ui.components.GlassSurface
import com.example.ui.components.IosChip
import com.example.ui.components.IosSearchField
import com.example.ui.components.IosSegmentedControl
import com.example.ui.theme.ExcelGreen
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.IosBlue
import com.example.ui.theme.LocalLiquidGlass
import com.example.ui.theme.PdfRed
import com.example.ui.theme.PptOrange
import com.example.ui.theme.TextGray
import com.example.ui.theme.WordBlue
import com.example.ui.viewmodel.HomeTab
import com.example.ui.viewmodel.HomeViewModel
import com.example.ui.viewmodel.SortOption
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenDocument: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val glass = LocalLiquidGlass.current

    val currentTab by viewModel.currentTab.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilterType.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val documents by viewModel.filteredDocuments.collectAsState()

    var showSortSheet by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var renameTargetDoc by remember { mutableStateOf<RecentDocumentEntity?>(null) }
    var deleteTargetDoc by remember { mutableStateOf<RecentDocumentEntity?>(null) }

    // SAF Open Document Picker
    val openDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onDocumentOpened(context, uri)
            onOpenDocument(uri)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // Header Section
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "KIRA DOCS",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                letterSpacing = 1.2.sp,
                                color = glass.accentColor
                            )
                            Text(
                                text = "Tài liệu",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp,
                                color = glass.contentPrimary
                            )
                        }

                        // Top Action Buttons
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Quick Sample Files
                            IconButton(
                                onClick = { viewModel.initializeSamples(context) },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Tạo tệp mẫu",
                                    tint = glass.contentPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Sort Button
                            IconButton(
                                onClick = { showSortSheet = true },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sort,
                                    contentDescription = "Sắp xếp",
                                    tint = glass.contentPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Open File from Device (SAF)
                            IconButton(
                                onClick = {
                                    openDocLauncher.launch(
                                        arrayOf(
                                            "application/pdf",
                                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                            "application/msword",
                                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                            "application/vnd.ms-excel",
                                            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                                            "application/vnd.ms-powerpoint",
                                            "text/plain",
                                            "text/csv",
                                            "*/*"
                                        )
                                    )
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = "Mở tệp",
                                    tint = glass.accentColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // Create New Document
                            IconButton(
                                onClick = { showCreateDialog = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(glass.accentColor)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Tạo mới",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Search Field
                    IosSearchField(
                        query = searchQuery,
                        onQueryChange = { viewModel.searchQuery.value = it },
                        placeholder = "Tìm tài liệu, bảng tính, bài trình bày…"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Tab Segmented Control
                    val tabs = listOf("Gần đây", "Yêu thích", "Tất cả")
                    val currentTabIdx = when (currentTab) {
                        HomeTab.RECENT -> 0
                        HomeTab.FAVORITES -> 1
                        HomeTab.ALL -> 2
                        HomeTab.SETTINGS -> 0
                    }
                    IosSegmentedControl(
                        items = tabs,
                        selectedIndex = currentTabIdx,
                        onItemSelected = { idx ->
                            viewModel.currentTab.value = when (idx) {
                                0 -> HomeTab.RECENT
                                1 -> HomeTab.FAVORITES
                                else -> HomeTab.ALL
                            }
                        },
                        labelProvider = { it }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Filter Chips Row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            IosChip(
                                selected = selectedFilter == null,
                                onClick = { viewModel.selectedFilterType.value = null },
                                label = "Tất cả"
                            )
                        }
                        items(DocumentType.values().toList()) { type ->
                            IosChip(
                                selected = selectedFilter == type,
                                onClick = { viewModel.selectedFilterType.value = type },
                                label = type.displayName
                            )
                        }
                    }
                }
            }

            // Documents List Section
            if (documents.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 60.dp, start = 32.dp, end = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        GlassSurface(
                            shape = CircleShape,
                            modifier = Modifier.size(72.dp),
                            elevation = 4.dp
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = glass.contentSecondary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Không tìm thấy tài liệu phù hợp" else "Chưa có tài liệu nào",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp,
                            color = glass.contentPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Mở tài liệu từ thiết bị hoặc bấm nút bên dưới để tạo các tệp văn phòng mẫu.",
                            fontFamily = InterFontFamily,
                            fontSize = 14.sp,
                            color = glass.contentSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = { viewModel.initializeSamples(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = glass.accentColor),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Tạo tài liệu mẫu ngay", fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else {
                items(documents, key = { it.uri }) { doc ->
                    DocumentCardItem(
                        doc = doc,
                        onClick = { onOpenDocument(Uri.parse(doc.uri)) },
                        onToggleFavorite = { viewModel.toggleFavorite(doc.uri, doc.isFavorite) },
                        onRename = { renameTargetDoc = doc },
                        onDelete = { deleteTargetDoc = doc },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Sort Bottom Sheet
        if (showSortSheet) {
            val sheetState = rememberModalBottomSheetState()
            ModalBottomSheet(
                onDismissRequest = { showSortSheet = false },
                sheetState = sheetState,
                containerColor = glass.surfaceColor,
                contentColor = glass.contentPrimary
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    Text(
                        text = "Sắp xếp theo",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = glass.contentPrimary,
                        modifier = Modifier.padding(bottom = 14.dp)
                    )

                    val sortOptions = listOf(
                        Pair(SortOption.DATE_DESC, "Mới mở gần đây nhất"),
                        Pair(SortOption.DATE_ASC, "Cũ nhất trước"),
                        Pair(SortOption.NAME_ASC, "Tên tài liệu (A → Z)"),
                        Pair(SortOption.NAME_DESC, "Tên tài liệu (Z → A)"),
                        Pair(SortOption.SIZE_DESC, "Kích thước lớn nhất"),
                        Pair(SortOption.SIZE_ASC, "Kích thước nhỏ nhất")
                    )

                    sortOptions.forEach { (option, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.sortOption.value = option
                                    showSortSheet = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                fontFamily = InterFontFamily,
                                fontSize = 16.sp,
                                fontWeight = if (sortOption == option) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (sortOption == option) glass.accentColor else glass.contentPrimary
                            )
                            if (sortOption == option) {
                                Text("✓", color = glass.accentColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Create Document Dialog
        if (showCreateDialog) {
            CreateDocumentDialog(
                onDismiss = { showCreateDialog = false },
                onCreate = { type, title ->
                    showCreateDialog = false
                    val uri = viewModel.createNewDocument(context, type, title)
                    if (uri != null) {
                        onOpenDocument(uri)
                    }
                }
            )
        }

        // Rename Dialog
        renameTargetDoc?.let { doc ->
            var newName by remember { mutableStateOf(doc.name) }
            AlertDialog(
                onDismissRequest = { renameTargetDoc = null },
                title = { Text("Đổi tên tài liệu", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = glass.accentColor,
                            focusedTextColor = glass.contentPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newName.isNotBlank()) {
                                viewModel.renameDocument(doc.uri, newName.trim())
                            }
                            renameTargetDoc = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = glass.accentColor)
                    ) {
                        Text("Lưu", fontFamily = InterFontFamily)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { renameTargetDoc = null }) {
                        Text("Hủy", fontFamily = InterFontFamily, color = glass.contentSecondary)
                    }
                }
            )
        }

        // Delete Dialog
        deleteTargetDoc?.let { doc ->
            AlertDialog(
                onDismissRequest = { deleteTargetDoc = null },
                title = { Text("Xóa khỏi danh sách", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold) },
                text = { Text("Bạn có chắc muốn xóa \"${doc.name}\" khỏi danh sách tài liệu?", fontFamily = InterFontFamily) },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteDocument(doc.uri)
                            deleteTargetDoc = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B30))
                    ) {
                        Text("Xóa", fontFamily = InterFontFamily)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deleteTargetDoc = null }) {
                        Text("Hủy", fontFamily = InterFontFamily, color = glass.contentSecondary)
                    }
                }
            )
        }
    }
}

@Composable
fun DocumentCardItem(
    doc: RecentDocumentEntity,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalLiquidGlass.current
    var menuExpanded by remember { mutableStateOf(false) }

    val docType = try {
        DocumentType.valueOf(doc.type)
    } catch (_: Exception) {
        DocumentType.TEXT
    }

    val (badgeBg, badgeText) = when (docType) {
        DocumentType.WORD -> Pair(WordBlue, "DOCX")
        DocumentType.EXCEL -> Pair(ExcelGreen, "XLSX")
        DocumentType.POWERPOINT -> Pair(PptOrange, "PPTX")
        DocumentType.PDF -> Pair(PdfRed, "PDF")
        DocumentType.TEXT -> Pair(TextGray, "TXT")
    }

    val formattedDate = remember(doc.lastOpenedTimestamp) {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        sdf.format(Date(doc.lastOpenedTimestamp))
    }

    val formattedSize = remember(doc.sizeBytes) {
        when {
            doc.sizeBytes > 1024 * 1024 -> String.format("%.1f MB", doc.sizeBytes / (1024.0 * 1024.0))
            doc.sizeBytes > 1024 -> String.format("%d KB", doc.sizeBytes / 1024)
            doc.sizeBytes > 0 -> "${doc.sizeBytes} B"
            else -> ""
        }
    }

    GlassSurface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        elevation = 3.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type Badge
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(badgeBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badgeText,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Metadata Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = doc.name,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = glass.contentPrimary,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formattedDate,
                        fontFamily = InterFontFamily,
                        fontSize = 12.sp,
                        color = glass.contentSecondary
                    )
                    if (formattedSize.isNotEmpty()) {
                        Text(
                            text = " • $formattedSize",
                            fontFamily = InterFontFamily,
                            fontSize = 12.sp,
                            color = glass.contentSecondary
                        )
                    }
                    if (doc.lastPage > 0) {
                        Text(
                            text = " • Trang ${doc.lastPage + 1}",
                            fontFamily = InterFontFamily,
                            fontSize = 12.sp,
                            color = glass.accentColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Favorite Button
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (doc.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Yêu thích",
                    tint = if (doc.isFavorite) Color(0xFFFFCC00) else glass.contentSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // More Options Dropdown
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Thêm",
                        tint = glass.contentSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Đổi tên", fontFamily = InterFontFamily) },
                        onClick = {
                            menuExpanded = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Xóa khỏi danh sách", fontFamily = InterFontFamily, color = Color(0xFFFF3B30)) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CreateDocumentDialog(
    onDismiss: () -> Unit,
    onCreate: (DocumentType, String) -> Unit
) {
    val glass = LocalLiquidGlass.current
    var selectedType by remember { mutableStateOf(DocumentType.WORD) }
    var title by remember { mutableStateOf("Tài liệu mới") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Tạo tài liệu mới",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column {
                Text(
                    text = "Chọn định dạng:",
                    fontFamily = InterFontFamily,
                    fontSize = 13.sp,
                    color = glass.contentSecondary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                val types = listOf(DocumentType.WORD, DocumentType.EXCEL, DocumentType.TEXT)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    types.forEach { t ->
                        val isSel = selectedType == t
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) glass.accentColor else glass.borderColor.copy(alpha = 0.2f))
                                .clickable {
                                    selectedType = t
                                    title = when (t) {
                                        DocumentType.WORD -> "Văn bản Word mới"
                                        DocumentType.EXCEL -> "Bảng tính mới"
                                        else -> "Ghi chú mới"
                                    }
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = t.displayName,
                                fontFamily = InterFontFamily,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                color = if (isSel) Color.White else glass.contentPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Tên tài liệu:",
                    fontFamily = InterFontFamily,
                    fontSize = 13.sp,
                    color = glass.contentSecondary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = glass.accentColor,
                        focusedTextColor = glass.contentPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(selectedType, title) },
                colors = ButtonDefaults.buttonColors(containerColor = glass.accentColor)
            ) {
                Text("Tạo", fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", fontFamily = InterFontFamily, color = glass.contentSecondary)
            }
        }
    )
}
