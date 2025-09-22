package com.jcraw.sophia

import com.jcraw.llm.LLMClient
import com.jcraw.llm.OpenAIClient
import com.jcraw.llm.OpenAIModel
import com.jcraw.sophia.philosophers.*
import com.jcraw.sophia.service.PhilosopherService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIf
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertIs

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

    @Test
    @EnabledIf("hasApiKey")
    fun `test complete UI workflow - full philosopher conversation`() = runBlocking {
        val apiKey = System.getenv("OPENAI_API_KEY")
        val service = PhilosopherService(apiKey)

        try {
            println("🧪 Testing complete UI workflow...")

            // Step 1: User selects philosophers (simulate UI selection)
            val allPhilosophers = service.getAllPhilosophers()
            assertTrue(allPhilosophers.isNotEmpty(), "Should have philosophers available")

            val selectedPhilosophers = listOf(
                allPhilosophers.first { it.id == "socrates" },
                allPhilosophers.first { it.id == "nietzsche" },
                allPhilosophers.first { it.id == "kant" }
            )

            println("🎭 Selected philosophers: ${selectedPhilosophers.map { it.name }}")

            // Step 2: User enters topic and config (simulate UI input)
            val topic = "What is the meaning of life?"
            val config = ConversationConfig(
                topic = topic,
                participants = selectedPhilosophers,
                maxRounds = 2,  // Shorter for testing
                maxWordsPerResponse = 100  // Shorter responses for cost control
            )

            println("💭 Topic: $topic")
            println("⚙️ Config: ${config.maxRounds} rounds, ${config.maxWordsPerResponse} words max")

            // Step 3: Verify initial state
            var state = service.conversationState.first()
            assertIs<ConversationState.NotStarted>(state, "Should start in NotStarted state")

            // Step 4: Start conversation (simulate user clicking "Start")
            println("🚀 Starting conversation...")
            service.startConversation(config)

            // Step 5: Wait for conversation to complete
            println("⏳ Waiting for conversation to complete...")

            withTimeout(120_000) { // 2 minute timeout
                var attempts = 0
                while (attempts < 50) { // Safety limit
                    state = service.conversationState.first()
                    when (state) {
                        is ConversationState.InProgress -> {
                            println("📈 Progress: Round ${state.currentRound}, Philosopher: ${state.currentPhilosopher?.name}")
                            kotlinx.coroutines.delay(2000) // Check every 2 seconds
                            attempts++
                        }
                        is ConversationState.Completed -> {
                            println("✅ Conversation completed!")
                            break
                        }
                        is ConversationState.Error -> {
                            throw RuntimeException("Conversation failed: ${state.message}", state.cause)
                        }
                        else -> {
                            kotlinx.coroutines.delay(1000)
                            attempts++
                        }
                    }
                }
            }

            // Step 6: Verify final state and results
            val finalState = service.conversationState.first()
            assertIs<ConversationState.Completed>(finalState, "Should complete successfully")

            println("🏁 Final state verification...")
            assertEquals(config.topic, finalState.config.topic, "Topic should match")
            assertEquals(selectedPhilosophers.size, finalState.config.participants.size, "Participant count should match")

            // Verify we got contributions from all philosophers
            val contributions = finalState.finalContributions
            assertTrue(contributions.isNotEmpty(), "Should have contributions")

            val philosopherNames = contributions.map { it.philosopher.name }.distinct()
            assertEquals(selectedPhilosophers.size, philosopherNames.size,
                "Should have contributions from all ${selectedPhilosophers.size} philosophers")

            // Verify round structure
            val rounds = finalState.rounds
            assertTrue(rounds.size <= config.maxRounds, "Should not exceed max rounds")

            println("📊 Conversation Summary:")
            println("   - Total rounds: ${rounds.size}")
            println("   - Total contributions: ${contributions.size}")
            println("   - Philosophers participated: ${philosopherNames.joinToString(", ")}")

            contributions.forEach { contribution ->
                val preview = contribution.response.take(80).replace("\n", " ")
                println("   - ${contribution.philosopher.name} (R${contribution.roundNumber}): $preview...")
                assertTrue(contribution.response.isNotBlank(), "Contribution should not be blank")
                assertTrue(contribution.response.length > 20, "Contribution should be substantive")
            }

            // Verify each philosopher spoke at least once
            selectedPhilosophers.forEach { philosopher ->
                val hasContribution = contributions.any { it.philosopher.id == philosopher.id }
                assertTrue(hasContribution, "${philosopher.name} should have at least one contribution")
            }

            println("🎉 Complete UI workflow test passed!")

        } catch (e: Exception) {
            println("❌ Complete UI workflow test failed: ${e.message}")
            e.printStackTrace()
            throw e
        } finally {
            service.close()
        }
    }
}