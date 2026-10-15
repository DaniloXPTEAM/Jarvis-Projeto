package com.gabi.assistant.ai

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

/**
 * Modelos compartilhados do formato "tool calling" da OpenAI, usados tanto
 * pelo Groq quanto pelo DeepSeek (ambos expõem uma API compatível com a da
 * OpenAI, incluindo `tools`/`tool_choice`/`tool_calls`).
 */
data class ToolFunctionDef(
    val name: String,
    val description: String,
    val parameters: JsonObject
)

data class ToolDef(
    val type: String = "function",
    val function: ToolFunctionDef
)

data class ToolCallFunction(
    val name: String,
    val arguments: String
)

data class ToolCall(
    val id: String,
    val type: String = "function",
    val function: ToolCallFunction
)

data class ToolMessage(
    val role: String,
    val content: String? = null,
    @SerializedName("tool_calls") val toolCalls: List<ToolCall>? = null,
    @SerializedName("tool_call_id") val toolCallId: String? = null
)

data class ToolChatRequest(
    val model: String,
    val messages: List<ToolMessage>,
    val tools: List<ToolDef>,
    @SerializedName("tool_choice") val toolChoice: String = "auto",
    val temperature: Float,
    @SerializedName("max_tokens") val maxTokens: Int
)

data class ToolChatChoice(val message: ToolMessage)
data class ToolChatResponse(val choices: List<ToolChatChoice>)

/**
 * As "ferramentas" que a Gabi pode escolher chamar em vez de responder em
 * texto puro — substituem a classificação por regex do CommandParser quando
 * há IA disponível (Groq/DeepSeek). Os nomes e parâmetros mapeiam 1:1 pros
 * CommandType existentes, então o resto do pipeline (executeCommand,
 * resolveSkillReply, confirmação) não precisa mudar.
 */
object GabiTools {

    private fun schema(build: JsonObject.() -> Unit): JsonObject =
        JsonObject().apply(build)

    private fun params(required: List<String> = emptyList(), build: JsonObject.() -> Unit): JsonObject {
        val props = JsonObject().apply(build)
        return schema {
            addProperty("type", "object")
            add("properties", props)
            if (required.isNotEmpty()) {
                add("required", JsonArray().apply { required.forEach { add(it) } })
            }
        }
    }

    private fun stringProp(description: String): JsonObject = schema {
        addProperty("type", "string")
        addProperty("description", description)
    }

    private fun tool(name: String, description: String, parameters: JsonObject): ToolDef =
        ToolDef(function = ToolFunctionDef(name, description, parameters))

    // Descrições enxutas de propósito: cada tool aqui é reenviada por INTEIRO em toda
    // mensagem (custo de token fixo, não só uma vez) — texto verboso vira custo real
    // de API a cada turno de conversa. requiresConfirmation já é reforçado no app
    // (AiToolRouter), então não precisa repetir isso na descrição pro modelo.
    val definitions: List<ToolDef> = listOf(
        tool("open_app", "Abre um app pelo nome.",
            params(listOf("app_name")) { add("app_name", stringProp("nome do app, minúsculo")) }),
        tool("call_contact", "Liga para um contato.",
            params(listOf("contact_name")) { add("contact_name", stringProp("nome do contato")) }),
        tool("set_alarm", "Define um alarme.",
            params { add("description", stringProp("descrição livre do alarme")) }),
        tool("search_web", "Busca no Google.",
            params(listOf("query")) { add("query", stringProp("termo de busca")) }),
        tool("search_youtube", "Busca no YouTube.",
            params(listOf("query")) { add("query", stringProp("termo de busca")) }),
        tool("search_spotify", "Busca uma música/artista/playlist no Spotify e abre os resultados.",
            params(listOf("query")) { add("query", stringProp("termo de busca (música, artista ou playlist)")) }),
        tool("send_whatsapp_message", "Manda mensagem de WhatsApp.",
            params(listOf("contact_name", "message")) {
                add("contact_name", stringProp("contato/grupo destinatário"))
                add("message", stringProp("texto da mensagem"))
            }),
        tool("read_notifications", "Lê notificações recentes em voz alta.", params {}),
        tool("open_settings", "Abre configurações do Android.", params {}),
        tool("take_photo", "Abre a câmera.", params {}),
        tool("get_weather", "Consulta o clima atual.",
            params { add("city", stringProp("cidade; vazio = padrão do usuário")) }),
        tool("set_reminder", "Cria lembrete interno num horário futuro.",
            params(listOf("subject", "trigger_at")) {
                add("subject", stringProp("do que se trata, poucas palavras"))
                add("trigger_at", stringProp("ISO-8601 (ex: 2026-08-21T20:00:00), calculado a partir da data/hora atual do prompt"))
            }),
        tool("share_content", "Compartilha texto via menu do Android.",
            params { add("text", stringProp("texto; vazio = última resposta da Gabi")) }),
        tool("bluetooth_connect", "Liga Bluetooth e roteia o áudio da Gabi pra ele.", params {}),
        tool("pause_music", "Pausa a música tocando no momento (ex: Spotify). Não fecha o app.", params {}),
        tool("ask_claude", "Encaminha pergunta pro Claude, só se o usuário pedir Claude explicitamente.",
            params(listOf("query")) { add("query", stringProp("pergunta a enviar")) }),
        tool("read_calendar", "Lê próximos compromissos da agenda em voz alta.", params {}),
        tool("create_calendar_event", "Cria evento real na agenda do sistema (não é lembrete interno).",
            params(listOf("title", "start_at")) {
                add("title", stringProp("título, poucas palavras"))
                add("start_at", stringProp("ISO-8601 (ex: 2026-08-22T15:00:00), calculado a partir da data/hora atual do prompt"))
            }),
        tool("read_gmail", "Confere e lê prévia (remetente/assunto/trecho) de email novo no Gmail.", params {}),
        tool("save_memory", "Guarda preferência/rotina/hábito/fato do usuário pra lembrar depois. Use sempre que ele ensinar algo sobre si, mesmo sem pedir pra 'salvar'.",
            params(listOf("key", "value", "type")) {
                add("key", stringProp("chave curta única, snake_case (ex: 'rotina_bom_dia')"))
                add("value", stringProp("o que lembrar, completo"))
                add("type", stringProp("USER_NAME, PREFERENCE, HABIT, ROUTINE, FACT ou FAVORITE_APP"))
            })
    )
}
