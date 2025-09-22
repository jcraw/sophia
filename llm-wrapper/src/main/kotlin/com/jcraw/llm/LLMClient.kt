package com.jcraw.llm

interface LLMClient {
    suspend fun chatCompletion(
        model: OpenAIModel,
        systemPrompt: String,
        userContext: String,
        maxTokens: Int = 1000,
        temperature: Double = 0.7
    ): OpenAIResponse
    
    fun close()
}