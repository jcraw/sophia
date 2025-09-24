package com.jcraw.sophia.database.repository

import com.jcraw.sophia.database.dao.ConversationDao
import com.jcraw.sophia.database.dao.ExposedConversationDao
import com.jcraw.sophia.database.entities.SavedConversation
import com.jcraw.sophia.database.entities.SavedPhilosopher
import com.jcraw.sophia.discussion.ConversationConfig
import com.jcraw.sophia.discussion.ConversationState
import com.jcraw.sophia.discussion.Philosopher
import com.jcraw.sophia.discussion.PhilosopherContribution
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.toKotlinInstant

interface ConversationRepository {
    suspend fun saveConversation(conversationState: ConversationState): Int?
    suspend fun saveContribution(conversationId: Int, contribution: PhilosopherContribution)
    suspend fun updateConversationComplete(conversationId: Int)
    suspend fun updateConversationError(conversationId: Int, errorMessage: String)
    suspend fun getConversation(conversationId: Int): SavedConversation?
    suspend fun getAllConversations(): List<SavedConversation>
    suspend fun getCompletedConversations(): List<SavedConversation>
    suspend fun getInProgressConversations(): List<SavedConversation>
    suspend fun deleteConversation(conversationId: Int): Boolean
}

class ConversationRepositoryImpl(
    private val dao: ConversationDao = ExposedConversationDao()
) : ConversationRepository {

    override suspend fun saveConversation(conversationState: ConversationState): Int? = withContext(Dispatchers.IO) {
        when (conversationState) {
            is ConversationState.InProgress -> {
                val participants = conversationState.config.participants.map { philosopher ->
                    SavedPhilosopher(
                        id = 0, // Will be assigned by database
                        externalId = philosopher.id,
                        name = philosopher.name,
                        description = philosopher.description,
                        era = philosopher.era,
                        nationality = philosopher.nationality
                    )
                }

                dao.saveConversation(
                    topic = conversationState.config.topic,
                    maxRounds = conversationState.config.maxRounds,
                    maxWordsPerResponse = conversationState.config.maxWordsPerResponse,
                    participants = participants,
                    status = "in_progress",
                    createdAt = Clock.System.now()
                )
            }
            is ConversationState.Completed -> {
                val participants = conversationState.config.participants.map { philosopher ->
                    SavedPhilosopher(
                        id = 0,
                        externalId = philosopher.id,
                        name = philosopher.name,
                        description = philosopher.description,
                        era = philosopher.era,
                        nationality = philosopher.nationality
                    )
                }

                val conversationId = dao.saveConversation(
                    topic = conversationState.config.topic,
                    maxRounds = conversationState.config.maxRounds,
                    maxWordsPerResponse = conversationState.config.maxWordsPerResponse,
                    participants = participants,
                    status = "completed",
                    createdAt = Clock.System.now()
                )

                // Save all contributions
                conversationState.finalContributions.forEach { contribution ->
                    val philosopherId = getPhilosopherId(conversationId, contribution.philosopher.id)
                    if (philosopherId != null) {
                        dao.savePhilosopherContribution(
                            conversationId = conversationId,
                            roundNumber = contribution.roundNumber,
                            philosopherId = philosopherId,
                            response = contribution.response,
                            timestamp = contribution.timestamp.toKotlinInstant(),
                            wordCount = contribution.wordCount
                        )
                    }
                }

                dao.updateConversationStatus(conversationId, "completed", Clock.System.now())
                conversationId
            }
            else -> null
        }
    }

    override suspend fun saveContribution(
        conversationId: Int,
        contribution: PhilosopherContribution
    ) = withContext(Dispatchers.IO) {
        val philosopherId = getPhilosopherId(conversationId, contribution.philosopher.id)
        if (philosopherId != null) {
            dao.savePhilosopherContribution(
                conversationId = conversationId,
                roundNumber = contribution.roundNumber,
                philosopherId = philosopherId,
                response = contribution.response,
                timestamp = contribution.timestamp.toKotlinInstant(),
                wordCount = contribution.wordCount
            )
        }
    }

    override suspend fun updateConversationComplete(conversationId: Int) = withContext(Dispatchers.IO) {
        dao.updateConversationStatus(
            conversationId = conversationId,
            status = "completed",
            completedAt = Clock.System.now()
        )
    }

    override suspend fun updateConversationError(
        conversationId: Int,
        errorMessage: String
    ) = withContext(Dispatchers.IO) {
        dao.updateConversationStatus(
            conversationId = conversationId,
            status = "error",
            completedAt = Clock.System.now(),
            errorMessage = errorMessage
        )
    }

    override suspend fun getConversation(conversationId: Int): SavedConversation? = withContext(Dispatchers.IO) {
        dao.getConversation(conversationId)
    }

    override suspend fun getAllConversations(): List<SavedConversation> = withContext(Dispatchers.IO) {
        dao.getAllConversations()
    }

    override suspend fun getCompletedConversations(): List<SavedConversation> = withContext(Dispatchers.IO) {
        dao.getConversationsByStatus("completed")
    }

    override suspend fun getInProgressConversations(): List<SavedConversation> = withContext(Dispatchers.IO) {
        dao.getConversationsByStatus("in_progress")
    }

    override suspend fun deleteConversation(conversationId: Int): Boolean = withContext(Dispatchers.IO) {
        dao.deleteConversation(conversationId)
    }

    private suspend fun getPhilosopherId(conversationId: Int, philosopherExternalId: String): Int? {
        val conversation = dao.getConversation(conversationId)
        return conversation?.participants?.find { it.externalId == philosopherExternalId }?.id
    }
}

// Extension functions to convert between domain models and database models
fun SavedConversation.toConversationConfig(): ConversationConfig {
    return ConversationConfig(
        topic = topic,
        participants = participants.map { it.toPhilosopher() },
        maxRounds = maxRounds,
        maxWordsPerResponse = maxWordsPerResponse
    )
}

fun SavedPhilosopher.toPhilosopher(): Philosopher {
    return Philosopher(
        id = externalId,
        name = name,
        description = description,
        systemPrompt = "", // System prompt not stored in database
        era = era,
        nationality = nationality
    )
}