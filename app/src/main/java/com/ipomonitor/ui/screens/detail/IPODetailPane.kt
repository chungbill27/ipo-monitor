package com.ipomonitor.ui.screens.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ipomonitor.data.model.AnalysisStatus
import com.ipomonitor.data.model.IPOEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IPODetailPane(
    record: IPOEntity,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    onAnalyzeClick: (Int) -> Unit = {},
    onRetryClick: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val analysisStatus = record.getAnalysisStatus()

    Column(modifier = modifier.fillMaxSize()) {
        if (showBackButton) {
            TopAppBar(
                title = { Text(record.companyNameZh.ifBlank { "詳情" }) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Status banner based on analysis state
            when (analysisStatus) {
                AnalysisStatus.FAILED -> {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                            Column(Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.error)
                                    Spacer(Modifier.width(8.dp))
                                    Text("解析失敗", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error)
                                }
                                record.errorMessage?.let {
                                    Spacer(Modifier.height(4.dp))
                                    Text(it, style = MaterialTheme.typography.bodySmall)
                                }
                                Spacer(Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilledTonalButton(onClick = { onRetryClick(record.hkexId) }) {
                                        Icon(Icons.Filled.Refresh, null, Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("重新解析")
                                    }
                                    if (record.pdfUrl.isNotBlank()) {
                                        OutlinedButton(onClick = {
                                            val fullUrl = if (record.pdfUrl.startsWith("http")) record.pdfUrl
                                            else "https://www1.hkexnews.hk${record.pdfUrl}"
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl)))
                                        }) {
                                            Icon(Icons.Filled.Description, null, Modifier.size(18.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("查看 PDF")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                AnalysisStatus.ANALYZING, AnalysisStatus.QUEUED -> {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    if (analysisStatus == AnalysisStatus.QUEUED) "排隊中，等待分析..."
                                    else "AI 正在解析招股書..."
                                )
                            }
                        }
                    }
                }
                AnalysisStatus.PENDING -> {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                            Column(Modifier.padding(16.dp)) {
                                Text("尚未分析", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(8.dp))
                                Text("點擊下方按鈕開始 AI 分析招股書", style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { onAnalyzeClick(record.hkexId) }) {
                                        Icon(Icons.Filled.AutoAwesome, null, Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("分析招股書")
                                    }
                                    if (record.pdfUrl.isNotBlank()) {
                                        OutlinedButton(onClick = {
                                            val fullUrl = if (record.pdfUrl.startsWith("http")) record.pdfUrl
                                            else "https://www1.hkexnews.hk${record.pdfUrl}"
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl)))
                                        }) {
                                            Icon(Icons.Filled.Description, null, Modifier.size(18.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("查看 PDF")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                AnalysisStatus.COMPLETED -> { /* No banner needed */ }
            }

            // Card 1: Basic Info (always visible)
            item {
                SectionCard("基本資訊") {
                    Text(
                        record.companyNameZh.ifBlank { "N/A" },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (record.companyNameEn.isNotBlank()) {
                        Text(record.companyNameEn, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))
                    InfoRow("入表日期", record.applicationDate.ifBlank { "N/A" })
                    InfoRow("是否再次入表", if (record.isRefiled) "是" else "否")
                    InfoRow("註冊地", record.registrationPlace)
                    InfoRow("保薦人", record.sponsor.ifBlank { "N/A" })
                    InfoRow("行業歸類", record.industryClassification ?: record.industry.ifBlank { "N/A" })
                    InfoRow("其他上市地", record.listedElsewhere)
                }
            }

            // Card 2: Business (only show if analyzed)
            if (analysisStatus == AnalysisStatus.COMPLETED) {
                item {
                    SectionCard("業務概覽") {
                        SubTitle("公司背景")
                        Text(record.companyBackground ?: "N/A", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(12.dp))
                        SubTitle("業務簡介")
                        Text(record.businessDescription ?: "N/A", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                // Card 3: Financials
                item {
                    SectionCard("財務數據") {
                        InfoRow("市值/估值", record.marketCap)
                        Spacer(Modifier.height(8.dp))
                        SubTitle("過去3年營收")
                        Text(record.revenueThreeYears ?: "N/A", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        SubTitle("過去3年稅後盈利")
                        Text(record.profitThreeYears ?: "N/A", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                // Card 4: Shareholders & Funding
                item {
                    SectionCard("股東與融資") {
                        SubTitle("主要股東")
                        Text(record.majorShareholders ?: "N/A", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(12.dp))
                        SubTitle("前期融資")
                        Text(record.priorFunding ?: "N/A", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(12.dp))
                        SubTitle("香港子公司")
                        Text(record.hkSubsidiary ?: "N/A", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                // Analysis metadata
                item {
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            record.analyzedBy?.let { InfoRow("分析模型", it) }
                            record.analyzedAt?.let {
                                val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(it))
                                InfoRow("分析時間", dateStr)
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String?) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value ?: "N/A", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(2f))
    }
}

@Composable
private fun SubTitle(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(4.dp))
}
