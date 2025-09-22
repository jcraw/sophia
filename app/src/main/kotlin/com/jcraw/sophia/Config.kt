package com.jcraw.sophia

import java.util.Properties

object Config {
    private val properties = Properties()
    
    init {
        val configStream = Config::class.java.classLoader.getResourceAsStream("config.properties")
        if (configStream != null) {
            properties.load(configStream)
        }
    }
    
    val openAiApiKey: String
        get() = properties.getProperty("openai.api.key", "")
}