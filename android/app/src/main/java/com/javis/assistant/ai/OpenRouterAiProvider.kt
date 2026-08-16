package com.javis.assistant.ai

import com.javis.assistant.data.remote.api.OpenRouterApiService
import com.javis.assistant.data.remote.models.ApiMessage
import com.javis.assistant.data.remote.models.ChatCompletionRequest
import com.javis.assistant.domain.model.ChatMessage
import com.javis.assistant.domain.model.MessageRole

/**
 * OpenRouter provider — routes to ANY model (Claude, GPT-4, Llama, Gemini, ...)
 * through a single OpenAI-compatible endpoint.
 *
 * Only FREE models are used by default. To swap models, change [defaultModel].
 * Full list: https://openrouter.ai/models
 *
 * Free models currently available:
 *  - meta-llama/llama-3.3-70b-instruct:free   (default — best free)
 *  - meta-llama/llama-3.1-8b-instruct:free    (faster)
 *  - qwen/qwen-2.5-72b-instruct:free          (great multilingual)
 *  - google/gemma-2-9b-it:free                (compact)
 */
class OpenRouterAiProvider(
    private val apiService: OpenRouterApiService,
    private val apiKey: String,
    private val defaultModel: String = "meta-llama/llama-3.3-70b-instruct:free"
) : AiProvider {

    override val name = "OpenRouter ($defaultModel)"

    override suspend fun generateResponse(
        messages: List<ChatMessage>,
        systemPrompt: String
    ): Result<String> = runCatching {
        val apiMessages = mutableListOf(
            ApiMessage(role = "system", content = systemPrompt)
        )
        apiMessages.addAll(
            messages.takeLast(20).map { msg ->
                ApiMessage(
                    role = when (msg.role) {
                        MessageRole.USER -> "user"
                        MessageRole.ASSISTANT -> "assistant"
                        MessageRole.SYSTEM -> "system"
                    },
                    content = msg.content
                )
            }
        )
        val request = ChatCompletionRequest(
            model = defaultModel,
            messages = apiMessages,
            maxTokens = 512
        )
        val response = apiService.chatCompletion(
            authorization = "Bearer $apiKey",
            request = request
        )
        response.choices.firstOrNull()?.message?.content
            ?: error("Empty response from OpenRouter")
    }
}
