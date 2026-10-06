package com.bysinbin.posea.ui.components

import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.bysinbin.posea.data.system.LauncherWidgetManager

@Composable
fun WidgetContainer(
    widgetIds: List<Int>,
    widgetManager: LauncherWidgetManager,
    onAddWidgetClick: () -> Unit,
    onRemoveWidget: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var widgetToRemove by remember { mutableStateOf<Int?>(null) }

    if (widgetIds.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (widgetIds.isEmpty()) {
            // Widget ekleme öneri kartı
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .clickable { onAddWidgetClick() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Widgets,
                            contentDescription = "Widget Ekle",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Widget Ekle",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Müzik Çalar, Hava Durumu vb. ekleyin",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Ekle",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        } else {
            // Eklenen widget'ları listele
            widgetIds.forEach { widgetId ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 220.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                ) {
                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        factory = { ctx ->
                            widgetManager.createHostView(ctx, widgetId) ?: View(ctx)
                        },
                        update = { /* auto-updates via AppWidgetHost */ }
                    )

                    // Widget Kaldır Butonu (Sağ Üst)
                    IconButton(
                        onClick = { widgetToRemove = widgetId },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Widget'ı Kaldır",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }

    // Widget Kaldırma Onay İletişim Kutusu
    widgetToRemove?.let { id ->
        AlertDialog(
            onDismissRequest = { widgetToRemove = null },
            title = { Text("Widget'ı Kaldır") },
            text = { Text("Bu widget ana ekrandan kaldırılsın mı?") },
            confirmButton = {
                TextButton(onClick = {
                    onRemoveWidget(id)
                    widgetToRemove = null
                }) {
                    Text("Kaldır", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { widgetToRemove = null }) {
                    Text("İptal")
                }
            }
        )
    }
}
