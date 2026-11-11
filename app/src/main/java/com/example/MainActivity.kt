package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.data.AppDatabase
import com.example.ui.components.IosGlassTabBar
import com.example.ui.components.TabItem
import com.example.ui.components.WallpaperScaffold
import com.example.ui.screens.DocumentViewerContainer
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.KiraTheme
import com.example.ui.viewmodel.HomeTab
import com.example.ui.viewmodel.HomeViewModel
import com.example.ui.viewmodel.SettingsViewModel
import com.example.ui.viewmodel.ViewerViewModel

class MainActivity : ComponentActivity() {
    private var initialDocumentUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Extract URI from incoming Intent if opened from another app
        handleIncomingIntent(intent)

        setContent {
            val context = LocalContext.current
            val database = remember { AppDatabase.getDatabase(context) }
            val settingsViewModel = remember { SettingsViewModel(database) }
            val homeViewModel = remember { HomeViewModel(database) }
            val viewerViewModel = remember { ViewerViewModel(database) }

            val pref by settingsViewModel.preferences.collectAsState()
            val wallpaperConfig by settingsViewModel.wallpaperConfig.collectAsState()

            val isDark = when (pref.themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            var activeDocUri by remember { mutableStateOf<Uri?>(initialDocumentUri) }
            var selectedNavIndex by remember { mutableIntStateOf(0) }

            // Auto seed sample files on first launch
            LaunchedEffect(Unit) {
                homeViewModel.initializeSamples(context)
            }

            KiraTheme(
                darkTheme = isDark,
                glassOpacity = pref.glassOpacity,
                reduceTransparency = pref.reduceTransparency
            ) {
                // If viewing a document, render the document viewer
                if (activeDocUri != null) {
                    val currentUri = activeDocUri!!
                    LaunchedEffect(currentUri) {
                        viewerViewModel.openDocument(context, currentUri)
                    }

                    DocumentViewerContainer(
                        viewModel = viewerViewModel,
                        onNavigateBack = {
                            activeDocUri = null
                        }
                    )
                } else {
                    // Wallpaper Scaffold for Home & Settings frames
                    WallpaperScaffold(config = wallpaperConfig) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Tab Screen Content
                            AnimatedContent(
                                targetState = selectedNavIndex,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "nav_content"
                            ) { tabIdx ->
                                when (tabIdx) {
                                    0 -> {
                                        homeViewModel.currentTab.value = HomeTab.ALL
                                        HomeScreen(
                                            viewModel = homeViewModel,
                                            onOpenDocument = { uri -> activeDocUri = uri }
                                        )
                                    }
                                    1 -> {
                                        homeViewModel.currentTab.value = HomeTab.RECENT
                                        HomeScreen(
                                            viewModel = homeViewModel,
                                            onOpenDocument = { uri -> activeDocUri = uri }
                                        )
                                    }
                                    else -> {
                                        SettingsScreen(viewModel = settingsViewModel)
                                    }
                                }
                            }

                            // Floating Liquid Glass Bottom Navigation Bar
                            val navItems = listOf(
                                TabItem("Tài liệu", Icons.Default.Description),
                                TabItem("Mới mở", Icons.Default.History),
                                TabItem("Cài đặt", Icons.Default.Settings)
                            )

                            IosGlassTabBar(
                                items = navItems,
                                selectedIndex = selectedNavIndex,
                                onTabSelected = { selectedNavIndex = it },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .windowInsetsPadding(WindowInsets.navigationBars)
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        if (action == Intent.ACTION_VIEW || action == Intent.ACTION_EDIT) {
            intent.data?.let { uri ->
                initialDocumentUri = uri
            }
        } else if (action == Intent.ACTION_SEND) {
            val streamUri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
            if (streamUri != null) {
                initialDocumentUri = streamUri
            }
        }
    }
}
