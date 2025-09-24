package com.jcraw.sophia.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jcraw.sophia.database.StoredConversation
import com.jcraw.sophia.discussion.ConversationState
import com.jcraw.sophia.discussion.Philosopher
import com.jcraw.sophia.discussion.ConversationConfig
import com.jcraw.sophia.service.PhilosopherService
import kotlinx.coroutines.launch

sealed class MainScreenMode {
    data object Setup : MainScreenMode()
    data object CurrentConversation : MainScreenMode()
    data class ViewHistoricConversation(val conversationId: String) : MainScreenMode()
}

@Composable
fun MainScreen(
    philosopherService: PhilosopherService,
    modifier: Modifier = Modifier
) {
    var mode by remember { mutableStateOf<MainScreenMode>(MainScreenMode.Setup) }
    var conversations by remember { mutableStateOf<List<StoredConversation>>(emptyList()) }
    var selectedConversationId by remember { mutableStateOf<String?>(null) }
    var selectedHistoricConversation by remember { mutableStateOf<StoredConversation?>(null) }

    val scope = rememberCoroutineScope()
    val conversationState by philosopherService.conversationState.collectAsState()

    // Load conversations on startup and refresh when needed
    LaunchedEffect(Unit) {
        conversations = philosopherService.getAllConversations()
    }

    // Auto-save conversations when they complete
    LaunchedEffect(conversationState) {
        if (conversationState is ConversationState.Completed) {
            philosopherService.saveCurrentConversation()
            conversations = philosopherService.getAllConversations()
        }
    }

    Row(modifier = modifier.fillMaxSize()) {
        // History Sidebar
        ConversationHistoryPanel(
            conversations = conversations,
            selectedConversationId = selectedConversationId,
            onConversationSelect = { conversationId ->
                selectedConversationId = conversationId
                scope.launch {
                    selectedHistoricConversation = philosopherService.loadConversation(conversationId)
                    mode = MainScreenMode.ViewHistoricConversation(conversationId)
                }
            },
            onNewConversation = {
                philosopherService.resetConversation()
                selectedConversationId = null
                selectedHistoricConversation = null
                mode = MainScreenMode.Setup
            },
            onDeleteConversation = { conversationId ->
                scope.launch {
                    if (philosopherService.deleteConversation(conversationId)) {
                        conversations = philosopherService.getAllConversations()
                        if (selectedConversationId == conversationId) {
                            selectedConversationId = null
                            selectedHistoricConversation = null
                            mode = MainScreenMode.Setup
                        }
                    }
                }
            },
            modifier = Modifier.width(320.dp)
        )

        // Main Content Area
        when (mode) {
            MainScreenMode.Setup -> {
                ConversationSetupScreen(
                    philosophers = philosopherService.getAllPhilosophers(),
                    onStartConversation = { config ->
                        scope.launch {
                            selectedConversationId = null
                            selectedHistoricConversation = null
                            philosopherService.startConversation(config)
                            mode = MainScreenMode.CurrentConversation
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            MainScreenMode.CurrentConversation -> {
                ConversationScreen(
                    state = conversationState,
                    onNewConversation = {
                        philosopherService.resetConversation()
                        selectedConversationId = null
                        selectedHistoricConversation = null
                        mode = MainScreenMode.Setup
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            is MainScreenMode.ViewHistoricConversation -> {
                selectedHistoricConversation?.let { conversation ->
                    HistoricConversationView(
                        conversation = conversation,
                        onNewConversation = {
                            philosopherService.resetConversation()
                            selectedConversationId = null
                            selectedHistoricConversation = null
                            mode = MainScreenMode.Setup
                        },
                        onStartSimilar = { topic, participants ->
                            val config = ConversationConfig(
                                topic = topic,
                                participants = participants,
                                maxRounds = conversation.maxRounds,
                                maxWordsPerResponse = conversation.maxWordsPerResponse
                            )
                            scope.launch {
                                selectedConversationId = null
                                selectedHistoricConversation = null
                                philosopherService.startConversation(config)
                                mode = MainScreenMode.CurrentConversation
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                } ?: run {
                    // Loading or error state
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(32.dp),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}