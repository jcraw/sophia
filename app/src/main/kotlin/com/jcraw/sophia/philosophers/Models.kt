package com.jcraw.sophia.philosophers

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant

data class Philosopher(
    val id: String,
    val name: String,
    val description: String,
    val systemPrompt: String,
    val era: String = "",
    val nationality: String = ""
)

data class PhilosopherContribution(
    val philosopher: Philosopher,
    val response: String,
    val timestamp: Instant = Instant.now(),
    val roundNumber: Int,
    val wordCount: Int = response.split("\\s+".toRegex()).size
)

data class ConversationRound(
    val roundNumber: Int,
    val contributions: List<PhilosopherContribution> = emptyList(),
    val isComplete: Boolean = false
) {
    fun addContribution(contribution: PhilosopherContribution): ConversationRound {
        return copy(contributions = contributions + contribution)
    }
}

data class ConversationConfig(
    val topic: String,
    val participants: List<Philosopher>,
    val maxRounds: Int = 3,
    val maxWordsPerResponse: Int = 150
) {
    init {
        require(participants.isNotEmpty()) { "At least one philosopher must participate" }
        require(maxRounds > 0) { "Must have at least one round" }
        require(topic.isNotBlank()) { "Topic cannot be blank" }
    }
}

sealed class ConversationState {
    data object NotStarted : ConversationState()
    data class InProgress(
        val config: ConversationConfig,
        val rounds: List<ConversationRound> = emptyList(),
        val currentRound: Int = 1,
        val currentPhilosopherIndex: Int = 0
    ) : ConversationState() {

        val currentPhilosopher: Philosopher?
            get() = if (currentPhilosopherIndex < config.participants.size) {
                config.participants[currentPhilosopherIndex]
            } else null

        val isComplete: Boolean
            get() = currentRound > config.maxRounds

        fun getCurrentRound(): ConversationRound? {
            return rounds.find { it.roundNumber == currentRound }
        }

        fun getAllContributions(): List<PhilosopherContribution> {
            return rounds.flatMap { it.contributions }.sortedBy { it.timestamp }
        }
    }

    data class Completed(
        val config: ConversationConfig,
        val rounds: List<ConversationRound>,
        val finalContributions: List<PhilosopherContribution>
    ) : ConversationState()

    data class Error(val message: String, val cause: Throwable? = null) : ConversationState()
}

class ConversationStateManager {
    private val _state = MutableStateFlow<ConversationState>(ConversationState.NotStarted)
    val state: StateFlow<ConversationState> = _state.asStateFlow()

    fun startConversation(config: ConversationConfig) {
        _state.value = ConversationState.InProgress(
            config = config,
            rounds = listOf(ConversationRound(roundNumber = 1))
        )
    }

    fun addContribution(contribution: PhilosopherContribution) {
        val currentState = _state.value
        if (currentState !is ConversationState.InProgress) return

        val updatedRounds = currentState.rounds.map { round ->
            if (round.roundNumber == contribution.roundNumber) {
                round.addContribution(contribution)
            } else round
        }

        val nextPhilosopherIndex = currentState.currentPhilosopherIndex + 1
        val shouldAdvanceRound = nextPhilosopherIndex >= currentState.config.participants.size

        if (shouldAdvanceRound) {
            val nextRoundNumber = currentState.currentRound + 1
            if (nextRoundNumber > currentState.config.maxRounds) {
                // Conversation completed
                _state.value = ConversationState.Completed(
                    config = currentState.config,
                    rounds = updatedRounds,
                    finalContributions = updatedRounds.flatMap { it.contributions }
                )
            } else {
                // Start next round
                _state.value = currentState.copy(
                    rounds = updatedRounds + ConversationRound(roundNumber = nextRoundNumber),
                    currentRound = nextRoundNumber,
                    currentPhilosopherIndex = 0
                )
            }
        } else {
            // Continue current round with next philosopher
            _state.value = currentState.copy(
                rounds = updatedRounds,
                currentPhilosopherIndex = nextPhilosopherIndex
            )
        }
    }

    fun setError(message: String, cause: Throwable? = null) {
        _state.value = ConversationState.Error(message, cause)
    }

    fun reset() {
        _state.value = ConversationState.NotStarted
    }
}