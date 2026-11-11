package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import com.example.kiraengine.DocumentType
import com.example.kiraengine.ExcelDocumentEngine
import com.example.kiraengine.PdfDocumentEngine
import com.example.kiraengine.PptDocumentEngine
import com.example.kiraengine.TextDocumentEngine
import com.example.kiraengine.WordDocumentEngine
import com.example.ui.components.GlassSurface
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.IosBlue
import com.example.ui.theme.LocalLiquidGlass
import com.example.ui.viewmodel.ReadingMode
import com.example.ui.viewmodel.ViewerViewModel

@Composable
fun DocumentViewerContainer(
    viewModel: ViewerViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val glass = LocalLiquidGlass.current

    val metadata by viewModel.metadata.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val readingMode by viewModel.readingMode.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()

    var showMenu by remember { mutableStateOf(false) }

    // Intercept back button
    BackHandler {
        onNavigateBack()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // Top Glass Navigation Bar
        GlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            shape = RoundedCornerShape(18.dp),
            elevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = glass.contentPrimary
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = metadata?.name ?: "Đang tải…",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = glass.contentPrimary,
                        maxLines = 1
                    )
                    metadata?.type?.displayName?.let { typeName ->
                        Text(
                            text = typeName,
                            fontFamily = InterFontFamily,
                            fontSize = 11.sp,
                            color = glass.accentColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Undo
                IconButton(
                    onClick = { viewModel.undo() },
                    enabled = canUndo,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Hoàn tác",
                        tint = if (canUndo) glass.contentPrimary else glass.contentSecondary.copy(alpha = 0.3f),
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Redo
                IconButton(
                    onClick = { viewModel.redo() },
                    enabled = canRedo,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Làm lại",
                        tint = if (canRedo) glass.contentPrimary else glass.contentSecondary.copy(alpha = 0.3f),
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Reading Mode Toggle
                IconButton(
                    onClick = {
                        val nextMode = when (readingMode) {
                            ReadingMode.NORMAL -> ReadingMode.NIGHT
                            ReadingMode.NIGHT -> ReadingMode.SEPIA
                            ReadingMode.SEPIA -> ReadingMode.NORMAL
                        }
                        viewModel.setReadingMode(nextMode)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Brightness4,
                        contentDescription = "Chế độ đọc",
                        tint = if (readingMode != ReadingMode.NORMAL) glass.accentColor else glass.contentSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Save
                IconButton(
                    onClick = {
                        viewModel.saveDocument(context) { success, msg ->
                            Toast.makeText(context, msg ?: if (success) "Đã lưu" else "Lỗi", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Lưu",
                        tint = glass.accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Share / Print / Export Menu
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Tùy chọn",
                            tint = glass.contentPrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Chia sẻ tệp", fontFamily = InterFontFamily) },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            onClick = {
                                showMenu = false
                                viewModel.shareDocument(context)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("In tài liệu", fontFamily = InterFontFamily) },
                            leadingIcon = { Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            onClick = {
                                showMenu = false
                                printDocument(context, viewModel)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Xuất sang PDF", fontFamily = InterFontFamily) },
                            leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            onClick = {
                                showMenu = false
                                viewModel.exportToPdf(context) { success, uri ->
                                    if (success && uri != null) {
                                        Toast.makeText(context, "Đã xuất PDF thành công!", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Không thể xuất PDF", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        // Body Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = glass.accentColor, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Đang phân tích tài liệu…",
                            fontFamily = InterFontFamily,
                            color = glass.contentSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
                errorMessage != null -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "Không thể mở tài liệu",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF3B30),
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage ?: "",
                            fontFamily = InterFontFamily,
                            color = glass.contentSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
                else -> {
                    val engine = viewModel.getActiveEngine()
                    when (engine) {
                        is WordDocumentEngine -> WordViewerScreen(engine, viewModel, readingMode)
                        is ExcelDocumentEngine -> ExcelViewerScreen(engine, viewModel, readingMode)
                        is PptDocumentEngine -> PptViewerScreen(engine, viewModel)
                        is PdfDocumentEngine -> PdfViewerScreen(engine, viewModel, readingMode)
                        is TextDocumentEngine -> TextViewerScreen(engine, viewModel, readingMode)
                        else -> {
                            Text("Định dạng không được hỗ trợ", fontFamily = InterFontFamily, color = glass.contentSecondary)
                        }
                    }
                }
            }
        }
    }
}

private fun printDocument(context: Context, viewModel: ViewerViewModel) {
    try {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        viewModel.exportToPdf(context) { success, uri ->
            if (success && uri != null) {
                val printAdapter = object : android.print.PrintDocumentAdapter() {
                    override fun onLayout(
                        oldAttributes: PrintAttributes?,
                        newAttributes: PrintAttributes?,
                        cancellationSignal: android.os.CancellationSignal?,
                        callback: LayoutResultCallback?,
                        metadata: android.os.Bundle?
                    ) {
                        callback?.onLayoutFinished(
                            android.print.PrintDocumentInfo.Builder("Document.pdf")
                                .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                                .build(),
                            true
                        )
                    }

                    override fun onWrite(
                        pages: Array<out android.print.PageRange>?,
                        destination: android.os.ParcelFileDescriptor?,
                        cancellationSignal: android.os.CancellationSignal?,
                        callback: WriteResultCallback?
                    ) {
                        try {
                            context.contentResolver.openInputStream(uri)?.use { input ->
                                android.os.ParcelFileDescriptor.AutoCloseOutputStream(destination).use { output ->
                                    input.copyTo(output)
                                }
                            }
                            callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                        } catch (e: Exception) {
                            callback?.onWriteFailed(e.localizedMessage)
                        }
                    }
                }
                printManager.print("KiraDocumentPrint", printAdapter, null)
            }
        }
    } catch (_: Exception) {}
}
