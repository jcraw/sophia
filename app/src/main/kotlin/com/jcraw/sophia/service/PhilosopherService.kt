package com.jcraw.sophia.service

import com.jcraw.llm.LLMClient
import com.jcraw.llm.OpenAIClient
import com.jcraw.sophia.database.SimpleConversationStorage
import com.jcraw.sophia.database.StoredConversation
import com.jcraw.sophia.database.StoredContribution
import com.jcraw.sophia.discussion.*
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.Instant

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
    private val summarizationEngine = SummarizationEngine(llmClient)
    private val directorEngine = DirectorEngine(llmClient)

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

    // Summarization functions
    suspend fun summarizeConversation(
        storedConversation: StoredConversation,
        config: SummarizationConfig = SummarizationConfig()
    ): ConversationSummary {
        println("🎬 Starting conversation summarization for: ${storedConversation.topic}")

        // Convert StoredConversation to ConversationState.Completed
        val completedState = convertStoredToCompletedState(storedConversation)

        val summary = summarizationEngine.summarizeConversation(completedState, config)

        // Save the summary to storage
        val summaryId = conversationStorage.saveSummary(storedConversation.id, summary)
        println("💾 Saved conversation summary with ID: $summaryId")

        return summary
    }

    suspend fun summarizeConversationAndGetId(
        storedConversation: StoredConversation,
        config: SummarizationConfig = SummarizationConfig()
    ): String {
        println("🎬 Starting conversation summarization for: ${storedConversation.topic}")

        // Convert StoredConversation to ConversationState.Completed
        val completedState = convertStoredToCompletedState(storedConversation)

        val summary = summarizationEngine.summarizeConversation(completedState, config)

        // Save the summary to storage
        val summaryId = conversationStorage.saveSummary(storedConversation.id, summary)
        println("💾 Saved conversation summary with ID: $summaryId")

        return summaryId
    }

    suspend fun loadSummary(summaryId: String): com.jcraw.sophia.database.StoredConversationSummary? {
        return conversationStorage.loadSummary(summaryId)
    }

    suspend fun getAllSummaries(): List<com.jcraw.sophia.database.StoredConversationSummary> {
        return conversationStorage.getAllSummaries()
    }

    suspend fun getSummariesForConversation(conversationId: String): List<com.jcraw.sophia.database.StoredConversationSummary> {
        return conversationStorage.getSummariesForConversation(conversationId)
    }

    // Video script functions
    suspend fun createVideoScript(
        summaryId: String,
        config: DirectorConfig = DirectorConfig()
    ): VideoScript {
        println("🎬 Starting video script creation for summary: $summaryId")

        val storedSummary = conversationStorage.loadSummary(summaryId)
            ?: throw IllegalArgumentException("Summary not found: $summaryId")

        // Convert stored summary to domain model
        val summary = convertStoredSummaryToDomain(storedSummary)

        val videoScript = directorEngine.createVideoScript(summary, config)

        // Save the video script to storage
        val scriptId = conversationStorage.saveVideoScript(summaryId, videoScript)
        println("💾 Saved video script with ID: $scriptId")

        return videoScript
    }

    suspend fun createVideoScriptAndGetId(
        summaryId: String,
        config: DirectorConfig = DirectorConfig()
    ): String {
        println("🎬 Starting video script creation for summary: $summaryId")

        val storedSummary = conversationStorage.loadSummary(summaryId)
            ?: throw IllegalArgumentException("Summary not found: $summaryId")

        // Convert stored summary to domain model
        val summary = convertStoredSummaryToDomain(storedSummary)

        val videoScript = directorEngine.createVideoScript(summary, config)

        // Save the video script to storage
        val scriptId = conversationStorage.saveVideoScript(summaryId, videoScript)
        println("💾 Saved video script with ID: $scriptId")

        return scriptId
    }

    suspend fun loadVideoScript(scriptId: String): com.jcraw.sophia.database.StoredVideoScript? {
        return conversationStorage.loadVideoScript(scriptId)
    }

    suspend fun getAllVideoScripts(): List<com.jcraw.sophia.database.StoredVideoScript> {
        return conversationStorage.getAllVideoScripts()
    }

    suspend fun getVideoScriptsForSummary(summaryId: String): List<com.jcraw.sophia.database.StoredVideoScript> {
        return conversationStorage.getVideoScriptsForSummary(summaryId)
    }

    private fun convertStoredSummaryToDomain(storedSummary: com.jcraw.sophia.database.StoredConversationSummary): ConversationSummary {
        val rounds = storedSummary.rounds.map { storedRound ->
            SummaryRound(
                roundNumber = storedRound.roundNumber,
                contributions = storedRound.contributions.map { storedContrib ->
                    SummaryContribution(
                        philosopherName = storedContrib.philosopherName,
                        response = storedContrib.response,
                        wordCount = storedContrib.wordCount
                    )
                }
            )
        }

        return ConversationSummary(
            originalTopic = storedSummary.originalTopic,
            condensedTopic = storedSummary.condensedTopic,
            participants = storedSummary.participants,
            rounds = rounds,
            videoNotes = storedSummary.videoNotes,
            createdAt = java.time.Instant.parse(storedSummary.createdAt)
        )
    }

    private suspend fun convertStoredToCompletedState(storedConversation: StoredConversation): ConversationState.Completed {
        // Convert stored conversation back to domain models
        val participants = storedConversation.participants.map { storedPhil ->
            philosopherRepository.getPhilosopherById(storedPhil.id)
                ?: throw IllegalStateException("Unknown philosopher: ${storedPhil.id}")
        }

        val config = ConversationConfig(
            topic = storedConversation.topic,
            participants = participants,
            maxRounds = storedConversation.maxRounds,
            maxWordsPerResponse = storedConversation.maxWordsPerResponse
        )

        // Group contributions by round and convert to domain models
        val contributionsByRound = storedConversation.contributions.groupBy { it.roundNumber }
        val rounds = contributionsByRound.map { (roundNum, contributions) ->
            val domainContributions = contributions.map { storedContrib ->
                val philosopher = participants.find { it.name == storedContrib.philosopherName }
                    ?: throw IllegalStateException("Philosopher not found: ${storedContrib.philosopherName}")

                PhilosopherContribution(
                    philosopher = philosopher,
                    response = storedContrib.response,
                    timestamp = java.time.Instant.parse(storedContrib.timestamp),
                    roundNumber = storedContrib.roundNumber,
                    wordCount = storedContrib.wordCount
                )
            }

            ConversationRound(
                roundNumber = roundNum,
                contributions = domainContributions,
                isComplete = true
            )
        }.sortedBy { it.roundNumber }

        val allContributions = rounds.flatMap { it.contributions }.sortedBy { it.timestamp }

        return ConversationState.Completed(
            config = config,
            rounds = rounds,
            finalContributions = allContributions
        )
    }

    fun close() {
        llmClient.close()
    }
}