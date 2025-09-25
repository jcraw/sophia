package com.jcraw.sophia.discussion

import com.jcraw.llm.LLMClient
import com.jcraw.llm.LLMConfig
import com.jcraw.sophia.config.SummarizationPrompts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

class SummarizationEngine(
    private val llmClient: LLMClient
) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun summarizeConversation(
        originalConversation: ConversationState.Completed,
        config: SummarizationConfig
    ): ConversationSummary = withContext(Dispatchers.IO) {

        val conversationText = formatConversationForSummarization(originalConversation)
        val participantNames = originalConversation.config.participants.map { it.name }

        val prompt = SummarizationPrompts.buildSummarizationPrompt(
            originalTopic = originalConversation.config.topic,
            participants = participantNames,
            originalConversation = conversationText,
            targetRounds = config.targetRounds,
            maxWordsPerResponse = config.maxWordsPerResponse
        )

        val model = LLMConfig.summarizationModel

        val response = llmClient.chatCompletion(
            model = model,
            systemPrompt = SummarizationPrompts.systemPrompt,
            userContext = prompt,
            temperature = LLMConfig.SUMMARIZATION_TEMPERATURE,
            maxTokens = LLMConfig.SUMMARIZATION_MAX_TOKENS
        )

        val responseText = response.choices.firstOrNull()?.message?.content?.trim()
            ?: throw IllegalArgumentException("Empty or null response from LLM")

        parseSummarizationResponse(responseText, originalConversation.config.topic)
    }

    private fun formatConversationForSummarization(conversation: ConversationState.Completed): String {
        return buildString {
            conversation.rounds.forEach { round ->
                appendLine("\n--- Round ${round.roundNumber} ---")
                round.contributions.forEach { contribution ->
                    appendLine("${contribution.philosopher.name}: ${contribution.response}")
                }
            }
        }.trim()
    }

    private fun parseSummarizationResponse(
        response: String,
        originalTopic: String
    ): ConversationSummary {
        try {
            val jsonObject = json.parseToJsonElement(response).jsonObject
            val summaryObject = jsonObject["summary"]?.jsonObject
                ?: throw IllegalArgumentException("Missing 'summary' field")

            val condensedTopic = summaryObject["condensedTopic"]?.jsonPrimitive?.content
                ?: originalTopic

            val participantsArray = summaryObject["participants"]?.jsonArray
                ?: throw IllegalArgumentException("Missing 'participants' field")
            val participants = participantsArray.map { it.jsonPrimitive.content }

            val roundsArray = summaryObject["rounds"]?.jsonArray
                ?: throw IllegalArgumentException("Missing 'rounds' field")

            val rounds = roundsArray.mapIndexed { index, roundElement ->
                val roundObject = roundElement.jsonObject
                val contributionsArray = roundObject["contributions"]?.jsonArray
                    ?: throw IllegalArgumentException("Missing 'contributions' field in round ${index + 1}")

                val contributions = contributionsArray.map { contributionElement ->
                    val contributionObject = contributionElement.jsonObject
                    val philosopherName = contributionObject["philosopherName"]?.jsonPrimitive?.content
                        ?: throw IllegalArgumentException("Missing 'philosopherName' field")
                    val responseText = contributionObject["response"]?.jsonPrimitive?.content
                        ?: throw IllegalArgumentException("Missing 'response' field")
                    val wordCount = contributionObject["wordCount"]?.jsonPrimitive?.content?.toIntOrNull()
                        ?: responseText.split("\\s+".toRegex()).size

                    SummaryContribution(
                        philosopherName = philosopherName,
                        response = responseText,
                        wordCount = wordCount
                    )
                }

                SummaryRound(
                    roundNumber = index + 1,
                    contributions = contributions
                )
            }

            val videoNotes = summaryObject["videoNotes"]?.jsonPrimitive?.content
                ?: "Condensed philosophical discussion suitable for short-form video content"

            return ConversationSummary(
                originalTopic = originalTopic,
                condensedTopic = condensedTopic,
                participants = participants,
                rounds = rounds,
                videoNotes = videoNotes
            )

        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to parse summarization response: ${e.message}", e)
        }
    }
}