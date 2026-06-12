package com.dysobo.xlyrawidget.data

import android.content.Context
import com.dysobo.xlyrawidget.widget.XLyraWidget
import kotlinx.coroutines.flow.first

class MissingConfigException(message: String) : IllegalStateException(message)

class XLyraRepository(context: Context) {
    private val appContext = context.applicationContext
    private val settingsStore = SettingsStore(appContext)
    private val tokenStore = SecureTokenStore(appContext)
    private val client = XLyraClient()

    suspend fun refreshNow(): XLyraSummary {
        settingsStore.markAttempt()
        val settings = settingsStore.settings.first()
        val baseUrl = settings.baseUrl
        val token = tokenStore.getToken()

        if (baseUrl.isBlank()) throw MissingConfigException("请先填写 xLyra 地址")
        if (token.isBlank()) throw MissingConfigException("请先保存 Admin Access Token")

        return try {
            val summary = client.fetchSummary(baseUrl, token)
            settingsStore.saveSummary(summary)
            XLyraWidget.updateAllWidgets(appContext)
            summary
        } catch (error: Exception) {
            settingsStore.saveError(error.message ?: "刷新失败")
            XLyraWidget.updateAllWidgets(appContext)
            throw error
        }
    }
}
