package com.gabi.assistant.ai

import com.google.gson.JsonParser
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeParseException

/** Resultado de uma rodada de roteamento via IA (function calling). */
sealed class RouteResult {
    /** A IA escolheu uma ferramenta — mapeada pro mesmo ParsedCommand do parser por regex. */
    data class Command(val parsed: ParsedCommand) : RouteResult()

    /** A IA decidiu que é só conversa normal; o texto já é a resposta final. */
    data class DirectReply(val text: String) : RouteResult()

    /** Falha (sem internet, sem chave, erro da API) — quem chamou deve cair pro parser offline. */
    object Failed : RouteResult()
}

/**
 * Converte a escolha de ferramenta da IA (Groq/DeepSeek, API compatível com
 * a da OpenAI) num ParsedCommand — o mesmo tipo que o CommandParser (regex)
 * produz — para reaproveitar sem mudanças o pipeline de execução/confirmação
 * já existente em JavisRepository.
 */
object AiToolRouter {

    fun route(result: Result<ToolMessage>): RouteResult {
        val message = result.getOrNull() ?: return RouteResult.Failed
        val toolCall = message.toolCalls?.firstOrNull()
        if (toolCall == null) {
            val content = message.content?.trim()
            return if (content.isNullOrBlank()) RouteResult.Failed else RouteResult.DirectReply(content)
        }
        return try {
            RouteResult.Command(toParsedCommand(toolCall))
        } catch (e: Exception) {
            RouteResult.Failed
        }
    }

    private fun toParsedCommand(call: ToolCall): ParsedCommand {
        val args = JsonParser.parseString(call.function.arguments).asJsonObject
        fun str(key: String): String = args.get(key)?.takeIf { !it.isJsonNull }?.asString ?: ""

        return when (call.function.name) {
            "open_app" -> ParsedCommand(CommandType.OPEN_APP, target = str("app_name"))

            "call_contact" -> {
                val contact = str("contact_name")
                ParsedCommand(
                    CommandType.CALL_CONTACT, target = contact,
                    requiresConfirmation = true, confirmationMessage = "Ligar para $contact?"
                )
            }

            "set_alarm" -> ParsedCommand(
                CommandType.SET_ALARM, target = str("description"),
                requiresConfirmation = true, confirmationMessage = "Definir um alarme?"
            )

            "search_web" -> ParsedCommand(CommandType.SEARCH_WEB, target = str("query"))
            "search_youtube" -> ParsedCommand(CommandType.SEARCH_YOUTUBE, target = str("query"))

            "send_whatsapp_message" -> {
                val contact = str("contact_name")
                val msg = str("message")
                ParsedCommand(
                    CommandType.SEND_WHATSAPP, target = contact, extra = msg,
                    requiresConfirmation = true,
                    confirmationMessage = "Enviar \"$msg\" para $contact no WhatsApp?"
                )
            }

            "read_notifications" -> ParsedCommand(CommandType.READ_NOTIFICATIONS)
            "open_settings" -> ParsedCommand(CommandType.OPEN_SETTINGS)
            "take_photo" -> ParsedCommand(CommandType.TAKE_PHOTO)
            "get_weather" -> ParsedCommand(CommandType.WEATHER, target = str("city"))

            "set_reminder" -> {
                val triggerAt = parseIsoToMillis(str("trigger_at"))
                    ?: throw IllegalArgumentException("trigger_at inválido")
                ParsedCommand(CommandType.SET_REMINDER, target = str("subject"), extra = triggerAt.toString())
            }

            "share_content" -> ParsedCommand(CommandType.SHARE, target = str("text"))
            "bluetooth_connect" -> ParsedCommand(CommandType.BLUETOOTH_CONNECT)
            "ask_claude" -> ParsedCommand(CommandType.ASK_CLAUDE, target = str("query"))

            else -> ParsedCommand(CommandType.UNKNOWN)
        }
    }

    private fun parseIsoToMillis(iso: String): Long? = try {
        LocalDateTime.parse(iso).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    } catch (e: DateTimeParseException) {
        null
    }

    /** Data/hora atual em ISO-8601, pra IA calcular horários relativos ("em 30 minutos", "amanhã às 9h"). */
    fun currentDateTimeContext(): String =
        "Data e hora atual: ${LocalDateTime.now()} (fuso: ${ZoneId.systemDefault()})."
}
