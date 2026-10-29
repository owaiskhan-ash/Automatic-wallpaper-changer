package com.autowall.changer.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.autowall.changer.R
import com.autowall.changer.work.Scheduler

/** A launcher widget that advances to the next saved photo with a single tap. */
class ChangeWallpaperWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { appWidgetId ->
            val views = RemoteViews(context.packageName, R.layout.widget_change_wallpaper)
            val intent = Intent(context, ChangeWallpaperWidgetProvider::class.java).apply {
                action = ACTION_CHANGE_WALLPAPER
                data = Uri.parse("autowall://widget/$appWidgetId")
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_change_button, pendingIntent)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_CHANGE_WALLPAPER) {
            Scheduler.changeNow(context.applicationContext)
            return
        }
        super.onReceive(context, intent)
    }

    private companion object {
        const val ACTION_CHANGE_WALLPAPER = "com.autowall.changer.action.CHANGE_WALLPAPER"
    }
}
