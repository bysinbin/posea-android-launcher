package com.bysinbin.posea.ui.main

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bysinbin.posea.model.AppModel
import com.bysinbin.posea.model.LauncherMode
import com.bysinbin.posea.ui.drawer.AppDrawerSheet
import com.bysinbin.posea.ui.home.HybridView
import com.bysinbin.posea.ui.home.MinimalistView
import com.bysinbin.posea.ui.home.StandardGridView
import com.bysinbin.posea.ui.onboarding.OnboardingScreen
import com.bysinbin.posea.ui.settings.SettingsDialog

@Composable
fun MainScreen(
    viewModel: LauncherViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedAppForMenu by remember { mutableStateOf<AppModel?>(null) }
    var pendingWidgetId by remember { mutableStateOf<Int?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current

    // Widget dinleyicisini yaşam döngüsüne bağla
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                viewModel.widgetManager.startListening()
            } else if (event == Lifecycle.Event.ON_STOP) {
                viewModel.widgetManager.stopListening()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.widgetManager.stopListening()
        }
    }

    // Widget Ayarlama Launcher'ı
    val configureWidgetLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val id = pendingWidgetId
        if (result.resultCode == Activity.RESULT_OK && id != null) {
            viewModel.addPinnedWidget(id)
        } else if (id != null) {
            viewModel.widgetManager.deleteAppWidgetId(id)
        }
        pendingWidgetId = null
    }

    // Widget Seçici (Picker) Launcher'ı
    val pickWidgetLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val id = pendingWidgetId
        if (result.resultCode == Activity.RESULT_OK && id != null) {
            val info = viewModel.widgetManager.getAppWidgetInfo(id)
            if (info != null) {
                if (info.configure != null) {
                    val configIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                        component = info.configure
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                    }
                    configureWidgetLauncher.launch(configIntent)
                } else {
                    viewModel.addPinnedWidget(id)
                    pendingWidgetId = null
                }
            }
        } else if (id != null) {
            viewModel.widgetManager.deleteAppWidgetId(id)
            pendingWidgetId = null
        }
    }

    val onAddWidget: () -> Unit = {
        val newId = viewModel.widgetManager.allocateAppWidgetId()
        pendingWidgetId = newId
        val pickIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, newId)
        }
        pickWidgetLauncher.launch(pickIntent)
    }

    // Geri tuşu kontrolü (Çekmece veya ayarlar açıksa kapat)
    BackHandler(enabled = uiState.isDrawerOpen || uiState.isSettingsOpen || selectedAppForMenu != null) {
        if (selectedAppForMenu != null) {
            selectedAppForMenu = null
        } else if (uiState.isDrawerOpen) {
            viewModel.setDrawerOpen(false)
        } else if (uiState.isSettingsOpen) {
            viewModel.setSettingsOpen(false)
        }
    }

    // Yükleme durumu
    if (uiState.isLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    // İlk Açılış Sihirbazı (Onboarding / Kullanıcıya Sorma Aşaması)
    if (!uiState.preferences.isOnboardingCompleted) {
        OnboardingScreen(
            availableApps = uiState.allApps,
            onComplete = { mode, style, favorites ->
                viewModel.completeOnboarding(mode, style, favorites)
            },
            modifier = modifier
        )
        return
    }

    // Ana Launcher Ekranı
    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Kullanıcı tercihine göre seçilen mod görünümü
        when (uiState.preferences.mode) {
            LauncherMode.MINIMALIST -> {
                MinimalistView(
                    favoriteApps = uiState.favoriteApps,
                    iconStyle = uiState.preferences.iconStyle,
                    onAppClick = { viewModel.launchApp(it) },
                    onAppLongClick = { selectedAppForMenu = it },
                    onOpenDrawer = { viewModel.setDrawerOpen(true) }
                )
            }
            LauncherMode.STANDARD -> {
                StandardGridView(
                    apps = uiState.allApps,
                    dockApps = uiState.favoriteApps,
                    iconStyle = uiState.preferences.iconStyle,
                    pinnedWidgetIds = uiState.preferences.pinnedWidgetIds,
                    widgetManager = viewModel.widgetManager,
                    onAddWidgetClick = onAddWidget,
                    onRemoveWidget = { viewModel.removePinnedWidget(it) },
                    onAppClick = { viewModel.launchApp(it) },
                    onAppLongClick = { selectedAppForMenu = it },
                    onOpenDrawer = { viewModel.setDrawerOpen(true) }
                )
            }
            LauncherMode.HYBRID -> {
                HybridView(
                    favoriteApps = uiState.favoriteApps,
                    allApps = uiState.allApps,
                    iconStyle = uiState.preferences.iconStyle,
                    pinnedWidgetIds = uiState.preferences.pinnedWidgetIds,
                    widgetManager = viewModel.widgetManager,
                    onAddWidgetClick = onAddWidget,
                    onRemoveWidget = { viewModel.removePinnedWidget(it) },
                    onAppClick = { viewModel.launchApp(it) },
                    onAppLongClick = { selectedAppForMenu = it },
                    onOpenDrawer = { viewModel.setDrawerOpen(true) }
                )
            }
        }

        // Uygulama Çekmecesi (Drawer)
        AppDrawerSheet(
            isOpen = uiState.isDrawerOpen,
            searchQuery = uiState.searchQuery,
            filteredApps = uiState.filteredApps,
            iconStyle = uiState.preferences.iconStyle,
            onQueryChange = { viewModel.onSearchQueryChange(it) },
            onAppClick = { viewModel.launchApp(it) },
            onAppLongClick = { selectedAppForMenu = it },
            onToggleFavorite = { viewModel.toggleFavorite(it) },
            onDismiss = { viewModel.setDrawerOpen(false) }
        )

        // Ayarlar İletişim Kutusu
        SettingsDialog(
            isOpen = uiState.isSettingsOpen,
            preferences = uiState.preferences,
            onModeChange = { viewModel.setLauncherMode(it) },
            onIconStyleChange = { viewModel.setIconStyle(it) },
            onResetOnboarding = { viewModel.resetOnboarding() },
            onDismiss = { viewModel.setSettingsOpen(false) }
        )

        // Uygulamaya Uzun Basınca Açılan Hızlı Menü
        selectedAppForMenu?.let { app ->
            AlertDialog(
                onDismissRequest = { selectedAppForMenu = null },
                title = { Text(app.label) },
                text = {
                    Text(
                        if (app.isFavorite) "Bu uygulamayı favorilerden kaldırmak veya uygulama bilgilerine gitmek ister misiniz?"
                        else "Bu uygulamayı favorilere eklemek veya uygulama bilgilerine gitmek ister misiniz?"
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.toggleFavorite(app.packageName)
                        selectedAppForMenu = null
                    }) {
                        Text(if (app.isFavorite) "Favorilerden Çıkar" else "Favorilere Ekle")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        viewModel.openAppInfo(app)
                        selectedAppForMenu = null
                    }) {
                        Text("Uygulama Bilgisi")
                    }
                }
            )
        }
    }
}
