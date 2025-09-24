package com.jcraw.sophia.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jcraw.sophia.database.StoredConversation
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun ConversationHistoryPanel(
    conversations: List<StoredConversation>,
    selectedConversationId: String?,
    onConversationSelect: (String) -> Unit,
    onNewConversation: () -> Unit,
    onDeleteConversation: (String) -> Unit,
    onSummarySelect: ((String, String) -> Unit)? = null, // summaryId, originalConversationId
    conversationSummaries: Map<String, List<com.jcraw.sophia.database.StoredConversationSummary>> = emptyMap(),
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxHeight(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column {
            // Header with New Conversation button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Conversations",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = onNewConversation
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Conversation"
                    )
                }
            }

            HorizontalDivider()

            // Conversations list
            if (conversations.isEmpty()) {
                EmptyHistoryState(
                    onNewConversation = onNewConversation,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(conversations) { conversation ->
                        ConversationHistoryItem(
                            conversation = conversation,
                            isSelected = conversation.id == selectedConversationId,
                            onSelect = { onConversationSelect(conversation.id) },
                            onDelete = { onDeleteConversation(conversation.id) },
                            summaries = conversationSummaries[conversation.id] ?: emptyList(),
                            onSummarySelect = onSummarySelect
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationHistoryItem(
    conversation: StoredConversation,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    summaries: List<com.jcraw.sophia.database.StoredConversationSummary>,
    onSummarySelect: ((String, String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = if (isSelected) {
            CardDefaults.cardElevation(defaultElevation = 4.dp)
        } else {
            CardDefaults.cardElevation()
        }
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = conversation.topic,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${conversation.participants.size} philosophers • ${conversation.contributions.size} contributions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = formatConversationDate(conversation.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ConversationStatusChip(status = conversation.status)

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Conversation",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Show summaries if available
            if (summaries.isNotEmpty() && onSummarySelect != null) {
                Spacer(modifier = Modifier.height(8.dp))

                Column {
                    Text(
                        text = "Summaries (${summaries.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    summaries.take(2).forEach { summary -> // Show max 2 summaries
                        SummaryChip(
                            summary = summary,
                            onClick = { onSummarySelect(summary.id, conversation.id) }
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }

                    if (summaries.size > 2) {
                        Text(
                            text = "+${summaries.size - 2} more",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationStatusChip(
    status: String,
    modifier: Modifier = Modifier
) {
    val (color, text) = when (status) {
        "completed" -> MaterialTheme.colorScheme.primary to "Done"
        "in_progress" -> MaterialTheme.colorScheme.secondary to "Active"
        "error" -> MaterialTheme.colorScheme.error to "Error"
        else -> MaterialTheme.colorScheme.outline to status
    }

    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.12f),
        contentColor = color,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun EmptyHistoryState(
    onNewConversation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No conversations yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Start your first philosophical discussion",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onNewConversation) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Start Discussion")
        }
    }
}

private fun formatConversationDate(createdAt: String): String {
    return try {
        val instant = Instant.parse(createdAt)
        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        "${localDateTime.month.name.take(3)} ${localDateTime.dayOfMonth}, ${formatTime(localDateTime.hour, localDateTime.minute)}"
    } catch (e: Exception) {
        "Unknown date"
    }
}

private fun formatTime(hour: Int, minute: Int): String {
    val amPm = if (hour >= 12) "PM" else "AM"
    val displayHour = if (hour > 12) hour - 12 else if (hour == 0) 12 else hour
    return "$displayHour:${minute.toString().padStart(2, '0')} $amPm"
}

@Composable
private fun SummaryChip(
    summary: com.jcraw.sophia.database.StoredConversationSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f),
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${summary.rounds.size}R • ${summary.totalWordCount}W",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}