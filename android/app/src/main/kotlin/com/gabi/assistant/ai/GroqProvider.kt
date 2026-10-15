package com.gabi.assistant.ai

import android.util.Log
import com.google.gson.annotations.SerializedName
import com.gabi.assistant.data.model.AiMessage
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class GroqRequest(
    val model: String,
    val messages: List<GroqMessage>,
    val temperature: Float,
    @SerializedName("max_tokens") val maxTokens: Int,
    val stream: Boolean = false
)

data class GroqMessage(val role: String, val content: String)

data class GroqResponse(val choices: List<GroqChoice>)

data class GroqChoice(
    val message: GroqMessage,
    @SerializedName("finish_reason") val finishReason: String?
)

interface GroqApi {
    @POST("openai/v1/chat/completions")
    suspend fun chat(
        @Header("Authorization") auth: String,
        @Body request: GroqRequest
    ): GroqResponse

    @POST("openai/v1/chat/completions")
    suspend fun chatWithTools(
        @Header("Authorization") auth: String,
        @Body request: ToolChatRequest
    ): ToolChatResponse
}

@Singleton
class GroqProvider @Inject constructor() : AiModelProvider {

    override val name = "Groq"

    var apiKey: String = ""

    private val api: GroqApi by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.NONE
            })
            .build()

        Retrofit.Builder()
            .baseUrl("https://api.groq.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GroqApi::class.java)
    }

    override suspend fun chat(
        messages: List<AiMessage>,
        systemPrompt: String,
        temperature: Float,
        maxTokens: Int
    ): Result<String> {
        return try {
            val allMessages = mutableListOf(GroqMessage("system", systemPrompt)) +
                    messages.map { GroqMessage(it.role, it.content) }

            val response = withRateLimitRetry {
                api.chat(
                    auth = "Bearer $apiKey",
                    request = GroqRequest(
                        model = MODEL,
                        messages = allMessages,
                        temperature = temperature,
                        maxTokens = maxTokens
                    )
                )
            }
            val content = response.choices.firstOrNull()?.message?.content
                ?: return Result.failure(Exception("Empty response from Groq"))
            Result.success(content.trim())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun chatWithTools(
        messages: List<AiMessage>,
        systemPrompt: String,
        tools: List<ToolDef>
    ): Result<ToolMessage> {
        return try {
            val allMessages = mutableListOf(ToolMessage(role = "system", content = systemPrompt)) +
                    messages.map { ToolMessage(role = it.role, content = it.content) }

            val response = withRateLimitRetry {
                api.chatWithTools(
                    auth = "Bearer $apiKey",
                    request = ToolChatRequest(
                        model = MODEL,
                        messages = allMessages,
                        tools = tools,
                        temperature = 0.3f,
                        maxTokens = 1024
                    )
                )
            }
            val message = response.choices.firstOrNull()?.message
                ?: return Result.failure(Exception("Empty response from Groq"))
            Result.success(message)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * O plano grátis do Groq tem limite de 8000 tokens/minuto (a conta inteira, não por
     * modelo — testado em 21/08/2026). O header `x-ratelimit-reset-tokens` mostra que
     * ele se recompõe em frações de segundo, não minutos — então um 429/402 pontual
     * (ex.: duas mensagens seguidas mais o prompt de sistema + as 15 ferramentas, que
     * já pesam >1000 tokens fixos por chamada) vale a pena tentar de novo uma vez em vez
     * de já desistir e mostrar erro pro usuário.
     */
    private suspend fun <T> withRateLimitRetry(block: suspend () -> T): T =
        try {
            block()
        } catch (e: HttpException) {
            if (e.code() == 429 || e.code() == 402) {
                Log.d("GabiLatency", "Groq rate limit (${e.code()}), tentando de novo em 1.5s")
                delay(1500)
                block()
            } else {
                throw e
            }
        }

    companion object {
        // llama-3.3-70b-versatile foi descontinuado pelo Groq (toda chamada retornava
        // 404 model_not_found — causa raiz de "a Gabi não responde nada" em 21/08/2026).
        // openai/gpt-oss-120b testado e confirmado: chat normal e tool-calling ok.
        const val MODEL = "openai/gpt-oss-120b"
    }
}
