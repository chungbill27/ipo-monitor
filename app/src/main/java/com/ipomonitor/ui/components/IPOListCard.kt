package com.ipomonitor.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ipomonitor.data.model.IPOListItem

/**
 * Card component for displaying an IPO entry in the list view.
 * Shows company name, date, industry, and processing status.
 */
@Composable
fun IPOListCard(
    item: IPOListItem,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status indicator
            StatusIcon(status = item.status)

            Spacer(modifier = Modifier.width(12.dp))

            // Main content
            Column(modifier = Modifier.weight(1f)) {
                // Company name (Chinese)
                Text(
                    text = item.companyNameZh ?: "未知公司",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Company name (English)
                if (!item.companyNameEn.isNullOrBlank()) {
                    Text(
                        text = item.companyNameEn,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Date and industry row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Application date
                    if (!item.applicationDate.isNullOrBlank()) {
                        Text(
                            text = item.applicationDate,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Industry tag
                    if (!item.industryClassification.isNullOrBlank()) {
                        SuggestionChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = item.industryClassification,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.height(24.dp)
                        )
                    }

                    // Refiled badge
                    if (item.isRefiled) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("再次入表", style = MaterialTheme.typography.labelSmall) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            ),
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusIcon(status: String) {
    when (status) {
        "success" -> Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = "解析完成",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        "processing", "pending" -> CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp
        )
        "failed", "timeout" -> Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = "解析失敗",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(24.dp)
        )
        else -> Icon(
            imageVector = Icons.Filled.Refresh,
            contentDescription = "等待中",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
    }
}
