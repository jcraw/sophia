package com.jcraw.sophia.config

import com.jcraw.llm.OpenAIModel

/**
 * Bridge between config module and llm-wrapper module.
 * Converts config models to llm-wrapper models to avoid circular dependencies.
 */
object LLMBridge {

    fun toOpenAIModel(model: LLMModel): OpenAIModel = when (model) {
        LLMModel.GPT4Turbo -> OpenAIModel.GPT4Turbo
        LLMModel.GPT4TurboPreview -> OpenAIModel.GPT4TurboPreview
        LLMModel.GPT4 -> OpenAIModel.GPT4
        LLMModel.GPT4_32K -> OpenAIModel.GPT4_32K
        LLMModel.GPT3_5Turbo -> OpenAIModel.GPT3_5Turbo
        LLMModel.GPT3_5Turbo16K -> OpenAIModel.GPT3_5Turbo16K
        LLMModel.GPT4_1Preview -> OpenAIModel.GPT4_1Preview
        LLMModel.GPT4_1Mini -> OpenAIModel.GPT4_1Mini
        LLMModel.GPT4_1Nano -> OpenAIModel.GPT4_1Nano
    }
}