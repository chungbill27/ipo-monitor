package com.ipomonitor.ui.screens.list

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ipomonitor.data.model.AnalysisStatus
import com.ipomonitor.data.model.IPOListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IPOListPane(
    recentIPOs: List<IPOListItem>,
    historicalIPOs: List<IPOListItem>,
    availableMonths: List<String>,
    availableIndustries: List<String>,
    selectedMonth: String?,
    selectedIndustry: String?,
    searchQuery: String,
    selectedHkexId: Int?,
    hasMoreHistorical: Boolean,
    isLoadingMore: Boolean,
    isRefreshing: Boolean,
    onItemClick: (Int) -> Unit,
    onAnalyzeClick: (Int) -> Unit,
    onLoadMore: () -> Unit,
    onMonthSelected: (String?) -> Unit,
    onIndustrySelected: (String?) -> Unit,
    onSearchChanged: (String) -> Unit,
    onRefresh: () -> Unit,
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Top bar
        TopAppBar(
            title = { Text("IPO Monitor", fontWeight = FontWeight.Bold) },
            actions = {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                }
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Filled.Refresh, "重新整理")
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Filled.Settings, "設定")
                }
            }
        )

        // Search field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChanged,
            placeholder = { Text("搜尋公司名稱、保薦人...") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChanged("") }) {
                        Icon(Icons.Filled.Clear, "清除")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        )

        // Filter Row: Two dropdown menus side by side
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Month dropdown
            FilterDropdown(
                label = "月份",
                selectedValue = selectedMonth,
                options = availableMonths,
                onSelected = onMonthSelected,
                modifier = Modifier.weight(1f)
            )

            // Industry dropdown
            FilterDropdown(
                label = "行業",
                selectedValue = selectedIndustry,
                options = availableIndustries,
                onSelected = onIndustrySelected,
                modifier = Modifier.weight(1f)
            )
        }

        // Count info + active filter indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "共 ${recentIPOs.size + historicalIPOs.size} 間公司",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (selectedMonth != null || selectedIndustry != null) {
                TextButton(
                    onClick = {
                        onMonthSelected(null)
                        onIndustrySelected(null)
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Filled.Clear, null, Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("清除篩選", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Main List
        Box(modifier = Modifier.fillMaxSize()) {
            if (recentIPOs.isEmpty() && historicalIPOs.isEmpty() && !isRefreshing) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Filled.Inbox,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("暫無入表記錄", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("點擊刷新按鈕同步港交所數據", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Recent section
                    if (recentIPOs.isNotEmpty()) {
                        item {
                            Text(
                                text = "2026年6月後入表",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(recentIPOs, key = { it.hkexId }) { item ->
                            IPOListCard(
                                item = item,
                                isSelected = item.hkexId == selectedHkexId,
                                onItemClick = { onItemClick(item.hkexId) },
                                onAnalyzeClick = { onAnalyzeClick(item.hkexId) }
                            )
                        }
                    }

                    // Historical section
                    if (historicalIPOs.isNotEmpty()) {
                        item {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Text(
                                text = "歷史入表記錄",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(historicalIPOs, key = { it.hkexId }) { item ->
                            IPOListCard(
                                item = item,
                                isSelected = item.hkexId == selectedHkexId,
                                onItemClick = { onItemClick(item.hkexId) },
                                onAnalyzeClick = { onAnalyzeClick(item.hkexId) }
                            )
                        }
                    }

                    // Load more button
                    if (hasMoreHistorical) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                OutlinedButton(
                                    onClick = onLoadMore,
                                    enabled = !isLoadingMore
                                ) {
                                    if (isLoadingMore) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    Text("載入更多（每次10間）")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============ Filter Dropdown Component ============

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterDropdown(
    label: String,
    selectedValue: String?,
    options: List<String>,
    onSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedValue ?: "全部$label",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            singleLine = true,
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodySmall
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // "All" option
            DropdownMenuItem(
                text = { Text("全部$label") },
                onClick = {
                    onSelected(null)
                    expanded = false
                },
                leadingIcon = {
                    if (selectedValue == null) {
                        Icon(Icons.Filled.Check, null, Modifier.size(16.dp))
                    }
                }
            )
            HorizontalDivider()
            // Actual options
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                    leadingIcon = {
                        if (selectedValue == option) {
                            Icon(Icons.Filled.Check, null, Modifier.size(16.dp))
                        }
                    }
                )
            }
        }
    }
}

// ============ IPO List Card ============

@Composable
private fun IPOListCard(
    item: IPOListItem,
    isSelected: Boolean,
    onItemClick: () -> Unit,
    onAnalyzeClick: () -> Unit
) {
    val status = item.getAnalysisStatus()

    Card(
        onClick = onItemClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(if (isSelected) 4.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Row 1: Name + Status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.companyNameZh.ifBlank { item.companyNameEn },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.companyNameEn.isNotBlank() && item.companyNameZh.isNotBlank()) {
                        Text(
                            text = item.companyNameEn,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                StatusBadge(status = status)
            }

            Spacer(Modifier.height(6.dp))

            // Row 2: Date + Industry
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.applicationDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.industry.isNotBlank()) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(item.industry, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.height(24.dp)
                    )
                }
            }

            // Row 3: Action button (only for PENDING / FAILED)
            if (status == AnalysisStatus.PENDING || status == AnalysisStatus.FAILED) {
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    FilledTonalButton(
                        onClick = onAnalyzeClick,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            if (status == AnalysisStatus.FAILED) Icons.Filled.Refresh else Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (status == AnalysisStatus.FAILED) "重試分析" else "分析招股書",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            // Analyzing indicator
            if (status == AnalysisStatus.ANALYZING || status == AnalysisStatus.QUEUED) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (status == AnalysisStatus.QUEUED) "排隊中..." else "AI 分析中...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// ============ Status Badge ============

@Composable
private fun StatusBadge(status: AnalysisStatus) {
    val (containerColor, contentColor, text) = when (status) {
        AnalysisStatus.PENDING -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "待分析"
        )
        AnalysisStatus.QUEUED -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            "排隊中"
        )
        AnalysisStatus.ANALYZING -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            "分析中"
        )
        AnalysisStatus.COMPLETED -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            "已完成"
        )
        AnalysisStatus.FAILED -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            "失敗"
        )
    }

    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}
