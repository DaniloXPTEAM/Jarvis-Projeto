package com.javis.assistant.data.remote.api

import com.javis.assistant.data.remote.models.ChatCompletionRequest
import com.javis.assistant.data.remote.models.ChatCompletionResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * OpenRouter API — OpenAI-compatible chat completions.
 * Docs: https://openrouter.ai/docs
 * Models: https://openrouter.ai/models
 *
 * The user can swap between Claude, GPT-4, Llama, Gemini, etc — all
 * from a single API key.
 */
interface OpenRouterApiService {
    @POST("api/v1/chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: ChatCompletionRequest
    ): ChatCompletionResponse
}
