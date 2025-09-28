package com.jcraw.sophia.discussion

import com.jcraw.sophia.config.PhilosopherSystemPrompts

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
            systemPrompt = PhilosopherSystemPrompts.socrates
        ),

        Philosopher(
            id = "nietzsche",
            name = "Friedrich Nietzsche",
            description = "German philosopher who challenged traditional morality and religion",
            era = "19th Century",
            nationality = "German",
            systemPrompt = PhilosopherSystemPrompts.nietzsche
        ),

        Philosopher(
            id = "kant",
            name = "Immanuel Kant",
            description = "German philosopher who developed critical philosophy and categorical imperatives",
            era = "18th Century",
            nationality = "German",
            systemPrompt = PhilosopherSystemPrompts.kant
        ),

        Philosopher(
            id = "aristotle",
            name = "Aristotle",
            description = "Ancient Greek philosopher, student of Plato, tutor to Alexander the Great",
            era = "Ancient Greece",
            nationality = "Greek",
            systemPrompt = PhilosopherSystemPrompts.aristotle
        ),

        Philosopher(
            id = "sartre",
            name = "Jean-Paul Sartre",
            description = "French existentialist philosopher emphasizing freedom and responsibility",
            era = "20th Century",
            nationality = "French",
            systemPrompt = PhilosopherSystemPrompts.sartre
        ),

        Philosopher(
            id = "confucius",
            name = "Confucius",
            description = "Chinese philosopher focused on ethics, morality, and social harmony",
            era = "Ancient China",
            nationality = "Chinese",
            systemPrompt = PhilosopherSystemPrompts.confucius
        ),

        Philosopher(
            id = "rousseau",
            name = "Jean-Jacques Rousseau",
            description = "French philosopher of the Enlightenment, known for social contract theory",
            era = "18th Century",
            nationality = "French",
            systemPrompt = PhilosopherSystemPrompts.rousseau
        ),

        Philosopher(
            id = "marcusAurelius",
            name = "Marcus Aurelius",
            description = "Roman Emperor and Stoic philosopher, author of Meditations",
            era = "Ancient Rome",
            nationality = "Roman",
            systemPrompt = PhilosopherSystemPrompts.marcusAurelius
        ),

        Philosopher(
            id = "laoTzu",
            name = "Lao Tzu",
            description = "Ancient Chinese philosopher and founder of Taoism",
            era = "Ancient China",
            nationality = "Chinese",
            systemPrompt = PhilosopherSystemPrompts.laoTzu
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