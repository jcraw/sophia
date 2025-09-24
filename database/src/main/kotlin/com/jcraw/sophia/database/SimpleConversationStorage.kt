package com.jcraw.sophia.database

import com.jcraw.sophia.discussion.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant

@Serializable
data class StoredConversation(
    val id: String,
    val topic: String,
    val participants: List<StoredPhilosopher>,
    val maxRounds: Int,
    val maxWordsPerResponse: Int,
    val status: String, // "in_progress", "completed", "error"
    val createdAt: String,
    val completedAt: String? = null,
    val errorMessage: String? = null,
    val contributions: List<StoredContribution> = emptyList()
)

@Serializable
data class StoredPhilosopher(
    val id: String,
    val name: String,
    val description: String,
    val era: String,
    val nationality: String
)

@Serializable
data class StoredContribution(
    val philosopherId: String,
    val philosopherName: String,
    val response: String,
    val timestamp: String,
    val roundNumber: Int,
    val wordCount: Int
)

@Serializable
data class StoredConversationSummary(
    val id: String,
    val originalConversationId: String,
    val originalTopic: String,
    val condensedTopic: String,
    val participants: List<String>,
    val rounds: List<StoredSummaryRound>,
    val videoNotes: String,
    val createdAt: String,
    val totalWordCount: Int
)

@Serializable
data class StoredSummaryRound(
    val roundNumber: Int,
    val contributions: List<StoredSummaryContribution>
)

@Serializable
data class StoredSummaryContribution(
    val philosopherName: String,
    val response: String,
    val wordCount: Int
)

class SimpleConversationStorage(
    private val storageDirectory: String = "conversations"
) {
    private val mutex = Mutex()
    private val summariesDirectory = "$storageDirectory/summaries"
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    init {
        File(storageDirectory).mkdirs()
        File(summariesDirectory).mkdirs()
    }

    suspend fun saveConversation(
        conversationState: ConversationState,
        existingId: String? = null
    ): String = mutex.withLock {
        val id = existingId ?: generateId()

        val stored = when (conversationState) {
            is ConversationState.InProgress -> {
                StoredConversation(
                    id = id,
                    topic = conversationState.config.topic,
                    participants = conversationState.config.participants.map { it.toStored() },
                    maxRounds = conversationState.config.maxRounds,
                    maxWordsPerResponse = conversationState.config.maxWordsPerResponse,
                    status = "in_progress",
                    createdAt = Instant.now().toString(),
                    contributions = conversationState.getAllContributions().map { it.toStored() }
                )
            }
            is ConversationState.Completed -> {
                StoredConversation(
                    id = id,
                    topic = conversationState.config.topic,
                    participants = conversationState.config.participants.map { it.toStored() },
                    maxRounds = conversationState.config.maxRounds,
                    maxWordsPerResponse = conversationState.config.maxWordsPerResponse,
                    status = "completed",
                    createdAt = Instant.now().toString(),
                    completedAt = Instant.now().toString(),
                    contributions = conversationState.finalContributions.map { it.toStored() }
                )
            }
            is ConversationState.Summarizing -> {
                // Save the original conversation as completed
                StoredConversation(
                    id = id,
                    topic = conversationState.originalConversation.config.topic,
                    participants = conversationState.originalConversation.config.participants.map { it.toStored() },
                    maxRounds = conversationState.originalConversation.config.maxRounds,
                    maxWordsPerResponse = conversationState.originalConversation.config.maxWordsPerResponse,
                    status = "completed",
                    createdAt = Instant.now().toString(),
                    completedAt = Instant.now().toString(),
                    contributions = conversationState.originalConversation.finalContributions.map { it.toStored() }
                )
            }
            is ConversationState.SummarizationComplete -> {
                // Save the original conversation as completed
                StoredConversation(
                    id = id,
                    topic = conversationState.originalConversation.config.topic,
                    participants = conversationState.originalConversation.config.participants.map { it.toStored() },
                    maxRounds = conversationState.originalConversation.config.maxRounds,
                    maxWordsPerResponse = conversationState.originalConversation.config.maxWordsPerResponse,
                    status = "completed",
                    createdAt = Instant.now().toString(),
                    completedAt = Instant.now().toString(),
                    contributions = conversationState.originalConversation.finalContributions.map { it.toStored() }
                )
            }
            is ConversationState.Error -> {
                val existingConversation = loadConversation(id)
                existingConversation?.copy(
                    status = "error",
                    errorMessage = conversationState.message,
                    completedAt = Instant.now().toString()
                ) ?: StoredConversation(
                    id = id,
                    topic = "Error occurred",
                    participants = emptyList(),
                    maxRounds = 1,
                    maxWordsPerResponse = 100,
                    status = "error",
                    createdAt = Instant.now().toString(),
                    completedAt = Instant.now().toString(),
                    errorMessage = conversationState.message
                )
            }
            ConversationState.NotStarted -> return@withLock id
        }

        val file = File(storageDirectory, "$id.json")
        file.writeText(json.encodeToString(stored))
        id
    }

    suspend fun loadConversation(id: String): StoredConversation? = mutex.withLock {
        val file = File(storageDirectory, "$id.json")
        if (!file.exists()) return@withLock null

        try {
            json.decodeFromString<StoredConversation>(file.readText())
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getAllConversations(): List<StoredConversation> = mutex.withLock {
        File(storageDirectory).listFiles { _, name -> name.endsWith(".json") }
            ?.mapNotNull { file ->
                try {
                    json.decodeFromString<StoredConversation>(file.readText())
                } catch (e: Exception) {
                    null
                }
            }
            ?.sortedByDescending { it.createdAt }
            ?: emptyList()
    }

    suspend fun getConversationsByStatus(status: String): List<StoredConversation> {
        return getAllConversations().filter { it.status == status }
    }

    suspend fun deleteConversation(id: String): Boolean = mutex.withLock {
        val file = File(storageDirectory, "$id.json")
        file.delete()
    }

    // Summary storage methods
    suspend fun saveSummary(
        originalConversationId: String,
        summary: com.jcraw.sophia.discussion.ConversationSummary
    ): String = mutex.withLock {
        val id = generateSummaryId()

        val storedSummary = StoredConversationSummary(
            id = id,
            originalConversationId = originalConversationId,
            originalTopic = summary.originalTopic,
            condensedTopic = summary.condensedTopic,
            participants = summary.participants,
            rounds = summary.rounds.map { round ->
                StoredSummaryRound(
                    roundNumber = round.roundNumber,
                    contributions = round.contributions.map { contrib ->
                        StoredSummaryContribution(
                            philosopherName = contrib.philosopherName,
                            response = contrib.response,
                            wordCount = contrib.wordCount
                        )
                    }
                )
            },
            videoNotes = summary.videoNotes,
            createdAt = summary.createdAt.toString(),
            totalWordCount = summary.totalWordCount
        )

        val file = File(summariesDirectory, "$id.json")
        file.writeText(json.encodeToString(storedSummary))
        id
    }

    suspend fun loadSummary(id: String): StoredConversationSummary? = mutex.withLock {
        val file = File(summariesDirectory, "$id.json")
        if (!file.exists()) return@withLock null

        try {
            json.decodeFromString<StoredConversationSummary>(file.readText())
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getAllSummaries(): List<StoredConversationSummary> = mutex.withLock {
        File(summariesDirectory).listFiles { _, name -> name.endsWith(".json") }
            ?.mapNotNull { file ->
                try {
                    json.decodeFromString<StoredConversationSummary>(file.readText())
                } catch (e: Exception) {
                    null
                }
            }
            ?.sortedByDescending { it.createdAt }
            ?: emptyList()
    }

    suspend fun getSummariesForConversation(conversationId: String): List<StoredConversationSummary> {
        return getAllSummaries().filter { it.originalConversationId == conversationId }
    }

    suspend fun deleteSummary(id: String): Boolean = mutex.withLock {
        val file = File(summariesDirectory, "$id.json")
        file.delete()
    }

    private fun generateId(): String {
        return "conv_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }

    private fun generateSummaryId(): String {
        return "summary_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
}

// Extension functions for conversion
private fun Philosopher.toStored(): StoredPhilosopher {
    return StoredPhilosopher(
        id = id,
        name = name,
        description = description,
        era = era,
        nationality = nationality
    )
}

private fun PhilosopherContribution.toStored(): StoredContribution {
    return StoredContribution(
        philosopherId = philosopher.id,
        philosopherName = philosopher.name,
        response = response,
        timestamp = timestamp.toString(),
        roundNumber = roundNumber,
        wordCount = wordCount
    )
}

fun StoredPhilosopher.toPhilosopher(): Philosopher {
    return Philosopher(
        id = id,
        name = name,
        description = description,
        systemPrompt = "", // System prompt not stored
        era = era,
        nationality = nationality
    )
}