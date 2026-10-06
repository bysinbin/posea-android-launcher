package com.bysinbin.posea.model

import android.os.UserHandle
import androidx.compose.runtime.Immutable

/**
 * Launcher modları:
 * - MINIMALIST: Sadece favori uygulamalar, sade saat ve metin/monochrome odak.
 * - STANDARD: Klasik ızgara (Grid), dock ve widget yerleşimi.
 * - HYBRID: Sayfa 1 minimalist odak, yana kaydırınca standart ızgara.
 */
enum class LauncherMode(val title: String, val description: String) {
    MINIMALIST(
        title = "Minimalist (Odak)",
        description = "Dikkati dağıtmayan sade liste, saat ve temel uygulamalar."
    ),
    STANDARD(
        title = "Standart (Izgara)",
        description = "Alışılmış ızgara görünümü, dock barı ve zengin masaüstü."
    ),
    HYBRID(
        title = "Hibrit (Dengeli)",
        description = "Ana ekran minimalist odak; yana kaydırınca klasik ızgara."
    )
}

/**
 * İkon gösterim tercihleri
 */
enum class IconStyle(val title: String, val description: String) {
    COLOR(
        title = "Renkli",
        description = "Uygulamaların orijinal renkli simgeleri."
    ),
    MONOCHROME(
        title = "Monochrome",
        description = "Zarif, göz yormayan tek renkli simgeler."
    ),
    TEXT_ONLY(
        title = "Sadece Metin",
        description = "İkonsuz, tamamen tipografik minimalizm."
    )
}

/**
 * Cihazda yüklü olan başlatılabilir uygulama bilgisi
 */
@Immutable
data class AppModel(
    val label: String,
    val packageName: String,
    val activityName: String,
    val userHandle: UserHandle? = null,
    val isFavorite: Boolean = false
)

/**
 * Kullanıcı tercihleri ve Launcher durumu
 */
@Immutable
data class UserPreferences(
    val mode: LauncherMode = LauncherMode.HYBRID,
    val iconStyle: IconStyle = IconStyle.COLOR,
    val favoritePackages: Set<String> = emptySet(),
    val pinnedWidgetIds: List<Int> = emptyList(),
    val isOnboardingCompleted: Boolean = false,
    val showClock: Boolean = true,
    val autoOpenKeyboard: Boolean = false,
    val gridColumns: Int = 4
)

/**
 * Kullanıcının istediği favori uygulamaları (YT Music, WhatsApp, Telefon, Chrome, Mesajlar, Instagram)
 * cihazdaki yüklü uygulamalar arasından otomatik tespit eder.
 */
fun detectPreferredFavorites(apps: List<AppModel>): Set<String> {
    val targetPackages = listOf(
        "com.google.android.apps.youtube.music",
        "com.whatsapp",
        "com.google.android.dialer",
        "com.android.dialer",
        "com.android.chrome",
        "com.google.android.apps.messaging",
        "com.android.mms",
        "com.instagram.android"
    )

    val selected = mutableSetOf<String>()

    // 1. Doğrudan paket adı eşleşmeleri
    for (target in targetPackages) {
        if (apps.any { it.packageName == target }) {
            selected.add(target)
        }
    }

    // 2. Paket adı farklıysa kelime bazlı eşleştirme
    fun findByKeyword(keywords: List<String>): String? {
        return apps.firstOrNull { app ->
            val label = app.label.lowercase()
            val pkg = app.packageName.lowercase()
            keywords.any { k -> label.contains(k) || pkg.contains(k) }
        }?.packageName
    }

    // YT Music
    if (!selected.any { it.contains("youtube.music") }) {
        findByKeyword(listOf("youtube music", "yt music", "music"))?.let { selected.add(it) }
    }
    // WhatsApp
    if (!selected.any { it.contains("whatsapp") }) {
        findByKeyword(listOf("whatsapp"))?.let { selected.add(it) }
    }
    // Telefon
    if (!selected.any { it.contains("dialer") || it.contains("phone") }) {
        findByKeyword(listOf("telefon", "phone", "dialer"))?.let { selected.add(it) }
    }
    // Chrome
    if (!selected.any { it.contains("chrome") }) {
        findByKeyword(listOf("chrome", "tarayıcı", "browser"))?.let { selected.add(it) }
    }
    // Mesajlar
    if (!selected.any { it.contains("messaging") || it.contains("mms") }) {
        findByKeyword(listOf("mesaj", "message", "sms"))?.let { selected.add(it) }
    }
    // Instagram
    if (!selected.any { it.contains("instagram") }) {
        findByKeyword(listOf("instagram"))?.let { selected.add(it) }
    }

    return if (selected.isNotEmpty()) selected else apps.take(5).map { it.packageName }.toSet()
}
