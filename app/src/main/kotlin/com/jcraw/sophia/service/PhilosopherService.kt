package com.jcraw.sophia.service

import com.jcraw.llm.LLMClient
import com.jcraw.llm.OpenAIClient
import com.jcraw.sophia.discussion.*
import kotlinx.coroutines.flow.StateFlow

class PhilosopherService(
    apiKey: String,
    private val philosopherRepository: PhilosopherRepository = DefaultPhilosopherRepository()
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

    fun close() {
        llmClient.close()
    }
}