package com.dysobo.xlyrawidget.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.xlyraDataStore by preferencesDataStore(name = "xlyra_widget")

data class AppSettings(
    val baseUrl: String = "",
    val refreshMinutes: Int = 30,
    val summary: XLyraSummary? = null,
    val lastError: String? = null,
    val lastSuccessAtMillis: Long? = null,
    val lastAttemptAtMillis: Long? = null,
)

class SettingsStore(context: Context) {
    private val appContext = context.applicationContext
    private val dataStore = appContext.xlyraDataStore

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            baseUrl = prefs[BASE_URL].orEmpty(),
            refreshMinutes = prefs[REFRESH_MINUTES] ?: 30,
            summary = XLyraSummary.fromJsonOrNull(prefs[SUMMARY_JSON]),
            lastError = prefs[LAST_ERROR],
            lastSuccessAtMillis = prefs[LAST_SUCCESS_AT],
            lastAttemptAtMillis = prefs[LAST_ATTEMPT_AT],
        )
    }

    suspend fun saveConfig(baseUrl: String, refreshMinutes: Int) {
        dataStore.edit { prefs ->
            prefs[BASE_URL] = baseUrl.trim().trimEnd('/')
            prefs[REFRESH_MINUTES] = refreshMinutes.coerceAtLeast(15)
        }
    }

    suspend fun markAttempt() {
        dataStore.edit { prefs ->
            prefs[LAST_ATTEMPT_AT] = System.currentTimeMillis()
        }
    }

    suspend fun saveSummary(summary: XLyraSummary) {
        dataStore.edit { prefs ->
            prefs[SUMMARY_JSON] = summary.toJson()
            prefs[LAST_SUCCESS_AT] = System.currentTimeMillis()
            prefs.remove(LAST_ERROR)
        }
    }

    suspend fun saveError(message: String) {
        dataStore.edit { prefs ->
            prefs[LAST_ERROR] = message.take(240)
            prefs[LAST_ATTEMPT_AT] = System.currentTimeMillis()
        }
    }

    private companion object {
        val BASE_URL = stringPreferencesKey("base_url")
        val REFRESH_MINUTES = intPreferencesKey("refresh_minutes")
        val SUMMARY_JSON = stringPreferencesKey("summary_json")
        val LAST_ERROR = stringPreferencesKey("last_error")
        val LAST_SUCCESS_AT = longPreferencesKey("last_success_at")
        val LAST_ATTEMPT_AT = longPreferencesKey("last_attempt_at")
    }
}
