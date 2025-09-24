package com.jcraw.sophia.discussion

import com.jcraw.llm.LLMClient
import com.jcraw.sophia.config.DirectorPrompts
import com.jcraw.sophia.config.LLMBridge
import com.jcraw.sophia.config.LLMConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

class DirectorEngine(
    private val llmClient: LLMClient
) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun createVideoScript(
        summary: ConversationSummary,
        config: DirectorConfig
    ): VideoScript = withContext(Dispatchers.IO) {

        val summaryText = formatSummaryForDirector(summary)

        val prompt = DirectorPrompts.buildScriptCreationPrompt(
            summary = summaryText,
            estimatedDuration = "60 seconds"
        )

        val model = LLMBridge.toOpenAIModel(LLMConfig.directorModel)

        val response = llmClient.chatCompletion(
            model = model,
            systemPrompt = DirectorPrompts.systemPrompt,
            userContext = prompt,
            temperature = LLMConfig.DIRECTOR_TEMPERATURE,
            maxTokens = LLMConfig.SUMMARIZATION_MAX_TOKENS * 2 // More tokens needed for scene descriptions
        )

        val responseText = response.choices.firstOrNull()?.message?.content?.trim()
            ?: throw IllegalArgumentException("Empty or null response from LLM")

        parseVideoScriptResponse(responseText, summary)
    }

    private fun formatSummaryForDirector(summary: ConversationSummary): String {
        return buildString {
            appendLine("PHILOSOPHICAL DISCUSSION SUMMARY")
            appendLine("Original Topic: ${summary.originalTopic}")
            appendLine("Condensed Topic: ${summary.condensedTopic}")
            appendLine("Participants: ${summary.participants.joinToString(", ")}")
            appendLine("Total Word Count: ${summary.totalWordCount}")
            appendLine()
            appendLine("Video Notes: ${summary.videoNotes}")
            appendLine()

            summary.rounds.forEach { round ->
                appendLine("--- Round ${round.roundNumber} (${round.wordCount} words) ---")
                round.contributions.forEach { contribution ->
                    appendLine("${contribution.philosopherName}: ${contribution.response}")
                }
                appendLine()
            }
        }.trim()
    }

    private fun parseVideoScriptResponse(
        response: String,
        originalSummary: ConversationSummary
    ): VideoScript {
        try {
            val jsonObject = json.parseToJsonElement(response).jsonObject
            val scriptObject = jsonObject["videoScript"]?.jsonObject
                ?: throw IllegalArgumentException("Missing 'videoScript' field")

            val title = scriptObject["title"]?.jsonPrimitive?.content
                ?: "Philosophical Discussion: ${originalSummary.condensedTopic}"

            val description = scriptObject["description"]?.jsonPrimitive?.content
                ?: "A philosophical discussion between ${originalSummary.participants.joinToString(", ")}"

            val estimatedDuration = scriptObject["estimatedDuration"]?.jsonPrimitive?.content
                ?: "60 seconds"

            val scenesArray = scriptObject["scenes"]?.jsonArray
                ?: throw IllegalArgumentException("Missing 'scenes' field")

            val scenes = scenesArray.mapIndexed { index, sceneElement ->
                val sceneObject = sceneElement.jsonObject

                val sceneNumber = sceneObject["sceneNumber"]?.jsonPrimitive?.int
                    ?: (index + 1)

                val typeString = sceneObject["type"]?.jsonPrimitive?.content
                    ?: "DIALOGUE"
                val type = parseSceneType(typeString)

                val duration = sceneObject["duration"]?.jsonPrimitive?.content
                    ?: "10 seconds"

                val imagePrompt = sceneObject["imagePrompt"]?.jsonPrimitive?.content
                    ?: throw IllegalArgumentException("Missing 'imagePrompt' field in scene $sceneNumber")

                val dialogue = sceneObject["dialogue"]?.jsonPrimitive?.contentOrNull

                val philosopherName = sceneObject["philosopherName"]?.jsonPrimitive?.contentOrNull

                val directorNotes = sceneObject["directorNotes"]?.jsonPrimitive?.contentOrNull

                Scene(
                    sceneNumber = sceneNumber,
                    type = type,
                    duration = duration,
                    imagePrompt = imagePrompt,
                    dialogue = dialogue,
                    philosopherName = philosopherName,
                    directorNotes = directorNotes
                )
            }

            val productionNotesArray = scriptObject["productionNotes"]?.jsonArray
            val productionNotes = productionNotesArray?.map { it.jsonPrimitive.content } ?: emptyList()

            return VideoScript(
                title = title,
                description = description,
                estimatedDuration = estimatedDuration,
                scenes = scenes,
                productionNotes = productionNotes
            )

        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to parse video script response: ${e.message}", e)
        }
    }

    private fun parseSceneType(typeString: String): SceneType {
        return when (typeString.uppercase()) {
            "OPENING" -> SceneType.OPENING
            "DIALOGUE" -> SceneType.DIALOGUE
            "TRANSITION" -> SceneType.TRANSITION
            "CLOSING" -> SceneType.CLOSING
            else -> SceneType.DIALOGUE
        }
    }
}