package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kiraengine.EditCommand
import com.example.kiraengine.TextDocumentEngine
import com.example.ui.components.GlassSurface
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.IosBlue
import com.example.ui.theme.LocalLiquidGlass
import com.example.ui.viewmodel.ReadingMode
import com.example.ui.viewmodel.ViewerViewModel

@Composable
fun TextViewerScreen(
    engine: TextDocumentEngine,
    viewModel: ViewerViewModel,
    readingMode: ReadingMode,
    modifier: Modifier = Modifier
) {
    val glass = LocalLiquidGlass.current
    var fullText by remember(engine.content) { mutableStateOf(engine.content) }

    val paperColor = when (readingMode) {
        ReadingMode.NORMAL -> Color.White
        ReadingMode.NIGHT -> Color(0xFF1E2430)
        ReadingMode.SEPIA -> Color(0xFFFAF4E8)
    }

    val textColor = when (readingMode) {
        ReadingMode.NORMAL -> Color(0xFF111827)
        ReadingMode.NIGHT -> Color(0xFFF3F4F6)
        ReadingMode.SEPIA -> Color(0xFF3E2723)
    }

    val lineNumColor = when (readingMode) {
        ReadingMode.NIGHT -> Color(0xFF6B7280)
        else -> Color(0xFF9CA3AF)
    }

    val lineCount = remember(fullText) { fullText.count { it == '\n' } + 1 }
    val wordCount = remember(fullText) { fullText.split("\\s+".toRegex()).count { it.isNotBlank() } }

    Column(modifier = modifier.fillMaxSize()) {
        // Status bar header with line/word count
        GlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$lineCount dòng  •  $wordCount từ  •  ${fullText.length} ký tự",
                    fontFamily = InterFontFamily,
                    fontSize = 12.sp,
                    color = glass.contentSecondary
                )
            }
        }

        // Text Content Paper Canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .shadow(elevation = 4.dp, shape = RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(paperColor)
                .padding(14.dp)
        ) {
            BasicTextField(
                value = fullText,
                onValueChange = { newT ->
                    fullText = newT
                    viewModel.applyEdit(EditCommand.TextUpdate(engine.content, newT))
                },
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    color = textColor
                ),
                cursorBrush = SolidColor(IosBlue),
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
