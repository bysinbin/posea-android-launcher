package com.bysinbin.posea.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.provider.Settings
import com.bysinbin.posea.model.AppModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class AppsRepository(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager

    /**
     * Cihazdaki tüm başlatılabilir uygulamaları listeleyen ve
     * yeni uygulama yüklenmesi/kaldırılması durumunda otomatik tetiklenen akış (Flow).
     */
    val installedAppsFlow: Flow<List<AppModel>> = callbackFlow {
        val handler = Handler(Looper.getMainLooper())

        val callback = object : LauncherApps.Callback() {
            override fun onPackageAdded(packageName: String, user: UserHandle) {
                trySend(loadApps())
            }

            override fun onPackageRemoved(packageName: String, user: UserHandle) {
                trySend(loadApps())
            }

            override fun onPackageChanged(packageName: String, user: UserHandle) {
                trySend(loadApps())
            }

            override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
                trySend(loadApps())
            }

            override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
                trySend(loadApps())
            }
        }

        launcherApps.registerCallback(callback, handler)
        // İlk yüklemeyi hemen gönder
        trySend(loadApps())

        awaitClose {
            launcherApps.unregisterCallback(callback)
        }
    }.flowOn(ioDispatcher)

    /**
     * Yüklü uygulamaları getirir ve alfabetik sıralar
     */
    fun loadApps(): List<AppModel> {
        val appList = mutableListOf<AppModel>()
        val profiles = userManager.userProfiles

        for (profile in profiles) {
            val activities = launcherApps.getActivityList(null, profile)
            for (activity in activities) {
                // Launcher'ın kendisini listeden hariç tut
                if (activity.applicationInfo.packageName == context.packageName) {
                    continue
                }

                val label = try {
                    val rawLabel = activity.label?.toString()
                    if (rawLabel.isNullOrBlank() || rawLabel.startsWith("@string/") || rawLabel.startsWith("@0x")) {
                        context.packageManager.getApplicationLabel(activity.applicationInfo).toString()
                    } else {
                        rawLabel
                    }
                } catch (e: Exception) {
                    activity.applicationInfo.packageName
                }
                val icon = try {
                    activity.getBadgedIcon(0)
                } catch (e: Exception) {
                    try {
                        context.packageManager.getApplicationIcon(activity.applicationInfo)
                    } catch (e2: Exception) {
                        null
                    }
                }

                appList.add(
                    AppModel(
                        label = label,
                        packageName = activity.applicationInfo.packageName,
                        activityName = activity.name,
                        userHandle = profile,
                        icon = icon
                    )
                )
            }
        }

        // Alfabetik sıralama (Türkçe ve yerel harf duyarlılığı ile)
        return appList.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
    }

    /**
     * Uygulamayı başlatır
     */
    suspend fun launchApp(app: AppModel): Boolean = withContext(ioDispatcher) {
        try {
            val componentName = ComponentName(app.packageName, app.activityName)
            launcherApps.startMainActivity(
                componentName,
                app.userHandle ?: Process.myUserHandle(),
                null,
                null
            )
            true
        } catch (e: Exception) {
            // Alternatif klasik başlatma
            try {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    true
                } else {
                    false
                }
            } catch (e2: Exception) {
                false
            }
        }
    }

    /**
     * Uygulama sistem bilgi sayfasını açar
     */
    fun openAppInfo(packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // ignore
        }
    }

    /**
     * Bildirim panelini aşağı kaydırır (Gesture / hızlı erişim)
     */
    fun expandNotificationShade() {
        try {
            val statusBarService = context.getSystemService("statusbar")
            val statusBarManagerClass = Class.forName("android.app.StatusBarManager")
            val expandMethod = statusBarManagerClass.getMethod("expandNotificationsPanel")
            expandMethod.invoke(statusBarService)
        } catch (e: Exception) {
            // ignore
        }
    }
}
