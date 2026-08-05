package com.javis.assistant.ai

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.Normalizer
import javax.inject.Inject
import javax.inject.Singleton

enum class CommandType {
    OPEN_APP, CALL_CONTACT, CALL_NUMBER, SET_ALARM, SEARCH_WEB,
    SEND_WHATSAPP, READ_NOTIFICATIONS, OPEN_SETTINGS, TAKE_PHOTO,
    SEARCH_YOUTUBE, OPEN_WHATSAPP_CHAT, UNKNOWN
}

data class ParsedCommand(
    val type: CommandType,
    val target: String = "",
    val extra: String = "",
    val requiresConfirmation: Boolean = false,
    val confirmationMessage: String = ""
)

@Singleton
class CommandParser @Inject constructor(
    @ApplicationContext private val context: Context
) {

    // Aliases de apps — inglês + português (tudo em minúsculas, sem acento)
    private val appAliases = mapOf(
        "whatsapp" to "com.whatsapp",
        "zap" to "com.whatsapp",
        "youtube" to "com.google.android.youtube",
        "chrome" to "com.android.chrome",
        "navegador" to "com.android.chrome",
        "camera" to "android.media.action.IMAGE_CAPTURE",
        "calculadora" to "com.android.calculator2",
        "calculator" to "com.android.calculator2",
        "settings" to "android.settings.SETTINGS",
        "configuracoes" to "android.settings.SETTINGS",
        "ajustes" to "android.settings.SETTINGS",
        "contacts" to "com.android.contacts",
        "contatos" to "com.android.contacts",
        "agenda" to "com.android.contacts",
        "files" to "com.android.documentsui",
        "arquivos" to "com.android.documentsui",
        "gallery" to "com.android.gallery3d",
        "galeria" to "com.android.gallery3d",
        "fotos" to "com.android.gallery3d",
        "maps" to "com.google.android.apps.maps",
        "mapas" to "com.google.android.apps.maps",
        "gmail" to "com.google.android.gm",
        "email" to "com.google.android.gm",
        "phone" to "com.android.dialer",
        "telefone" to "com.android.dialer",
        "discador" to "com.android.dialer",
        "messages" to "com.android.messaging",
        "mensagens" to "com.android.messaging",
        "sms" to "com.android.messaging",
        "facebook" to "com.facebook.katana",
        "instagram" to "com.instagram.android",
        "tiktok" to "com.zhiliaoapp.musically",
        "telegram" to "org.telegram.messenger",
        "twitter" to "com.twitter.android",
        "spotify" to "com.spotify.music",
        "musica" to "com.google.android.music",
        "music" to "com.google.android.music",
        "netflix" to "com.netflix.mediaclient",
        "snapchat" to "com.snapchat.android",
        "deepseek" to "com.deepseek.chat",
        "chatgpt" to "com.openai.chatgpt",
        "clock" to "com.android.deskclock",
        "relogio" to "com.android.deskclock",
        "alarme" to "com.android.deskclock",
        "alarm" to "com.android.deskclock",
        "calendar" to "com.android.calendar",
        "calendario" to "com.android.calendar",
        "amazon" to "com.amazon.mShop.android.shopping"
    )

    fun parse(input: String): ParsedCommand {
        val lower = input.lowercase().trim()
        val norm = stripAccents(lower) // casa comandos mesmo com acentos ("ligar pra joão")

        return when {
            // ── ABRIR APP ──────────────────────────────────────────────
            norm.matches(Regex(".*(abrir|abra|abre|abri|iniciar|inicie|inicia|rodar|rode|open|launch|start|go to|take me to)\\s+(\\w+).*")) -> {
                val app = extractAppName(lower, listOf(
                    "abrir", "abra", "abre", "abri", "iniciar", "inicie", "inicia", "rodar", "rode",
                    "open", "launch", "start", "go to", "take me to"
                ))
                ParsedCommand(CommandType.OPEN_APP, target = app)
            }

            // ── LIGAR PARA CONTATO ─────────────────────────────────────
            norm.matches(Regex(".*(ligar|ligue|liga|telefonar|telefone|chamar|chama|call|dial|ring|phone)\\s+(.+)")) -> {
                val contact = extractAfter(lower, listOf(
                    "ligar para", "ligue para", "liga para", "ligar pro", "ligue pro", "liga pro",
                    "ligar pra", "ligue pra", "liga pra", "telefonar para", "telefone para",
                    "ligar", "ligue", "liga", "telefonar", "telefone", "chamar", "chama",
                    "call", "dial", "ring", "phone"
                ))
                ParsedCommand(
                    CommandType.CALL_CONTACT,
                    target = contact,
                    requiresConfirmation = true,
                    confirmationMessage = "Ligar para $contact?"
                )
            }

            // ── ALARME / LEMBRETE ──────────────────────────────────────
            norm.matches(Regex(".*(definir.*alarme|configurar.*alarme|arme.*alarme|me acorde|acordar|despertador|alarme|lembrete|me lembra|me lembrar|set.*alarm|wake me.*at|remind me.*at).*")) -> {
                ParsedCommand(
                    CommandType.SET_ALARM,
                    target = lower,
                    requiresConfirmation = true,
                    confirmationMessage = "Definir um alarme?"
                )
            }

            // ── BUSCAR NO YOUTUBE ──────────────────────────────────────
            norm.matches(Regex(".*(procurar|pesquisar|buscar|pesquisa|procura|busca|encontrar|assistir|ver|search|look up|google|find)\\s+(.+)(no youtube|no yt|youtube).*")) -> {
                val query = extractSearchQuery(lower)
                ParsedCommand(CommandType.SEARCH_YOUTUBE, target = query)
            }

            // ── BUSCAR NA WEB ──────────────────────────────────────────
            norm.matches(Regex(".*(procurar|pesquisar|buscar|pesquisa|procura|busca|encontrar|google|search|look up|find|browse)\\s+(.+)")) -> {
                val query = extractSearchQuery(lower)
                ParsedCommand(CommandType.SEARCH_WEB, target = query)
            }

            // ── ENVIAR MENSAGEM (WhatsApp) ─────────────────────────────
            norm.matches(Regex(".*(enviar|envie|envia|mandar|mande|manda|mensagem|zap|whatsapp|responder|responde|reply|send|message|text).*(para|pro|pra|p/|ao|to|for)\\s+(\\w+).*")) -> {
                val to = extractRecipient(lower)
                val msg = extractMessage(lower)
                ParsedCommand(
                    CommandType.SEND_WHATSAPP,
                    target = to,
                    extra = msg,
                    requiresConfirmation = true,
                    confirmationMessage = "Enviar \"$msg\" para $to no WhatsApp?"
                )
            }

            // ── LER NOTIFICAÇÕES / MENSAGENS ───────────────────────────
            norm.contains("notificac") || norm.contains("mensagem") || norm.contains("tem msg") ||
                    norm.contains("o que eu perdi") || norm.contains("ler minhas") ||
                    norm.contains("notifications") || norm.contains("any messages") ||
                    norm.contains("what did i miss") -> {
                ParsedCommand(CommandType.READ_NOTIFICATIONS)
            }

            // ── ABRIR CONFIGURAÇÕES ────────────────────────────────────
            norm.contains("configurac") || norm.contains("ajustes") || norm.contains("wi-fi") ||
                    norm.contains("wifi") || norm.contains("bluetooth") || norm.contains("settings") -> {
                ParsedCommand(CommandType.OPEN_SETTINGS)
            }

            else -> ParsedCommand(CommandType.UNKNOWN)
        }
    }

    private fun extractAppName(input: String, prefixes: List<String>): String {
        var result = input.trim()
        for (prefix in prefixes) {
            result = result.replace(Regex("\\b$prefix\\b"), "").trim()
        }
        // remove artigos iniciais (pt/en)
        result = result.replaceFirst(Regex("^(o|a|os|as|the)\\s+"), "").trim()
        return appAliases[result.trim()] ?: result.trim()
    }

    private fun extractAfter(input: String, words: List<String>): String {
        var result = input
        for (word in words.sortedByDescending { it.length }) {
            val idx = result.indexOf(word)
            if (idx >= 0) {
                result = result.substring(idx + word.length).trim()
                break
            }
        }
        // remove conectores iniciais (para/pro/pra/the)
        result = result.replaceFirst(Regex("^(para|pro|pra|p/|ao|a|o|the)\\s+"), "").trim()
        return result.trim()
    }

    private fun extractSearchQuery(input: String): String {
        return input
            .replace(Regex("(?i)\\b(procurar|pesquisar|buscar|pesquisa|procura|busca|encontrar|assistir|ver|search|look up|google|find|browse|no youtube|no yt|youtube|por|on youtube|on yt|for)\\b"), "")
            .replaceFirst(Regex("^(o|a|os|as|the)\\s+"), "")
            .trim()
    }

    private fun extractRecipient(input: String): String {
        val regex = Regex("(?i)(?:para o|para a|para|pro|pra|p/|ao|à|to|for)\\s+(\\w+)")
        return regex.find(input)?.groupValues?.get(1) ?: ""
    }

    private fun extractMessage(input: String): String {
        val patterns = listOf(
            Regex("(?i)(?:dizendo que|que diz que|que diz|que fala|falando|dizendo|mensagem|saying|message|text|that|tell them)\\s+[\"']?(.+)[\"']?$"),
            Regex("(?i)(?:responder|respond|reply)\\s+(?:para \\w+ |ao \\w+ |to \\w+ )?(?:com|with|dizendo|saying)?\\s+[\"']?(.+)[\"']?$")
        )
        for (p in patterns) {
            p.find(input)?.groupValues?.get(1)?.let { if (it.isNotBlank()) return it.trim() }
        }
        return input
    }

    private fun stripAccents(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD)
            .replace(Regex("[\\p{InCombiningDiacriticalMarks}]"), "")

    fun getPackageName(appAlias: String): String {
        return appAliases[appAlias.lowercase()] ?: appAlias
    }

    fun getLaunchIntent(packageNameOrAction: String): Intent? {
        return when {
            packageNameOrAction.startsWith("android.") ||
                    packageNameOrAction.startsWith("com.android.") -> {
                try {
                    Intent(packageNameOrAction).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                } catch (e: Exception) {
                    context.packageManager.getLaunchIntentForPackage(packageNameOrAction)
                }
            }
            else -> context.packageManager.getLaunchIntentForPackage(packageNameOrAction)
        }
    }
}
