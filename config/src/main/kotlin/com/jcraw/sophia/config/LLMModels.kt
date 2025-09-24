package com.jcraw.sophia.config

import kotlinx.serialization.Serializable

@Serializable
data class ModelPricing(
    val inputCostPer1M: Double,
    val outputCostPer1M: Double
)

sealed class LLMModel(
    val modelId: String,
    val pricing: ModelPricing
) {
    data object GPT4Turbo : LLMModel("gpt-4-turbo", ModelPricing(10.0, 30.0))
    data object GPT4TurboPreview : LLMModel("gpt-4-turbo-preview", ModelPricing(10.0, 30.0))
    data object GPT4 : LLMModel("gpt-4", ModelPricing(30.0, 60.0))
    data object GPT4_32K : LLMModel("gpt-4-32k", ModelPricing(60.0, 120.0))
    data object GPT3_5Turbo : LLMModel("gpt-3.5-turbo", ModelPricing(0.5, 1.5))
    data object GPT3_5Turbo16K : LLMModel("gpt-3.5-turbo-16k", ModelPricing(3.0, 4.0))
    data object GPT4_1Preview : LLMModel("gpt-4.1-preview", ModelPricing(2.5, 10.0))
    data object GPT4_1Mini : LLMModel("gpt-4.1-mini", ModelPricing(0.15, 0.60))
    data object GPT4_1Nano : LLMModel("gpt-4.1-nano", ModelPricing(0.15, 0.60))

    override fun toString(): String = modelId
}

object LLMConfig {
    // Default model for philosophical discussions - cost-effective as per guidelines
    val defaultPhilosophicalModel: LLMModel = LLMModel.GPT4_1Nano

    // Model for conversation summarization - slightly higher capability for analysis
    val summarizationModel: LLMModel = LLMModel.GPT4_1Nano

    // Temperature settings for different use cases
    const val CREATIVE_TEMPERATURE = 0.8 // For philosophical discussions
    const val BALANCED_TEMPERATURE = 0.5 // For general tasks
    const val PRECISE_TEMPERATURE = 0.2 // For factual responses
    const val SUMMARIZATION_TEMPERATURE = 0.3 // For summarization - precise but some creativity

    // Token limits
    const val DEFAULT_MAX_TOKENS = 300 // ~150 words as per current logic
    const val LONG_RESPONSE_MAX_TOKENS = 600 // For detailed responses
    const val SUMMARIZATION_MAX_TOKENS = 1000 // For summarization output including JSON structure
}