package com.bysinbin.posea.ui.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bysinbin.posea.model.IconStyle
import com.bysinbin.posea.model.LauncherMode
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.bysinbin.posea.model.AppModel
import com.bysinbin.posea.model.UserPreferences

import androidx.compose.material3.Switch
import com.bysinbin.posea.ui.components.IconPackManager

import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.bysinbin.posea.data.system.LauncherHelper

@Composable
fun SettingsDialog(
    isOpen: Boolean,
    preferences: UserPreferences,
    allApps: List<AppModel>,
    onModeChange: (LauncherMode) -> Unit,
    onIconStyleChange: (IconStyle) -> Unit,
    onGridColumnsChange: (Int) -> Unit,
    onToggleHideApp: (String) -> Unit,
    onAmoledBlackChange: (Boolean) -> Unit = {},
    onDynamicThemeChange: (Boolean) -> Unit = {},
    onIconPackChange: (String?) -> Unit = {},
    onResetOnboarding: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isDefaultLauncher by remember { mutableStateOf(LauncherHelper.isDefaultLauncher(context)) }
    var isHiddenAppsOpen by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isDefaultLauncher = LauncherHelper.isDefaultLauncher(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (isHiddenAppsOpen) {
        HiddenAppsDialog(
            isOpen = true,
            allApps = allApps,
            hiddenPackages = preferences.hiddenPackages,
            iconStyle = preferences.iconStyle,
            onToggleHide = onToggleHideApp,
            onDismiss = { isHiddenAppsOpen = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Launcher Ayarları",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Mod Seçimi
                Text(
                    text = "Arayüz Modu",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))

                LauncherMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onModeChange(mode) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = preferences.mode == mode,
                            onClick = { onModeChange(mode) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = mode.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = mode.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = DividerDefaults.color.copy(alpha = 0.5f)
                )

                // Izgara Sütun Sayısı
                Text(
                    text = "Izgara Düzeni (Sütun Sayısı)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(3, 4, 5, 6).forEach { cols ->
                        FilterChip(
                            selected = preferences.gridColumns == cols,
                            onClick = { onGridColumnsChange(cols) },
                            label = {
                                Text(
                                    text = "$cols",
                                    fontWeight = if (preferences.gridColumns == cols) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = DividerDefaults.color.copy(alpha = 0.5f)
                )

                // İkon Stili Seçimi
                Text(
                    text = "İkon Stili",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))

                IconStyle.entries.forEach { style ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onIconStyleChange(style) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = preferences.iconStyle == style,
                            onClick = { onIconStyleChange(style) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = style.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = style.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = DividerDefaults.color.copy(alpha = 0.5f)
                )

                // AMOLED Saf Siyah Modu
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAmoledBlackChange(!preferences.isAmoledBlack) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AMOLED Saf Siyah Modu",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Piksel tasarrufu için gerçek siyah (#000000) arka plan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = preferences.isAmoledBlack,
                        onCheckedChange = onAmoledBlackChange
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Material You Dinamik Renkler
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDynamicThemeChange(!preferences.isDynamicTheme) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Material You (Dinamik Renkler)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Duvar kağıdınızın renk tonlarına göre arayüz renkleri.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = preferences.isDynamicTheme,
                        onCheckedChange = onDynamicThemeChange
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = DividerDefaults.color.copy(alpha = 0.5f)
                )

                // İkon Paketi Seçimi
                Text(
                    text = "İkon Paketi",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                val installedPacks = remember { IconPackManager.getInstalledIconPacks(context) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onIconPackChange(null) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = preferences.selectedIconPackPackage == null,
                        onClick = { onIconPackChange(null) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sistem Varsayılanı", style = MaterialTheme.typography.bodyMedium)
                }

                installedPacks.forEach { pack ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onIconPackChange(pack.packageName) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = preferences.selectedIconPackPackage == pack.packageName,
                            onClick = { onIconPackChange(pack.packageName) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(pack.label, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                if (installedPacks.isEmpty()) {
                    Text(
                        text = "Cihazınızda üçüncü parti ikon paketi (Whicons vb.) bulunamadı. Play Store'dan yükleyebilirsiniz.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = DividerDefaults.color.copy(alpha = 0.5f)
                )

                // Gizlenen Uygulamalar Yönetimi
                OutlinedButton(
                    onClick = { isHiddenAppsOpen = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.VisibilityOff, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (preferences.hiddenPackages.isEmpty()) "Uygulamaları Gizle"
                        else "Gizlenen Uygulamalar (${preferences.hiddenPackages.size})"
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Sistem Varsayılan Launcher Durumu & Seçimi
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDefaultLauncher) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (!isDefaultLauncher) {
                                LauncherHelper.requestDefaultLauncher(context)
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isDefaultLauncher) Icons.Default.CheckCircle else Icons.Default.Home,
                            contentDescription = null,
                            tint = if (isDefaultLauncher) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isDefaultLauncher) "Varsayılan Launcher Aktif" else "Varsayılan Launcher Yap",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isDefaultLauncher) "Posea birincil ana ekranınız." else "Her zaman Posea'yı açmak için dokunun.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!isDefaultLauncher) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Kurulum Sihirbazını Yeniden Başlat
                TextButton(
                    onClick = {
                        onResetOnboarding()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Kurulum Sihirbazını Tekrar Başlat")
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Tamam")
            }
        }
    )
}
