package com.jcraw.sophia.database.entities

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object ConversationTable : IntIdTable("conversations") {
    val topic = varchar("topic", 500)
    val maxRounds = integer("max_rounds")
    val maxWordsPerResponse = integer("max_words_per_response")
    val status = varchar("status", 50) // "in_progress", "completed", "error"
    val createdAt = timestamp("created_at")
    val completedAt = timestamp("completed_at").nullable()
    val errorMessage = text("error_message").nullable()
}

object PhilosopherTable : IntIdTable("philosophers") {
    val externalId = varchar("external_id", 100).uniqueIndex()
    val name = varchar("name", 100)
    val description = text("description")
    val era = varchar("era", 100)
    val nationality = varchar("nationality", 100)
}

object ConversationParticipantTable : IntIdTable("conversation_participants") {
    val conversationId = reference("conversation_id", ConversationTable)
    val philosopherId = reference("philosopher_id", PhilosopherTable)
    val participantOrder = integer("participant_order")
}

object ConversationRoundTable : IntIdTable("conversation_rounds") {
    val conversationId = reference("conversation_id", ConversationTable)
    val roundNumber = integer("round_number")
    val isComplete = bool("is_complete").default(false)
}

object PhilosopherContributionTable : IntIdTable("philosopher_contributions") {
    val conversationId = reference("conversation_id", ConversationTable)
    val roundId = reference("round_id", ConversationRoundTable)
    val philosopherId = reference("philosopher_id", PhilosopherTable)
    val response = text("response")
    val timestamp = timestamp("timestamp")
    val roundNumber = integer("round_number")
    val wordCount = integer("word_count")
}

@Serializable
data class SavedConversation(
    val id: Int,
    val topic: String,
    val maxRounds: Int,
    val maxWordsPerResponse: Int,
    val status: String,
    val createdAt: Instant,
    val completedAt: Instant?,
    val errorMessage: String?,
    val participants: List<SavedPhilosopher>,
    val rounds: List<SavedConversationRound>
)

@Serializable
data class SavedPhilosopher(
    val id: Int,
    val externalId: String,
    val name: String,
    val description: String,
    val era: String,
    val nationality: String
)

@Serializable
data class SavedConversationRound(
    val id: Int,
    val roundNumber: Int,
    val isComplete: Boolean,
    val contributions: List<SavedPhilosopherContribution>
)

@Serializable
data class SavedPhilosopherContribution(
    val id: Int,
    val philosopherId: Int,
    val philosopherName: String,
    val response: String,
    val timestamp: Instant,
    val roundNumber: Int,
    val wordCount: Int
)