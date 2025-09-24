package com.jcraw.sophia.discussion

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

data class SummarizationConfig(
    val targetRounds: Int = 3,
    val maxWordsPerResponse: Int = 50,
    val preserveOriginalParticipants: Boolean = true
) {
    init {
        require(targetRounds > 0) { "Must have at least one round in summary" }
        require(maxWordsPerResponse > 0) { "Must allow at least one word per response" }
    }
}

data class ConversationSummary(
    val originalTopic: String,
    val condensedTopic: String,
    val participants: List<String>,
    val rounds: List<SummaryRound>,
    val videoNotes: String,
    val createdAt: Instant = Instant.now()
) {
    val totalWordCount: Int
        get() = rounds.flatMap { it.contributions }.sumOf { it.wordCount }

    fun toConversationConfig(philosophers: List<Philosopher>): ConversationConfig {
        val selectedPhilosophers = participants.mapNotNull { name ->
            philosophers.find { it.name.equals(name, ignoreCase = true) }
        }
        return ConversationConfig(
            topic = condensedTopic,
            participants = selectedPhilosophers,
            maxRounds = rounds.size,
            maxWordsPerResponse = rounds.flatMap { it.contributions }.maxOfOrNull { it.wordCount } ?: 50
        )
    }
}

data class DirectorConfig(
    val includeOpeningShot: Boolean = true,
    val includeClosingShot: Boolean = true,
    val sceneTransitionStyle: String = "philosophical_atmosphere"
)

data class VideoScript(
    val title: String,
    val description: String,
    val estimatedDuration: String,
    val scenes: List<Scene>,
    val productionNotes: List<String>,
    val createdAt: Instant = Instant.now()
) {
    val totalScenes: Int
        get() = scenes.size

    val hasDialogueScenes: Boolean
        get() = scenes.any { it.type == SceneType.DIALOGUE }
}

data class Scene(
    val sceneNumber: Int,
    val type: SceneType,
    val duration: String,
    val imagePrompt: String,
    val dialogue: String? = null,
    val philosopherName: String? = null,
    val directorNotes: String? = null
) {
    val isDialogueScene: Boolean
        get() = type == SceneType.DIALOGUE && philosopherName != null
}

sealed class SceneType {
    data object OPENING : SceneType()
    data object DIALOGUE : SceneType()
    data object TRANSITION : SceneType()
    data object CLOSING : SceneType()
}

data class SummaryRound(
    val roundNumber: Int,
    val contributions: List<SummaryContribution>
) {
    val wordCount: Int
        get() = contributions.sumOf { it.wordCount }
}

data class SummaryContribution(
    val philosopherName: String,
    val response: String,
    val wordCount: Int
)

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

    data class Summarizing(
        val originalConversation: Completed,
        val config: SummarizationConfig
    ) : ConversationState()

    data class SummarizationComplete(
        val originalConversation: Completed,
        val summary: ConversationSummary
    ) : ConversationState()

    data class CreatingVideoScript(
        val summary: ConversationSummary,
        val config: DirectorConfig
    ) : ConversationState()

    data class VideoScriptComplete(
        val summary: ConversationSummary,
        val videoScript: VideoScript
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

    fun startSummarization(config: SummarizationConfig) {
        val currentState = _state.value
        if (currentState !is ConversationState.Completed) {
            setError("Can only summarize completed conversations")
            return
        }
        _state.value = ConversationState.Summarizing(currentState, config)
    }

    fun completeSummarization(summary: ConversationSummary) {
        val currentState = _state.value
        if (currentState !is ConversationState.Summarizing) {
            setError("Not currently summarizing")
            return
        }
        _state.value = ConversationState.SummarizationComplete(
            originalConversation = currentState.originalConversation,
            summary = summary
        )
    }

    fun startVideoScriptCreation(config: DirectorConfig) {
        val currentState = _state.value
        if (currentState !is ConversationState.SummarizationComplete) {
            setError("Can only create video scripts from completed summaries")
            return
        }
        _state.value = ConversationState.CreatingVideoScript(currentState.summary, config)
    }

    fun completeVideoScript(videoScript: VideoScript) {
        val currentState = _state.value
        if (currentState !is ConversationState.CreatingVideoScript) {
            setError("Not currently creating video script")
            return
        }
        _state.value = ConversationState.VideoScriptComplete(
            summary = currentState.summary,
            videoScript = videoScript
        )
    }

    fun reset() {
        _state.value = ConversationState.NotStarted
    }
}