package com.bysinbin.posea.data.system

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings

object LauncherHelper {

    fun isDefaultLauncher(context: Context): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }
            val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.resolveActivity(
                    intent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            }
            val defaultPkg = resolveInfo?.activityInfo?.packageName
            defaultPkg == context.packageName
        } catch (_: Exception) {
            false
        }
    }

    fun requestDefaultLauncher(context: Context) {
        // 1. En doğrudan ve üretici bağımsız (özellikle Xiaomi/HyperOS, Samsung, Pixel vb.) yöntem: ACTION_HOME_SETTINGS
        try {
            val homeSettingsIntent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(homeSettingsIntent)
            return
        } catch (_: Exception) {}

        // 2. Android 10+ RoleManager yöntemi
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = context.getSystemService(RoleManager::class.java)
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    return
                }
            }
        } catch (_: Exception) {}

        // 3. Fallback: Varsayılan Uygulamalar / Uygulama Ayarları
        try {
            val defaultAppsIntent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(defaultAppsIntent)
            return
        } catch (_: Exception) {}

        // 4. Fallback: Genel Ayarlar
        try {
            val settingsIntent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(settingsIntent)
        } catch (_: Exception) {}
    }
}
