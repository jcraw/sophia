package com.jcraw.sophia.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jcraw.sophia.discussion.Philosopher
import com.jcraw.sophia.discussion.ConversationConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationSetupScreen(
    philosophers: List<Philosopher>,
    onStartConversation: (ConversationConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var topic by remember { mutableStateOf("") }
    var selectedPhilosophers by remember { mutableStateOf(setOf<String>()) }
    var rounds by remember { mutableIntStateOf(3) }
    var maxWords by remember { mutableIntStateOf(150) }

    val canStart = topic.isNotBlank() && selectedPhilosophers.isNotEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Setup Philosophical Discussion",
            style = MaterialTheme.typography.headlineMedium
        )

        // Topic Input
        OutlinedTextField(
            value = topic,
            onValueChange = { topic = it },
            label = { Text("Discussion Topic") },
            placeholder = { Text("e.g., What is the meaning of life?") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        // Philosopher Selection
        Text(
            text = "Select Philosophers",
            style = MaterialTheme.typography.titleMedium
        )

        LazyColumn(
            modifier = Modifier.height(200.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(philosophers) { philosopher ->
                PhilosopherSelectionItem(
                    philosopher = philosopher,
                    isSelected = philosopher.id in selectedPhilosophers,
                    onSelectionChange = { isSelected ->
                        selectedPhilosophers = if (isSelected) {
                            selectedPhilosophers + philosopher.id
                        } else {
                            selectedPhilosophers - philosopher.id
                        }
                    }
                )
            }
        }

        // Configuration Options
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Rounds: $rounds", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = rounds.toFloat(),
                    onValueChange = { rounds = it.toInt() },
                    valueRange = 1f..5f,
                    steps = 3
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text("Max Words: $maxWords", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = maxWords.toFloat(),
                    onValueChange = { maxWords = it.toInt() },
                    valueRange = 50f..300f,
                    steps = 4
                )
            }
        }

        // Selected Philosophers Summary
        if (selectedPhilosophers.isNotEmpty()) {
            Text(
                text = "Selected: ${selectedPhilosophers.size} philosopher(s)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Start Button
        Button(
            onClick = {
                val selectedPhils = philosophers.filter { it.id in selectedPhilosophers }
                val config = ConversationConfig(
                    topic = topic,
                    participants = selectedPhils,
                    maxRounds = rounds,
                    maxWordsPerResponse = maxWords
                )
                onStartConversation(config)
            },
            enabled = canStart,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start Discussion")
        }
    }
}

@Composable
private fun PhilosopherSelectionItem(
    philosopher: Philosopher,
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = { onSelectionChange(!isSelected) }
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = philosopher.name,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "${philosopher.era} • ${philosopher.nationality}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = philosopher.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Checkbox(
                checked = isSelected,
                onCheckedChange = onSelectionChange
            )
        }
    }
}