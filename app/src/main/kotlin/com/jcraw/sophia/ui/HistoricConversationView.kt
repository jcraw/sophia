package com.jcraw.sophia.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jcraw.sophia.database.StoredConversation
import com.jcraw.sophia.database.StoredContribution
import com.jcraw.sophia.database.toPhilosopher
import com.jcraw.sophia.discussion.Philosopher
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoricConversationView(
    conversation: StoredConversation,
    onNewConversation: () -> Unit,
    onStartSimilar: (String, List<Philosopher>) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Header
        HistoricConversationHeader(
            conversation = conversation,
            onNewConversation = onNewConversation,
            onStartSimilar = onStartSimilar
        )

        HorizontalDivider()

        // Conversation Content
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Conversation Summary Card
            item {
                ConversationSummaryCard(conversation = conversation)
            }

            // Contributions
            items(conversation.contributions) { contribution ->
                HistoricContributionCard(contribution = contribution)
            }

            // Final summary if completed
            if (conversation.status == "completed") {
                item {
                    CompletionSummaryCard(conversation = conversation)
                }
            } else if (conversation.status == "error") {
                item {
                    ErrorSummaryCard(
                        errorMessage = conversation.errorMessage ?: "Unknown error occurred"
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoricConversationHeader(
    conversation: StoredConversation,
    onNewConversation: () -> Unit,
    onStartSimilar: (String, List<Philosopher>) -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = conversation.topic,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                Text(
                    text = formatHistoricDate(conversation.createdAt) +
                           if (conversation.completedAt != null) " • Completed" else " • ${conversation.status}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        actions = {
            TextButton(
                onClick = {
                    val participants = conversation.participants.map { it.toPhilosopher() }
                    onStartSimilar(conversation.topic, participants)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Restart Similar")
            }

            TextButton(onClick = onNewConversation) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Discussion")
            }
        },
        modifier = modifier
    )
}

@Composable
private fun ConversationSummaryCard(
    conversation: StoredConversation,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Discussion Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Philosophers: ${conversation.participants.size}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = conversation.participants.joinToString(", ") { it.name },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column {
                    Text(
                        text = "Contributions: ${conversation.contributions.size}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Max rounds: ${conversation.maxRounds}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoricContributionCard(
    contribution: StoredContribution,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = contribution.philosopherName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Round ${contribution.roundNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = contribution.response,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${contribution.wordCount} words",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = formatContributionTime(contribution.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CompletionSummaryCard(
    conversation: StoredConversation,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Discussion Completed",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "This philosophical discussion concluded successfully with ${conversation.contributions.size} contributions from ${conversation.participants.size} philosophers across ${conversation.maxRounds} rounds.",
                style = MaterialTheme.typography.bodyMedium
            )

            conversation.completedAt?.let { completedAt ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Completed: ${formatHistoricDate(completedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ErrorSummaryCard(
    errorMessage: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Discussion Interrupted",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "This discussion was interrupted due to an error:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

private fun formatHistoricDate(dateString: String): String {
    return try {
        val instant = Instant.parse(dateString)
        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        // Format as "Jan 15, 2024 at 3:45 PM"
        "${localDateTime.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${localDateTime.dayOfMonth}, ${localDateTime.year} at ${formatTime(localDateTime.hour, localDateTime.minute)}"
    } catch (e: Exception) {
        dateString
    }
}

private fun formatContributionTime(timestampString: String): String {
    return try {
        val instant = Instant.parse(timestampString)
        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        formatTime(localDateTime.hour, localDateTime.minute)
    } catch (e: Exception) {
        timestampString
    }
}

private fun formatTime(hour: Int, minute: Int): String {
    val amPm = if (hour >= 12) "PM" else "AM"
    val displayHour = if (hour > 12) hour - 12 else if (hour == 0) 12 else hour
    return "$displayHour:${minute.toString().padStart(2, '0')} $amPm"
}