package com.bysinbin.posea.data.system

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings

object GestureNavigationHelper {

    private const val KEY_FORCE_FSG_NAV_BAR = "force_fsg_nav_bar"
    private const val GESTURE_OVERLAY_PACKAGE = "com.android.internal.systemui.navbar.gestural"

    /**
     * Uygulamanın WRITE_SECURE_SETTINGS iznine sahip olup olmadığını denetler
     */
    fun hasSecureSettingsPermission(context: Context): Boolean {
        return context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) ==
                PackageManager.PERMISSION_GRANTED
    }

    /**
     * Xiaomi / HyperOS tam ekran hareketlerinin zorlanıp zorlanmadığını okur
     */
    fun isFullScreenGesturesForced(context: Context): Boolean {
        return try {
            Settings.Global.getInt(context.contentResolver, KEY_FORCE_FSG_NAV_BAR, 0) == 1
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Tam ekran hareketlerini zorla aktif veya deaktif eder
     */
    fun setFullScreenGesturesForced(context: Context, enable: Boolean): Boolean {
        if (!hasSecureSettingsPermission(context)) return false

        return try {
            val value = if (enable) 1 else 0
            val success = Settings.Global.putInt(context.contentResolver, KEY_FORCE_FSG_NAV_BAR, value)

            try {
                // Stok Android gestural navigasyon overlay katmanı
                val action = if (enable) "enable" else "disable"
                Runtime.getRuntime().exec(arrayOf("cmd", "overlay", action, GESTURE_OVERLAY_PACKAGE))
            } catch (_: Exception) {}

            success
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Başlatıcı her ön plana geldiğinde veya açıldığında ayarı korumak için kontrol eder
     */
    fun ensureGesturesIfEnabled(context: Context, shouldBeEnabled: Boolean) {
        if (!shouldBeEnabled || !hasSecureSettingsPermission(context)) return
        try {
            val current = Settings.Global.getInt(context.contentResolver, KEY_FORCE_FSG_NAV_BAR, 0)
            if (current != 1) {
                setFullScreenGesturesForced(context, true)
            }
        } catch (_: Exception) {}
    }

    fun getAdbGrantCommand(context: Context): String {
        return "adb shell pm grant ${context.packageName} android.permission.WRITE_SECURE_SETTINGS"
    }

    fun getAdbForceCommand(): String {
        return "adb shell settings put global force_fsg_nav_bar 1"
    }
}
