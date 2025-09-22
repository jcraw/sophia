package com.jcraw.sophia

import com.jcraw.llm.LLMClient
import com.jcraw.llm.OpenAIClient
import com.jcraw.llm.OpenAIModel
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIf
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LLMIntegrationTest {

    companion object {
        @JvmStatic
        fun hasApiKey(): Boolean {
            val apiKey = System.getenv("OPENAI_API_KEY")
            println("🔍 Debug: OPENAI_API_KEY = '${apiKey?.take(10)}...' (length: ${apiKey?.length})")
            return !apiKey.isNullOrBlank()
        }
    }

    @Test
    @EnabledIf("hasApiKey")
    fun `test real LLM integration with GPT4_1Nano`() = runBlocking {
        // Arrange
        val apiKey = System.getenv("OPENAI_API_KEY")
        val llmClient: LLMClient = OpenAIClient(apiKey)

        val systemPrompt = """
            You are a helpful assistant. Keep your responses concise and direct.
        """.trimIndent()

        val userPrompt = "What is the capital of France? Answer in one word."

        println("🧪 Testing LLM integration...")
        println("🔑 API Key: ${apiKey.take(10)}...")
        println("🎯 Model: ${OpenAIModel.GPT4_1Nano.modelId}")
        println("📝 System prompt: $systemPrompt")
        println("❓ User prompt: $userPrompt")

        try {
            // Act
            val response = llmClient.chatCompletion(
                model = OpenAIModel.GPT4_1Nano,
                systemPrompt = systemPrompt,
                userContext = userPrompt,
                maxTokens = 50,
                temperature = 0.1
            )

            // Assert
            assertNotNull(response, "Response should not be null")
            assertTrue(response.choices.isNotEmpty(), "Should have at least one choice")

            val responseText = response.choices.first().message.content.trim()
            assertNotNull(responseText, "Response text should not be null")
            assertTrue(responseText.isNotBlank(), "Response text should not be blank")

            println("✅ LLM Response: '$responseText'")
            println("📊 Usage: ${response.usage.totalTokens} tokens (${response.usage.promptTokens}+${response.usage.completionTokens})")
            println("💰 Cost: $${String.format("%.4f", OpenAIModel.GPT4_1Nano.calculateCost(response.usage))}")

            // Basic validation that it's a reasonable response
            assertTrue(responseText.length > 0, "Response should have content")
            assertTrue(responseText.length < 100, "Response should be concise")

            println("🎉 LLM integration test passed!")

        } catch (e: Exception) {
            println("❌ LLM integration test failed: ${e.message}")
            e.printStackTrace()
            throw e
        } finally {
            llmClient.close()
        }
    }

    @Test
    @EnabledIf("hasApiKey")
    fun `test philosopher conversation flow`() = runBlocking {
        // Arrange
        val apiKey = System.getenv("OPENAI_API_KEY")
        val llmClient: LLMClient = OpenAIClient(apiKey)

        val socraticPrompt = """
            You are Socrates, the ancient Greek philosopher. You are known for asking probing questions
            to examine assumptions and arrive at truth. Keep your response to exactly one sentence.
        """.trimIndent()

        val topic = "What is wisdom?"

        println("🧪 Testing philosopher conversation flow...")
        println("🏛️ Philosopher: Socrates")
        println("💭 Topic: $topic")

        try {
            // Act
            val response = llmClient.chatCompletion(
                model = OpenAIModel.GPT4_1Nano,
                systemPrompt = socraticPrompt,
                userContext = "Topic for discussion: \"$topic\"\n\nPlease provide your initial thoughts on this topic as Socrates. Share your philosophical perspective and approach to this question. Keep your response concise but substantive (around 50 words).",
                maxTokens = 150,
                temperature = 0.8
            )

            // Assert
            assertNotNull(response, "Response should not be null")
            assertTrue(response.choices.isNotEmpty(), "Should have at least one choice")

            val responseText = response.choices.first().message.content.trim()
            assertNotNull(responseText, "Response text should not be null")
            assertTrue(responseText.isNotBlank(), "Response text should not be blank")

            println("🗣️ Socrates says: '$responseText'")
            println("📊 Usage: ${response.usage.totalTokens} tokens")

            // Validate it sounds like a philosophical response
            assertTrue(responseText.length > 10, "Response should be substantive")
            assertTrue(responseText.length < 500, "Response should be reasonably concise")

            println("🎉 Philosopher conversation test passed!")

        } catch (e: Exception) {
            println("❌ Philosopher conversation test failed: ${e.message}")
            e.printStackTrace()
            throw e
        } finally {
            llmClient.close()
        }
    }
}