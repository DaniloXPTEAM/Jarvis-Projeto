package com.gabi.assistant.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabi.assistant.ai.CommandType
import com.gabi.assistant.ai.ParsedCommand
import com.gabi.assistant.data.model.Message
import com.gabi.assistant.data.model.VoiceState
import com.gabi.assistant.domain.JavisRepository
import com.gabi.assistant.voice.JavisSpeechRecognizer
import com.gabi.assistant.voice.JavisVoiceService
import com.gabi.assistant.voice.SpeechEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.Normalizer
import javax.inject.Inject

/** Quanto tempo depois da última interação real ela continua respondendo sem
 *  precisar ouvir o nome de novo — igual o modo "follow-up" da Alexa/Google. */
// 20s (valor original) fechava a janela no meio de uma conversa real, com o usuário
// parando um instante pra pensar/ler a tela — ele esperava continuar sem repetir
// "Gabi" e ela "simplesmente parava de responder", sem aviso nenhum do porquê.
// Reportado e confirmado ao vivo em 13/10/2026.
private const val CONVERSATION_WINDOW_MS = 60_000L

// Fase 0 do plano de latência (alexa-referencia/PLANO_DE_ALTERACOES.md) — só
// medição, filtrar no logcat com `adb logcat -s GabiLatency`.
private const val LATENCY_TAG = "GabiLatency"

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val voiceState: VoiceState = VoiceState(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val pendingCommand: ParsedCommand? = null,
    val inputText: String = "",
    // Toque-pra-ativar (em vez de segurar): enquanto true, a Gabi volta a ouvir sozinha
    // depois de cada resposta dela, sem precisar tocar de novo — igual modo de voz do
    // ChatGPT. Só desliga quando o usuário toca de novo ou um erro real acontece.
    val continuousMode: Boolean = false
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: JavisRepository,
    private val speechRecognizer: JavisSpeechRecognizer,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    // Controla a "janela de conversa ativa" do filtro de palavra de ativação — ver
    // uso em sendMessage(). 0 = nunca conversou ainda nesta sessão, sempre exige o nome.
    private var lastActiveConversationAt = 0L

    val sessionId: String = repository.currentSessionId

    init {
        observeMessages()
        observeSpeech()
        observeProactiveNotifications()
        sendGreeting()
    }

    /** Enquanto esta tela (o app) estiver aberta, deixa a Gabi avisar sozinha sobre WhatsApp/Gmail novos. */
    private fun observeProactiveNotifications() {
        viewModelScope.launch {
            repository.watchProactiveNotifications(sessionId)
        }
    }

    private fun observeMessages() {
        viewModelScope.launch {
            repository.getMessages(sessionId).collect { msgs ->
                _uiState.update { it.copy(messages = msgs) }
            }
        }
    }

    private fun observeSpeech() {
        viewModelScope.launch {
            speechRecognizer.events.collect { event ->
                when (event) {
                    is SpeechEvent.Started -> {
                        _uiState.update { it.copy(voiceState = it.voiceState.copy(isListening = true)) }
                    }
                    is SpeechEvent.Partial -> {
                        _uiState.update { it.copy(voiceState = it.voiceState.copy(partialText = event.text)) }
                    }
                    is SpeechEvent.Result -> {
                        _uiState.update {
                            it.copy(
                                voiceState = it.voiceState.copy(isListening = false, partialText = ""),
                                inputText = event.text
                            )
                        }
                        sendMessage(event.text, fromVoice = true)
                    }
                    is SpeechEvent.Error -> {
                        // Códigos 6/7 (timeout/sem fala reconhecida) são o normal do modo
                        // contínuo — a maior parte do tempo ouvindo é silêncio ou ruído
                        // ambiente. Sem tratar isso como "erro real", cada silêncio derrubava
                        // o modo contínuo de vez, obrigando a tocar de novo toda hora.
                        // -1 (NO_SPEECH_DETECTED) adicionado em 13/10/2026: confirmado ao
                        // vivo em log que o serviço de voz do aparelho (Xiaomi/AGSA) devolve
                        // esse código o tempo todo, mesmo com o usuário falando normalmente —
                        // tratar como erro grave desligava o modo contínuo sem motivo real.
                        val expected = event.code == 7 || event.code == 6 || event.code == -1
                        _uiState.update {
                            it.copy(
                                voiceState = it.voiceState.copy(isListening = false, partialText = ""),
                                error = if (expected) null else event.message,
                                continuousMode = if (expected) it.continuousMode else false
                            )
                        }
                        if (expected && _uiState.value.continuousMode) {
                            startVoiceService()
                        }
                    }
                    is SpeechEvent.Ended -> {
                        _uiState.update { it.copy(voiceState = it.voiceState.copy(isListening = false)) }
                    }
                    null -> {}
                }
            }
        }
    }

    private fun sendGreeting() {
        viewModelScope.launch {
            repository.greet(sessionId)
        }
    }

    /**
     * Manda a fala/texto pro repositório, que roteia via IA (ou regex offline se não
     * houver IA disponível) e decide sozinho se alguma ação precisa de confirmação —
     * a tela só reage ao resultado, não faz sua própria classificação por regex antes
     * (isso já causou um bug: uma ação identificada só pela IA nunca pedia confirmação
     * nem executava, porque essa checagem local não sabia dela).
     */
    /**
     * [fromVoice] distingue fala captada pelo microfone (onde o filtro de palavra de
     * ativação abaixo se aplica) de texto DIGITADO na tela de Chat (onde não faz
     * sentido nenhum exigir "Gabi" — quem está digitando pra ela já está,
     * obviamente, falando com ela).
     */
    fun sendMessage(text: String, fromVoice: Boolean = false) {
        if (text.isBlank()) return

        // Botões Sim/Não só existem na tela de Chat — quem fala com a Gabi pela tela
        // inicial (o uso normal, ela é assistente de VOZ) nunca via essa UI, então toda
        // ação que pedia confirmação (mandar WhatsApp, alarme…) ficava pendurada pra
        // sempre: a Gabi perguntava em voz alta, o usuário respondia "sim" falando, e
        // essa resposta ia pro Groq como mensagem nova, ignorando o comando pendente
        // por completo. Intercepta a resposta aqui antes de rotear pra IA.
        _uiState.value.pendingCommand?.let { _ ->
            when (matchesConfirmation(text)) {
                true -> { confirmPendingCommand(true); return }
                false -> { confirmPendingCommand(false); return }
                null -> _uiState.update { it.copy(pendingCommand = null) } // resposta ambígua: não trava a conversa, segue como mensagem nova
            }
        }

        // "Gabi, vai dormir"/"se desliga": pedido explícito (24/08/2026) de desligar o
        // modo de escuta contínua por voz, sem precisar tocar na tela nem fechar o app
        // de verdade — checado localmente (sem chamar a IA) pra ser instantâneo e
        // confiável, em vez de depender do modelo interpretar certo.
        if (_uiState.value.continuousMode && matchesSleepCommand(text)) {
            toggleContinuousMode()
            viewModelScope.launch {
                repository.sayAsAssistant("Tá bom, parei de ouvir. Toca em mim de novo quando precisar.", sessionId)
            }
            return
        }

        // Pedido explícito (26/08/2026): no modo contínuo, ela só deve reagir quando
        // chamada pelo nome (ou variação tipo "Gabi, vamos conversar") — senão qualquer
        // papo dela perto de outras pessoas vira comando sem querer. Fica ativa por
        // uma JANELA depois de cada interação real (segue o padrão da Alexa/Google:
        // não precisa repetir o nome toda vez numa conversa contínua, só depois de um
        // silêncio). Só vale pra fala captada pelo microfone — texto digitado no chat
        // é sempre intencional, não precisa "chamar" ela por escrito. Também não se
        // aplica às respostas de confirmação/sono acima, que já retornaram antes daqui.
        val now = System.currentTimeMillis()
        val inActiveWindow = now - lastActiveConversationAt < CONVERSATION_WINDOW_MS
        if (fromVoice && !inActiveWindow && !containsWakeWord(text)) {
            // O handler de SpeechEvent.Result (acima) já escreveu este texto em
            // inputText antes de chamar sendMessage() — sem limpar aqui, ele ficava
            // visível na caixa pra sempre (texto "escrito mas nunca enviado"),
            // parecendo que o app travou. Reportado e confirmado ao vivo em 13/10/2026.
            _uiState.update { it.copy(inputText = "") }
            continueListeningIfContinuous()
            return
        }
        lastActiveConversationAt = now
        val cleanedText = if (fromVoice) stripWakeWord(text) else text

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, inputText = "", error = null) }
            try {
                repository.sendMessage(cleanedText, sessionId)
                    .onSuccess { outcome ->
                        _uiState.update { it.copy(pendingCommand = outcome.pendingCommand) }
                    }
                    .onFailure { e ->
                        _uiState.update { it.copy(error = "A Gabi não conseguiu responder: ${e.message ?: "erro desconhecido"}") }
                    }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                // Rede de segurança: se algo além do sendMessage() estourar sem passar por
                // Result (ex.: exceção não tratada em algum ponto novo do pipeline), isso
                // garante que a UI nunca fica travada em "Pensando…" pra sempre.
                _uiState.update { it.copy(error = "Erro inesperado: ${e.message ?: e::class.simpleName}") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
                // repository.sendMessage() já espera a fala da Gabi terminar de tocar
                // (speak() é suspend até o áudio acabar) antes de devolver — chegando aqui,
                // é seguro voltar a ouvir sem captar a própria voz dela pelo microfone.
                Log.d(LATENCY_TAG, "T7 fim da fala, religando escuta t=${SystemClock.elapsedRealtime()}")
                continueListeningIfContinuous()
            }
        }
    }

    /** Volta a ouvir sozinha se o modo contínuo (toque-pra-ativar) estiver ligado. */
    private fun continueListeningIfContinuous() {
        if (_uiState.value.continuousMode) startVoiceService()
    }

    /**
     * Toque único na Gabi/botão de mic: liga ou desliga o modo de escuta contínua.
     *
     * Antes chamava `speechRecognizer.startListening()` direto — isso só funciona com
     * a Gabi em primeiro plano; assim que outro app assume a tela (ex.: pedir pra abrir
     * o YouTube e depois pedir outro trailer), o Android para de entregar áudio pro
     * reconhecedor de um app em segundo plano sem justificativa. Já existia um serviço
     * em primeiro plano pronto pra isso (`JavisVoiceService`, tipo "microphone", com
     * notificação própria) mas nunca tinha sido ligado a este fluxo — reportado em
     * 24/08/2026 ("uma vez que o app [YouTube] estivesse aberto, ela não conseguia
     * atender a novos pedidos").
     */
    fun toggleContinuousMode() {
        val turningOn = !_uiState.value.continuousMode
        _uiState.update { it.copy(continuousMode = turningOn) }
        if (turningOn) {
            repository.stopSpeaking()
            startVoiceService()
        } else {
            stopVoiceService()
        }
    }

    private fun startVoiceService() {
        val intent = Intent(context, JavisVoiceService::class.java).apply {
            action = JavisVoiceService.ACTION_START_LISTENING
        }
        context.startForegroundService(intent)
    }

    private fun stopVoiceService() {
        val intent = Intent(context, JavisVoiceService::class.java).apply {
            action = JavisVoiceService.ACTION_STOP
        }
        context.startService(intent)
    }

    fun confirmPendingCommand(confirmed: Boolean) {
        val cmd = _uiState.value.pendingCommand ?: return
        _uiState.update { it.copy(pendingCommand = null) }

        if (!confirmed) {
            viewModelScope.launch {
                repository.sayAsAssistant("Beleza, não vou fazer isso.", sessionId)
                continueListeningIfContinuous()
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // executeConfirmedCommand devolve uma frase pronta quando o resultado real
            // importa (ex.: WhatsApp — "consegui" vs "não achei o contato" vs "não
            // consegui escrever" pedem reação diferente do usuário); nos outros casos
            // devolve null e usa a genérica de sempre.
            val override = repository.executeConfirmedCommand(cmd)

            val reply = override ?: when (cmd.type) {
                CommandType.CALL_CONTACT -> "Ligando para ${cmd.target}…"
                CommandType.SET_ALARM -> "Configurando alarme…"
                else -> "Pronto."
            }
            repository.sayAsAssistant(reply, sessionId)
            _uiState.update { it.copy(isLoading = false) }
            continueListeningIfContinuous()
        }
    }

    /**
     * Minúsculo, sem acento, sem pontuação — o reconhecedor de voz às vezes devolve
     * "Sim." ou "Sim!" com pontuação, o que quebrava o match antigo (só tirava acento,
     * não pontuação) e fazia respostas curtas caírem como "ambíguas" à toa.
     */
    private fun normalizeSpeech(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("[\\p{InCombiningDiacriticalMarks}]"), "")
            .lowercase()
            .filter { it.isLetter() || it.isWhitespace() }
            .trim()

    /**
     * true = confirmou, false = recusou, null = não deu pra saber (não é resposta de sim/não).
     * Checa negativas primeiro porque "não" nunca deve cair no match de "n"/prefixos de "sim".
     */
    private fun matchesConfirmation(text: String): Boolean? {
        val norm = normalizeSpeech(text)
        val noWords = listOf("nao", "cancela", "cancelar", "esquece", "deixa pra la", "para", "no", "nunca", "negativo")
        val yesWords = listOf(
            "sim", "pode", "manda", "mandar", "envia", "enviar", "confirma", "confirmar",
            "isso", "claro", "faz", "faça", "vai", "ok", "certo", "positivo", "yes", "sure"
        )
        if (noWords.any { norm == it || norm.startsWith("$it ") }) return false
        if (yesWords.any { norm == it || norm.startsWith("$it ") }) return true
        return null
    }

    /** "Gabi, vai dormir" / "se desliga" / "para de ouvir" — desliga o modo contínuo por voz. */
    private fun matchesSleepCommand(text: String): Boolean {
        val norm = normalizeSpeech(text)
        val sleepPhrases = listOf(
            "vai dormir", "pode dormir", "se desliga", "desliga", "desativa",
            "para de ouvir", "para de escutar", "pare de ouvir", "pare de escutar", "boa noite"
        )
        return sleepPhrases.any { norm.contains(it) }
    }

    /** Detecta o nome dela na fala — "gabi" como palavra inteira, não substring de outra coisa. */
    private fun containsWakeWord(text: String): Boolean {
        val norm = normalizeSpeech(text)
        return norm.split(" ").any { it == "gabi" }
    }

    /** Tira o "Gabi" do início da frase antes de mandar pra IA, pra não confundir o pedido de verdade. */
    private fun stripWakeWord(text: String): String =
        text.replace(Regex("(?i)\\bgabi\\b[,]?\\s*"), " ").trim().ifBlank { text }

    fun updateInput(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        speechRecognizer.destroy()
        super.onCleared()
    }
}
