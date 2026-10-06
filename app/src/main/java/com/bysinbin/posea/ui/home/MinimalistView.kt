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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bysinbin.posea.model.AppModel
import com.bysinbin.posea.model.IconStyle
import com.bysinbin.posea.data.system.LauncherWidgetManager
import com.bysinbin.posea.ui.components.AppListItem
import com.bysinbin.posea.ui.components.ClockHeader
import com.bysinbin.posea.ui.components.WidgetContainer

@Composable
fun MinimalistView(
    favoriteApps: List<AppModel>,
    iconStyle: IconStyle,
    homeWidgetIds: List<Int> = emptyList(),
    widgetManager: LauncherWidgetManager? = null,
    onAddWidgetClick: () -> Unit = {},
    onRemoveWidget: (Int) -> Unit = {},
    onAppClick: (AppModel) -> Unit,
    onAppLongClick: (AppModel) -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Sade Dijital Saat & Tarih
        ClockHeader(
            alignment = Alignment.Start,
            textColor = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Odak / Favori Uygulamalar Listesi & Anasayfa Widget'ları
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Anasayfa Widget Desteği
            if (widgetManager != null) {
                item(key = "home_widgets") {
                    WidgetContainer(
                        widgetIds = homeWidgetIds,
                        widgetManager = widgetManager,
                        onAddWidgetClick = onAddWidgetClick,
                        onRemoveWidget = onRemoveWidget
                    )
                }
            }

            items(
                items = favoriteApps,
                key = { "${it.packageName}/${it.activityName}" },
                contentType = { "fav_app_item" }
            ) { app ->
                AppListItem(
                    app = app,
                    iconStyle = iconStyle,
                    onAppClick = onAppClick,
                    onAppLongClick = onAppLongClick,
                    textColor = MaterialTheme.colorScheme.onBackground
                )
            }

            if (favoriteApps.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Henüz favori uygulama seçilmedi.\nÇekmeceden favori ekleyebilirsiniz.",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Alt Hızlı Arama & Tüm Uygulamalar Çubuğu
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .clip(RoundedCornerShape(26.dp))
                .clickable { onOpenDrawer() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Ara",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Tüm uygulamaları ara...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.Apps,
                    contentDescription = "Çekmece",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
