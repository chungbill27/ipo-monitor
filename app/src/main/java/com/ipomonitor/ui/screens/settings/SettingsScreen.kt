package com.ipomonitor.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ipomonitor.data.model.AIProvider
import com.ipomonitor.data.model.GeminiModel
import com.ipomonitor.data.model.OpenAIModel
import com.ipomonitor.util.CheckFrequency
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentProvider: AIProvider,
    currentGeminiModel: GeminiModel,
    currentOpenAIModel: OpenAIModel?,
    currentFrequency: CheckFrequency,
    workHoursOnly: Boolean,
    lastCheckTime: Long,
    analysisCount: Int,
    onProviderChanged: (AIProvider) -> Unit,
    onGeminiModelChanged: (GeminiModel) -> Unit,
    onOpenAIModelChanged: (OpenAIModel) -> Unit,
    onFrequencyChanged: (CheckFrequency) -> Unit,
    onWorkHoursChanged: (Boolean) -> Unit,
    onChangeApiKey: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("設定", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, "返回")
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ============ Monitoring Settings ============
            SettingsSectionHeader(
                icon = Icons.Filled.Schedule,
                title = "監控設定"
            )

            // Check frequency dropdown
            FrequencySelector(
                currentFrequency = currentFrequency,
                onFrequencyChanged = onFrequencyChanged
            )

            // Work hours only switch
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("只在工作時間檢查", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "週一至週五 9:00-18:00（港交所工作時間）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = workHoursOnly,
                        onCheckedChange = onWorkHoursChanged
                    )
                }
            }

            // Last check time display
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("上次檢查時間", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            formatLastCheckTime(lastCheckTime),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = if (lastCheckTime > 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ============ AI Model Settings ============
            SettingsSectionHeader(
                icon = Icons.Filled.AutoAwesome,
                title = "AI 模型設定"
            )

            // Provider selection
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("AI 服務提供者", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AIProvider.entries.forEach { provider ->
                            FilterChip(
                                selected = currentProvider == provider,
                                onClick = { onProviderChanged(provider) },
                                label = { Text(provider.displayName) },
                                leadingIcon = if (currentProvider == provider) {
                                    { Icon(Icons.Filled.Check, null, Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Model selector based on provider
                    when (currentProvider) {
                        AIProvider.GEMINI -> {
                            GeminiModelSelector(
                                currentModel = currentGeminiModel,
                                onModelChanged = onGeminiModelChanged
                            )
                        }
                        AIProvider.OPENAI -> {
                            OpenAIModelSelector(
                                currentModel = currentOpenAIModel ?: OpenAIModel.GPT_4O,
                                onModelChanged = onOpenAIModelChanged
                            )
                        }
                    }
                }
            }

            // Change API Key button
            OutlinedButton(
                onClick = onChangeApiKey,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Key, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("更換 API Key")
            }

            Spacer(Modifier.height(8.dp))

            // ============ Usage Stats ============
            SettingsSectionHeader(
                icon = Icons.Filled.Analytics,
                title = "使用統計"
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("已分析次數", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "$analysisCount 次",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "預估費用：約 $${String.format("%.2f", analysisCount * 0.03)} USD",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ============ Tips ============
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Lightbulb,
                            null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "省電提示",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "建議到手機「設定 → 電池 → 應用耗電管理 → IPO Monitor」選擇「不限制」，確保背景檢查不被系統中斷。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ============ Sub-components ============

@Composable
private fun SettingsSectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FrequencySelector(
    currentFrequency: CheckFrequency,
    onFrequencyChanged: (CheckFrequency) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("檢查頻率", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = currentFrequency.displayName,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    CheckFrequency.entries.forEach { frequency ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(frequency.displayName)
                                    if (frequency == CheckFrequency.MINUTES_15) {
                                        Text(
                                            "較耗電，建議關閉電池優化",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (frequency == CheckFrequency.MANUAL) {
                                        Text(
                                            "關閉自動檢查，僅手動刷新",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            onClick = {
                                onFrequencyChanged(frequency)
                                expanded = false
                            },
                            leadingIcon = {
                                if (currentFrequency == frequency) {
                                    Icon(Icons.Filled.Check, null, Modifier.size(16.dp))
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GeminiModelSelector(
    currentModel: GeminiModel,
    onModelChanged: (GeminiModel) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = currentModel.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Gemini 模型") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            GeminiModel.entries.forEach { model ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(model.displayName)
                            when (model) {
                                GeminiModel.GEMINI_35_FLASH -> Text(
                                    "最新最強，推薦使用",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                GeminiModel.GEMINI_35_FLASH_LITE -> Text(
                                    "最便宜，速度最快",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                else -> {}
                            }
                        }
                    },
                    onClick = {
                        onModelChanged(model)
                        expanded = false
                    },
                    leadingIcon = {
                        if (currentModel == model) {
                            Icon(Icons.Filled.Check, null, Modifier.size(16.dp))
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OpenAIModelSelector(
    currentModel: OpenAIModel,
    onModelChanged: (OpenAIModel) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = currentModel.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("GPT 模型") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            OpenAIModel.entries.forEach { model ->
                DropdownMenuItem(
                    text = { Text(model.displayName) },
                    onClick = {
                        onModelChanged(model)
                        expanded = false
                    },
                    leadingIcon = {
                        if (currentModel == model) {
                            Icon(Icons.Filled.Check, null, Modifier.size(16.dp))
                        }
                    }
                )
            }
        }
    }
}

// ============ Utility ============

private fun formatLastCheckTime(timestamp: Long): String {
    if (timestamp == 0L) return "尚未檢查"
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
