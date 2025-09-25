package com.jcraw.llm

import kotlinx.serialization.Serializable

@Serializable
data class ModelPricing(
    val inputCostPer1M: Double,
    val outputCostPer1M: Double
)

sealed class OpenAIModel(
    val modelId: String,
    val pricing: ModelPricing
) {
    // GPT-5 models (current focus)
    data object GPT5_Nano : OpenAIModel("gpt-5-nano", ModelPricing(0.05, 0.40))
    data object GPT5_Mini : OpenAIModel("gpt-5-mini", ModelPricing(0.25, 2.00))

    // Legacy GPT-4.1 models
    data object GPT4_1Preview : OpenAIModel("gpt-4.1-preview", ModelPricing(2.5, 10.0))
    data object GPT4_1Mini : OpenAIModel("gpt-4.1-mini", ModelPricing(0.40, 1.60))
    data object GPT4_1Nano : OpenAIModel("gpt-4.1-nano", ModelPricing(0.10, 0.40))

    fun calculateCost(usage: OpenAIUsage): Double {
        val inputCost = (usage.promptTokens / 1_000_000.0) * pricing.inputCostPer1M
        val outputCost = (usage.completionTokens / 1_000_000.0) * pricing.outputCostPer1M
        return inputCost + outputCost
    }

    override fun toString(): String = modelId
}

// Configuration for LLM usage
object LLMConfig {
    // Default model for philosophical discussions - using gpt-5-nano
    val defaultPhilosophicalModel: OpenAIModel = OpenAIModel.GPT5_Nano

    // Model for conversation summarization
    val summarizationModel: OpenAIModel = OpenAIModel.GPT4_1Mini

    // Model for video script creation
    val directorModel: OpenAIModel = OpenAIModel.GPT4_1Mini

    // Temperature settings for different use cases
    const val CREATIVE_TEMPERATURE = 0.8 // For philosophical discussions
    const val BALANCED_TEMPERATURE = 0.5 // For general tasks
    const val PRECISE_TEMPERATURE = 0.2 // For factual responses
    const val SUMMARIZATION_TEMPERATURE = 0.3 // For summarization - precise but some creativity
    const val DIRECTOR_TEMPERATURE = 0.7 // For video script creation - high creativity for visual descriptions

    // Token limits - GPT-5 needs higher limits due to different tokenization
    const val DEFAULT_MAX_TOKENS = 1000 // ~400 words for GPT-5
    const val LONG_RESPONSE_MAX_TOKENS = 1500 // For detailed responses
    const val SUMMARIZATION_MAX_TOKENS = 3000 // For summarization output - GPT-4.1-mini has larger context
}