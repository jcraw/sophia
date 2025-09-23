package com.jcraw.sophia.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jcraw.sophia.discussion.ConversationState
import com.jcraw.sophia.discussion.PhilosopherContribution

@Composable
fun ConversationScreen(
    state: ConversationState,
    onNewConversation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        when (state) {
            is ConversationState.NotStarted -> {
                EmptyStateMessage(
                    message = "No conversation started",
                    onAction = onNewConversation,
                    actionText = "Start New Discussion"
                )
            }

            is ConversationState.InProgress -> {
                InProgressConversationView(
                    state = state,
                    onNewConversation = onNewConversation
                )
            }

            is ConversationState.Completed -> {
                CompletedConversationView(
                    state = state,
                    onNewConversation = onNewConversation
                )
            }

            is ConversationState.Error -> {
                ErrorStateMessage(
                    message = state.message,
                    onRetry = onNewConversation
                )
            }
        }
    }
}

@Composable
private fun InProgressConversationView(
    state: ConversationState.InProgress,
    onNewConversation: () -> Unit
) {
    val listState = rememberLazyListState()
    val contributions = state.getAllContributions()

    // Auto-scroll to bottom when new contributions are added
    LaunchedEffect(contributions.size) {
        if (contributions.isNotEmpty()) {
            listState.animateScrollToItem(contributions.size)
        }
    }

    Column {
        // Header
        ConversationHeader(
            topic = state.config.topic,
            currentRound = state.currentRound,
            maxRounds = state.config.maxRounds,
            currentPhilosopher = state.currentPhilosopher?.name,
            onNewConversation = onNewConversation
        )

        HorizontalDivider()

        // Contributions List
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(contributions) { contribution ->
                ContributionCard(contribution = contribution)
            }

            // Show thinking indicator
            state.currentPhilosopher?.let { philosopher ->
                item {
                    ThinkingIndicator(philosopherName = philosopher.name)
                }
            }
        }
    }
}

@Composable
private fun CompletedConversationView(
    state: ConversationState.Completed,
    onNewConversation: () -> Unit
) {
    val listState = rememberLazyListState()

    Column {
        // Header
        ConversationHeader(
            topic = state.config.topic,
            currentRound = state.config.maxRounds,
            maxRounds = state.config.maxRounds,
            currentPhilosopher = null,
            onNewConversation = onNewConversation,
            isCompleted = true
        )

        HorizontalDivider()

        // Final Contributions
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Discussion Complete",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "The philosophical discussion has concluded with ${state.finalContributions.size} contributions across ${state.config.maxRounds} rounds.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            items(state.finalContributions) { contribution ->
                ContributionCard(contribution = contribution)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationHeader(
    topic: String,
    currentRound: Int,
    maxRounds: Int,
    currentPhilosopher: String?,
    onNewConversation: () -> Unit,
    isCompleted: Boolean = false
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = topic,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                Text(
                    text = if (isCompleted) {
                        "Completed - $maxRounds rounds"
                    } else {
                        "Round $currentRound of $maxRounds" +
                        (currentPhilosopher?.let { " • $it thinking..." } ?: "")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        actions = {
            TextButton(onClick = onNewConversation) {
                Text("New Discussion")
            }
        }
    )
}

@Composable
private fun ContributionCard(
    contribution: PhilosopherContribution,
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
                    text = contribution.philosopher.name,
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

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${contribution.wordCount} words",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ThinkingIndicator(
    philosopherName: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "$philosopherName is thinking...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyStateMessage(
    message: String,
    onAction: () -> Unit,
    actionText: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onAction) {
            Text(actionText)
        }
    }
}

@Composable
private fun ErrorStateMessage(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Error: $message",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Try Again")
        }
    }
}