package com.jcraw.sophia.database

import com.jcraw.sophia.discussion.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import java.io.File
import java.time.Instant

class SimpleConversationStorageTest {

    private lateinit var storage: SimpleConversationStorage
    private val testDirectory = "test_conversations"

    @BeforeEach
    fun setup() {
        storage = SimpleConversationStorage(testDirectory)
    }

    @AfterEach
    fun cleanup() {
        File(testDirectory).deleteRecursively()
    }

    @Test
    fun `should save and load in-progress conversation`() = runTest {
        val philosophers = listOf(
            createTestPhilosopher("socrates", "Socrates"),
            createTestPhilosopher("kant", "Kant")
        )
        val config = ConversationConfig(
            topic = "What is justice?",
            participants = philosophers,
            maxRounds = 2,
            maxWordsPerResponse = 100
        )
        val state = ConversationState.InProgress(config)

        val conversationId = storage.saveConversation(state)

        assertNotNull(conversationId)
        val savedConversation = storage.loadConversation(conversationId)
        assertNotNull(savedConversation)
        assertEquals("What is justice?", savedConversation!!.topic)
        assertEquals("in_progress", savedConversation.status)
        assertEquals(2, savedConversation.participants.size)
    }

    @Test
    fun `should save completed conversation with contributions`() = runTest {
        val philosopher = createTestPhilosopher("nietzsche", "Nietzsche")
        val config = ConversationConfig(
            topic = "What is power?",
            participants = listOf(philosopher)
        )
        val contribution = PhilosopherContribution(
            philosopher = philosopher,
            response = "Will to power is the fundamental drive.",
            timestamp = Instant.now(),
            roundNumber = 1
        )
        val rounds = listOf(
            ConversationRound(1, listOf(contribution))
        )
        val state = ConversationState.Completed(
            config = config,
            rounds = rounds,
            finalContributions = listOf(contribution)
        )

        val conversationId = storage.saveConversation(state)
        val savedConversation = storage.loadConversation(conversationId)

        assertNotNull(savedConversation)
        assertEquals("completed", savedConversation!!.status)
        assertEquals(1, savedConversation.contributions.size)
        assertEquals("Will to power is the fundamental drive.",
                    savedConversation.contributions.first().response)
    }

    @Test
    fun `should retrieve conversations by status`() = runTest {
        val philosopher = createTestPhilosopher("confucius", "Confucius")
        val config1 = ConversationConfig("Topic 1", listOf(philosopher))
        val config2 = ConversationConfig("Topic 2", listOf(philosopher))

        val state1 = ConversationState.InProgress(config1)
        val state2 = ConversationState.Completed(config2, emptyList(), emptyList())

        storage.saveConversation(state1)
        storage.saveConversation(state2)

        val completed = storage.getConversationsByStatus("completed")
        val inProgress = storage.getConversationsByStatus("in_progress")

        assertEquals(1, completed.size)
        assertEquals(1, inProgress.size)
        assertEquals("Topic 2", completed.first().topic)
        assertEquals("Topic 1", inProgress.first().topic)
    }

    @Test
    fun `should delete conversation`() = runTest {
        val philosopher = createTestPhilosopher("sartre", "Sartre")
        val config = ConversationConfig(
            topic = "What is existence?",
            participants = listOf(philosopher)
        )
        val state = ConversationState.InProgress(config)
        val conversationId = storage.saveConversation(state)

        val deleted = storage.deleteConversation(conversationId)
        assertTrue(deleted)

        val savedConversation = storage.loadConversation(conversationId)
        assertEquals(null, savedConversation)
    }


    private fun createTestPhilosopher(id: String, name: String): Philosopher {
        return Philosopher(
            id = id,
            name = name,
            description = "Test philosopher",
            systemPrompt = "You are $name",
            era = "Test Era",
            nationality = "Test Nation"
        )
    }
}