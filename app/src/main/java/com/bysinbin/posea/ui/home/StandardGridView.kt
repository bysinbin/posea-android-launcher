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
    onExpandNotifications: () -> Unit = {},
    onHomeScreenLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // 1. Sabit Saat & Tarih Başlığı (Liste kayarken asla baştan çizilmez ve takılma yapmaz)
        ClockHeader(
            alignment = Alignment.CenterHorizontally,
            textColor = MaterialTheme.colorScheme.onBackground,
            onLongClick = onHomeScreenLongClick
        )

        // 2. Eklenmiş Widget'lar (Yalnızca kullanıcı eklediyse gösterilir)
        if (pinnedWidgetIds.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            WidgetContainer(
                widgetIds = pinnedWidgetIds,
                widgetManager = widgetManager,
                onAddWidgetClick = onAddWidgetClick,
                onRemoveWidget = onRemoveWidget
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Dinamik Sütun Sayısı ile Saf Uygulama ve Klasör Izgarası (Tamamen homojen 120 FPS)
        LazyVerticalGrid(
            columns = GridCells.Fixed(gridColumns.coerceIn(3, 6)),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
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

            // 5. Uygulama İkonları Izgarası (Seçilen sütun sayısı, 100% Atlanabilir / Skippable)
            items(
                items = apps,
                key = { "${it.packageName}/${it.activityName}" },
                contentType = { "app_grid_item" }
            ) { app ->
                AppGridItem(
                    app = app,
                    iconStyle = iconStyle,
                    onAppClick = onAppClick,
                    onAppLongClick = onAppLongClick,
                    badgeCount = badgeCounts[app.packageName] ?: 0,
                    textColor = MaterialTheme.colorScheme.onBackground
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
