package com.jcraw.sophia.discussion

import com.jcraw.llm.LLMClient
import com.jcraw.llm.LLMConfig
import com.jcraw.sophia.config.ConversationPrompts
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.delay
import java.time.Instant

class ConversationEngine(
    private val llmClient: LLMClient,
    private val stateManager: ConversationStateManager
) {
    val state: StateFlow<ConversationState> = stateManager.state

    suspend fun startConversation(config: ConversationConfig) {
        try {
            println("🚀 Starting conversation with ${config.participants.size} philosophers on topic: '${config.topic}'")
            stateManager.startConversation(config)
            processNextContribution()
        } catch (e: Exception) {
            println("❌ Failed to start conversation: ${e.message}")
            e.printStackTrace()
            stateManager.setError("Failed to start conversation", e)
        }
    }

    suspend fun processNextContribution() {
        val currentState = state.value
        if (currentState !is ConversationState.InProgress) {
            println("⚠️ processNextContribution called but state is not InProgress: ${currentState::class.simpleName}")
            return
        }

        val philosopher = currentState.currentPhilosopher
        if (philosopher == null) {
            println("⚠️ No current philosopher available")
            return
        }

        println("💭 Generating response for ${philosopher.name} (Round ${currentState.currentRound})")

        try {
            val context = buildContextForPhilosopher(philosopher, currentState)
            val prompt = buildPromptForPhilosopher(philosopher, currentState.config.topic, context)

            println("📝 System prompt length: ${philosopher.systemPrompt.length} chars")
            println("📝 User context length: ${prompt.length} chars")
            val model = LLMConfig.defaultPhilosophicalModel
            println("🎯 Using model: ${model.modelId}")

            val response = llmClient.chatCompletion(
                model = model,
                systemPrompt = philosopher.systemPrompt,
                userContext = prompt,
                maxTokens = if (model.modelId.startsWith("gpt-5"))
                    LLMConfig.DEFAULT_MAX_TOKENS
                else
                    currentState.config.maxWordsPerResponse * 2, // Rough conversion for GPT-4
                temperature = LLMConfig.CREATIVE_TEMPERATURE
            )

            println("✅ Received LLM response for ${philosopher.name}")
            val responseText = response.choices.firstOrNull()?.message?.content?.trim()

            if (responseText.isNullOrBlank()) {
                throw RuntimeException("Empty or null response from LLM. Choices: ${response.choices.size}")
            }

            println("📜 ${philosopher.name} response: ${responseText.take(100)}...")

            val contribution = PhilosopherContribution(
                philosopher = philosopher,
                response = responseText,
                timestamp = Instant.now(),
                roundNumber = currentState.currentRound
            )

            stateManager.addContribution(contribution)
            println("✅ Added contribution for ${philosopher.name}")

            // Small delay to make conversation feel more natural
            delay(500)

            // Continue with next philosopher if conversation isn't complete
            val newState = state.value
            if (newState is ConversationState.InProgress && !newState.isComplete) {
                println("⏭️ Continuing to next philosopher...")
                processNextContribution()
            } else {
                println("🎉 Conversation complete or stopped")
            }

        } catch (e: Exception) {
            println("❌ Failed to generate response for ${philosopher.name}: ${e.message}")
            e.printStackTrace()
            stateManager.setError("Failed to generate response for ${philosopher.name}", e)
        }
    }

    private fun buildContextForPhilosopher(
        philosopher: Philosopher,
        state: ConversationState.InProgress
    ): String {
        val allContributions = state.getAllContributions()

        if (allContributions.isEmpty()) {
            return "This is the start of a philosophical discussion on the topic: \"${state.config.topic}\""
        }

        val context = StringBuilder()
        context.appendLine("Previous contributions to this philosophical discussion on \"${state.config.topic}\":")
        context.appendLine()

        // Group contributions by round for clarity
        allContributions.groupBy { it.roundNumber }.forEach { (round, contributions) ->
            context.appendLine("=== Round $round ===")
            contributions.forEach { contribution ->
                context.appendLine("${contribution.philosopher.name}: ${contribution.response}")
                context.appendLine()
            }
        }

        if (state.currentRound > 1) {
            context.appendLine("Now beginning Round ${state.currentRound}.")
        }

        return context.toString()
    }

    private fun buildPromptForPhilosopher(
        philosopher: Philosopher,
        topic: String,
        context: String
    ): String {
        return if (context.contains("Previous contributions")) {
            ConversationPrompts.buildFollowUpPrompt(philosopher.name, topic, context)
        } else {
            ConversationPrompts.buildInitialPrompt(philosopher.name, topic)
        }
    }

    fun reset() {
        stateManager.reset()
    }
}