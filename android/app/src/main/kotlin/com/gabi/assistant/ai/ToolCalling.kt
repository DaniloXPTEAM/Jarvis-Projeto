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

    val definitions: List<ToolDef> = listOf(
        tool(
            "open_app", "Abre um aplicativo no aparelho pelo nome (ex.: whatsapp, youtube, chrome).",
            params(listOf("app_name")) { add("app_name", stringProp("Nome do app, em minúsculas.")) }
        ),
        tool(
            "call_contact", "Liga para um contato da agenda pelo nome. Sempre exige confirmação do usuário antes de ligar.",
            params(listOf("contact_name")) { add("contact_name", stringProp("Nome do contato a ligar.")) }
        ),
        tool(
            "set_alarm", "Define um alarme no relógio do aparelho.",
            params { add("description", stringProp("Descrição livre do alarme pedido pelo usuário.")) }
        ),
        tool(
            "search_web", "Busca um termo no Google.",
            params(listOf("query")) { add("query", stringProp("O que buscar.")) }
        ),
        tool(
            "search_youtube", "Busca um termo no YouTube.",
            params(listOf("query")) { add("query", stringProp("O que buscar no YouTube.")) }
        ),
        tool(
            "send_whatsapp_message", "Envia uma mensagem de WhatsApp para um contato. Sempre exige confirmação antes de enviar.",
            params(listOf("contact_name", "message")) {
                add("contact_name", stringProp("Nome do contato/grupo destinatário."))
                add("message", stringProp("Texto da mensagem a enviar."))
            }
        ),
        tool("read_notifications", "Lê as notificações recentes do aparelho em voz alta.", params {}),
        tool("open_settings", "Abre as configurações do sistema Android.", params {}),
        tool("take_photo", "Abre a câmera para tirar uma foto.", params {}),
        tool(
            "get_weather", "Consulta a previsão do tempo/clima atual de uma cidade.",
            params { add("city", stringProp("Cidade consultada. Vazio = cidade padrão do usuário.")) }
        ),
        tool(
            "set_reminder",
            "Cria um lembrete que vai notificar o usuário num horário específico no futuro.",
            params(listOf("subject", "trigger_at")) {
                add("subject", stringProp("Do que o lembrete trata, em poucas palavras."))
                add(
                    "trigger_at",
                    stringProp(
                        "Data e hora alvo em ISO-8601 (ex.: 2026-08-21T20:00:00), calculada a partir " +
                            "da data/hora atual informada no prompt e do que o usuário pediu " +
                            "(\"em 30 minutos\", \"amanhã às 9h\", etc.)."
                    )
                )
            }
        ),
        tool(
            "share_content", "Compartilha um texto (geralmente a última resposta da Gabi) via o menu de compartilhamento do Android.",
            params { add("text", stringProp("Texto a compartilhar. Vazio = usa a última resposta da Gabi.")) }
        ),
        tool("bluetooth_connect", "Liga o Bluetooth e roteia o áudio da Gabi pra um fone/caixa conectado.", params {}),
        tool(
            "ask_claude", "Encaminha uma pergunta específica pro Claude (Anthropic) responder, quando o usuário pede explicitamente para falar com o Claude.",
            params(listOf("query")) { add("query", stringProp("A pergunta a enviar ao Claude.")) }
        ),
        tool(
            "read_calendar", "Lê os próximos compromissos da agenda/calendário do usuário em voz alta.",
            params {}
        ),
        tool(
            "create_calendar_event",
            "Cria um novo compromisso/evento na agenda do usuário (não é um lembrete interno do app, é um evento de calendário de verdade).",
            params(listOf("title", "start_at")) {
                add("title", stringProp("Título do compromisso, em poucas palavras."))
                add(
                    "start_at",
                    stringProp(
                        "Data e hora de início em ISO-8601 (ex.: 2026-08-22T15:00:00), calculada " +
                            "a partir da data/hora atual informada no prompt."
                    )
                )
            }
        ),
        tool(
            "read_gmail",
            "Verifica se chegou email novo no Gmail e lê a prévia (remetente, assunto e trecho do corpo) " +
                "das notificações recentes do Gmail. Não lê o corpo completo do email.",
            params {}
        )
    )
}
