package com.gabi.assistant.ai

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import com.gabi.assistant.accessibility.JavisAccessibilityService
import com.gabi.assistant.utils.isPackageInstalled
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.Normalizer
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

enum class CommandType {
    OPEN_APP, CALL_CONTACT, CALL_NUMBER, SET_ALARM, SEARCH_WEB,
    SEND_WHATSAPP, READ_NOTIFICATIONS, OPEN_SETTINGS, TAKE_PHOTO,
    SEARCH_YOUTUBE, SEARCH_SPOTIFY, OPEN_WHATSAPP_CHAT,
    WEATHER, ASK_CLAUDE, SET_REMINDER, SHARE, BLUETOOTH_CONNECT,
    READ_CALENDAR, CREATE_CALENDAR_EVENT, READ_GMAIL, SAVE_MEMORY,
    PAUSE_MEDIA,
    UNKNOWN
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

        // ── Orquestração Claude ("fale com o Claude e...", "pergunte ao Claude...") ──
        if (isClaudeRequest(norm)) {
            return ParsedCommand(CommandType.ASK_CLAUDE, target = extractClaudeQuery(lower))
        }

        // ── Lembrete ("me lembre de X às Yh" / "em 30 minutos") ──
        parseReminder(lower, norm)?.let { (subject, triggerAt) ->
            return ParsedCommand(
                CommandType.SET_REMINDER,
                target = subject,
                extra = triggerAt.toString()
            )
        }

        // ── Clima ──
        if (isWeatherRequest(norm)) {
            return ParsedCommand(CommandType.WEATHER, target = extractCity(lower))
        }

        // ── Criar compromisso na agenda ("marcar reunião amanhã às 15h") ──
        parseCalendarEvent(lower, norm)?.let { (title, startAt) ->
            return ParsedCommand(
                CommandType.CREATE_CALENDAR_EVENT,
                target = title,
                extra = startAt.toString()
            )
        }

        // ── Ler agenda ──
        if (isReadCalendarRequest(norm)) {
            return ParsedCommand(CommandType.READ_CALENDAR)
        }

        // ── Ler Gmail ──
        if (isReadGmailRequest(norm)) {
            return ParsedCommand(CommandType.READ_GMAIL)
        }

        // ── Compartilhar (ACTION_SEND) ──
        if (norm.contains("compartilh") || norm.contains("share")) {
            return ParsedCommand(CommandType.SHARE, target = extractShareText(lower))
        }

        // ── Bluetooth (conectar / rotear áudio) ──
        if (isBluetoothConnect(norm)) {
            return ParsedCommand(CommandType.BLUETOOTH_CONNECT)
        }

        // ── Pausar música ("pausa a musica", "para a musica", "pause a music", "stop a musica") ──
        if (norm.matches(Regex(".*\\b(pausa|pause|para|parar|stop)\\b.*\\b(musica|som|spotify|cancao|music)\\b.*"))) {
            return ParsedCommand(CommandType.PAUSE_MEDIA)
        }

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

            // ── ALARME ─────────────────────────────────────────────────
            norm.matches(Regex(".*(definir.*alarme|configurar.*alarme|arme.*alarme|me acorde|acordar|despertador|alarme|set.*alarm|wake me.*at|remind me.*at).*")) -> {
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

    // ──────────────────────────────────────────────────────────────────
    //  CLAUDE
    // ──────────────────────────────────────────────────────────────────
    private fun isClaudeRequest(norm: String): Boolean = norm.contains("claude")

    private fun extractClaudeQuery(lower: String): String {
        val norm = stripAccents(lower)
        var q = norm
        listOf(
            "fale com o claude e", "fale com o claude", "fale com claude",
            "pergunte ao claude", "pergunte pro claude", "pergunte para o claude",
            "entre em contato com o claude e", "entre em contato com o claude",
            "consulte o claude", "consultar o claude", "consulta o claude",
            "pede pro claude", "pede para o claude", "peca ao claude", "peca para o claude",
            "com o claude e", "com o claude", "ao claude", "pro claude", "para o claude",
            "claude"
        ).sortedByDescending { it.length }.forEach { q = q.replace(it, " ", ignoreCase = true) }
        q = q.replace(Regex("^\\s*(e|de|que|para|pra|se)\\s+", RegexOption.IGNORE_CASE), "").trim()
        return q.ifBlank { lower.trim() }
    }

    // ──────────────────────────────────────────────────────────────────
    //  CLIMA
    // ──────────────────────────────────────────────────────────────────
    private fun isWeatherRequest(norm: String): Boolean {
        if (norm.contains("clima") || norm.contains("previsao") || norm.contains("temperatura") ||
            norm.contains("vai chover") || norm.contains("chuva hoje")) return true
        // "tempo" só conta se houver contexto de clima
        if (norm.contains("tempo") && (norm.contains("agora") || norm.contains("hoje") ||
                    norm.contains("em ") || norm.contains("voa"))) return true
        return false
    }

    private fun extractCity(lower: String): String {
        val norm = stripAccents(lower)
        Regex("(?:em|no|na|de|para|do|da)\\s+([a-z\\s]{3,40})").find(norm)?.let {
            return it.groupValues[1].trim().split(Regex("\\s+(hoje|amanha|agora|e|com)\\b")).first().trim()
        }
        return ""
    }

    private fun isBluetoothConnect(norm: String): Boolean {
        if (!norm.contains("bluetooth")) return false
        return norm.matches(Regex(".*(conectar|conecte|ligar|ligue|ativar|ative|fones?|caixa|som|alto falante|speaker|audio|auricular).*"))
    }

    private fun extractShareText(lower: String): String {
        var s = lower
        listOf("compartilhar isso", "compartilhar", "compartilha", "compartilhe", "share")
            .sortedByDescending { it.length }
            .forEach { s = s.replace(it, " ", ignoreCase = true) }
        s = s.replaceFirst(Regex("^\\s*(o|a|isso|este|esta|que|e)\\s+", RegexOption.IGNORE_CASE), "").trim()
        return s
    }

    // ──────────────────────────────────────────────────────────────────
    //  LEMBRETE  → (assunto, millis) ou null
    // ──────────────────────────────────────────────────────────────────
    private fun parseReminder(lower: String, norm: String): Pair<String, Long>? {
        val triggers = listOf(
            "me lembre", "me lembra", "lembre me", "lembre-me", "me avise", "me avisar",
            "avise me", "me lembrar", "novo lembrete", "crie um lembrete", "criar lembrete", "lembrete"
        )
        if (triggers.none { norm.contains(it) }) return null
        if (norm.contains("alarme")) return null // é alarme, não lembrete
        val triggerAt = extractDateTime(norm) ?: return null
        return cleanSubject(lower, triggers, fallback = "Lembrete") to triggerAt
    }

    // ──────────────────────────────────────────────────────────────────
    //  COMPROMISSO NA AGENDA → (título, millis) ou null
    // ──────────────────────────────────────────────────────────────────
    private fun parseCalendarEvent(lower: String, norm: String): Pair<String, Long>? {
        val triggers = listOf(
            "marcar um compromisso", "marca um compromisso", "criar um compromisso",
            "criar evento", "crie um evento", "novo evento", "marcar na agenda",
            "marca na agenda", "agendar reuniao", "agende uma reuniao", "compromisso", "reuniao"
        )
        if (triggers.none { norm.contains(it) }) return null
        val triggerAt = extractDateTime(norm) ?: return null
        return cleanSubject(lower, triggers, fallback = "Compromisso") to triggerAt
    }

    /** Extrai um horário-alvo (relativo ou "às HH:MM") do texto normalizado, ou null se não achar. */
    private fun extractDateTime(norm: String): Long? {
        val now = Calendar.getInstance()
        val cal = (now.clone() as Calendar)

        // relativo: "em N (minuto|hora...)" ou "daqui a N ..."
        val rel = Regex("(?:em|daqui a|em cerca de)\\s+(\\d+)\\s*(minuto|minutos|min|hora|horas|h|hr|hrs)")
            .find(norm)
        if (rel != null) {
            val n = rel.groupValues[1].toIntOrNull() ?: return null
            val unit = rel.groupValues[2]
            val mins = if (unit.startsWith("hora") || unit == "h" || unit.startsWith("hr")) n * 60 else n
            cal.add(Calendar.MINUTE, mins)
            return cal.timeInMillis
        }

        // horário: "às HH(:MM)?h?"
        val clk = Regex("[aàá]s\\s*(\\d{1,2})(?:h|hs|:)?(?:(\\d{2}))?").find(norm)
        if (clk != null) {
            val h = clk.groupValues[1].toIntOrNull()?.coerceIn(0, 23) ?: return null
            val mm = clk.groupValues[2].takeIf { it.isNotBlank() }?.toIntOrNull()?.coerceIn(0, 59) ?: 0
            cal.set(Calendar.HOUR_OF_DAY, h)
            cal.set(Calendar.MINUTE, mm)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            if (norm.contains("depois de amanha")) cal.add(Calendar.DAY_OF_YEAR, 2)
            else if (norm.contains("amanha")) cal.add(Calendar.DAY_OF_YEAR, 1)
            if (cal.timeInMillis <= now.timeInMillis) cal.add(Calendar.DAY_OF_YEAR, 1)
            return cal.timeInMillis
        }

        return null
    }

    /** Remove gatilhos e marcas de tempo do texto original, sobra só o "assunto". */
    private fun cleanSubject(lower: String, triggers: List<String>, fallback: String): String {
        var subject = lower
        triggers.sortedByDescending { it.length }.forEach {
            subject = subject.replace(it, " ", ignoreCase = true)
        }
        subject = subject
            .replace(Regex("[aàá]s\\s*\\d{1,2}(?:h|hs|:)?(?:(\\d{2}))?", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("\\bhoje\\b", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("\\bamanh[ãa]\\b", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("depois de amanh[ãa]", RegexOption.IGNORE_CASE), " ")
            .replace(
                Regex("(?:em|daqui a|em cerca de)\\s+\\d+\\s*(?:minuto|minutos|min|hora|horas|h|hr|hrs)",
                    RegexOption.IGNORE_CASE), " "
            )
            .replace(Regex("[,;:\\.]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        subject = subject.replaceFirst(Regex("^\\s*(de|que|para|pra|e|se)\\s+", RegexOption.IGNORE_CASE), "").trim()
        return subject.ifBlank { fallback }
    }

    // ──────────────────────────────────────────────────────────────────
    //  LER AGENDA / LER GMAIL
    // ──────────────────────────────────────────────────────────────────
    private fun isReadCalendarRequest(norm: String): Boolean {
        val triggers = listOf(
            "minha agenda", "meus compromissos", "o que eu tenho hoje", "o que tenho hoje",
            "o que eu tenho amanha", "tenho algum compromisso", "tem algum compromisso",
            "ler minha agenda", "ver minha agenda", "meus proximos compromissos"
        )
        return triggers.any { norm.contains(it) }
    }

    private fun isReadGmailRequest(norm: String): Boolean {
        if (!(norm.contains("gmail") || norm.contains("email") || norm.contains("e-mail"))) return false
        return norm.matches(Regex(".*(tem|chegou|ler|leia|novo|novos|verificar|confere|checar).*"))
    }

    private fun extractAppName(input: String, prefixes: List<String>): String {
        var result = input.trim()
        for (prefix in prefixes) {
            result = result.replace(Regex("\\b$prefix\\b"), "").trim()
        }
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
        val pkg = appAliases[appAlias.lowercase()] ?: appAlias
        return if (pkg == JavisAccessibilityService.WA_PACKAGE) resolveWhatsAppPackage() else pkg
    }

    /**
     * "whatsapp"/"zap" sempre apontava fixo pra com.whatsapp — quem só tem o WhatsApp
     * Business instalado (ou usa ele como principal) nunca conseguia nem abrir o app
     * por voz. Prefere o WhatsApp normal quando os dois estão instalados (é o mais comum),
     * cai pro Business se só ele existir, e mantém o normal como último fallback.
     */
    fun resolveWhatsAppPackage(): String = when {
        context.isPackageInstalled(JavisAccessibilityService.WA_PACKAGE) -> JavisAccessibilityService.WA_PACKAGE
        context.isPackageInstalled(JavisAccessibilityService.WA_PACKAGE_BUSINESS) -> JavisAccessibilityService.WA_PACKAGE_BUSINESS
        else -> JavisAccessibilityService.WA_PACKAGE
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
