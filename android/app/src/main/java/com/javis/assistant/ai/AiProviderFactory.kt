package com.javis.assistant.ai

import com.javis.assistant.data.remote.api.DeepSeekApiService
import com.javis.assistant.data.remote.api.GroqApiService
    private val deepSeekApiService: DeepSeekApiService,
    private val openRouterApiService: OpenRouterApiService
) {
    fun create(provider: AiProvider, apiKey: String): com.javis.assistant.ai.AiProvider {
        return when (provider) {
            AiProvider.OPENROUTER -> OpenRouterAiProvider(openRouterApiService, apiKey)
            AiProvider.GROQ -> GroqAiProvider(groqApiService, apiKey)
            AiProvider.DEEPSEEK -> DeepSeekAiProvider(deepSeekApiService, apiKey)
            AiProvider.CLAUDE -> throw IllegalStateException("Claude provider not implemented yet")63378f4 (chore: finish Gabi setup)
        }
    }
}
