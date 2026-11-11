package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.IosBlue
import com.example.ui.theme.IosGreen
import com.example.ui.theme.LocalLiquidGlass
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Liquid Glass Surface Card with frosted glow, 1dp subtle border, and spring press feedback.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(22.dp),
    backgroundColor: Color? = null,
    borderColor: Color? = null,
    borderWidth: Dp = 1.dp,
    elevation: Dp = 8.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val glass = LocalLiquidGlass.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val pressScale = remember { Animatable(1f) }

    val actualBg = backgroundColor ?: glass.surfaceColor
    val actualBorder = borderColor ?: glass.borderColor

    val clickModifier = if (onClick != null) {
        Modifier.pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    scope.launch {
                        pressScale.animateTo(
                            0.975f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    }
                    val released = tryAwaitRelease()
                    scope.launch {
                        pressScale.animateTo(
                            1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                    }
                    if (released) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClick()
                    }
                }
            )
        }
    } else Modifier

    Surface(
        modifier = modifier
            .scale(pressScale.value)
            .then(clickModifier)
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = if (glass.isDarkTheme) Color.Black.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.08f),
                spotColor = if (glass.isDarkTheme) Color.Black.copy(alpha = 0.6f) else Color(0xFF007AFF).copy(alpha = 0.1f)
            )
            .border(borderWidth, actualBorder, shape)
            .clip(shape),
        color = actualBg,
        shape = shape
    ) {
        CompositionLocalProvider(LocalContentColor provides glass.contentPrimary) {
            content()
        }
    }
}

/**
 * iOS Grouped Section (like iOS Settings)
 */
@Composable
fun IosGroupedSection(
    modifier: Modifier = Modifier,
    title: String? = null,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val glass = LocalLiquidGlass.current
    Column(modifier = modifier.fillMaxWidth()) {
        if (!title.isNullOrEmpty()) {
            Text(
                text = title.uppercase(),
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                letterSpacing = 0.8.sp,
                color = glass.contentSecondary,
                modifier = Modifier.padding(start = 16.dp, bottom = 6.dp, top = 14.dp)
            )
        }
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                content = content
            )
        }
        if (!footer.isNullOrEmpty()) {
            Text(
                text = footer,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                color = glass.contentSecondary,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 10.dp)
            )
        }
    }
}

/**
 * iOS Grouped Item inside an IosGroupedSection
 */
@Composable
fun IosGroupedItem(
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconTint: Color = IosBlue,
    iconBackground: Color = iconTint.copy(alpha = 0.15f),
    title: String,
    subtitle: String? = null,
    trailingText: String? = null,
    showChevron: Boolean = false,
    showDivider: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val glass = LocalLiquidGlass.current
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClick()
                    }
                } else Modifier
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    color = glass.contentPrimary
                )
                if (!subtitle.isNullOrEmpty()) {
                    Text(
                        text = subtitle,
                        fontFamily = InterFontFamily,
                        fontSize = 13.sp,
                        color = glass.contentSecondary,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
            }

            if (!trailingText.isNullOrEmpty()) {
                Text(
                    text = trailingText,
                    fontFamily = InterFontFamily,
                    fontSize = 15.sp,
                    color = glass.contentSecondary,
                    modifier = Modifier.padding(end = 6.dp)
                )
            }

            if (trailingContent != null) {
                trailingContent()
            }

            if (showChevron) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = glass.contentSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = if (icon != null) 62.dp else 16.dp),
                thickness = 0.5.dp,
                color = glass.borderColor.copy(alpha = 0.35f)
            )
        }
    }
}

/**
 * iOS Liquid Glass Floating Bottom Tab Bar
 */
@Composable
fun IosGlassTabBar(
    modifier: Modifier = Modifier,
    items: List<TabItem>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    val glass = LocalLiquidGlass.current
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassSurface(
            shape = RoundedCornerShape(32.dp),
            elevation = 16.dp,
            borderWidth = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    val isSelected = index == selectedIndex
                    val pillScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.05f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "pill_scale"
                    )

                    Column(
                        modifier = Modifier
                            .scale(pillScale)
                            .clip(RoundedCornerShape(20.dp))
                            .then(
                                if (isSelected) {
                                    Modifier.background(glass.accentColor.copy(alpha = 0.15f))
                                } else Modifier
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (!isSelected) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onTabSelected(index)
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (isSelected) glass.accentColor else glass.contentSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.label,
                            fontFamily = InterFontFamily,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) glass.accentColor else glass.contentSecondary
                        )
                    }
                }
            }
        }
    }
}

data class TabItem(
    val label: String,
    val icon: ImageVector
)

/**
 * iOS Segmented Control
 */
@Composable
fun <T> IosSegmentedControl(
    items: List<T>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    labelProvider: (T) -> String,
    modifier: Modifier = Modifier
) {
    val glass = LocalLiquidGlass.current
    val haptic = LocalHapticFeedback.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (glass.isDarkTheme) Color(0xFF1E2430) else Color(0x33767680)
            )
            .padding(2.dp)
    ) {
        val segmentWidth = maxWidth / items.size
        val indicatorOffset by animateDpAsState(
            targetValue = segmentWidth * selectedIndex,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "seg_offset"
        )

        // Active indicator pill
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(segmentWidth)
                .fillMaxHeight()
                .shadow(elevation = 3.dp, shape = RoundedCornerShape(10.dp))
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (glass.isDarkTheme) Color(0xFF2C3440) else Color.White
                )
        )

        // Segments Row
        Row(modifier = Modifier.fillMaxSize()) {
            items.forEachIndexed { index, item ->
                val isSelected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (!isSelected) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onItemSelected(index)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = labelProvider(item),
                        fontFamily = InterFontFamily,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isSelected) glass.contentPrimary else glass.contentSecondary
                    )
                }
            }
        }
    }
}

/**
 * iOS Switch Toggle
 */
@Composable
fun IosSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val glass = LocalLiquidGlass.current
    val haptic = LocalHapticFeedback.current
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 22.dp else 2.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "thumb_offset"
    )

    val bgColor = when {
        !enabled -> Color.Gray.copy(alpha = 0.3f)
        checked -> IosGreen
        glass.isDarkTheme -> Color(0xFF393E46)
        else -> Color(0xFFE5E5EA)
    }

    Box(
        modifier = modifier
            .width(51.dp)
            .height(31.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onCheckedChange(!checked)
            }
            .padding(horizontal = 2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(27.dp)
                .shadow(elevation = 3.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

/**
 * iOS Slider
 */
@Composable
fun IosSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    modifier: Modifier = Modifier
) {
    val glass = LocalLiquidGlass.current
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        modifier = modifier,
        colors = SliderDefaults.colors(
            thumbColor = Color.White,
            activeTrackColor = glass.accentColor,
            inactiveTrackColor = if (glass.isDarkTheme) Color(0xFF2C3440) else Color(0xFFE5E5EA)
        )
    )
}

/**
 * iOS Search Field with Frosted Glass look
 */
@Composable
fun IosSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onSearch: (() -> Unit)? = null
) {
    val glass = LocalLiquidGlass.current

    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        shape = RoundedCornerShape(14.dp),
        elevation = 2.dp,
        borderWidth = 0.8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = glass.contentSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        fontFamily = InterFontFamily,
                        fontSize = 15.sp,
                        color = glass.contentSecondary
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = InterFontFamily,
                        fontSize = 15.sp,
                        color = glass.contentPrimary
                    ),
                    cursorBrush = SolidColor(glass.accentColor),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch?.invoke() }),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Xóa",
                        tint = glass.contentSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * iOS Filter Chip
 */
@Composable
fun IosChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    val glass = LocalLiquidGlass.current
    val haptic = LocalHapticFeedback.current

    val bg = if (selected) {
        glass.accentColor
    } else {
        glass.surfaceColor
    }

    val contentColor = if (selected) {
        Color.White
    } else {
        glass.contentPrimary
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(
                1.dp,
                if (selected) Color.Transparent else glass.borderColor,
                RoundedCornerShape(20.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier
                        .size(14.dp)
                        .padding(end = 4.dp)
                )
            }
            Text(
                text = label,
                fontFamily = InterFontFamily,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = contentColor
            )
        }
    }
}
