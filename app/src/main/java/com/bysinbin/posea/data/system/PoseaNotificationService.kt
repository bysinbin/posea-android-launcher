package com.bysinbin.posea.data.system

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MediaTrackInfo(
    val title: String,
    val artist: String,
    val packageName: String
)

class PoseaNotificationService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        updateNotificationCounts()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        if (instance == this) instance = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        updateNotificationCounts()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        updateNotificationCounts()
    }

    private fun updateNotificationCounts() {
        try {
            val active = activeNotifications ?: return
            val counts = mutableMapOf<String, Int>()
            var foundMedia: MediaTrackInfo? = null

            for (notif in active) {
                if (!notif.isOngoing && notif.packageName != packageName) {
                    counts[notif.packageName] = (counts[notif.packageName] ?: 0) + 1
                }

                if (foundMedia == null) {
                    val extras = notif.notification.extras
                    val isMedia = notif.notification.category == android.app.Notification.CATEGORY_TRANSPORT ||
                            extras.containsKey(android.app.Notification.EXTRA_MEDIA_SESSION)
                    if (isMedia) {
                        val title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString()?.trim()
                        val artist = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString()?.trim()
                        if (!title.isNullOrEmpty()) {
                            foundMedia = MediaTrackInfo(
                                title = title,
                                artist = artist ?: "",
                                packageName = notif.packageName
                            )
                        }
                    }
                }
            }
            _badgeCounts.value = counts
            _currentTrack.value = foundMedia
        } catch (_: Exception) {}
    }

    companion object {
        var instance: PoseaNotificationService? = null
        private val _badgeCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
        val badgeCounts: StateFlow<Map<String, Int>> = _badgeCounts.asStateFlow()

        private val _currentTrack = MutableStateFlow<MediaTrackInfo?>(null)
        val currentTrack: StateFlow<MediaTrackInfo?> = _currentTrack.asStateFlow()

        fun isNotificationAccessGranted(context: Context): Boolean {
            val cn = ComponentName(context, PoseaNotificationService::class.java)
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            return flat != null && flat.contains(cn.flattenToString())
        }
    }
}
