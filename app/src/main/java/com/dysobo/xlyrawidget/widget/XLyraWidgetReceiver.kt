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

open class BaseXLyraWidgetReceiver(
    private val variant: XLyraWidgetVariant,
) : AppWidgetProvider() {
    private val target: XLyraWidget.WidgetTarget
        get() = XLyraWidget.WidgetTarget(javaClass, variant)

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
                    XLyraWidget.updateWidget(context, appWidgetManager, widgetId, settings, target)
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
            val component = intent.component ?: return
            val ids = manager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                val loading = XLyraWidget.buildLoadingViews(context, target)
                ids.forEach { manager.updateAppWidget(it, loading) }
            }
            XLyraRefreshWorker.enqueueOneTime(context)
        }
    }

    private companion object {
        val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}

class XLyraWidgetReceiver : BaseXLyraWidgetReceiver(XLyraWidgetVariant.Full4x3)

class XLyraWidget4x2Receiver : BaseXLyraWidgetReceiver(XLyraWidgetVariant.Compact4x2)

class XLyraWidget4x1Receiver : BaseXLyraWidgetReceiver(XLyraWidgetVariant.Cost4x1)

class XLyraWidgetCost2x1Receiver : BaseXLyraWidgetReceiver(XLyraWidgetVariant.Cost2x1)

class XLyraWidgetQuota2x1Receiver : BaseXLyraWidgetReceiver(XLyraWidgetVariant.Quota2x1)
