package com.jcraw.sophia.database.dao

import com.jcraw.sophia.database.entities.*
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

interface ConversationDao {
    suspend fun saveConversation(
        topic: String,
        maxRounds: Int,
        maxWordsPerResponse: Int,
        participants: List<SavedPhilosopher>,
        status: String = "in_progress",
        createdAt: Instant = kotlinx.datetime.Clock.System.now()
    ): Int

    suspend fun updateConversationStatus(
        conversationId: Int,
        status: String,
        completedAt: Instant? = null,
        errorMessage: String? = null
    )

    suspend fun savePhilosopherContribution(
        conversationId: Int,
        roundNumber: Int,
        philosopherId: Int,
        response: String,
        timestamp: Instant,
        wordCount: Int
    ): Int

    suspend fun getConversation(conversationId: Int): SavedConversation?

    suspend fun getAllConversations(): List<SavedConversation>

    suspend fun getConversationsByStatus(status: String): List<SavedConversation>

    suspend fun deleteConversation(conversationId: Int): Boolean
}

class ExposedConversationDao : ConversationDao {

    override suspend fun saveConversation(
        topic: String,
        maxRounds: Int,
        maxWordsPerResponse: Int,
        participants: List<SavedPhilosopher>,
        status: String,
        createdAt: Instant
    ): Int = transaction {
        // Insert conversation
        val conversationId = ConversationTable.insertAndGetId {
            it[ConversationTable.topic] = topic
            it[ConversationTable.maxRounds] = maxRounds
            it[ConversationTable.maxWordsPerResponse] = maxWordsPerResponse
            it[ConversationTable.status] = status
            it[ConversationTable.createdAt] = createdAt
        }.value

        // Ensure philosophers exist and get their IDs
        val philosopherIds = participants.mapIndexed { index, philosopher ->
            val existingId = PhilosopherTable.select {
                PhilosopherTable.externalId eq philosopher.externalId
            }.singleOrNull()?.get(PhilosopherTable.id)?.value

            val philosopherId = existingId ?: PhilosopherTable.insertAndGetId {
                it[externalId] = philosopher.externalId
                it[name] = philosopher.name
                it[description] = philosopher.description
                it[era] = philosopher.era
                it[nationality] = philosopher.nationality
            }.value

            // Link philosopher to conversation
            ConversationParticipantTable.insert {
                it[ConversationParticipantTable.conversationId] = conversationId
                it[ConversationParticipantTable.philosopherId] = philosopherId
                it[participantOrder] = index
            }

            philosopherId
        }

        conversationId
    }

    override suspend fun updateConversationStatus(
        conversationId: Int,
        status: String,
        completedAt: Instant?,
        errorMessage: String?
    ): Unit = transaction {
        ConversationTable.update({ ConversationTable.id eq conversationId }) {
            it[ConversationTable.status] = status
            if (completedAt != null) {
                it[ConversationTable.completedAt] = completedAt
            }
            if (errorMessage != null) {
                it[ConversationTable.errorMessage] = errorMessage
            }
        }
    }

    override suspend fun savePhilosopherContribution(
        conversationId: Int,
        roundNumber: Int,
        philosopherId: Int,
        response: String,
        timestamp: Instant,
        wordCount: Int
    ): Int = transaction {
        // Ensure round exists
        val roundId = ConversationRoundTable.select {
            (ConversationRoundTable.conversationId eq conversationId) and
            (ConversationRoundTable.roundNumber eq roundNumber)
        }.singleOrNull()?.get(ConversationRoundTable.id)?.value
            ?: ConversationRoundTable.insertAndGetId {
                it[ConversationRoundTable.conversationId] = conversationId
                it[ConversationRoundTable.roundNumber] = roundNumber
            }.value

        // Insert contribution
        PhilosopherContributionTable.insertAndGetId {
            it[PhilosopherContributionTable.conversationId] = conversationId
            it[PhilosopherContributionTable.roundId] = roundId
            it[PhilosopherContributionTable.philosopherId] = philosopherId
            it[PhilosopherContributionTable.response] = response
            it[PhilosopherContributionTable.timestamp] = timestamp
            it[PhilosopherContributionTable.roundNumber] = roundNumber
            it[PhilosopherContributionTable.wordCount] = wordCount
        }.value
    }

    override suspend fun getConversation(conversationId: Int): SavedConversation? = transaction {
        val conversationRow = ConversationTable.select {
            ConversationTable.id eq conversationId
        }.singleOrNull() ?: return@transaction null

        val participants = getConversationParticipants(conversationId)
        val rounds = getConversationRounds(conversationId)

        SavedConversation(
            id = conversationId,
            topic = conversationRow[ConversationTable.topic],
            maxRounds = conversationRow[ConversationTable.maxRounds],
            maxWordsPerResponse = conversationRow[ConversationTable.maxWordsPerResponse],
            status = conversationRow[ConversationTable.status],
            createdAt = conversationRow[ConversationTable.createdAt],
            completedAt = conversationRow[ConversationTable.completedAt],
            errorMessage = conversationRow[ConversationTable.errorMessage],
            participants = participants,
            rounds = rounds
        )
    }

    override suspend fun getAllConversations(): List<SavedConversation> = transaction {
        ConversationTable.selectAll().map { row ->
            val conversationId = row[ConversationTable.id].value
            SavedConversation(
                id = conversationId,
                topic = row[ConversationTable.topic],
                maxRounds = row[ConversationTable.maxRounds],
                maxWordsPerResponse = row[ConversationTable.maxWordsPerResponse],
                status = row[ConversationTable.status],
                createdAt = row[ConversationTable.createdAt],
                completedAt = row[ConversationTable.completedAt],
                errorMessage = row[ConversationTable.errorMessage],
                participants = getConversationParticipants(conversationId),
                rounds = getConversationRounds(conversationId)
            )
        }
    }

    override suspend fun getConversationsByStatus(status: String): List<SavedConversation> = transaction {
        ConversationTable.select { ConversationTable.status eq status }.map { row ->
            val conversationId = row[ConversationTable.id].value
            SavedConversation(
                id = conversationId,
                topic = row[ConversationTable.topic],
                maxRounds = row[ConversationTable.maxRounds],
                maxWordsPerResponse = row[ConversationTable.maxWordsPerResponse],
                status = row[ConversationTable.status],
                createdAt = row[ConversationTable.createdAt],
                completedAt = row[ConversationTable.completedAt],
                errorMessage = row[ConversationTable.errorMessage],
                participants = getConversationParticipants(conversationId),
                rounds = getConversationRounds(conversationId)
            )
        }
    }

    override suspend fun deleteConversation(conversationId: Int): Boolean = transaction {
        // Delete in reverse dependency order
        PhilosopherContributionTable.deleteWhere {
            PhilosopherContributionTable.conversationId eq conversationId
        }
        ConversationRoundTable.deleteWhere {
            ConversationRoundTable.conversationId eq conversationId
        }
        ConversationParticipantTable.deleteWhere {
            ConversationParticipantTable.conversationId eq conversationId
        }
        val deleted = ConversationTable.deleteWhere {
            ConversationTable.id eq conversationId
        }
        deleted > 0
    }

    private fun getConversationParticipants(conversationId: Int): List<SavedPhilosopher> {
        return (ConversationParticipantTable innerJoin PhilosopherTable)
            .select { ConversationParticipantTable.conversationId eq conversationId }
            .orderBy(ConversationParticipantTable.participantOrder)
            .map { row ->
                SavedPhilosopher(
                    id = row[PhilosopherTable.id].value,
                    externalId = row[PhilosopherTable.externalId],
                    name = row[PhilosopherTable.name],
                    description = row[PhilosopherTable.description],
                    era = row[PhilosopherTable.era],
                    nationality = row[PhilosopherTable.nationality]
                )
            }
    }

    private fun getConversationRounds(conversationId: Int): List<SavedConversationRound> {
        return ConversationRoundTable.select {
            ConversationRoundTable.conversationId eq conversationId
        }.orderBy(ConversationRoundTable.roundNumber).map { roundRow ->
            val roundId = roundRow[ConversationRoundTable.id].value
            val contributions = getContributionsForRound(roundId)

            SavedConversationRound(
                id = roundId,
                roundNumber = roundRow[ConversationRoundTable.roundNumber],
                isComplete = roundRow[ConversationRoundTable.isComplete],
                contributions = contributions
            )
        }
    }

    private fun getContributionsForRound(roundId: Int): List<SavedPhilosopherContribution> {
        return (PhilosopherContributionTable innerJoin PhilosopherTable)
            .select { PhilosopherContributionTable.roundId eq roundId }
            .orderBy(PhilosopherContributionTable.timestamp)
            .map { row ->
                SavedPhilosopherContribution(
                    id = row[PhilosopherContributionTable.id].value,
                    philosopherId = row[PhilosopherTable.id].value,
                    philosopherName = row[PhilosopherTable.name],
                    response = row[PhilosopherContributionTable.response],
                    timestamp = row[PhilosopherContributionTable.timestamp],
                    roundNumber = row[PhilosopherContributionTable.roundNumber],
                    wordCount = row[PhilosopherContributionTable.wordCount]
                )
            }
    }
}