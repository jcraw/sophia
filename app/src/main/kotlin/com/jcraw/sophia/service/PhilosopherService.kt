package com.jcraw.sophia.service

import com.jcraw.llm.LLMClient
import com.jcraw.llm.OpenAIClient
import com.jcraw.sophia.database.SimpleConversationStorage
import com.jcraw.sophia.database.StoredConversation
import com.jcraw.sophia.discussion.*
import kotlinx.coroutines.flow.StateFlow

class PhilosopherService(
    apiKey: String,
    private val philosopherRepository: PhilosopherRepository = DefaultPhilosopherRepository(),
    private val conversationStorage: SimpleConversationStorage = SimpleConversationStorage()
) {
    private val llmClient: LLMClient = OpenAIClient(apiKey).also {
        println("🔑 Initialized OpenAI client with API key: ${apiKey.take(10)}...")
    }
    private val stateManager = ConversationStateManager()
    private val conversationEngine = ConversationEngine(llmClient, stateManager)

    val conversationState: StateFlow<ConversationState> = conversationEngine.state

    fun getAllPhilosophers(): List<Philosopher> = philosopherRepository.getAllPhilosophers()

    fun searchPhilosophers(query: String): List<Philosopher> =
        philosopherRepository.searchPhilosophers(query)

    suspend fun startConversation(config: ConversationConfig) {
        println("🎭 PhilosopherService starting conversation with: ${config.participants.map { it.name }}")
        conversationEngine.startConversation(config)
    }

    fun resetConversation() {
        conversationEngine.reset()
    }

    // History management functions
    suspend fun getAllConversations(): List<StoredConversation> {
        return conversationStorage.getAllConversations()
    }

    suspend fun getCompletedConversations(): List<StoredConversation> {
        return conversationStorage.getConversationsByStatus("completed")
    }

    suspend fun loadConversation(conversationId: String): StoredConversation? {
        return conversationStorage.loadConversation(conversationId)
    }

    suspend fun deleteConversation(conversationId: String): Boolean {
        return conversationStorage.deleteConversation(conversationId)
    }

    suspend fun saveCurrentConversation(): String? {
        val currentState = conversationState.value
        return if (currentState !is ConversationState.NotStarted) {
            conversationStorage.saveConversation(currentState)
        } else null
    }

    fun close() {
        llmClient.close()
    }
}