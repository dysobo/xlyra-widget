package com.dysobo.xlyrawidget.data

import org.json.JSONArray
import org.json.JSONObject

data class ModelCost(
    val modelKey: String,
    val cost: Double,
)

data class XLyraSummary(
    val date: String,
    val refreshAtSeconds: Long,
    val todayCost: Double?,
    val totalCost: Double?,
    val todayRequests: Long?,
    val totalRequests: Long?,
    val todayTokens: Long?,
    val totalTokens: Long?,
    val rpmUsed: Long?,
    val rpmLimit: Long?,
    val tpmUsed: Long?,
    val tpmLimit: Long?,
    val modelTop3Today: List<ModelCost>,
    val codexAccountCount: Int?,
    val codexFiveHourRemainingPercent: Int?,
    val codexFiveHourResetAt: Long?,
    val codexWeeklyRemainingPercent: Int?,
    val codexWeeklyResetAt: Long?,
) {
    fun toJson(): String {
        val root = JSONObject()
            .put("date", date)
            .put("refresh_at", refreshAtSeconds)
            .put("kpis", JSONObject()
                .putNullable("today_cost", todayCost)
                .putNullable("total_cost", totalCost)
                .putNullable("today_requests", todayRequests)
                .putNullable("total_requests", totalRequests)
                .putNullable("today_tokens", todayTokens)
                .putNullable("total_tokens", totalTokens)
                .putNullable("rpm_used", rpmUsed)
                .putNullable("rpm_limit", rpmLimit)
                .putNullable("tpm_used", tpmUsed)
                .putNullable("tpm_limit", tpmLimit))
            .put("model_top3_today", JSONArray().also { array ->
                modelTop3Today.forEach {
                    array.put(JSONObject().put("model_key", it.modelKey).put("cost", it.cost))
                }
            })
            .put("codex_quota", JSONObject()
                .putNullable("account_count", codexAccountCount)
                .put("five_hour", JSONObject()
                    .putNullable("remaining_percent", codexFiveHourRemainingPercent)
                    .putNullable("reset_at", codexFiveHourResetAt))
                .put("weekly", JSONObject()
                    .putNullable("remaining_percent", codexWeeklyRemainingPercent)
                    .putNullable("reset_at", codexWeeklyResetAt)))
        return root.toString()
    }

    companion object {
        fun fromJson(json: String): XLyraSummary {
            val root = JSONObject(json)
            val kpis = root.optJSONObject("kpis") ?: JSONObject()
            val quota = root.optJSONObject("codex_quota") ?: JSONObject()
            val fiveHour = quota.optJSONObject("five_hour") ?: JSONObject()
            val weekly = quota.optJSONObject("weekly") ?: JSONObject()
            val topModels = root.optJSONArray("model_top3_today") ?: JSONArray()

            return XLyraSummary(
                date = root.optString("date", ""),
                refreshAtSeconds = root.optLong("refresh_at", 0L),
                todayCost = kpis.optNullableDouble("today_cost"),
                totalCost = kpis.optNullableDouble("total_cost"),
                todayRequests = kpis.optNullableLong("today_requests"),
                totalRequests = kpis.optNullableLong("total_requests"),
                todayTokens = kpis.optNullableLong("today_tokens"),
                totalTokens = kpis.optNullableLong("total_tokens"),
                rpmUsed = kpis.optNullableLong("rpm_used"),
                rpmLimit = kpis.optNullableLong("rpm_limit"),
                tpmUsed = kpis.optNullableLong("tpm_used"),
                tpmLimit = kpis.optNullableLong("tpm_limit"),
                modelTop3Today = (0 until topModels.length()).mapNotNull { index ->
                    val item = topModels.optJSONObject(index) ?: return@mapNotNull null
                    val key = item.optString("model_key", "")
                    if (key.isBlank()) null else ModelCost(key, item.optDouble("cost", 0.0))
                },
                codexAccountCount = quota.optNullableInt("account_count"),
                codexFiveHourRemainingPercent = fiveHour.optNullableInt("remaining_percent"),
                codexFiveHourResetAt = fiveHour.optNullableLong("reset_at"),
                codexWeeklyRemainingPercent = weekly.optNullableInt("remaining_percent"),
                codexWeeklyResetAt = weekly.optNullableLong("reset_at"),
            )
        }

        fun fromJsonOrNull(json: String?): XLyraSummary? {
            if (json.isNullOrBlank()) return null
            return runCatching { fromJson(json) }.getOrNull()
        }
    }
}

private fun JSONObject.putNullable(key: String, value: Any?): JSONObject {
    if (value == null) put(key, JSONObject.NULL) else put(key, value)
    return this
}

private fun JSONObject.optNullableDouble(key: String): Double? {
    if (!has(key) || isNull(key)) return null
    return optDouble(key)
}

private fun JSONObject.optNullableLong(key: String): Long? {
    if (!has(key) || isNull(key)) return null
    return optLong(key)
}

private fun JSONObject.optNullableInt(key: String): Int? {
    if (!has(key) || isNull(key)) return null
    return optInt(key)
}
