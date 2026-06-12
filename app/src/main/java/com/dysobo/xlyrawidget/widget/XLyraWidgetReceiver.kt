package com.dysobo.xlyrawidget.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import com.dysobo.xlyrawidget.data.SettingsStore
import com.dysobo.xlyrawidget.worker.XLyraRefreshWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class XLyraWidgetReceiver : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val pendingResult = goAsync()
        widgetScope.launch {
            try {
                val settings = SettingsStore(context).settings.first()
                appWidgetIds.forEach { widgetId ->
                    XLyraWidget.updateWidget(context, appWidgetManager, widgetId, settings)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == XLyraWidget.ACTION_REFRESH) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(intent.component)
            if (ids != null && ids.isNotEmpty()) {
                val loading = XLyraWidget.buildLoadingViews(context)
                ids.forEach { manager.updateAppWidget(it, loading) }
            }
            XLyraRefreshWorker.enqueueOneTime(context)
        }
    }

    private companion object {
        val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
