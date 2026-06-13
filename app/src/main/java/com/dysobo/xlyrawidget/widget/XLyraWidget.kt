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
import com.dysobo.xlyrawidget.data.XLyraSummary
import com.dysobo.xlyrawidget.data.formatCost
import com.dysobo.xlyrawidget.data.formatCount
import com.dysobo.xlyrawidget.data.formatEpochSeconds
import com.dysobo.xlyrawidget.data.formatEpochShort
import kotlinx.coroutines.flow.first

enum class XLyraWidgetVariant {
    Full4x3,
    Compact4x2,
    Cost4x1,
    Cost2x1,
    Quota2x1,
}

object XLyraWidget {
    const val ACTION_REFRESH = "com.dysobo.xlyrawidget.action.REFRESH_WIDGET"

    private val targets = listOf(
        WidgetTarget(XLyraWidgetReceiver::class.java, XLyraWidgetVariant.Full4x3),
        WidgetTarget(XLyraWidget4x2Receiver::class.java, XLyraWidgetVariant.Compact4x2),
        WidgetTarget(XLyraWidget4x1Receiver::class.java, XLyraWidgetVariant.Cost4x1),
        WidgetTarget(XLyraWidgetCost2x1Receiver::class.java, XLyraWidgetVariant.Cost2x1),
        WidgetTarget(XLyraWidgetQuota2x1Receiver::class.java, XLyraWidgetVariant.Quota2x1),
    )

    suspend fun updateAllWidgets(context: Context) {
        val appContext = context.applicationContext
        val manager = AppWidgetManager.getInstance(appContext)
        val settings = SettingsStore(appContext).settings.first()

        targets.forEach { target ->
            val component = ComponentName(appContext, target.receiverClass)
            val ids = manager.getAppWidgetIds(component)
            ids.forEach { updateWidget(appContext, manager, it, settings, target) }
        }
    }

    fun updateWidget(
        context: Context,
        manager: AppWidgetManager,
        widgetId: Int,
        settings: AppSettings,
        target: WidgetTarget,
    ) {
        manager.updateAppWidget(widgetId, buildRemoteViews(context, settings, target))
    }

    fun buildRemoteViews(context: Context, settings: AppSettings, target: WidgetTarget): RemoteViews {
        val views = RemoteViews(context.packageName, layoutFor(target.variant))
        when (target.variant) {
            XLyraWidgetVariant.Full4x3 -> bindFull4x3(views, settings)
            XLyraWidgetVariant.Compact4x2 -> bindCompact4x2(views, settings)
            XLyraWidgetVariant.Cost4x1 -> bindCost4x1(views, settings)
            XLyraWidgetVariant.Cost2x1 -> bindCost2x1(views, settings)
            XLyraWidgetVariant.Quota2x1 -> bindQuota2x1(views, settings)
        }

        views.setOnClickPendingIntent(rootIdFor(target.variant), launchPendingIntent(context))
        refreshIdFor(target.variant)?.let { refreshId ->
            views.setOnClickPendingIntent(refreshId, refreshPendingIntent(context, target.receiverClass))
        }
        return views
    }

    fun buildLoadingViews(context: Context, target: WidgetTarget): RemoteViews {
        val views = RemoteViews(context.packageName, layoutFor(target.variant))
        when (target.variant) {
            XLyraWidgetVariant.Full4x3 -> views.setTextViewText(R.id.widget_updated, "刷新中")
            XLyraWidgetVariant.Compact4x2 -> views.setTextViewText(R.id.widget_4x2_updated, "刷新中")
            else -> Unit
        }
        views.setOnClickPendingIntent(rootIdFor(target.variant), launchPendingIntent(context))
        refreshIdFor(target.variant)?.let { refreshId ->
            views.setOnClickPendingIntent(refreshId, refreshPendingIntent(context, target.receiverClass))
        }
        return views
    }

    private fun bindFull4x3(views: RemoteViews, settings: AppSettings) {
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
    }

    private fun bindCompact4x2(views: RemoteViews, settings: AppSettings) {
        val summary = settings.summary
        views.setTextViewText(R.id.widget_4x2_updated, updateText(settings))
        views.setTextViewText(R.id.widget_4x2_today_cost, formatCost(summary?.todayCost))
        views.setTextViewText(R.id.widget_4x2_today_tokens, formatCount(summary?.todayTokens))
        views.setTextViewText(R.id.widget_4x2_total_cost, formatCost(summary?.totalCost))
        views.setTextViewText(R.id.widget_4x2_5h, percentText(summary?.codexFiveHourRemainingPercent))
        views.setTextViewText(R.id.widget_4x2_7d, percentText(summary?.codexWeeklyRemainingPercent))
        views.setTextViewText(R.id.widget_4x2_models, modelPairText(settings))
    }

    private fun bindCost4x1(views: RemoteViews, settings: AppSettings) {
        val summary = settings.summary
        views.setTextViewText(R.id.widget_4x1_today_cost, formatCost(summary?.todayCost))
        views.setTextViewText(R.id.widget_4x1_today_tokens, formatCount(summary?.todayTokens))
        views.setTextViewText(R.id.widget_4x1_total_cost, formatCost(summary?.totalCost))
    }

    private fun bindCost2x1(views: RemoteViews, settings: AppSettings) {
        val summary = settings.summary
        views.setTextViewText(R.id.widget_cost_2x1_today_cost, formatCost(summary?.todayCost))
        views.setTextViewText(R.id.widget_cost_2x1_today_tokens, "${formatCount(summary?.todayTokens)} Tokens")
    }

    private fun bindQuota2x1(views: RemoteViews, settings: AppSettings) {
        val summary = settings.summary
        views.setTextViewText(R.id.widget_quota_2x1_5h_value, percentText(summary?.codexFiveHourRemainingPercent))
        views.setTextViewText(R.id.widget_quota_2x1_7d_value, percentText(summary?.codexWeeklyRemainingPercent))
        views.setProgressBar(R.id.widget_quota_2x1_5h_progress, 100, percentValue(summary?.codexFiveHourRemainingPercent), false)
        views.setProgressBar(R.id.widget_quota_2x1_7d_progress, 100, percentValue(summary?.codexWeeklyRemainingPercent), false)
    }

    private fun launchPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(context, 10, intent, pendingIntentFlags())
    }

    private fun refreshPendingIntent(context: Context, receiverClass: Class<*>): PendingIntent {
        val intent = Intent(context, receiverClass).setAction(ACTION_REFRESH)
        return PendingIntent.getBroadcast(context, receiverClass.name.hashCode(), intent, pendingIntentFlags())
    }

    private fun pendingIntentFlags(): Int {
        val immutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        return PendingIntent.FLAG_UPDATE_CURRENT or immutable
    }

    private fun layoutFor(variant: XLyraWidgetVariant): Int = when (variant) {
        XLyraWidgetVariant.Full4x3 -> R.layout.xlyra_widget
        XLyraWidgetVariant.Compact4x2 -> R.layout.xlyra_widget_4x2
        XLyraWidgetVariant.Cost4x1 -> R.layout.xlyra_widget_4x1
        XLyraWidgetVariant.Cost2x1 -> R.layout.xlyra_widget_cost_2x1
        XLyraWidgetVariant.Quota2x1 -> R.layout.xlyra_widget_quota_2x1
    }

    private fun rootIdFor(variant: XLyraWidgetVariant): Int = when (variant) {
        XLyraWidgetVariant.Full4x3 -> R.id.widget_root
        XLyraWidgetVariant.Compact4x2 -> R.id.widget_root_4x2
        XLyraWidgetVariant.Cost4x1 -> R.id.widget_root_4x1
        XLyraWidgetVariant.Cost2x1 -> R.id.widget_root_cost_2x1
        XLyraWidgetVariant.Quota2x1 -> R.id.widget_root_quota_2x1
    }

    private fun refreshIdFor(variant: XLyraWidgetVariant): Int? = when (variant) {
        XLyraWidgetVariant.Full4x3 -> R.id.widget_refresh
        XLyraWidgetVariant.Compact4x2 -> R.id.widget_4x2_refresh
        else -> null
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

    private fun modelPairText(settings: AppSettings): String {
        settings.lastError?.let { return it }
        val models = settings.summary?.modelTop3Today.orEmpty()
        if (models.isEmpty()) return if (settings.baseUrl.isBlank()) "打开 App 配置 xLyra" else "Top --"
        return models.take(2).joinToString(" · ") { "${it.modelKey} ${formatCost(it.cost)}" }
    }

    private fun updateText(settings: AppSettings): String {
        val summary = settings.summary
        if (settings.lastError != null) return "错误"
        if (settings.baseUrl.isBlank()) return "未配置"
        if (summary == null) return "等待刷新"
        return "刷新 ${formatEpochSeconds(summary.refreshAtSeconds)}"
    }

    private fun percentText(value: Int?): String = value?.coerceIn(0, 100)?.let { "$it%" } ?: "--"

    private fun percentValue(value: Int?): Int = value?.coerceIn(0, 100) ?: 0

    data class WidgetTarget(
        val receiverClass: Class<*>,
        val variant: XLyraWidgetVariant,
    )
}
