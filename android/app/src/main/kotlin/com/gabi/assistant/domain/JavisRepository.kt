package com.gabi.assistant.domain

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.util.Log
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
import com.gabi.assistant.data.model.MemoryType
import com.gabi.assistant.data.model.MessageRole
import com.gabi.assistant.memory.MemoryManager
import com.gabi.assistant.notifications.JavisNotificationListenerService
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import com.gabi.assistant.bluetooth.BluetoothController
import com.gabi.assistant.reminder.ReminderScheduler
import com.gabi.assistant.skills.SpotifyClient
import com.gabi.assistant.skills.WeatherAgent
import com.gabi.assistant.storage.JavisPreferences
import com.gabi.assistant.util.ShareHelper
import com.gabi.assistant.voice.AndroidTtsFallback
import com.gabi.assistant.voice.ElevenLabsTts
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resultado de [JavisRepository.sendMessage]: a resposta em si, e opcionalmente um
 * comando pendente de confirmação (ligar/mandar WhatsApp/alarme) — nesse caso [reply]
 * já É a pergunta de confirmação, não uma resposta de conversa normal.
 */
data class SendMessageOutcome(
    val reply: String,
    val pendingCommand: ParsedCommand? = null
)

enum class WhatsAppSendResult { Success, AccessibilityDisabled, ContactNotFound, SendFailed }

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
    private val bluetoothController: BluetoothController,
    private val spotifyClient: SpotifyClient
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
        // Fase 0 do plano de latência (alexa-referencia/PLANO_DE_ALTERACOES.md) — só
        // medição, filtrar no logcat com `adb logcat -s GabiLatency`.
        private const val LATENCY_TAG = "GabiLatency"
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
    private suspend fun resolvePendingProactivePrompt(userText: String, sessionId: String): Result<SendMessageOutcome>? {
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
        return Result.success(SendMessageOutcome(reply))
    }

    /**
     * Insere e fala uma mensagem DA GABI diretamente (status de ação, confirmação
     * recusada, etc.) — ao contrário de [sendMessage], não trata o texto como fala do
     * usuário nem passa pelo roteamento de novo.
     */
    suspend fun sayAsAssistant(text: String, sessionId: String) {
        messageDao.insert(Message(sessionId = sessionId, role = MessageRole.ASSISTANT, content = text))
        speak(text)
    }

    suspend fun sendMessage(userText: String, sessionId: String): Result<SendMessageOutcome> {
        Log.d(LATENCY_TAG, "T2 sendMessage start t=${SystemClock.elapsedRealtime()}")
        messageDao.insert(Message(sessionId = sessionId, role = MessageRole.USER, content = userText))
        resolvePendingProactivePrompt(userText, sessionId)?.let { return it }

        val recentMessages = messageDao.getRecentMessages(sessionId, 20)
        val contextSummary = memoryManager.buildContextSummary()
        val systemPrompt = buildString {
            append(JavisPersonality.SYSTEM_PROMPT)
            if (contextSummary.isNotBlank()) {
                append("\n\nO que você sabe sobre este usuário: $contextSummary")
            }
        }
        // getRecentMessages vem em ORDER BY timestamp DESC (mais nova primeiro, de
        // propósito — outros usos, tipo achar a última resposta pra compartilhar,
        // dependem disso). Pra API de chat, que espera ordem cronológica com a
        // mensagem mais recente por ÚLTIMO, precisa pegar as 16 mais novas (.take,
        // não .takeLast, já que a lista é DESC) e inverter. Sem isso, a IA recebia a
        // conversa de trás pra frente conforme crescia — respondendo a perguntas
        // antigas em vez da mensagem atual (bug real, reproduzido em 21/08/2026).
        val aiMessages = recentMessages
            .filter { it.role != MessageRole.SYSTEM }
            .take(16)
            .reversed()
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
        Log.d(LATENCY_TAG, "T3 routeWithAi end t=${SystemClock.elapsedRealtime()} route=${route?.javaClass?.simpleName}")

        val result: Result<SendMessageOutcome> = if (route is RouteResult.DirectReply) {
            Result.success(SendMessageOutcome(route.text))
        } else {
            val parsed: ParsedCommand = (route as? RouteResult.Command)?.parsed
                ?: commandParser.parse(userText)

            if (parsed.requiresConfirmation) {
                // Não executa nem chama a IA de novo — a resposta JÁ é a pergunta de
                // confirmação, e quem chamou (a UI) recebe o comando pendente pra
                // mostrar os botões Sim/Não e decidir depois via executeConfirmedCommand.
                val question = parsed.confirmationMessage.ifBlank { "Quer que eu faça isso?" }
                Result.success(SendMessageOutcome(question, pendingCommand = parsed))
            } else {
                if (parsed.type != CommandType.UNKNOWN) executeCommand(parsed)

                // Skills que produzem a própria resposta (clima, Claude, lembrete) — pulam a IA
                val skillReply: String? = resolveSkillReply(parsed, sessionId)

                if (skillReply != null) {
                    Result.success(SendMessageOutcome(skillReply))
                } else {
                    var r = primary.chat(aiMessages, systemPrompt)
                    if (r.isFailure) {
                        Log.d(LATENCY_TAG, "fallback para ${fallback.name} (primary=${primary.name} falhou) t=${SystemClock.elapsedRealtime()}")
                        r = fallback.chat(aiMessages, systemPrompt)
                    }
                    Log.d(LATENCY_TAG, "T4 primary/fallback.chat end t=${SystemClock.elapsedRealtime()}")
                    r.map { SendMessageOutcome(it) }
                }
            }
        }

        result.onSuccess { outcome ->
            messageDao.insert(
                Message(sessionId = sessionId, role = MessageRole.ASSISTANT, content = outcome.reply)
            )
            speak(outcome.reply)
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
            Log.d(LATENCY_TAG, "routeWithAi: provedor primário falhou (${toolResult.exceptionOrNull()?.javaClass?.simpleName}: ${toolResult.exceptionOrNull()?.message}), tentando o outro t=${SystemClock.elapsedRealtime()}")
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
    private suspend fun resolveSkillReply(parsed: ParsedCommand, sessionId: String): String? = when (parsed.type) {
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
                messageDao.getRecentMessages(sessionId, 8).firstOrNull { it.role == MessageRole.ASSISTANT }?.content ?: ""
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
            when {
                !bluetoothController.isEnabled() ->
                    "Não consegui ligar o Bluetooth. Abri as configurações pra você ativar."
                // Já tem algo pareado — caminho rápido de sempre, sem gastar ~15s
                // escaneando à toa.
                bluetoothController.bondedDeviceNames().isNotEmpty() -> {
                    bluetoothController.routeAudioToBluetooth(am)
                    "Pronto! Bluetooth conectado. O som da Gabi vai sair no seu fone ou caixa agora."
                }
                // Nada pareado ainda — antes disso a Gabi só sabia rotear pro que já
                // estava pareado, nunca procurava aparelho novo de verdade (limitação
                // conhecida, reportada pelo usuário). Agora busca de verdade
                // (`startDiscovery`) e abre a tela nativa de parear — parear
                // programaticamente pede confirmação de PIN/tela do sistema de
                // qualquer jeito, então é mais confiável deixar o usuário tocar.
                else -> {
                    val found = bluetoothController.discoverDevices()
                    bluetoothController.openBluetoothSettings()
                    if (found.isEmpty()) {
                        "Não achei nenhum aparelho Bluetooth novo por perto. Abri as configurações pra você tentar de novo."
                    } else {
                        "Achei ${found.joinToString(", ")} por perto. Abri as configurações de Bluetooth pra você parear."
                    }
                }
            }
        }

        CommandType.READ_CALENDAR -> readUpcomingEvents()

        CommandType.CREATE_CALENDAR_EVENT -> {
            val startAt = parsed.extra.toLongOrNull() ?: return null
            createCalendarEvent(parsed.target, startAt)
        }

        CommandType.READ_GMAIL -> readGmailPreview()

        CommandType.PAUSE_MEDIA -> pauseMedia()

        CommandType.READ_NOTIFICATIONS -> readNotifications()

        CommandType.SAVE_MEMORY -> {
            val parts = parsed.extra.split("|", limit = 2)
            val type = parts.getOrNull(0)?.let {
                try { MemoryType.valueOf(it) } catch (e: IllegalArgumentException) { MemoryType.PREFERENCE }
            } ?: MemoryType.PREFERENCE
            val value = parts.getOrNull(1) ?: ""
            if (value.isBlank()) {
                null
            } else {
                memoryManager.remember(type, parsed.target, value)
                "Beleza, vou lembrar disso."
            }
        }

        // Fase 3 do plano de latência (alexa-referencia/PLANO_DE_ALTERACOES.md): estes
        // comandos são "fire-and-forget" (já executados em executeCommand, logo acima
        // no fluxo de sendMessage) e não têm resultado nenhum que varie — antes disso
        // caíam no `else -> null`, o que forçava uma SEGUNDA chamada de IA só pra gerar
        // uma frase de confirmação óbvia ("Abrindo o YouTube"). Frase fixa aqui elimina
        // esse round-trip extra por completo pra esses casos, sem mudar o que a Gabi
        // realmente diz (mesma frase que a IA já gerava na prática).
        CommandType.OPEN_APP -> "Abrindo ${parsed.target}."
        CommandType.SEARCH_WEB -> "Buscando \"${parsed.target}\"."
        CommandType.SEARCH_YOUTUBE -> "Buscando \"${parsed.target}\" no YouTube."
        CommandType.SEARCH_SPOTIFY -> "Buscando \"${parsed.target}\" no Spotify."
        CommandType.OPEN_SETTINGS -> "Abrindo as configurações."
        CommandType.TAKE_PHOTO -> "Abrindo a câmera."
        CommandType.OPEN_WHATSAPP_CHAT -> "Abrindo a conversa com ${parsed.target} no WhatsApp."

        else -> null
    }

    private val _isSpeaking = MutableStateFlow(false)
    /** Se a Gabi está falando agora (qualquer motor, ElevenLabs ou nativo) — usado pra
     *  animar o Orb/personagem em tempo real, não é só um getter que precisa ser sondado. */
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    // Fase 4 do plano de latência (alexa-referencia/PLANO_DE_ALTERACOES.md) — antes a
    // Gabi nunca pedia nem soltava foco de áudio. Pedir foco TRANSIENT enquanto fala é
    // o motivo normal (e esperado, é o que qualquer assistente de voz faz) de música
    // pausar DURANTE a resposta dela — soltar o foco logo depois permite o app de
    // música retomar sozinho. O problema real que isso não resolve sozinho (ela pausar
    // música mesmo só ESPERANDO, sem estar falando) é do `ContinuousMicMonitor`, que
    // não pede foco nenhum pra ficar ouvindo em segundo plano.
    private var audioFocusRequest: AudioFocusRequest? = null

    private fun requestSpeakingAudioFocus() {
        val audioManager = context.getSystemService(AudioManager::class.java) ?: return
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(attrs)
            .build()
        audioFocusRequest = request
        try {
            audioManager.requestAudioFocus(request)
        } catch (e: Exception) {
            Log.w("JavisRepository", "Falha ao pedir foco de áudio: ${e.message}")
        }
    }

    private fun abandonSpeakingAudioFocus() {
        val audioManager = context.getSystemService(AudioManager::class.java) ?: return
        audioFocusRequest?.let {
            try {
                audioManager.abandonAudioFocusRequest(it)
            } catch (e: Exception) {
                Log.w("JavisRepository", "Falha ao soltar foco de áudio: ${e.message}")
            }
        }
        audioFocusRequest = null
    }

    private suspend fun speakWithAndroidTts(text: String) {
        androidTts.speechRate = prefs.speechRate.first()
        androidTts.pitch = prefs.ttsPitch.first()
        androidTts.speak(text)
    }

    suspend fun speak(text: String) {
        _isSpeaking.value = true
        requestSpeakingAudioFocus()
        try {
            val useElevenLabs = prefs.useElevenLabs.first()
            val elKey = prefs.elevenLabsApiKey.first()

            if (useElevenLabs && elKey.isNotBlank()) {
                elevenLabsTts.apiKey = elKey
                elevenLabsTts.voiceId = prefs.elevenLabsVoiceId.first()
                    .ifBlank { ElevenLabsTts.DEFAULT_VOICE_ID }
                // Se a ElevenLabs falhar (voz não adicionada à VoiceLab da conta,
                // créditos zerados, rede), cai pro TTS nativo em vez de ficar em
                // silêncio total — antes o erro só ia pro Logcat, nunca virava som
                // nenhum nem avisava o usuário.
                elevenLabsTts.speak(text).onFailure { e ->
                    Log.w("JavisRepository", "ElevenLabs falhou (\"$e\"), caindo pro TTS nativo")
                    speakWithAndroidTts(text)
                }
            } else {
                speakWithAndroidTts(text)
            }
        } finally {
            abandonSpeakingAudioFocus()
            _isSpeaking.value = false
        }
    }

    fun stopSpeaking() {
        elevenLabsTts.stopSpeaking()
        androidTts.stop()
    }

    suspend fun executeCommand(cmd: ParsedCommand) {
        when (cmd.type) {
            CommandType.OPEN_APP -> launchApp(cmd.target)
            CommandType.SEARCH_WEB -> searchWeb(cmd.target)
            CommandType.SEARCH_YOUTUBE -> searchYoutube(cmd.target)
            CommandType.SEARCH_SPOTIFY -> searchSpotify(cmd.target)
            CommandType.OPEN_SETTINGS -> openSettings()
            CommandType.TAKE_PHOTO -> openCamera()
            CommandType.OPEN_WHATSAPP_CHAT -> openWhatsAppChat(cmd.target)
            else -> {}
        }
    }

    /**
     * Devolve uma frase pra falar no lugar da genérica de quem chamou, só quando o
     * resultado real precisa ser dito (por ora, só o WhatsApp — as outras ações são
     * fire-and-forget e não têm como falhar de um jeito que valha explicar por voz).
     */
    suspend fun executeConfirmedCommand(cmd: ParsedCommand): String? {
        return when (cmd.type) {
            CommandType.CALL_CONTACT -> { callContact(cmd.target); null }
            CommandType.CALL_NUMBER -> { callNumber(cmd.target); null }
            CommandType.SET_ALARM -> { setAlarm(cmd.target); null }
            CommandType.SEND_WHATSAPP -> whatsAppResultMessage(sendWhatsAppMessage(cmd.target, cmd.extra), cmd.target)
            else -> { executeCommand(cmd); null }
        }
    }

    private fun whatsAppResultMessage(result: WhatsAppSendResult, contact: String): String = when (result) {
        WhatsAppSendResult.Success -> "Pronto! Mandei a mensagem pra $contact no WhatsApp."
        WhatsAppSendResult.AccessibilityDisabled ->
            "Não consegui — o serviço de acessibilidade da Gabi não está ativado. Vai em Ajustes, permissões, e ativa o serviço de acessibilidade."
        WhatsAppSendResult.ContactNotFound ->
            "Abri o WhatsApp mas não achei \"$contact\" nem na lista de conversas nem na busca."
        WhatsAppSendResult.SendFailed ->
            "Abri a conversa com $contact mas não consegui escrever ou enviar a mensagem."
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

    /**
     * Antes disso, "toca uma música"/"abre o Spotify" só existia como OPEN_APP — abria
     * o Spotify na tela inicial dele e parava aí, mas a IA respondia em texto como se
     * tivesse escolhido e tocado alguma coisa (alucinação: ela narra a ação sem ter
     * ferramenta nenhuma pra executá-la de verdade). Reportado em 24/08/2026 ("ela
     * disse que ia botar uma música... e não fez nada").
     *
     * O deep link `spotify:search:<query>` é o mais conhecido, mas é um esquema não
     * documentado oficialmente e com bug reportado pela própria comunidade Spotify:
     * abre o app mas a tela de resultados fica em branco. A documentação oficial da
     * Spotify só cobre link pra conteúdo já identificado por ID (`spotify:track:...`),
     * não busca livre por texto — pra buscar de verdade precisaria da Web API deles
     * (search endpoint, com Client ID/Secret gerados de graça em
     * developer.spotify.com/dashboard) resolvendo o texto pra um ID de faixa antes de
     * linkar. Isso ainda não foi implementado (depende do usuário criar essa chave).
     * **Atualizado em 26/08/2026**: agora, se o usuário configurou um Client ID/Secret
     * da Spotify em Ajustes, tenta resolver a busca pra uma faixa específica via
     * `SpotifyClient` (Web API de verdade) e abre `spotify:track:<id>` — esse formato
     * É documentado e confirmado ao vivo abrindo/tocando a faixa direto, sem precisar
     * o usuário escolher entre resultados. Sem chave configurada (ou se a busca não
     * achar nada), cai pro link de busca de sempre — confirmado ao vivo abrindo
     * certinho na tela de resultados (usuário ainda escolhe/toca manualmente).
     */
    suspend fun searchSpotify(query: String) {
        // Chave própria do usuário (Ajustes) tem prioridade; senão usa a nativa do
        // app (`local.properties`, mesmo padrão do GROQ_API_KEY) — dá pra funcionar
        // "de fábrica" sem precisar digitar nada em Ajustes.
        val clientId = prefs.spotifyClientId.first().ifBlank { BuildConfig.SPOTIFY_CLIENT_ID }
        val clientSecret = prefs.spotifyClientSecret.first().ifBlank { BuildConfig.SPOTIFY_CLIENT_SECRET }
        if (clientId.isNotBlank() && clientSecret.isNotBlank()) {
            spotifyClient.clientId = clientId
            spotifyClient.clientSecret = clientSecret
            val trackUri = try {
                spotifyClient.resolveTrackUri(query)
            } catch (e: Exception) {
                Log.e("JavisRepository", "searchSpotify: exceção resolvendo track URI: ${e.message}", e)
                null
            }
            if (trackUri != null) {
                val playIntent = Intent(Intent.ACTION_VIEW, Uri.parse(trackUri))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(playIntent)
                    return
                } catch (e: Exception) {
                    Log.e("JavisRepository", "searchSpotify: falha abrindo $trackUri: ${e.message}", e)
                }
            }
        }

        val webIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://open.spotify.com/search/${Uri.encode(query)}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(webIntent)
        } catch (_: Exception) {
            val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("spotify:search:${Uri.encode(query)}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try { context.startActivity(appIntent) } catch (_: Exception) {}
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
        launchApp(commandParser.resolveWhatsAppPackage())
    }

    /**
     * Antes essa função só devolvia Boolean, e o `Não consegui" genérico da falha
     * (quando dava) não distinguia "serviço de acessibilidade desligado" de
     * "contato não achado" de "achei o chat mas não consegui escrever" — cada motivo
     * pede uma ação diferente do usuário, e sem isso ela ficava muda sobre qual.
     */
    suspend fun sendWhatsAppMessage(contact: String, message: String): WhatsAppSendResult {
        launchApp(commandParser.resolveWhatsAppPackage())
        kotlinx.coroutines.delay(2000)
        val acc = JavisAccessibilityService.getInstance() ?: return WhatsAppSendResult.AccessibilityDisabled
        // As chamadas de acessibilidade usam Thread.sleep internamente (inclusive o
        // fallback de busca, que pode dormir ~1s a mais) — tira isso da thread
        // principal pra não travar a UI enquanto espera a tela do WhatsApp responder.
        val opened = withContext(Dispatchers.IO) { acc.openWhatsAppChatByName(contact) }
        if (!opened) return WhatsAppSendResult.ContactNotFound
        kotlinx.coroutines.delay(1500)
        val sent = withContext(Dispatchers.IO) { acc.whatsAppSendMessage(message) }
        return if (sent) WhatsAppSendResult.Success else WhatsAppSendResult.SendFailed
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

    /**
     * `getInstance()` sozinho tratava "serviço não bound agora" como "permissão nunca
     * concedida" — mas o Android desconecta o listener sem avisar (ex.: app em background
     * há muito tempo) mesmo com a permissão real intacta em Configurações. Aqui checamos
     * o OS primeiro e, se a permissão existe mas o serviço caiu, pedimos rebind.
     */
    private fun resolveNotificationService(): JavisNotificationListenerService? {
        JavisNotificationListenerService.getInstance()?.let { return it }
        if (JavisNotificationListenerService.hasAccess(context)) {
            JavisNotificationListenerService.requestRebind(context)
        }
        return null
    }

    fun readNotifications(): String {
        val service = resolveNotificationService()
            ?: return if (JavisNotificationListenerService.hasAccess(context))
                "Só um segundo, deixa eu me reconectar às notificações. Pergunta de novo em alguns segundos."
            else "O acesso às notificações não está ativado. Vá nas Configurações para permitir."
        val items = service.getFilteredNotifications()
        if (items.isEmpty()) return "Você não tem notificações ativas agora."
        // Antes pegava até 10 notificações com o texto INTEIRO — pra quem tem grupo de
        // WhatsApp movimentado (ou spam de grupo de venda), isso virava um muro de
        // texto gigante, ilegível/impossível de ouvir em voz alta. Confirmado ao vivo
        // em 26/08/2026: um "leia minhas notificações" trouxe uma parede de mensagens
        // de grupos aleatórios de venda de conta e chip. Agora limita bem mais e corta
        // cada texto num tamanho falável.
        val shown = items.take(5)
        val summary = shown.joinToString("\n") {
            val text = it.text.take(80).let { t -> if (it.text.length > 80) "$t…" else t }
            "De ${it.appName}: ${it.title} — $text"
        }
        val extra = items.size - shown.size
        return if (extra > 0) "$summary\n\n(e mais $extra notificaç${if (extra == 1) "ão" else "ões"})" else summary
    }

    /** Pausa a música tocando agora (Spotify) — reaproveita o acesso de notificações
     *  que o serviço já tem (MediaController), sem permissão nova. */
    fun pauseMedia(): String {
        val service = resolveNotificationService()
            ?: return if (JavisNotificationListenerService.hasAccess(context))
                "Só um segundo, deixa eu me reconectar. Pergunta de novo em alguns segundos."
            else "Não consigo controlar mídia sem o acesso a notificações ativado. Vá nas Configurações para permitir."
        return if (service.pauseSpotify()) "Pronto, pausei a música." else "Não achei nenhuma música tocando pra pausar."
    }

    /** Prévia de emails novos — lê a notificação do Gmail (remetente/assunto/trecho), não o corpo completo. */
    fun readGmailPreview(): String {
        val service = resolveNotificationService()
            ?: return if (JavisNotificationListenerService.hasAccess(context))
                "Só um segundo, deixa eu me reconectar às notificações. Pergunta de novo em alguns segundos."
            else "O acesso às notificações não está ativado. Vá nas Configurações para permitir, aí eu consigo avisar quando chegar email."
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
