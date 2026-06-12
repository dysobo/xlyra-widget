package com.dysobo.xlyrawidget.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class XLyraHttpException(
    val statusCode: Int,
    message: String,
) : IOException(message)

class XLyraClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build(),
) {
    suspend fun fetchSummary(baseUrl: String, token: String): XLyraSummary = withContext(Dispatchers.IO) {
        val endpoint = baseUrl.trim().trimEnd('/') + "/api/v1/dashboard/epaper-summary"
        val request = Request.Builder()
            .url(endpoint)
            .get()
            .header("Accept", "application/json")
            .header("X-Access-Token", token)
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw XLyraHttpException(response.code, parseError(body, response.message))
            }
            return@withContext XLyraSummary.fromJson(body)
        }
    }

    private fun parseError(body: String, fallback: String): String {
        if (body.isBlank()) return fallback.ifBlank { "请求失败" }
        return runCatching {
            val error = JSONObject(body).optJSONObject("error")
            val code = error?.optString("code").orEmpty()
            val message = error?.optString("message").orEmpty()
            val requestId = error?.optString("request_id").orEmpty()
            listOf(code, message, requestId).filter { it.isNotBlank() }.joinToString(" / ")
        }.getOrNull()?.ifBlank { null } ?: fallback.ifBlank { "请求失败" }
    }
}
