package com.javis.assistant.ai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orquestrador Claude (Anthropic).
 *
 * Uso: quando o usuário diz "fale com o Claude e...", "pergunte ao Claude...",
 * a Gabi repassa a pergunta pro Claude e fala a resposta dele.
 *
 * Requer ANTHROPIC_API_KEY (informada na tela de Ajustes). Sem chave, a Gabi
 * avisa que precisa configurar.
 */
@Singleton
class ClaudeProvider @Inject constructor() {

    var apiKey: String = ""

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .build()
    }

    /** Faz uma pergunta ao Claude e devolve o texto da resposta (pt-BR). */
    suspend fun ask(query: String): String = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("model", MODEL)
            put("max_tokens", 1024)
            put("system", CLAUDE_SYSTEM)
            put(
                "messages",
                JSONArray().put(JSONObject().put("role", "user").put("content", query))
            )
        }.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .header("content-type", "application/json")
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        val raw = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
            Log.e(TAG, "Claude erro ${response.code}: $raw")
            throw Exception("Claude ${response.code}")
        }
        val json = JSONObject(raw.ifBlank { "{}" })
        val text = json.optJSONArray("content")?.optJSONObject(0)?.optString("text")
        if (text.isNullOrBlank()) throw Exception("Claude sem resposta")
        text.trim()
    }

    companion object {
        private const val TAG = "ClaudeProvider"
        // Sonnet: bom para raciocínio/verificações que o usuário pede ao Claude
        const val MODEL = "claude-3-5-sonnet-20241022"
        const val CLAUDE_SYSTEM =
            "Você é o Claude, atuando como orquestrador inteligente da Gabi (uma assistente " +
                "pessoal em português brasileiro). Responda SEMPRE em português brasileiro, de forma " +
                "clara, direta e acionável. Quando o usuário pedir para verificar viabilidade, criar " +
                "algo, planejar ou analisar, dê um resposta útil e objetiva, em poucos parágrafos. " +
                "Suas palavras serão lidas em voz alta pela Gabi."
    }
}
