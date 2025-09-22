package com.jcraw.llm

data class ModelPricing(
    val inputCostPer1M: Double,
    val outputCostPer1M: Double
)

sealed class OpenAIModel(
    val modelId: String,
    val pricing: ModelPricing
) {
    data object GPT4Turbo : OpenAIModel("gpt-4-turbo", ModelPricing(10.0, 30.0))
    data object GPT4TurboPreview : OpenAIModel("gpt-4-turbo-preview", ModelPricing(10.0, 30.0))
    data object GPT4 : OpenAIModel("gpt-4", ModelPricing(30.0, 60.0))
    data object GPT4_32K : OpenAIModel("gpt-4-32k", ModelPricing(60.0, 120.0))
    data object GPT3_5Turbo : OpenAIModel("gpt-3.5-turbo", ModelPricing(0.5, 1.5))
    data object GPT3_5Turbo16K : OpenAIModel("gpt-3.5-turbo-16k", ModelPricing(3.0, 4.0))
    data object GPT4_1Preview : OpenAIModel("gpt-4.1-preview", ModelPricing(2.5, 10.0))
    data object GPT4_1Mini : OpenAIModel("gpt-4.1-mini", ModelPricing(0.15, 0.60))
    data object GPT4_1Nano : OpenAIModel("gpt-4.1-nano", ModelPricing(0.15, 0.60))

    fun calculateCost(usage: OpenAIUsage): Double {
        val inputCost = (usage.promptTokens / 1_000_000.0) * pricing.inputCostPer1M
        val outputCost = (usage.completionTokens / 1_000_000.0) * pricing.outputCostPer1M
        return inputCost + outputCost
    }

    override fun toString(): String = modelId
}