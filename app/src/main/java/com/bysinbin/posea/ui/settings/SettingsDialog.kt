package com.bysinbin.posea.ui.settings

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Widgets
import com.bysinbin.posea.data.system.PoseaAccessibilityService
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.bysinbin.posea.data.system.BackupManager
import com.bysinbin.posea.data.system.BiometricHelper
import com.bysinbin.posea.data.system.LauncherHelper
import com.bysinbin.posea.data.system.PoseaNotificationService
import com.bysinbin.posea.model.AppModel
import com.bysinbin.posea.model.IconStyle
import com.bysinbin.posea.model.LauncherMode
import com.bysinbin.posea.model.UserPreferences
import com.bysinbin.posea.ui.components.IconPackManager

@Composable
fun SettingsDialog(
    isOpen: Boolean,
    preferences: UserPreferences,
    allApps: List<AppModel>,
    onModeChange: (LauncherMode) -> Unit,
    onIconStyleChange: (IconStyle) -> Unit,
    onClockStyleChange: (com.bysinbin.posea.model.ClockStyle) -> Unit = {},
    onHideAppLabelsChange: (Boolean) -> Unit = {},
    onGridColumnsChange: (Int) -> Unit,
    onToggleHideApp: (String) -> Unit,
    onAmoledBlackChange: (Boolean) -> Unit = {},
    onDynamicThemeChange: (Boolean) -> Unit = {},
    onIconPackChange: (String?) -> Unit = {},
    onShowNotificationBadgesChange: (Boolean) -> Unit = {},
    onToggleLockApp: (String) -> Unit = {},
    onRestorePreferences: (UserPreferences) -> Unit = {},
    onResetOnboarding: () -> Unit,
    onAddHomeWidgetClick: () -> Unit = {},
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isDefaultLauncher by remember { mutableStateOf(LauncherHelper.isDefaultLauncher(context)) }
    var hasNotificationAccess by remember { mutableStateOf(PoseaNotificationService.isNotificationAccessGranted(context)) }
    var isHiddenAppsOpen by remember { mutableStateOf(false) }
    var isLockedAppsOpen by remember { mutableStateOf(false) }
    var isRestoreDialogOpen by remember { mutableStateOf(false) }
    var restoreInputText by remember { mutableStateOf("") }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isDefaultLauncher = LauncherHelper.isDefaultLauncher(context)
                hasNotificationAccess = PoseaNotificationService.isNotificationAccessGranted(context)
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

    if (isLockedAppsOpen) {
        LockedAppsDialog(
            isOpen = true,
            allApps = allApps,
            lockedPackages = preferences.lockedPackages,
            iconStyle = preferences.iconStyle,
            onToggleLock = onToggleLockApp,
            onDismiss = { isLockedAppsOpen = false }
        )
    }

    if (isRestoreDialogOpen) {
        AlertDialog(
            onDismissRequest = { isRestoreDialogOpen = false },
            title = { Text("Yedeği Geri Yükle") },
            text = {
                Column {
                    Text(
                        text = "Daha önce dışa aktarılan Posea JSON yedeğini buraya yapıştırın:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = restoreInputText,
                        onValueChange = { restoreInputText = it },
                        placeholder = { Text("{ \"version\": 1, ... }") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        maxLines = 8
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = cm.primaryClip
                            if (clip != null && clip.itemCount > 0) {
                                val text = clip.getItemAt(0).text?.toString() ?: ""
                                restoreInputText = text
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Panodan Otomatik Yapıştır")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val parsed = BackupManager.parseFromJson(restoreInputText)
                    if (parsed != null) {
                        onRestorePreferences(parsed)
                        Toast.makeText(context, "Yedek başarıyla yüklendi!", Toast.LENGTH_SHORT).show()
                        isRestoreDialogOpen = false
                    } else {
                        Toast.makeText(context, "Geçersiz yedek JSON verisi!", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("Geri Yükle")
                }
            },
            dismissButton = {
                TextButton(onClick = { isRestoreDialogOpen = false }) {
                    Text("İptal")
                }
            }
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

                // Saat Stili (Klasik Dijital / BCD İkili Saat)
                Text(
                    text = "Saat Stili",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))

                com.bysinbin.posea.model.ClockStyle.entries.forEach { style ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onClockStyleChange(style) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = preferences.clockStyle == style,
                            onClick = { onClockStyleChange(style) }
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

                // Widget Yönetimi
                Text(
                    text = "Widget Yönetimi",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDismiss()
                            onAddHomeWidgetClick()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Widgets,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Widget Ekle",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Masaüstüne saat, hava durumu veya müzik çalar ekleyin",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Ekle",
                            tint = MaterialTheme.colorScheme.primary
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

                // Masaüstünde Uygulama İsimlerini Gizle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onHideAppLabelsChange(!preferences.hideAppLabels) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Masaüstü İsimlerini Gizle",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Daha temiz ve minimalist bir masaüstü için uygulama adlarını gizler.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = preferences.hideAppLabels,
                        onCheckedChange = onHideAppLabelsChange
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

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

                Spacer(modifier = Modifier.height(6.dp))

                // Bildirim Rozetleri (Noktaları)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onShowNotificationBadgesChange(!preferences.showNotificationBadges) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Bildirim Rozetleri (Noktaları)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Uygulama simgeleri üzerinde aktif bildirim noktalarını gösterir.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = preferences.showNotificationBadges,
                        onCheckedChange = onShowNotificationBadgesChange
                    )
                }

                if (preferences.showNotificationBadges && !hasNotificationAccess) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    })
                                } catch (_: Exception) {}
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Bildirim erişim iznini açmak için dokunun",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = DividerDefaults.color.copy(alpha = 0.5f)
                )

                // Çift Dokunarak Ekranı Kilitle
                val isAccessibilityEnabled = PoseaAccessibilityService.isEnabled()
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            try {
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                })
                            } catch (_: Exception) {}
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = if (isAccessibilityEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Çift Dokunarak Ekranı Kilitle",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isAccessibilityEnabled) "Aktif · Saate veya boş alana çift dokunarak kilitleyin" else "Erişilebilirlik izni gerekli · Açmak için dokunun",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isAccessibilityEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                            )
                        }
                    }
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

                // Güvenlik ve Gizlilik (Gizlenen & Kilitli Uygulamalar)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { isHiddenAppsOpen = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.VisibilityOff, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (preferences.hiddenPackages.isEmpty()) "Gizle"
                            else "Gizli (${preferences.hiddenPackages.size})",
                            fontSize = 12.sp
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val activity = context as? Activity
                            if (activity != null && BiometricHelper.isDeviceSecure(activity)) {
                                BiometricHelper.authenticate(
                                    activity = activity,
                                    title = "Kasa Erişimi",
                                    subtitle = "Kilitli uygulamaları yönetmek için doğrulayın",
                                    onSuccess = { isLockedAppsOpen = true },
                                    onError = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
                                )
                            } else {
                                isLockedAppsOpen = true
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (preferences.lockedPackages.isEmpty()) "Kasa Kilidi"
                            else "Kilitli (${preferences.lockedPackages.size})",
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Yedekleme ve Geri Yükleme
                Text(
                    text = "Yedekleme ve Geri Yükleme",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val json = BackupManager.exportToJson(preferences)
                            BackupManager.copyToClipboard(context, json)
                            BackupManager.shareBackup(context, json)
                            Toast.makeText(context, "Yedek panoya kopyalandı ve paylaşıldı!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Yedek Paylaş", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            restoreInputText = ""
                            isRestoreDialogOpen = true
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Geri Yükle", fontSize = 12.sp)
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = DividerDefaults.color.copy(alpha = 0.5f)
                )

                // Sistem Varsayılan Launcher Durumu & Seçimi
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDefaultLauncher) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            LauncherHelper.requestDefaultLauncher(context)
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
                                text = if (isDefaultLauncher) "Varsayılan Başlatıcı Ayarı" else "Varsayılan Launcher Yap",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isDefaultLauncher) "Varsayılan ana ekranı yönetmek veya değiştirmek için dokunun." else "Her zaman Posea'yı açmak için dokunun.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
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
