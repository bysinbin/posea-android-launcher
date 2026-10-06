package com.bysinbin.posea.data.system

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.os.Bundle

class LauncherWidgetManager(
    private val context: Context,
    val hostId: Int = 2048
) {
    val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context)
    val appWidgetHost: AppWidgetHost = AppWidgetHost(context, hostId)

    fun startListening() {
        try {
            appWidgetHost.startListening()
        } catch (_: Exception) {}
    }

    fun stopListening() {
        try {
            appWidgetHost.stopListening()
        } catch (_: Exception) {}
    }

    fun allocateAppWidgetId(): Int {
        return appWidgetHost.allocateAppWidgetId()
    }

    fun deleteAppWidgetId(appWidgetId: Int) {
        try {
            appWidgetHost.deleteAppWidgetId(appWidgetId)
        } catch (_: Exception) {}
    }

    fun getAppWidgetInfo(appWidgetId: Int): AppWidgetProviderInfo? {
        return appWidgetManager.getAppWidgetInfo(appWidgetId)
    }

    fun bindAppWidgetIdIfAllowed(appWidgetId: Int, info: AppWidgetProviderInfo): Boolean {
        return try {
            appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, info.provider)
        } catch (e: Exception) {
            false
        }
    }

    fun createHostView(context: Context, appWidgetId: Int): AppWidgetHostView? {
        val info = getAppWidgetInfo(appWidgetId) ?: return null
        return try {
            appWidgetHost.createView(context, appWidgetId, info).apply {
                setAppWidget(appWidgetId, info)
            }
        } catch (e: Exception) {
            null
        }
    }
}
