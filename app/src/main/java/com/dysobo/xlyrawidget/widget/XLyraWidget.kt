package com.dysobo.xlyrawidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.dysobo.xlyrawidget.MainActivity
import com.dysobo.xlyrawidget.R
import com.dysobo.xlyrawidget.data.AppSettings
import com.dysobo.xlyrawidget.data.SettingsStore
import com.dysobo.xlyrawidget.data.formatCost
import com.dysobo.xlyrawidget.data.formatCount
import com.dysobo.xlyrawidget.data.formatEpochSeconds
import com.dysobo.xlyrawidget.data.formatEpochShort
import kotlinx.coroutines.flow.first

object XLyraWidget {
    const val ACTION_REFRESH = "com.dysobo.xlyrawidget.action.REFRESH_WIDGET"

    suspend fun updateAllWidgets(context: Context) {
        val appContext = context.applicationContext
        val manager = AppWidgetManager.getInstance(appContext)
        val component = ComponentName(appContext, XLyraWidgetReceiver::class.java)
        val ids = manager.getAppWidgetIds(component)
        if (ids.isEmpty()) return

        val settings = SettingsStore(appContext).settings.first()
        ids.forEach { updateWidget(appContext, manager, it, settings) }
    }

    fun updateWidget(
        context: Context,
        manager: AppWidgetManager,
        widgetId: Int,
        settings: AppSettings,
    ) {
        manager.updateAppWidget(widgetId, buildRemoteViews(context, settings))
    }

    fun buildRemoteViews(context: Context, settings: AppSettings): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.xlyra_widget)
        val summary = settings.summary

        views.setTextViewText(R.id.widget_updated, updateText(settings))
        views.setTextViewText(R.id.widget_cost_today, formatCost(summary?.todayCost))
        views.setTextViewText(R.id.widget_total_cost, formatCost(summary?.totalCost))
        views.setTextViewText(R.id.widget_requests_today, formatCount(summary?.todayRequests))
        views.setTextViewText(R.id.widget_tokens_today, formatCount(summary?.todayTokens))
        views.setTextViewText(R.id.widget_rate, "${formatCount(summary?.rpmUsed)} / ${formatCount(summary?.tpmUsed)}")
        views.setTextViewText(R.id.widget_5h_value, percentText(summary?.codexFiveHourRemainingPercent))
        views.setTextViewText(R.id.widget_5h_reset, formatEpochSeconds(summary?.codexFiveHourResetAt))
        views.setTextViewText(R.id.widget_7d_value, percentText(summary?.codexWeeklyRemainingPercent))
        views.setTextViewText(R.id.widget_7d_reset, formatEpochShort(summary?.codexWeeklyResetAt))
        views.setProgressBar(R.id.widget_5h_progress, 100, percentValue(summary?.codexFiveHourRemainingPercent), false)
        views.setProgressBar(R.id.widget_7d_progress, 100, percentValue(summary?.codexWeeklyRemainingPercent), false)
        views.setTextViewText(R.id.widget_model_1, modelLine(settings, 0))
        views.setTextViewText(R.id.widget_model_2, modelLine(settings, 1))

        views.setOnClickPendingIntent(R.id.widget_root, launchPendingIntent(context))
        views.setOnClickPendingIntent(R.id.widget_refresh, refreshPendingIntent(context))
        return views
    }

    fun buildLoadingViews(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.xlyra_widget)
        views.setTextViewText(R.id.widget_updated, "刷新中")
        views.setOnClickPendingIntent(R.id.widget_root, launchPendingIntent(context))
        views.setOnClickPendingIntent(R.id.widget_refresh, refreshPendingIntent(context))
        return views
    }

    private fun launchPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(context, 10, intent, pendingIntentFlags())
    }

    private fun refreshPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, XLyraWidgetReceiver::class.java)
            .setAction(ACTION_REFRESH)
        return PendingIntent.getBroadcast(context, 11, intent, pendingIntentFlags())
    }

    private fun pendingIntentFlags(): Int {
        val immutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        return PendingIntent.FLAG_UPDATE_CURRENT or immutable
    }

    private fun modelLine(settings: AppSettings, index: Int): String {
        settings.lastError?.let { return it }
        val models = settings.summary?.modelTop3Today.orEmpty()
        val model = models.getOrNull(index)
        if (model == null) {
            return if (index == 0 && settings.baseUrl.isBlank()) "打开 App 配置 xLyra" else "Top${index + 1} --"
        }
        return "Top${index + 1}  ${model.modelKey}  ${formatCost(model.cost)}"
    }

    private fun updateText(settings: AppSettings): String {
        val summary = settings.summary
        if (settings.lastError != null) return "错误：${settings.lastError}"
        if (settings.baseUrl.isBlank()) return "未配置"
        if (summary == null) return "等待刷新"
        return "刷新 ${formatEpochSeconds(summary.refreshAtSeconds)}"
    }

    private fun percentText(value: Int?): String = value?.coerceIn(0, 100)?.let { "$it%" } ?: "--"

    private fun percentValue(value: Int?): Int = value?.coerceIn(0, 100) ?: 0
}
