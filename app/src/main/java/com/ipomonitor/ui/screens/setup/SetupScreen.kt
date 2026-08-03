package com.ipomonitor.ui.screens.setup

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ipomonitor.data.model.AIProvider
import com.ipomonitor.data.model.GeminiModel
import com.ipomonitor.data.model.OpenAIModel
import kotlinx.coroutines.launch

/**
 * First-launch setup screen with multi-model support.
 * Supports Gemini and OpenAI GPT model selection.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    onSetupComplete: () -> Unit,
    onValidateGeminiKey: suspend (String, GeminiModel) -> Boolean,
    onValidateOpenAIKey: suspend (String, OpenAIModel) -> Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedProvider by remember { mutableStateOf(AIProvider.GEMINI) }
    var apiKey by remember { mutableStateOf("") }
    var selectedModel by remember { mutableStateOf(OpenAIModel.GPT_4O) }
    var selectedGeminiModel by remember { mutableStateOf(GeminiModel.GEMINI_35_FLASH) }
    var showModelDropdown by remember { mutableStateOf(false) }
    var showGeminiModelDropdown by remember { mutableStateOf(false) }
    var isValidating by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var currentStep by remember { mutableIntStateOf(0) }

    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            // App Icon & Title
            Icon(
                imageVector = Icons.Filled.TrendingUp,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "IPO Monitor",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "港股新股入表智能監控",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(32.dp))

            // Step indicator
            LinearProgressIndicator(
                progress = { if (currentStep == 0) 0.33f else if (currentStep == 1) 0.66f else 1f },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(32.dp))

            // Step 0: Feature highlights
            AnimatedVisibility(visible = currentStep == 0) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "只需兩步即可開始",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(24.dp))

                    FeatureItem(
                        icon = Icons.Filled.Notifications,
                        title = "自動監控",
                        description = "每小時自動檢查港交所，發現新入表公司即時通知"
                    )
                    FeatureItem(
                        icon = Icons.Filled.AutoAwesome,
                        title = "按需 AI 分析",
                        description = "手動選擇感興趣的公司進行招股書深度解析，節省費用"
                    )
                    FeatureItem(
                        icon = Icons.Filled.PhoneAndroid,
                        title = "折疊屏優化",
                        description = "專為 Oppo Find N5 設計的雙欄自適應介面"
                    )

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = { currentStep = 1 },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("開始設定")
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Filled.ArrowForward, contentDescription = null)
                    }
                }
            }

            // Step 1: Model selection
            AnimatedVisibility(visible = currentStep == 1) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "選擇 AI 模型",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "用於分析招股書 PDF 並提取核心資訊",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(24.dp))

                    // Gemini Card
                    Card(
                        onClick = { selectedProvider = AIProvider.GEMINI },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedProvider == AIProvider.GEMINI)
                                MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = selectedProvider == AIProvider.GEMINI,
                                    onClick = { selectedProvider = AIProvider.GEMINI }
                                )
                                Column(modifier = Modifier.padding(start = 12.dp)) {
                                    Text("Google Gemini", fontWeight = FontWeight.Bold)
                                    Text(
                                        "推薦 · 原生 PDF 支援 · 免費額度充足",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Gemini model dropdown
                            AnimatedVisibility(visible = selectedProvider == AIProvider.GEMINI) {
                                Column(modifier = Modifier.padding(start = 48.dp, top = 8.dp)) {
                                    ExposedDropdownMenuBox(
                                        expanded = showGeminiModelDropdown,
                                        onExpandedChange = { showGeminiModelDropdown = it }
                                    ) {
                                        OutlinedTextField(
                                            value = selectedGeminiModel.displayName,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("選擇模型") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showGeminiModelDropdown) },
                                            modifier = Modifier
                                                .menuAnchor()
                                                .fillMaxWidth()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = showGeminiModelDropdown,
                                            onDismissRequest = { showGeminiModelDropdown = false }
                                        ) {
                                            GeminiModel.entries.forEach { model ->
                                                DropdownMenuItem(
                                                    text = { Text(model.displayName) },
                                                    onClick = {
                                                        selectedGeminiModel = model
                                                        showGeminiModelDropdown = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // OpenAI Card
                    Card(
                        onClick = { selectedProvider = AIProvider.OPENAI },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedProvider == AIProvider.OPENAI)
                                MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = selectedProvider == AIProvider.OPENAI,
                                    onClick = { selectedProvider = AIProvider.OPENAI }
                                )
                                Column(modifier = Modifier.padding(start = 12.dp)) {
                                    Text("OpenAI GPT", fontWeight = FontWeight.Bold)
                                    Text(
                                        "高準確度 · 每百萬 token $2.50-$5.00",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Model dropdown
                            AnimatedVisibility(visible = selectedProvider == AIProvider.OPENAI) {
                                Column(modifier = Modifier.padding(start = 48.dp, top = 8.dp)) {
                                    ExposedDropdownMenuBox(
                                        expanded = showModelDropdown,
                                        onExpandedChange = { showModelDropdown = it }
                                    ) {
                                        OutlinedTextField(
                                            value = selectedModel.displayName,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("選擇模型") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showModelDropdown) },
                                            modifier = Modifier
                                                .menuAnchor()
                                                .fillMaxWidth()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = showModelDropdown,
                                            onDismissRequest = { showModelDropdown = false }
                                        ) {
                                            OpenAIModel.entries.forEach { model ->
                                                DropdownMenuItem(
                                                    text = { Text(model.displayName) },
                                                    onClick = {
                                                        selectedModel = model
                                                        showModelDropdown = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = { currentStep = 2 },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("下一步")
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Filled.ArrowForward, contentDescription = null)
                    }

                    TextButton(onClick = { currentStep = 0 }) {
                        Text("返回")
                    }
                }
            }

            // Step 2: API Key input
            AnimatedVisibility(visible = currentStep == 2) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "輸入 ${selectedProvider.displayName} API Key",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(24.dp))

                    // Get API Key button
                    OutlinedButton(
                        onClick = {
                            val url = when (selectedProvider) {
                                AIProvider.GEMINI -> "https://aistudio.google.com/apikey"
                                AIProvider.OPENAI -> "https://platform.openai.com/api-keys"
                            }
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            when (selectedProvider) {
                                AIProvider.GEMINI -> "前往 Google AI Studio 取得 Key"
                                AIProvider.OPENAI -> "前往 OpenAI Platform 取得 Key"
                            }
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // API Key input
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = {
                            apiKey = it
                            showError = false
                        },
                        label = {
                            Text(
                                when (selectedProvider) {
                                    AIProvider.GEMINI -> "Gemini API Key"
                                    AIProvider.OPENAI -> "OpenAI API Key"
                                }
                            )
                        },
                        placeholder = {
                            Text(
                                when (selectedProvider) {
                                    AIProvider.GEMINI -> "AIza..."
                                    AIProvider.OPENAI -> "sk-..."
                                }
                            )
                        },
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = "切換顯示"
                                )
                            }
                        },
                        isError = showError,
                        supportingText = if (showError) {
                            { Text(errorMessage) }
                        } else null,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    // Security notice
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Key 使用 AES-256 加密存儲在本機，不會上傳至任何伺服器",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Validate button
                    Button(
                        onClick = {
                            scope.launch {
                                isValidating = true
                                showError = false
                                val isValid = when (selectedProvider) {
                                    AIProvider.GEMINI -> onValidateGeminiKey(apiKey.trim(), selectedGeminiModel)
                                    AIProvider.OPENAI -> onValidateOpenAIKey(apiKey.trim(), selectedModel)
                                }
                                isValidating = false
                                if (isValid) {
                                    onSetupComplete()
                                } else {
                                    showError = true
                                    errorMessage = "API Key 無效，請檢查後重試"
                                }
                            }
                        },
                        enabled = apiKey.isNotBlank() && !isValidating,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isValidating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("驗證中...")
                        } else {
                            Text("驗證並開始使用")
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Filled.Check, contentDescription = null)
                        }
                    }

                    TextButton(onClick = { currentStep = 1 }) {
                        Text("返回選擇模型")
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun FeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
