package com.gabi.assistant.ai

import com.gabi.assistant.data.model.AiMessage
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

interface OpenRouterApi {
    @POST("api/v1/chat/completions")
    suspend fun chat(
        @Header("Authorization") auth: String,
        @Body request: DsRequest
    ): DsResponse

    @POST("api/v1/chat/completions")
    suspend fun chatWithTools(
        @Header("Authorization") auth: String,
        @Body request: ToolChatRequest
    ): ToolChatResponse
}

/**
 * Terceiro provedor de IA (15/10/2026) — pedido explícito do usuário: "nunca mais
 * parar de ter crédito". OpenRouter agrega vários modelos/provedores numa chave só,
 * com modelos gratuitos de verdade (sufixo `:free`). Formato de requisição é o mesmo
 * compatível com OpenAI que Groq/DeepSeek já usam (`DsRequest`/`ToolChatRequest`
 * reaproveitados, não são específicos do DeepSeek apesar do nome).
 *
 * Modelo escolhido (`nvidia/nemotron-3-super-120b-a12b:free`): confirmado na hora
 * (15/10/2026) via `GET openrouter.ai/api/v1/models` que é gratuito, suporta
 * `tools` (function calling, necessário pro roteamento da Gabi) e tem contexto
 * grande. Testado ao vivo por HTTP direto antes de entrar no código — responde
 * normal, só precisa de `max_tokens` com folga (ele "pensa" antes de responder,
 * igual o gpt-oss-120b do Groq, e isso consome parte do orçamento de tokens).
 */
@Singleton
class OpenRouterProvider @Inject constructor() : AiModelProvider {

    override val name = "OpenRouter"

    var apiKey: String = ""

    private val api: OpenRouterApi by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl("https://openrouter.ai/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenRouterApi::class.java)
    }

    override suspend fun chat(
        messages: List<AiMessage>,
        systemPrompt: String,
        temperature: Float,
        maxTokens: Int
    ): Result<String> {
        return try {
            val allMessages = mutableListOf(DsMessage("system", systemPrompt)) +
                    messages.map { DsMessage(it.role, it.content) }

            val response = api.chat(
                auth = "Bearer $apiKey",
                request = DsRequest(
                    model = MODEL,
                    messages = allMessages,
                    temperature = temperature,
                    maxTokens = maxTokens
                )
            )
            val content = response.choices.firstOrNull()?.message?.content
            if (content.isNullOrBlank()) {
                Result.failure(Exception("Empty response from OpenRouter"))
            } else {
                Result.success(content.trim())
            }
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

            val response = api.chatWithTools(
                auth = "Bearer $apiKey",
                request = ToolChatRequest(
                    model = MODEL,
                    messages = allMessages,
                    tools = tools,
                    temperature = 0.3f,
                    // Esse modelo "pensa" antes de responder (reasoning consome parte
                    // do orçamento) — 1024 bastou nos testes por curl, mas com folga
                    // maior que o Groq/DeepSeek (que não gastam tokens pensando do
                    // mesmo jeito) pra não cortar a resposta de verdade no meio.
                    maxTokens = 1536
                )
            )
            val message = response.choices.firstOrNull()?.message
            if (message == null || (message.toolCalls.isNullOrEmpty() && message.content.isNullOrBlank())) {
                Result.failure(Exception("Empty response from OpenRouter"))
            } else {
                Result.success(message)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val MODEL = "nvidia/nemotron-3-super-120b-a12b:free"
    }
}
