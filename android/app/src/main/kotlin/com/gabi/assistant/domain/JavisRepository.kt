package com.gabi.assistant.domain

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.ContactsContract
import com.gabi.assistant.BuildConfig
import com.gabi.assistant.accessibility.JavisAccessibilityService
import com.gabi.assistant.ai.AiModelProvider
import com.gabi.assistant.ai.AiToolRouter
import com.gabi.assistant.ai.ClaudeProvider
import com.gabi.assistant.ai.CommandParser
import com.gabi.assistant.ai.CommandType
import com.gabi.assistant.ai.DeepSeekProvider
import com.gabi.assistant.ai.GroqProvider
import com.gabi.assistant.ai.JavisPersonality
import com.gabi.assistant.ai.ParsedCommand
import com.gabi.assistant.ai.RouteResult
import com.gabi.assistant.data.db.MessageDao
import com.gabi.assistant.data.model.AiMessage
import com.gabi.assistant.data.model.AiProvider
import com.gabi.assistant.data.model.Message
import com.gabi.assistant.data.model.MessageRole
import com.gabi.assistant.memory.MemoryManager
import com.gabi.assistant.notifications.JavisNotificationListenerService
import android.media.AudioManager
import com.gabi.assistant.bluetooth.BluetoothController
import com.gabi.assistant.reminder.ReminderScheduler
import com.gabi.assistant.skills.WeatherAgent
import com.gabi.assistant.storage.JavisPreferences
import com.gabi.assistant.util.ShareHelper
import com.gabi.assistant.voice.AndroidTtsFallback
import com.gabi.assistant.voice.ElevenLabsTts
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JavisRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val groqProvider: GroqProvider,
    private val deepSeekProvider: DeepSeekProvider,
    private val messageDao: MessageDao,
    private val memoryManager: MemoryManager,
    private val prefs: JavisPreferences,
    private val elevenLabsTts: ElevenLabsTts,
    private val androidTts: AndroidTtsFallback,
    private val commandParser: CommandParser,
    private val weatherAgent: WeatherAgent,
    private val claudeProvider: ClaudeProvider,
    private val reminderScheduler: ReminderScheduler,
    private val shareHelper: ShareHelper,
    private val bluetoothController: BluetoothController
) {
    val currentSessionId: String = UUID.randomUUID().toString()

    // Estado do aviso proativo pendente ("Quer que eu leia ou é segredo?") — enquanto o
    // app está aberto, guarda a última pergunta feita sozinha pra interpretar a próxima
    // fala do usuário como resposta a ela, antes de cair no roteamento normal.
    private var pendingProactivePackage: String? = null
    private var pendingProactiveText: String = ""
    private var pendingProactiveAt: Long = 0L

    companion object {
        private const val GMAIL_PACKAGE = "com.google.android.gm"
        private const val WHATSAPP_PACKAGE = "com.whatsapp"
        private const val PENDING_PROMPT_TIMEOUT_MS = 90_000L
    }

    fun getMessages(sessionId: String): Flow<List<Message>> =
        messageDao.getMessagesBySession(sessionId)

    /**
     * Fica ouvindo notificação nova de WhatsApp/Gmail enquanto o app está aberto (chamado
     * do ChatViewModel) e avisa a Gabi sozinha, se `proactiveNotifications` estiver ligado
     * em Ajustes. Não funciona com o app em segundo plano — é o escopo escolhido pra
     * primeira versão, evita precisar de um foreground service dedicado.
     */
    suspend fun watchProactiveNotifications(sessionId: String) {
        JavisNotificationListenerService.newNotification.collect { item ->
            if (!prefs.proactiveNotifications.first()) return@collect
            val label = when (item.packageName) {
                WHATSAPP_PACKAGE -> "zap"
                GMAIL_PACKAGE -> "email"
                else -> return@collect
            }
            pendingProactivePackage = item.packageName
            pendingProactiveText = item.text
            pendingProactiveAt = System.currentTimeMillis()
            val announcement = "Você recebeu um $label de ${item.title}. Quer que eu leia ou é segredo?"
            messageDao.insert(Message(sessionId = sessionId, role = MessageRole.ASSISTANT, content = announcement))
            speak(announcement)
        }
    }

    /** Se houver um aviso proativo pendente, interpreta a fala atual como resposta a ele. */
    private suspend fun resolvePendingProactivePrompt(userText: String, sessionId: String): Result<String>? {
        if (pendingProactivePackage == null) return null
        val expired = System.currentTimeMillis() - pendingProactiveAt > PENDING_PROMPT_TIMEOUT_MS
        pendingProactivePackage = null
        if (expired) return null

        val norm = java.text.Normalizer.normalize(userText.lowercase(), java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        val wantsRead = listOf("le", "leia", "ler", "sim", "quero", "pode", "manda", "claro").any { norm.contains(it) }
        val wantsSecret = listOf("segredo", "nao", "depois", "deixa", "guarda").any { norm.contains(it) }
        if (!wantsRead && !wantsSecret) return null

        val reply = if (wantsRead) {
            pendingProactiveText.ifBlank { "Só veio o aviso mesmo, sem prévia de texto." }
        } else {
            "Combinado, fica em segredo."
        }
        messageDao.insert(Message(sessionId = sessionId, role = MessageRole.ASSISTANT, content = reply))
        speak(reply)
        return Result.success(reply)
    }

    suspend fun sendMessage(userText: String, sessionId: String): Result<String> {
        messageDao.insert(Message(sessionId = sessionId, role = MessageRole.USER, content = userText))
        resolvePendingProactivePrompt(userText, sessionId)?.let { return it }
        memoryManager.learnFromConversation(userText)

        val recentMessages = messageDao.getRecentMessages(20)
        val contextSummary = memoryManager.buildContextSummary()
        val systemPrompt = buildString {
            append(JavisPersonality.SYSTEM_PROMPT)
            if (contextSummary.isNotBlank()) {
                append("\n\nO que você sabe sobre este usuário: $contextSummary")
            }
        }
        val aiMessages = recentMessages
            .filter { it.role != MessageRole.SYSTEM }
            .takeLast(16)
            .map { AiMessage(it.role.name.lowercase(), it.content) }

        val providerPref: AiProvider = prefs.aiProvider.first()
        // Chave própria do usuário (Ajustes) tem prioridade; senão usa a nativa do app.
        groqProvider.apiKey = prefs.groqApiKey.first().ifBlank { BuildConfig.GROQ_API_KEY }
        deepSeekProvider.apiKey = prefs.deepSeekApiKey.first()
        val primary: AiModelProvider = if (providerPref == AiProvider.GROQ) groqProvider else deepSeekProvider
        val fallback: AiModelProvider = if (providerPref == AiProvider.GROQ) deepSeekProvider else groqProvider

        // Roteamento: deixa a própria IA escolher a ferramenta certa (function calling),
        // em vez do CommandParser por regex. Se não houver IA disponível (sem chave/sem
        // internet), cai pro parser offline — mesma robustez de antes.
        val route = routeWithAi(aiMessages, systemPrompt, providerPref)

        val result: Result<String> = if (route is RouteResult.DirectReply) {
            Result.success(route.text)
        } else {
            val parsed: ParsedCommand = (route as? RouteResult.Command)?.parsed
                ?: commandParser.parse(userText)

            if (parsed.type != CommandType.UNKNOWN && !parsed.requiresConfirmation) {
                executeCommand(parsed)
            }

            // Skills que produzem a própria resposta (clima, Claude, lembrete) — pulam a IA
            val skillReply: String? = resolveSkillReply(parsed)

            if (skillReply != null) {
                Result.success(skillReply)
            } else {
                var r = primary.chat(aiMessages, systemPrompt)
                if (r.isFailure) r = fallback.chat(aiMessages, systemPrompt)
                r
            }
        }

        result.onSuccess { reply ->
            messageDao.insert(
                Message(sessionId = sessionId, role = MessageRole.ASSISTANT, content = reply)
            )
            speak(reply)
        }

        return result
    }

    /** Tenta rotear via IA (function calling); retorna null se nenhum provedor respondeu. */
    private suspend fun routeWithAi(
        aiMessages: List<AiMessage>,
        systemPrompt: String,
        providerPref: AiProvider
    ): RouteResult? {
        val routingPrompt = "$systemPrompt\n\n${AiToolRouter.currentDateTimeContext()}"
        var toolResult = if (providerPref == AiProvider.GROQ) {
            groqProvider.chatWithTools(aiMessages, routingPrompt)
        } else {
            deepSeekProvider.chatWithTools(aiMessages, routingPrompt)
        }
        if (toolResult.isFailure) {
            toolResult = if (providerPref == AiProvider.GROQ) {
                deepSeekProvider.chatWithTools(aiMessages, routingPrompt)
            } else {
                groqProvider.chatWithTools(aiMessages, routingPrompt)
            }
        }
        val route = AiToolRouter.route(toolResult)
        return if (route == RouteResult.Failed) null else route
    }

    /** Skills que devolvem uma resposta direta (pulam o Groq). null = deixa pro Groq. */
    private suspend fun resolveSkillReply(parsed: ParsedCommand): String? = when (parsed.type) {
        CommandType.WEATHER -> weatherAgent.getWeatherResponse(parsed.target)

        CommandType.SET_REMINDER -> {
            val triggerAt = parsed.extra.toLongOrNull() ?: return null
            reminderScheduler.schedule(parsed.target, triggerAt)
            "Pronto! Vou te lembrar de ${parsed.target} às ${formatReminderTime(triggerAt)}."
        }

        CommandType.ASK_CLAUDE -> {
            val key = prefs.anthropicApiKey.first()
            if (key.isBlank()) {
                "Para falar com o Claude, adiciona a Chave da API Anthropic lá em Ajustes."
            } else {
                try {
                    claudeProvider.apiKey = key
                    "O Claude respondeu: ${claudeProvider.ask(parsed.target)}"
                } catch (e: Exception) {
                    "Não consegui falar com o Claude agora. Tenta de novo."
                }
            }
        }

        CommandType.SHARE -> {
            val toShare = parsed.target.ifBlank {
                messageDao.getRecentMessages(8).firstOrNull { it.role == MessageRole.ASSISTANT }?.content ?: ""
            }
            if (toShare.isNotBlank()) {
                shareHelper.shareText(toShare)
                "Abrindo pra compartilhar."
            } else {
                "Ainda não tenho nada pra compartilhar."
            }
        }

        CommandType.BLUETOOTH_CONNECT -> {
            bluetoothController.requestEnable()
            val am = context.getSystemService(AudioManager::class.java)
            if (bluetoothController.isEnabled()) {
                bluetoothController.routeAudioToBluetooth(am)
                "Pronto! Bluetooth conectado. O som da Gabi vai sair no seu fone ou caixa agora."
            } else {
                "Não consegui ligar o Bluetooth. Abri as configurações pra você ativar."
            }
        }

        CommandType.READ_CALENDAR -> readUpcomingEvents()

        CommandType.CREATE_CALENDAR_EVENT -> {
            val startAt = parsed.extra.toLongOrNull() ?: return null
            createCalendarEvent(parsed.target, startAt)
        }

        CommandType.READ_GMAIL -> readGmailPreview()

        CommandType.READ_NOTIFICATIONS -> readNotifications()

        else -> null
    }

    private val _isSpeaking = MutableStateFlow(false)
    /** Se a Gabi está falando agora (qualquer motor, ElevenLabs ou nativo) — usado pra
     *  animar o Orb/personagem em tempo real, não é só um getter que precisa ser sondado. */
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    suspend fun speak(text: String) {
        _isSpeaking.value = true
        try {
            val useElevenLabs = prefs.useElevenLabs.first()
            val elKey = prefs.elevenLabsApiKey.first()

            if (useElevenLabs && elKey.isNotBlank()) {
                elevenLabsTts.apiKey = elKey
                elevenLabsTts.voiceId = prefs.elevenLabsVoiceId.first()
                    .ifBlank { ElevenLabsTts.DEFAULT_VOICE_ID }
                elevenLabsTts.speak(text)
            } else {
                androidTts.speechRate = prefs.speechRate.first()
                androidTts.pitch = prefs.ttsPitch.first()
                androidTts.speak(text)
            }
        } finally {
            _isSpeaking.value = false
        }
    }

    fun stopSpeaking() {
        elevenLabsTts.stopSpeaking()
        androidTts.stop()
    }

    fun executeCommand(cmd: ParsedCommand) {
        when (cmd.type) {
            CommandType.OPEN_APP -> launchApp(cmd.target)
            CommandType.SEARCH_WEB -> searchWeb(cmd.target)
            CommandType.SEARCH_YOUTUBE -> searchYoutube(cmd.target)
            CommandType.OPEN_SETTINGS -> openSettings()
            CommandType.TAKE_PHOTO -> openCamera()
            CommandType.OPEN_WHATSAPP_CHAT -> openWhatsAppChat(cmd.target)
            else -> {}
        }
    }

    suspend fun executeConfirmedCommand(cmd: ParsedCommand) {
        when (cmd.type) {
            CommandType.CALL_CONTACT -> callContact(cmd.target)
            CommandType.CALL_NUMBER -> callNumber(cmd.target)
            CommandType.SET_ALARM -> setAlarm(cmd.target)
            CommandType.SEND_WHATSAPP -> sendWhatsAppMessage(cmd.target, cmd.extra)
            else -> executeCommand(cmd)
        }
    }

    fun launchApp(packageNameOrAlias: String) {
        val pkg = commandParser.getPackageName(packageNameOrAlias)
        val intent = commandParser.getLaunchIntent(pkg)
            ?: Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setPackage(pkg)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try { context.startActivity(intent) } catch (_: Exception) {}
    }

    fun searchWeb(query: String) {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try { context.startActivity(intent) } catch (_: Exception) {}
    }

    fun searchYoutube(query: String) {
        val ytIntent = Intent(Intent.ACTION_SEARCH).apply {
            setPackage("com.google.android.youtube")
            putExtra("query", query)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(ytIntent)
        } catch (_: Exception) {
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try { context.startActivity(webIntent) } catch (_: Exception) {}
        }
    }

    fun openSettings() {
        val intent = Intent(android.provider.Settings.ACTION_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try { context.startActivity(intent) } catch (_: Exception) {}
    }

    fun openCamera() {
        val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try { context.startActivity(intent) } catch (_: Exception) {}
    }

    fun openWhatsAppChat(contactName: String) {
        launchApp("com.whatsapp")
    }

    suspend fun sendWhatsAppMessage(contact: String, message: String): Boolean {
        launchApp("com.whatsapp")
        kotlinx.coroutines.delay(2000)
        val acc = JavisAccessibilityService.getInstance() ?: return false
        val opened = acc.openWhatsAppChatByName(contact)
        if (!opened) return false
        kotlinx.coroutines.delay(1500)
        return acc.whatsAppSendMessage(message)
    }

    fun callContact(name: String) {
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            ),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
            arrayOf("%$name%"),
            null
        )
        cursor?.use {
            if (it.moveToFirst()) {
                val number = it.getString(0)
                val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try { context.startActivity(callIntent) } catch (_: Exception) {}
                return
            }
        }
        val fallback = Intent(Intent.ACTION_DIAL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try { context.startActivity(fallback) } catch (_: Exception) {}
    }

    fun callNumber(number: String) {
        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try { context.startActivity(intent) } catch (_: Exception) {}
    }

    fun setAlarm(description: String) {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(AlarmClock.EXTRA_MESSAGE, "Alarme da Gabi")
        }
        try { context.startActivity(intent) } catch (_: Exception) {}
    }

    fun readNotifications(): String {
        val service = JavisNotificationListenerService.getInstance()
            ?: return "O acesso às notificações não está ativado. Vá nas Configurações para permitir."
        val items = service.getFilteredNotifications()
        if (items.isEmpty()) return "Você não tem notificações ativas agora."
        return items.take(10).joinToString("\n") { "De ${it.appName}: ${it.title} — ${it.text}" }
    }

    /** Prévia de emails novos — lê a notificação do Gmail (remetente/assunto/trecho), não o corpo completo. */
    fun readGmailPreview(): String {
        val service = JavisNotificationListenerService.getInstance()
            ?: return "O acesso às notificações não está ativado. Vá nas Configurações para permitir, aí eu consigo avisar quando chegar email."
        val emails = service.getFilteredNotifications().filter { it.packageName == GMAIL_PACKAGE }
        if (emails.isEmpty()) return "Não tem nenhum email novo agora."
        val list = emails.take(5).joinToString("\n") { "${it.title} — ${it.text}" }
        return "Você tem ${emails.size} email${if (emails.size > 1) "s" else ""} novo${if (emails.size > 1) "s" else ""}:\n$list"
    }

    /** Lê os próximos compromissos da agenda do sistema (todos os calendários visíveis). */
    fun readUpcomingEvents(days: Int = 7): String {
        return try {
            val projection = arrayOf(
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN
            )
            val now = System.currentTimeMillis()
            val end = now + days * 24L * 60 * 60 * 1000
            val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().apply {
                ContentUris.appendId(this, now)
                ContentUris.appendId(this, end)
            }.build()

            val events = mutableListOf<Pair<String, Long>>()
            context.contentResolver.query(uri, projection, null, null, "${CalendarContract.Instances.BEGIN} ASC")
                ?.use { cursor ->
                    while (cursor.moveToNext()) {
                        val title = cursor.getString(0) ?: "(sem título)"
                        val begin = cursor.getLong(1)
                        events.add(title to begin)
                    }
                }

            if (events.isEmpty()) return "Você não tem nenhum compromisso agendado pros próximos $days dias."
            val fmt = SimpleDateFormat("EEEE, dd/MM 'às' HH:mm", Locale("pt", "BR"))
            val list = events.take(10).joinToString("\n") { (title, begin) -> "$title — ${fmt.format(Date(begin))}" }
            "Seus próximos compromissos:\n$list"
        } catch (e: SecurityException) {
            "Preciso da permissão de Agenda pra ler seus compromissos. Vá em Ajustes > Permissões pra liberar."
        } catch (e: Exception) {
            "Não consegui acessar a agenda agora."
        }
    }

    /** Cria um evento de verdade na agenda do usuário (não é um lembrete interno do app). */
    fun createCalendarEvent(title: String, startAtMillis: Long, durationMinutes: Int = 60): String {
        return try {
            val calendarId = defaultWritableCalendarId()
                ?: return "Não achei nenhuma agenda configurada no aparelho pra criar o compromisso."

            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.TITLE, title.ifBlank { "Compromisso" })
                put(CalendarContract.Events.DTSTART, startAtMillis)
                put(CalendarContract.Events.DTEND, startAtMillis + durationMinutes * 60 * 1000L)
                put(CalendarContract.Events.EVENT_TIMEZONE, java.util.TimeZone.getDefault().id)
            }
            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            if (uri == null) {
                "Não consegui criar o compromisso na agenda."
            } else {
                "Pronto! Criei \"$title\" na sua agenda pra ${formatReminderTime(startAtMillis)}."
            }
        } catch (e: SecurityException) {
            "Preciso da permissão de Agenda pra criar compromissos. Vá em Ajustes > Permissões pra liberar."
        } catch (e: Exception) {
            "Não consegui criar o compromisso agora."
        }
    }

    private fun defaultWritableCalendarId(): Long? {
        val projection = arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL)
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI, projection,
            "${CalendarContract.Calendars.VISIBLE} = 1 AND ${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?",
            arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString()),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getLong(0)
        }
        return null
    }

    suspend fun getGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val userName = memoryManager.getUserName()
        val name = if (userName != null) ", $userName" else ""
        return when (hour) {
            in 5..11 -> "Bom dia$name. Aqui é a Gabi. Como posso ajudar?"
            in 12..16 -> "Boa tarde$name. A Gabi está pronta. O que você precisa?"
            in 17..20 -> "Boa noite$name. A Gabi à disposição."
            else -> "Oi$name, já é tarde. Mas a Gabi tá aqui — o que houve?"
        }
    }

    /** Mostra a saudação da Gabi como mensagem da assistente, sem chamar a IA. */
    suspend fun greet(sessionId: String) {
        val greeting = getGreeting()
        messageDao.insert(
            Message(sessionId = sessionId, role = MessageRole.ASSISTANT, content = greeting)
        )
        speak(greeting)
    }

    private fun formatReminderTime(millis: Long): String {
        val now = Calendar.getInstance()
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        val hm = SimpleDateFormat("HH:mm", Locale("pt", "BR")).format(Date(millis))
        val today = now.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)
        val tomorrowCal = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        val isTomorrow = cal.get(Calendar.YEAR) == tomorrowCal.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == tomorrowCal.get(Calendar.DAY_OF_YEAR)
        val day = when {
            today -> "hoje"
            isTomorrow -> "amanhã"
            else -> SimpleDateFormat("dd/MM", Locale("pt", "BR")).format(Date(millis))
        }
        return "$day às $hm"
    }

    suspend fun clearHistory() = messageDao.deleteAll()
}
