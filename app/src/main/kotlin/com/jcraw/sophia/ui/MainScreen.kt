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
    data class ViewSummary(val summaryId: String, val originalConversationId: String) : MainScreenMode()
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
    var selectedSummary by remember { mutableStateOf<com.jcraw.sophia.database.StoredConversationSummary?>(null) }
    var conversationSummaries by remember { mutableStateOf<Map<String, List<com.jcraw.sophia.database.StoredConversationSummary>>>(emptyMap()) }

    val scope = rememberCoroutineScope()
    val conversationState by philosopherService.conversationState.collectAsState()

    // Load conversations and summaries on startup and refresh when needed
    LaunchedEffect(Unit) {
        conversations = philosopherService.getAllConversations()
        // Load summaries for all conversations
        val summariesMap = mutableMapOf<String, List<com.jcraw.sophia.database.StoredConversationSummary>>()
        conversations.forEach { conversation ->
            val summariesForConversation = philosopherService.getSummariesForConversation(conversation.id)
            if (summariesForConversation.isNotEmpty()) {
                summariesMap[conversation.id] = summariesForConversation
            }
        }
        conversationSummaries = summariesMap
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
                selectedSummary = null
                mode = MainScreenMode.Setup
            },
            onDeleteConversation = { conversationId ->
                scope.launch {
                    if (philosopherService.deleteConversation(conversationId)) {
                        conversations = philosopherService.getAllConversations()
                        // Refresh summaries
                        val summariesMap = mutableMapOf<String, List<com.jcraw.sophia.database.StoredConversationSummary>>()
                        conversations.forEach { conversation ->
                            val summariesForConversation = philosopherService.getSummariesForConversation(conversation.id)
                            if (summariesForConversation.isNotEmpty()) {
                                summariesMap[conversation.id] = summariesForConversation
                            }
                        }
                        conversationSummaries = summariesMap

                        if (selectedConversationId == conversationId) {
                            selectedConversationId = null
                            selectedHistoricConversation = null
                            selectedSummary = null
                            mode = MainScreenMode.Setup
                        }
                    }
                }
            },
            onSummarySelect = { summaryId, originalConversationId ->
                scope.launch {
                    selectedSummary = philosopherService.loadSummary(summaryId)
                    selectedHistoricConversation = philosopherService.loadConversation(originalConversationId)
                    mode = MainScreenMode.ViewSummary(summaryId, originalConversationId)
                }
            },
            conversationSummaries = conversationSummaries,
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
                        onSummarizeForVideo = { storedConversation ->
                            scope.launch {
                                try {
                                    val summaryId = philosopherService.summarizeConversationAndGetId(storedConversation)
                                    val summary = philosopherService.loadSummary(summaryId)
                                    if (summary != null) {
                                        selectedSummary = summary
                                        mode = MainScreenMode.ViewSummary(summaryId, storedConversation.id)

                                        // Refresh summaries in sidebar
                                        val updatedSummariesForConversation = philosopherService.getSummariesForConversation(storedConversation.id)
                                        conversationSummaries = conversationSummaries + (storedConversation.id to updatedSummariesForConversation)

                                        println("✅ Summarization complete! Navigating to summary view.")
                                    }
                                } catch (e: Exception) {
                                    println("❌ Summarization failed: ${e.message}")
                                    e.printStackTrace()
                                }
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

            is MainScreenMode.ViewSummary -> {
                selectedSummary?.let { summary ->
                    selectedHistoricConversation?.let { originalConversation ->
                        SummaryViewScreen(
                            summary = summary,
                            originalConversation = originalConversation,
                            onNewConversation = {
                                philosopherService.resetConversation()
                                selectedConversationId = null
                                selectedHistoricConversation = null
                                selectedSummary = null
                                mode = MainScreenMode.Setup
                            },
                            onStartSimilar = { topic, participants ->
                                val config = ConversationConfig(
                                    topic = topic,
                                    participants = participants,
                                    maxRounds = 3, // Default for summaries
                                    maxWordsPerResponse = 50 // Default for summaries
                                )
                                scope.launch {
                                    selectedConversationId = null
                                    selectedHistoricConversation = null
                                    selectedSummary = null
                                    philosopherService.startConversation(config)
                                    mode = MainScreenMode.CurrentConversation
                                }
                            },
                            onViewOriginal = {
                                // Navigate back to original conversation
                                val currentMode = mode as MainScreenMode.ViewSummary
                                mode = MainScreenMode.ViewHistoricConversation(currentMode.originalConversationId)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
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