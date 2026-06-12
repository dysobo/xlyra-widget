package com.dysobo.xlyrawidget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.dysobo.xlyrawidget.data.AppSettings
import com.dysobo.xlyrawidget.data.SecureTokenStore
import com.dysobo.xlyrawidget.data.SettingsStore
import com.dysobo.xlyrawidget.data.XLyraRepository
import com.dysobo.xlyrawidget.data.formatCost
import com.dysobo.xlyrawidget.data.formatCount
import com.dysobo.xlyrawidget.data.formatEpochSeconds
import com.dysobo.xlyrawidget.data.formatLimit
import com.dysobo.xlyrawidget.data.formatMillis
import com.dysobo.xlyrawidget.widget.XLyraWidget
import com.dysobo.xlyrawidget.worker.XLyraRefreshWorker
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            XLyraApp()
        }
    }
}

@Composable
private fun XLyraApp() {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF0F766E),
            secondary = Color(0xFF334155),
            background = Color(0xFFF7F8FA),
            surface = Color.White,
        ),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            XLyraScreen()
        }
    }
}

@Composable
private fun XLyraScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val settingsStore = remember { SettingsStore(context) }
    val tokenStore = remember { SecureTokenStore(context) }
    val repository = remember { XLyraRepository(context) }
    val scope = rememberCoroutineScope()
    val settings by settingsStore.settings.collectAsState(initial = AppSettings())

    var baseUrl by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var refreshMinutes by remember { mutableStateOf("30") }
    var tokenConfigured by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(settings.baseUrl, settings.refreshMinutes) {
        baseUrl = settings.baseUrl
        refreshMinutes = settings.refreshMinutes.toString()
        tokenConfigured = tokenStore.hasToken()
        XLyraRefreshWorker.schedule(context, settings.refreshMinutes)
        runCatching { XLyraWidget.updateAllWidgets(context) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Header(settings)
        Spacer(Modifier.height(18.dp))
        SummaryPanel(settings)
        Spacer(Modifier.height(18.dp))
        ConfigPanel(
            baseUrl = baseUrl,
            token = token,
            refreshMinutes = refreshMinutes,
            tokenConfigured = tokenConfigured,
            busy = busy,
            notice = notice,
            onBaseUrlChange = { baseUrl = it },
            onTokenChange = { token = it },
            onRefreshMinutesChange = { refreshMinutes = it.filter(Char::isDigit).take(4) },
            onSave = {
                scope.launch {
                    busy = true
                    notice = null
                    runCatching {
                        val minutes = refreshMinutes.toIntOrNull()?.coerceAtLeast(15) ?: 30
                        settingsStore.saveConfig(baseUrl, minutes)
                        if (token.isNotBlank()) {
                            tokenStore.saveToken(token)
                            token = ""
                            tokenConfigured = true
                        }
                        XLyraRefreshWorker.schedule(context, minutes)
                        XLyraWidget.updateAllWidgets(context)
                    }.onSuccess {
                        notice = "配置已保存"
                    }.onFailure {
                        notice = it.message ?: "保存失败"
                    }
                    busy = false
                }
            },
            onRefresh = {
                scope.launch {
                    busy = true
                    notice = null
                    runCatching { repository.refreshNow() }
                        .onSuccess { notice = "刷新成功" }
                        .onFailure { notice = it.message ?: "刷新失败" }
                    busy = false
                }
            },
        )
    }
}

@Composable
private fun Header(settings: AppSettings) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("xLyra 用量卡片", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                text = if (settings.baseUrl.isBlank()) "等待配置数据源" else settings.baseUrl,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF64748B),
            )
        }
        Text(
            text = "${settings.refreshMinutes} 分钟",
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFF0F766E),
        )
    }
}

@Composable
private fun SummaryPanel(settings: AppSettings) {
    val summary = settings.summary
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            KpiCard("今日成本", formatCost(summary?.todayCost), Modifier.weight(1f))
            KpiCard("累计成本", formatCost(summary?.totalCost), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            KpiCard("今日请求", formatCount(summary?.todayRequests), Modifier.weight(1f))
            KpiCard("今日 Tokens", formatCount(summary?.todayTokens), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            KpiCard("RPM", formatLimit(summary?.rpmUsed, summary?.rpmLimit), Modifier.weight(1f))
            KpiCard("TPM", formatLimit(summary?.tpmUsed, summary?.tpmLimit), Modifier.weight(1f))
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Codex OAuth", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("账号数 ${summary?.codexAccountCount ?: "--"}")
                    Text("5h ${summary?.codexFiveHourRemainingPercent ?: "--"}%")
                    Text("周 ${summary?.codexWeeklyRemainingPercent ?: "--"}%")
                }
                Text(
                    text = "5h 重置 ${formatEpochSeconds(summary?.codexFiveHourResetAt)} · 周重置 ${formatEpochSeconds(summary?.codexWeeklyResetAt)}",
                    color = Color(0xFF64748B),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("今日模型成本 Top", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                val models = summary?.modelTop3Today.orEmpty()
                if (models.isEmpty()) {
                    Text("暂无数据", color = Color(0xFF64748B))
                } else {
                    models.forEach { model ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(model.modelKey, modifier = Modifier.weight(1f), maxLines = 1)
                            Text(formatCost(model.cost), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
        Text(
            text = statusLine(settings),
            color = if (settings.lastError == null) Color(0xFF64748B) else Color(0xFFB91C1C),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun KpiCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = Color(0xFF64748B))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ConfigPanel(
    baseUrl: String,
    token: String,
    refreshMinutes: String,
    tokenConfigured: Boolean,
    busy: Boolean,
    notice: String?,
    onBaseUrlChange: (String) -> Unit,
    onTokenChange: (String) -> Unit,
    onRefreshMinutesChange: (String) -> Unit,
    onSave: () -> Unit,
    onRefresh: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("配置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = baseUrl,
            onValueChange = onBaseUrlChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("xLyra 地址") },
            placeholder = { Text("例如 https://example.com") },
        )
        OutlinedTextField(
            value = token,
            onValueChange = onTokenChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(if (tokenConfigured) "Admin Token（已保存，留空不改）" else "Admin Token") },
            visualTransformation = PasswordVisualTransformation(),
        )
        OutlinedTextField(
            value = refreshMinutes,
            onValueChange = onRefreshMinutesChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("后台刷新间隔（分钟，最低 15）") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onSave, enabled = !busy, modifier = Modifier.weight(1f)) {
                Text("保存配置")
            }
            OutlinedButton(onClick = onRefresh, enabled = !busy, modifier = Modifier.weight(1f)) {
                Text("立即刷新")
            }
        }
        if (notice != null) {
            Text(notice, color = Color(0xFF475569), style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun statusLine(settings: AppSettings): String {
    val summary = settings.summary
    val success = "成功 ${formatMillis(settings.lastSuccessAtMillis)} · 接口 ${formatEpochSeconds(summary?.refreshAtSeconds)}"
    return when {
        settings.lastError != null -> "最后错误：${settings.lastError}"
        summary != null -> success
        else -> "尚未获取数据"
    }
}
