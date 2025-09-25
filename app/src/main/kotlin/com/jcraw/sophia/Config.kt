package com.jcraw.sophia

import com.jcraw.llm.LLMConfig
import com.jcraw.llm.LLMProfile
import java.util.Properties

object Config {
    private val properties = Properties()

    init {
        val configStream = Config::class.java.classLoader.getResourceAsStream("config.properties")
        if (configStream != null) {
            properties.load(configStream)
        }

        // Initialize LLM profile based on configuration
        initializeLLMProfile()
    }

    val openAiApiKey: String
        get() = properties.getProperty("openai.api.key", "")

    private fun initializeLLMProfile() {
        // Check for profile override in properties first
        val profileName = properties.getProperty("llm.profile")
            ?: System.getProperty("llm.profile")
            ?: System.getenv("LLM_PROFILE")
            ?: "debug" // Default to debug for development

        val profile = when (profileName.lowercase()) {
            "production", "prod" -> LLMProfile.Production
            "balanced", "bal" -> LLMProfile.Balanced
            "debug", "dev", "test" -> LLMProfile.Debug
            else -> {
                println("Warning: Unknown LLM profile '$profileName', using Debug profile")
                LLMProfile.Debug
            }
        }

        LLMConfig.setProfile(profile)
        println("🤖 LLM Profile: $profileName -> ${profile::class.simpleName}")
        println("   📚 Philosophical: ${profile.philosophicalModel}")
        println("   📝 Summarization: ${profile.summarizationModel}")
        println("   🎬 Director: ${profile.directorModel}")
    }
}