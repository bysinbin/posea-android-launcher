package com.bysinbin.posea.model

import android.graphics.drawable.Drawable
import android.os.UserHandle

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
data class AppModel(
    val label: String,
    val packageName: String,
    val activityName: String,
    val userHandle: UserHandle? = null,
    val isFavorite: Boolean = false,
    val icon: Drawable? = null
)

/**
 * Kullanıcı tercihleri ve Launcher durumu
 */
data class UserPreferences(
    val mode: LauncherMode = LauncherMode.HYBRID,
    val iconStyle: IconStyle = IconStyle.COLOR,
    val favoritePackages: Set<String> = emptySet(),
    val isOnboardingCompleted: Boolean = false,
    val showClock: Boolean = true,
    val autoOpenKeyboard: Boolean = false,
    val gridColumns: Int = 4
)
