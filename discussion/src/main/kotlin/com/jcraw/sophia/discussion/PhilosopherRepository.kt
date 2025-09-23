package com.jcraw.sophia.discussion

import com.jcraw.sophia.config.PhilosopherPrompts

interface PhilosopherRepository {
    fun getAllPhilosophers(): List<Philosopher>
    fun getPhilosopherById(id: String): Philosopher?
    fun getPhilosophersByEra(era: String): List<Philosopher>
    fun searchPhilosophers(query: String): List<Philosopher>
}

class DefaultPhilosopherRepository : PhilosopherRepository {

    private val philosophers = listOf(
        Philosopher(
            id = "socrates",
            name = "Socrates",
            description = "Ancient Greek philosopher known for the Socratic method and ethical inquiry",
            era = "Ancient Greece",
            nationality = "Greek",
            systemPrompt = PhilosopherPrompts.socrates
        ),

        Philosopher(
            id = "nietzsche",
            name = "Friedrich Nietzsche",
            description = "German philosopher who challenged traditional morality and religion",
            era = "19th Century",
            nationality = "German",
            systemPrompt = PhilosopherPrompts.nietzsche
        ),

        Philosopher(
            id = "kant",
            name = "Immanuel Kant",
            description = "German philosopher who developed critical philosophy and categorical imperatives",
            era = "18th Century",
            nationality = "German",
            systemPrompt = PhilosopherPrompts.kant
        ),

        Philosopher(
            id = "aristotle",
            name = "Aristotle",
            description = "Ancient Greek philosopher, student of Plato, tutor to Alexander the Great",
            era = "Ancient Greece",
            nationality = "Greek",
            systemPrompt = PhilosopherPrompts.aristotle
        ),

        Philosopher(
            id = "sartre",
            name = "Jean-Paul Sartre",
            description = "French existentialist philosopher emphasizing freedom and responsibility",
            era = "20th Century",
            nationality = "French",
            systemPrompt = PhilosopherPrompts.sartre
        ),

        Philosopher(
            id = "confucius",
            name = "Confucius",
            description = "Chinese philosopher focused on ethics, morality, and social harmony",
            era = "Ancient China",
            nationality = "Chinese",
            systemPrompt = PhilosopherPrompts.confucius
        )
    )

    override fun getAllPhilosophers(): List<Philosopher> = philosophers

    override fun getPhilosopherById(id: String): Philosopher? =
        philosophers.find { it.id == id }

    override fun getPhilosophersByEra(era: String): List<Philosopher> =
        philosophers.filter { it.era.equals(era, ignoreCase = true) }

    override fun searchPhilosophers(query: String): List<Philosopher> =
        philosophers.filter { philosopher ->
            query.lowercase() in philosopher.name.lowercase() ||
            query.lowercase() in philosopher.description.lowercase() ||
            query.lowercase() in philosopher.era.lowercase() ||
            query.lowercase() in philosopher.nationality.lowercase()
        }
}