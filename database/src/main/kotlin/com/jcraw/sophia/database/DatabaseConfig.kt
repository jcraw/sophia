package com.jcraw.sophia.database

import com.jcraw.sophia.database.entities.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

object DatabaseConfig {
    private var database: Database? = null

    fun initialize(databasePath: String = "sophia_conversations.db") {
        // Ensure the directory exists
        val dbFile = File(databasePath)
        dbFile.parentFile?.mkdirs()

        database = Database.connect(
            url = "jdbc:sqlite:$databasePath",
            driver = "org.sqlite.JDBC"
        )

        // Create tables if they don't exist
        transaction {
            SchemaUtils.create(
                ConversationTable,
                PhilosopherTable,
                ConversationParticipantTable,
                ConversationRoundTable,
                PhilosopherContributionTable
            )
        }
    }

    fun initializeInMemory() {
        database = Database.connect(
            url = "jdbc:sqlite::memory:",
            driver = "org.sqlite.JDBC"
        )

        transaction {
            SchemaUtils.create(
                ConversationTable,
                PhilosopherTable,
                ConversationParticipantTable,
                ConversationRoundTable,
                PhilosopherContributionTable
            )
        }
    }

    fun isInitialized(): Boolean = database != null

    fun close() {
        database = null
    }
}