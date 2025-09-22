package com.jcraw.llm

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

@Serializable
data class OpenAIMessage(
    val role: String,
    val content: String
)

@Serializable
data class OpenAIRequest(
    val model: String,
    val messages: List<OpenAIMessage>,
    @SerialName("max_tokens") val maxTokens: Int = 1000,
    val temperature: Double = 0.7
)

@Serializable
data class OpenAIChoice(
    val message: OpenAIMessage,
    @SerialName("finish_reason") val finishReason: String
)

@Serializable
data class OpenAIUsage(
    @SerialName("prompt_tokens") val promptTokens: Int,
    @SerialName("completion_tokens") val completionTokens: Int,
    @SerialName("total_tokens") val totalTokens: Int
)

@Serializable
data class OpenAIResponse(
    val id: String,
    val `object`: String,
    val created: Long,
    val model: String,
    val choices: List<OpenAIChoice>,
    val usage: OpenAIUsage
)

class OpenAIClient(private val apiKey: String) : LLMClient {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 120_000L  // 2 minutes
            connectTimeoutMillis = 30_000L   // 30 seconds
            socketTimeoutMillis = 120_000L   // 2 minutes
        }
    }

    override suspend fun chatCompletion(
        model: OpenAIModel,
        systemPrompt: String,
        userContext: String,
        maxTokens: Int,
        temperature: Double
    ): OpenAIResponse {
        println("🌐 OpenAI API call starting...")
        println("   Model: ${model.modelId}")
        println("   Max tokens: $maxTokens")
        println("   Temperature: $temperature")
        println("   System prompt: ${systemPrompt.take(100)}...")
        println("   User context: ${userContext.take(100)}...")

        val messages = listOf(
            OpenAIMessage("system", systemPrompt),
            OpenAIMessage("user", userContext)
        )

        val request = OpenAIRequest(
            model = model.modelId,
            messages = messages,
            maxTokens = maxTokens,
            temperature = temperature
        )

        try {
            val httpResponse = client.post("https://api.openai.com/v1/chat/completions") {
                header("Authorization", "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            println("📡 HTTP Status: ${httpResponse.status}")

            if (httpResponse.status.value !in 200..299) {
                val errorBody = httpResponse.bodyAsText()
                println("❌ Error Response Body: $errorBody")
                throw RuntimeException("OpenAI API error: ${httpResponse.status} - $errorBody")
            }

            val response = httpResponse.body<OpenAIResponse>()

            val cost = model.calculateCost(response.usage)
            println("🔥 LLM API: ${model.modelId} | tokens=${response.usage.totalTokens} (${response.usage.promptTokens}+${response.usage.completionTokens}) | cost=$%.4f".format(cost))
            println("✅ OpenAI API call successful, received ${response.choices.size} choices")

            return response
        } catch (e: Exception) {
            println("❌ OpenAI API call failed: ${e.message}")
            println("   Exception type: ${e::class.simpleName}")
            e.printStackTrace()
            throw e
        }
    }

    override fun close() {
        client.close()
    }
}