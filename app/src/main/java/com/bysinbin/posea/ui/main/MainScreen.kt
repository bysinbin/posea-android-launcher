package com.bysinbin.posea.ui.main

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import com.bysinbin.posea.data.system.AppShortcutsManager
import com.bysinbin.posea.data.system.BiometricHelper
import com.bysinbin.posea.model.AppFolder
import com.bysinbin.posea.ui.components.FolderDetailDialog
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import com.bysinbin.posea.data.system.PoseaAccessibilityService

import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Widgets
import com.bysinbin.posea.data.system.PoseaNotificationService

private var expandNotificationsMethod: java.lang.reflect.Method? = null
private var statusBarServiceCache: Any? = null
private var isStatusBarReflectionInit = false

private fun expandNotificationPanel(context: android.content.Context) {
    try {
        if (!isStatusBarReflectionInit) {
            val statusBarService = context.getSystemService("statusbar")
            val statusBarManagerClass = Class.forName("android.app.StatusBarManager")
            expandNotificationsMethod = statusBarManagerClass.getMethod("expandNotificationsPanel")
            statusBarServiceCache = statusBarService
            isStatusBarReflectionInit = true
        }
        expandNotificationsMethod?.invoke(statusBarServiceCache)
    } catch (_: Exception) {}
}

@Composable
fun MainScreen(
    viewModel: LauncherViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val badgeCounts by PoseaNotificationService.badgeCounts.collectAsStateWithLifecycle()
    var selectedAppForMenu by remember { mutableStateOf<AppModel?>(null) }
    var selectedAppForFolder by remember { mutableStateOf<AppModel?>(null) }
    var activeFolder by remember { mutableStateOf<AppFolder?>(null) }
    var isNewFolderDialogOpen by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var appToRename by remember { mutableStateOf<AppModel?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var isHomeScreenMenuOpen by remember { mutableStateOf(false) }

    var pendingWidgetId by remember { mutableStateOf<Int?>(null) }
    var isAddingToHomeWidget by remember { mutableStateOf(false) }

    val context = LocalContext.current
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
            if (isAddingToHomeWidget) {
                viewModel.addHomeWidget(id)
            } else {
                viewModel.addPinnedWidget(id)
            }
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
                    if (isAddingToHomeWidget) {
                        viewModel.addHomeWidget(id)
                    } else {
                        viewModel.addPinnedWidget(id)
                    }
                    pendingWidgetId = null
                }
            }
        } else if (id != null) {
            viewModel.widgetManager.deleteAppWidgetId(id)
            pendingWidgetId = null
        }
    }

    val onAddPinnedWidget: () -> Unit = remember {
        {
            isAddingToHomeWidget = false
            val newId = viewModel.widgetManager.allocateAppWidgetId()
            pendingWidgetId = newId
            val pickIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, newId)
            }
            pickWidgetLauncher.launch(pickIntent)
        }
    }

    val onAddHomeWidget: () -> Unit = remember {
        {
            isAddingToHomeWidget = true
            val newId = viewModel.widgetManager.allocateAppWidgetId()
            pendingWidgetId = newId
            val pickIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, newId)
            }
            pickWidgetLauncher.launch(pickIntent)
        }
    }

    // Geri tuşu kontrolü (Çekmece, ayarlar veya menüler açıksa kapat)
    BackHandler(
        enabled = uiState.isDrawerOpen ||
                uiState.isSettingsOpen ||
                isHomeScreenMenuOpen ||
                selectedAppForMenu != null ||
                activeFolder != null ||
                selectedAppForFolder != null ||
                isNewFolderDialogOpen ||
                appToRename != null
    ) {
        if (appToRename != null) {
            appToRename = null
        } else if (activeFolder != null) {
            activeFolder = null
        } else if (isNewFolderDialogOpen) {
            isNewFolderDialogOpen = false
        } else if (selectedAppForFolder != null) {
            selectedAppForFolder = null
        } else if (selectedAppForMenu != null) {
            selectedAppForMenu = null
        } else if (isHomeScreenMenuOpen) {
            isHomeScreenMenuOpen = false
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
        val safeLaunchApp: (AppModel) -> Unit = remember(uiState.preferences.lockedPackages, context) {
            { app ->
                if (uiState.preferences.lockedPackages.contains(app.packageName)) {
                    val activity = context as? Activity
                    if (activity != null && BiometricHelper.isDeviceSecure(activity)) {
                        BiometricHelper.authenticate(
                            activity = activity,
                            title = app.label,
                            subtitle = "Uygulamayı açmak için doğrulayın",
                            onSuccess = { viewModel.launchApp(app) },
                            onError = { msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        )
                    } else {
                        viewModel.launchApp(app)
                    }
                } else {
                    viewModel.launchApp(app)
                }
            }
        }

        val onAppLongClick: (AppModel) -> Unit = remember { { selectedAppForMenu = it } }
        val onFolderClick: (AppFolder) -> Unit = remember { { activeFolder = it } }
        val onFolderLongClick: (AppFolder) -> Unit = remember { { activeFolder = it } }
        val onOpenDrawer: () -> Unit = remember { { viewModel.setDrawerOpen(true) } }
        val onRemovePinnedWidget: (Int) -> Unit = remember { { viewModel.removePinnedWidget(it) } }
        val onRemoveHomeWidget: (Int) -> Unit = remember { { viewModel.removeHomeWidget(it) } }
        val onOpenSettings: () -> Unit = remember { { viewModel.setSettingsOpen(true) } }
        val onExpandNotifications: () -> Unit = remember(context) { { expandNotificationPanel(context) } }
        val onHomeScreenLongClick: () -> Unit = remember { { isHomeScreenMenuOpen = true } }
        val onDrawerDismiss: () -> Unit = remember { { viewModel.setDrawerOpen(false) } }

        val onDoubleTapToLock: () -> Unit = remember(context) {
            {
                val locked = PoseaAccessibilityService.lockScreen()
                if (!locked) {
                    Toast.makeText(
                        context,
                        "Ekranı kilitlemek için Posea Erişilebilirlik iznini açın",
                        Toast.LENGTH_LONG
                    ).show()
                    try {
                        val intent = android.content.Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                }
            }
        }

        // Kullanıcı tercihine göre seçilen mod görünümü
        when (uiState.preferences.mode) {
            LauncherMode.MINIMALIST -> {
                MinimalistView(
                    favoriteApps = uiState.favoriteApps,
                    iconStyle = uiState.preferences.iconStyle,
                    clockStyle = uiState.preferences.clockStyle,
                    homeWidgetIds = uiState.preferences.homeWidgetIds,
                    widgetManager = viewModel.widgetManager,
                    badgeCounts = badgeCounts,
                    onAddWidgetClick = onAddHomeWidget,
                    onRemoveWidget = onRemoveHomeWidget,
                    onAppClick = safeLaunchApp,
                    onAppLongClick = onAppLongClick,
                    onOpenDrawer = onOpenDrawer,
                    onOpenSettings = onOpenSettings,
                    onExpandNotifications = onExpandNotifications,
                    onHomeScreenLongClick = onHomeScreenLongClick,
                    onDoubleTapToLock = onDoubleTapToLock
                )
            }
            LauncherMode.STANDARD -> {
                StandardGridView(
                    apps = uiState.allApps,
                    dockApps = uiState.favoriteApps,
                    folders = uiState.preferences.folders,
                    gridColumns = uiState.preferences.gridColumns,
                    iconStyle = uiState.preferences.iconStyle,
                    clockStyle = uiState.preferences.clockStyle,
                    hideAppLabels = uiState.preferences.hideAppLabels,
                    pinnedWidgetIds = uiState.preferences.pinnedWidgetIds,
                    widgetManager = viewModel.widgetManager,
                    badgeCounts = badgeCounts,
                    onAddWidgetClick = onAddPinnedWidget,
                    onRemoveWidget = onRemovePinnedWidget,
                    onFolderClick = onFolderClick,
                    onFolderLongClick = onFolderLongClick,
                    onAppClick = safeLaunchApp,
                    onAppLongClick = onAppLongClick,
                    onOpenDrawer = onOpenDrawer,
                    onOpenSettings = onOpenSettings,
                    onExpandNotifications = onExpandNotifications,
                    onHomeScreenLongClick = onHomeScreenLongClick,
                    onDoubleTapToLock = onDoubleTapToLock
                )
            }
            LauncherMode.HYBRID -> {
                HybridView(
                    favoriteApps = uiState.favoriteApps,
                    allApps = uiState.allApps,
                    folders = uiState.preferences.folders,
                    gridColumns = uiState.preferences.gridColumns,
                    iconStyle = uiState.preferences.iconStyle,
                    clockStyle = uiState.preferences.clockStyle,
                    hideAppLabels = uiState.preferences.hideAppLabels,
                    homeWidgetIds = uiState.preferences.homeWidgetIds,
                    pinnedWidgetIds = uiState.preferences.pinnedWidgetIds,
                    widgetManager = viewModel.widgetManager,
                    badgeCounts = badgeCounts,
                    onAddHomeWidgetClick = onAddHomeWidget,
                    onRemoveHomeWidget = onRemoveHomeWidget,
                    onAddPinnedWidgetClick = onAddPinnedWidget,
                    onRemovePinnedWidget = onRemovePinnedWidget,
                    onFolderClick = onFolderClick,
                    onFolderLongClick = onFolderLongClick,
                    onAppClick = safeLaunchApp,
                    onAppLongClick = onAppLongClick,
                    onOpenDrawer = onOpenDrawer,
                    onOpenSettings = onOpenSettings,
                    onExpandNotifications = onExpandNotifications,
                    onHomeScreenLongClick = onHomeScreenLongClick,
                    onDoubleTapToLock = onDoubleTapToLock
                )
            }
        }

        // Uygulama Çekmecesi (Drawer)
        AppDrawerSheet(
            isOpen = uiState.isDrawerOpen,
            searchQuery = uiState.searchQuery,
            filteredApps = uiState.filteredApps,
            recentApps = uiState.recentApps,
            iconStyle = uiState.preferences.iconStyle,
            onQueryChange = { viewModel.onSearchQueryChange(it) },
            onAppClick = safeLaunchApp,
            onAppLongClick = onAppLongClick,
            onToggleFavorite = { viewModel.toggleFavorite(it) },
            onDismiss = onDrawerDismiss,
            onOpenSettings = onOpenSettings
        )

        // Ayarlar İletişim Kutusu
        SettingsDialog(
            isOpen = uiState.isSettingsOpen,
            preferences = uiState.preferences,
            allApps = uiState.allApps + uiState.hiddenApps,
            onModeChange = { viewModel.setLauncherMode(it) },
            onIconStyleChange = { viewModel.setIconStyle(it) },
            onClockStyleChange = { viewModel.setClockStyle(it) },
            onHideAppLabelsChange = { viewModel.setHideAppLabels(it) },
            onGridColumnsChange = { viewModel.setGridColumns(it) },
            onToggleHideApp = { viewModel.toggleHideApp(it) },
            onAmoledBlackChange = { viewModel.setAmoledBlack(it) },
            onDynamicThemeChange = { viewModel.setDynamicTheme(it) },
            onIconPackChange = { viewModel.setIconPackPackage(it) },
            onShowNotificationBadgesChange = { viewModel.setShowNotificationBadges(it) },
            onToggleLockApp = { viewModel.toggleLockApp(it) },
            onRestorePreferences = { viewModel.restorePreferences(it) },
            onResetOnboarding = { viewModel.resetOnboarding() },
            onAddHomeWidgetClick = onAddHomeWidget,
            onDismiss = { viewModel.setSettingsOpen(false) }
        )

        // Ana Ekrana / Duvar Kağıdına Uzun Basınca Açılan Hızlı Menü
        if (isHomeScreenMenuOpen) {
            AlertDialog(
                onDismissRequest = { isHomeScreenMenuOpen = false },
                title = {
                    Text(
                        text = "Posea Launcher",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // 1. Launcher Ayarları
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    isHomeScreenMenuOpen = false
                                    viewModel.setSettingsOpen(true)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text("Launcher Ayarları", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                                    Text("Görünüm, modlar, ikonlar, gizlilik", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                                }
                            }
                        }

                        // 2. Widget Ekle
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    isHomeScreenMenuOpen = false
                                    onAddHomeWidget()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Widgets, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text("Widget Ekle", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                                    Text("Masaüstüne saat, hava durumu vb. ekle", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        // 3. Duvar Kâğıdı
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    isHomeScreenMenuOpen = false
                                    try {
                                        val intent = Intent(Intent.ACTION_SET_WALLPAPER)
                                        context.startActivity(Intent.createChooser(intent, "Duvar Kâğıdı Seç"))
                                    } catch (_: Exception) {}
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Wallpaper, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text("Duvar Kâğıdı", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                                    Text("Sistem duvar kâğıdını değiştir", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { isHomeScreenMenuOpen = false }) {
                        Text("Kapat")
                    }
                }
            )
        }

        // Uygulamaya Uzun Basınca Açılan Kapsamlı Hızlı Menü (Kısayollar, Favori, Gizle, Kilitle, Yeniden Adlandır, Klasöre Ekle, Bilgi)
        selectedAppForMenu?.let { app ->
            val shortcuts = remember(app.packageName) { AppShortcutsManager.getShortcuts(context, app.packageName) }
            val isLocked = uiState.preferences.lockedPackages.contains(app.packageName)

            AlertDialog(
                onDismissRequest = { selectedAppForMenu = null },
                title = { Text(app.label) },
                text = {
                    androidx.compose.foundation.layout.Column(
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
                    ) {
                        // Uygulama Kısayolları (WhatsApp Sohbetleri, Chrome Sekmeleri vb.)
                        if (shortcuts.isNotEmpty()) {
                            Text(
                                text = "Kısayollar",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                            )
                            shortcuts.forEach { shortcut ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            AppShortcutsManager.launchShortcut(context, shortcut)
                                            selectedAppForMenu = null
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (shortcut.iconBitmap != null) {
                                            Image(
                                                bitmap = shortcut.iconBitmap,
                                                contentDescription = shortcut.label,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = shortcut.label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = DividerDefaults.color.copy(alpha = 0.3f)
                            )
                        }

                        // 1. Favorilere Ekle / Kaldır
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.toggleFavorite(app.packageName)
                                    selectedAppForMenu = null
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(if (app.isFavorite) "Favorilerden Kaldır" else "Favorilere Ekle")
                            }
                        }

                        // 2. Uygulama Kilidi (Biyometrik Kasa)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.toggleLockApp(app.packageName)
                                    selectedAppForMenu = null
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (isLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(if (isLocked) "Uygulama Kilidini Kaldır" else "Uygulamayı Kilitle (Kasa)")
                            }
                        }

                        // 3. Yeniden Adlandır
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    renameInput = app.label
                                    appToRename = app
                                    selectedAppForMenu = null
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Yeniden Adlandır")
                            }
                        }

                        // 4. Uygulamayı Gizle
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.toggleHideApp(app.packageName)
                                    selectedAppForMenu = null
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Uygulamayı Gizle")
                            }
                        }

                        // 5. Klasöre Ekle
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedAppForFolder = app
                                    selectedAppForMenu = null
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Klasöre Ekle...")
                            }
                        }

                        // 6. Uygulama Bilgisi
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.openAppInfo(app)
                                    selectedAppForMenu = null
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Uygulama Bilgisi")
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedAppForMenu = null }) {
                        Text("Kapat")
                    }
                }
            )
        }

        // Uygulama Yeniden Adlandırma İletişim Kutusu
        appToRename?.let { app ->
            AlertDialog(
                onDismissRequest = { appToRename = null },
                title = { Text("Uygulamayı Yeniden Adlandır") },
                text = {
                    androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = renameInput,
                            onValueChange = { renameInput = it },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(app.label) }
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (renameInput.isNotBlank()) {
                            viewModel.setCustomAppName(app.packageName, renameInput)
                        }
                        appToRename = null
                    }) {
                        Text("Kaydet")
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = {
                            viewModel.resetCustomAppName(app.packageName)
                            appToRename = null
                        }) {
                            Text("Sıfırla")
                        }
                        TextButton(onClick = { appToRename = null }) {
                            Text("İptal")
                        }
                    }
                }
            )
        }

        // Klasör Seçme Menüsü ("Klasöre Ekle..." tıklandığında)
        selectedAppForFolder?.let { app ->
            AlertDialog(
                onDismissRequest = { selectedAppForFolder = null },
                title = { Text("'${app.label}' İçin Klasör Seçin") },
                text = {
                    androidx.compose.foundation.layout.Column(
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
                    ) {
                        // Mevcut Klasörler
                        uiState.preferences.folders.forEach { folder ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.addAppToFolder(folder.id, app.packageName)
                                        selectedAppForFolder = null
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(folder.name, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }

                        // Yeni Klasör Oluştur Butonu
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    newFolderName = ""
                                    isNewFolderDialogOpen = true
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Yeni Klasör Oluştur", color = MaterialTheme.colorScheme.primary, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedAppForFolder = null }) {
                        Text("İptal")
                    }
                }
            )
        }

        // Yeni Klasör Adı İletişim Kutusu
        if (isNewFolderDialogOpen && selectedAppForFolder != null) {
            val app = selectedAppForFolder!!
            AlertDialog(
                onDismissRequest = { isNewFolderDialogOpen = false },
                title = { Text("Yeni Klasör Oluştur") },
                text = {
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        placeholder = { Text("Klasör adı (örn: Oyunlar, Sosyal)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    androidx.compose.material3.Button(onClick = {
                        val name = newFolderName.trim().ifEmpty { "Yeni Klasör" }
                        viewModel.createFolder(name, listOf(app.packageName))
                        isNewFolderDialogOpen = false
                        selectedAppForFolder = null
                    }) {
                        Text("Oluştur")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { isNewFolderDialogOpen = false }) {
                        Text("İptal")
                    }
                }
            )
        }

        // Açık Olan Klasörün Detay Diyaloğu
        activeFolder?.let { folder ->
            // En güncel halini uiState.preferences.folders'dan al
            val currentFolder = uiState.preferences.folders.find { it.id == folder.id } ?: folder
            FolderDetailDialog(
                folder = currentFolder,
                allApps = uiState.allApps,
                iconStyle = uiState.preferences.iconStyle,
                onAppClick = safeLaunchApp,
                onRenameFolder = { viewModel.renameFolder(currentFolder.id, it) },
                onDeleteFolder = {
                    viewModel.deleteFolder(currentFolder.id)
                    activeFolder = null
                },
                onAddAppsToFolder = { newPackages ->
                    viewModel.updateFolderApps(currentFolder.id, newPackages)
                },
                onRemoveAppFromFolder = { pkg ->
                    viewModel.removeAppFromFolder(currentFolder.id, pkg)
                },
                onDismiss = { activeFolder = null }
            )
        }
    }
}
