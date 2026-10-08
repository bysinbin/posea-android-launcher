package com.bysinbin.posea.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.bysinbin.posea.data.system.LauncherWidgetManager
import com.bysinbin.posea.model.AppFolder
import com.bysinbin.posea.model.AppModel
import com.bysinbin.posea.model.IconStyle
import com.bysinbin.posea.ui.components.AppGridItem
import com.bysinbin.posea.ui.components.AppIcon
import com.bysinbin.posea.ui.components.ClockHeader
import com.bysinbin.posea.ui.components.FolderGridItem
import com.bysinbin.posea.ui.components.WidgetContainer

@Composable
fun StandardGridView(
    apps: List<AppModel>,
    dockApps: List<AppModel>,
    folders: List<AppFolder> = emptyList(),
    gridColumns: Int = 4,
    iconStyle: IconStyle,
    clockStyle: com.bysinbin.posea.model.ClockStyle = com.bysinbin.posea.model.ClockStyle.DIGITAL,
    hideAppLabels: Boolean = false,
    pinnedWidgetIds: List<Int>,
    widgetManager: LauncherWidgetManager,
    badgeCounts: Map<String, Int> = emptyMap(),
    onAddWidgetClick: () -> Unit,
    onRemoveWidget: (Int) -> Unit,
    onFolderClick: (AppFolder) -> Unit = {},
    onFolderLongClick: (AppFolder) -> Unit = {},
    onAppClick: (AppModel) -> Unit,
    onAppLongClick: (AppModel) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onExpandNotifications: () -> Unit = {},
    onHomeScreenLongClick: () -> Unit = {},
    onDoubleTapToLock: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()

    // Klasörlere dahil edilmiş uygulama paketleri kümesi (Klasördeki uygulamalar ana listede tekrar görünmez)
    val folderAppPackages = remember(folders) {
        folders.flatMap { it.packageNames }.toSet()
    }
    val standaloneApps = remember(apps, folderAppPackages) {
        if (folderAppPackages.isEmpty()) apps
        else apps.filterNot { folderAppPackages.contains(it.packageName) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
    ) {
        // Dinamik Sütun Sayısı ile Saf Uygulama ve Klasör Izgarası (Tamamen homojen 120 FPS)
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(gridColumns.coerceIn(3, 6)),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 0. Saat & Tarih & İsteğe Bağlı Widget Başlığı
            item(
                key = "clock_header",
                span = { GridItemSpan(maxLineSpan) },
                contentType = "clock_header"
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    ClockHeader(
                        alignment = Alignment.CenterHorizontally,
                        textColor = MaterialTheme.colorScheme.onBackground,
                        clockStyle = clockStyle,
                        onLongClick = onHomeScreenLongClick,
                        onDoubleClick = onDoubleTapToLock,
                        onSwipeDown = onExpandNotifications
                    )
                    if (pinnedWidgetIds.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        WidgetContainer(
                            widgetIds = pinnedWidgetIds,
                            widgetManager = widgetManager,
                            onAddWidgetClick = onAddWidgetClick,
                            onRemoveWidget = onRemoveWidget
                        )
                    }
                }
            }

            // 1. Klasörler / Gruplar
            items(
                items = folders,
                key = { "folder_${it.id}" },
                contentType = { "folder_item" }
            ) { folder ->
                FolderGridItem(
                    folder = folder,
                    allApps = apps,
                    iconStyle = iconStyle,
                    onClick = { onFolderClick(folder) },
                    onLongClick = { onFolderLongClick(folder) },
                    textColor = MaterialTheme.colorScheme.onBackground
                )
            }

            // 2. Uygulama İkonları Izgarası (Klasörde yer almayan bağımsız uygulamalar, 100% Atlanabilir / Skippable)
            items(
                items = standaloneApps,
                key = { it.packageName },
                contentType = { "app_grid_item" }
            ) { app ->
                AppGridItem(
                    app = app,
                    iconStyle = iconStyle,
                    onAppClick = onAppClick,
                    onAppLongClick = onAppLongClick,
                    badgeCount = badgeCounts[app.packageName] ?: 0,
                    textColor = MaterialTheme.colorScheme.onBackground,
                    hideLabel = hideAppLabels
                )
            }
        }

        // Alt Dock Barı
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // Dock uygulamaları
                dockApps.take(4).forEach { app ->
                    val click = remember(app, onAppClick) { { onAppClick(app) } }
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .clickable(onClick = click),
                        contentAlignment = Alignment.Center
                    ) {
                        AppIcon(
                            app = app,
                            iconStyle = iconStyle,
                            size = 46.dp,
                            badgeCount = badgeCounts[app.packageName] ?: 0
                        )
                    }
                }

                // Uygulama Çekmecesi Butonu
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f))
                        .clickable { onOpenDrawer() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = "Tüm Uygulamalar",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}
