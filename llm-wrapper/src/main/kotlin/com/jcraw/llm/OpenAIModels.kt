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

// Configuration profiles for different environments
sealed class LLMProfile(
    val philosophicalModel: OpenAIModel,
    val summarizationModel: OpenAIModel,
    val directorModel: OpenAIModel
) {
    // Debug/Testing - uses cheapest models for development
    data object Debug : LLMProfile(
        philosophicalModel = OpenAIModel.GPT4_1Nano,
        summarizationModel = OpenAIModel.GPT4_1Nano,
        directorModel = OpenAIModel.GPT4_1Nano
    )

    // Balanced - good quality at reasonable cost
    data object Balanced : LLMProfile(
        philosophicalModel = OpenAIModel.GPT5_Nano,
        summarizationModel = OpenAIModel.GPT4_1Mini,
        directorModel = OpenAIModel.GPT4_1Mini
    )

    // Production - highest quality models for release
    data object Production : LLMProfile(
        philosophicalModel = OpenAIModel.GPT5_Mini,
        summarizationModel = OpenAIModel.GPT5_Mini,
        directorModel = OpenAIModel.GPT5_Mini
    )
}

// Configuration for LLM usage
object LLMConfig {
    // Current active profile - defaults to Debug for development
    private var currentProfile: LLMProfile = LLMProfile.Debug

    // Public API to change profiles
    fun setProfile(profile: LLMProfile) {
        currentProfile = profile
    }

    fun getCurrentProfile(): LLMProfile = currentProfile

    // Model accessors that use the current profile
    val defaultPhilosophicalModel: OpenAIModel
        get() = currentProfile.philosophicalModel

    val summarizationModel: OpenAIModel
        get() = currentProfile.summarizationModel

    val directorModel: OpenAIModel
        get() = currentProfile.directorModel

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