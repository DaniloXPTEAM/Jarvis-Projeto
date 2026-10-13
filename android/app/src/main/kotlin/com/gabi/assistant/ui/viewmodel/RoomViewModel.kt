package com.gabi.assistant.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabi.assistant.notifications.JavisNotificationListenerService
import com.gabi.assistant.storage.JavisPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/**
 * Padrão State (o mesmo conceito visto no projeto de faculdade pesquisado no GitHub
 * — java, sem licença, não copiado, só a IDEIA de máquina de estados pra comportamento
 * de personagem): cada estado sabe sua própria pose e texto, e quem monta a tela só
 * pergunta "qual é o estado atual" sem precisar saber a lógica de qual pose mostrar.
 */
sealed class GabiRoomState {
    abstract val poseFile: String
    abstract val label: String
    /** Nome da pasta `assets/anim/<nome>/` com a sequência de PNGs (canal alfa
     *  real, não vídeo com fundo "assado") dessa cena — usada como camada
     *  preferida por cima do quarto quando existir; se não existir ainda, cai
     *  pra [poseFile] (imagem estática) sem quebrar nada. */
    abstract val animFolder: String

    /** Estado "de base" — o que ela estaria fazendo agora, pelo horário real do aparelho. */
    data class Routine(val hour: Int) : GabiRoomState() {
        override val poseFile: String = when (hour) {
            in 6..10 -> "gabi-comer.png"
            in 11..16 -> "gabi-computador.png"
            in 17..20 -> "gabi-ler.png"
            else -> "gabi-dormir.png"
        }
        override val label: String = when (hour) {
            in 6..10 -> "Tomando café da manhã"
            in 11..16 -> "Mexendo no computador"
            in 17..20 -> "Lendo um livro"
            else -> "Quase dormindo"
        }
        override val animFolder: String = when (hour) {
            in 6..10 -> "scene_comer"
            in 11..16 -> "scene_computador"
            in 17..20 -> "scene_ler"
            else -> "scene_dormir"
        }
    }

    /** Transição temporária — some sozinha depois de [Eating.DURATION_MS], ver [RoomViewModel.feed]. */
    object Eating : GabiRoomState() {
        const val DURATION_MS = 3_000L
        override val poseFile = "gabi-comer.png"
        override val label = "Nham nham! 😋"
        override val animFolder = "scene_comendo_reacao"
    }

    /** Look diferente do padrão selecionado no guarda-roupa — sem variação por horário
     *  ainda, porque só existe 1 pose de cada look extra (não um conjunto de rotina
     *  completo como o vestido vermelho). */
    data class WearingOutfit(val outfit: Outfit) : GabiRoomState() {
        override val poseFile: String = outfit.poseFile
        override val label: String = "Usando: ${outfit.label}"
        override val animFolder: String = "scene_outfit_${outfit.id}"
    }
}

/** Um "look" desbloqueável no guarda-roupa. */
data class Outfit(
    val id: String,
    val label: String,
    val poseFile: String,
    val unlockIndex: Int // 0 = já vem desbloqueado; 1, 2, 3... = quantos intervalos precisa esperar
)

object Outfits {
    /** IDs únicos e permanentes (usados como chave persistida) — trocar o [id] de um
     *  look existente reseta a seleção salva do usuário, então só o [poseFile]/[label]
     *  mudam quando a arte é atualizada. */
    val all = listOf(
        Outfit("vestido_vermelho", "Vestido Vermelho", "gabi-look-vestido-vermelho.png", unlockIndex = 0),
        Outfit("moletom_preto", "Moletom e Calça Preta", "gabi-look-moletom.png", unlockIndex = 1),
        Outfit("look_festa", "Look de Festa", "gabi-look-festa.png", unlockIndex = 2),
        Outfit("look_pijama_passeio", "Pijama de Passeio", "gabi-look-pijama.png", unlockIndex = 3)
    )
}

data class RoomUiState(
    val state: GabiRoomState = GabiRoomState.Routine(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)),
    val lastFedAt: Long = 0L,
    val unlockedCount: Int = 1,
    val nextUnlockInDays: Int = 0,
    val selectedOutfit: Outfit? = null,
    val isSpotifyPlaying: Boolean = false
)

@HiltViewModel
class RoomViewModel @Inject constructor(
    private val prefs: JavisPreferences
) : ViewModel() {

    private val _eatingOverride = MutableStateFlow(false)
    private val _selectedOutfit = MutableStateFlow<Outfit?>(null)

    init {
        viewModelScope.launch {
            // Marca a "data de nascimento" do Quarto na primeira vez que ele é aberto —
            // é a partir daqui que a contagem do guarda-roupa começa a valer.
            if (prefs.gabiFirstUseAt.first() == 0L) {
                prefs.setGabiFirstUseAt(System.currentTimeMillis())
            }
            // Sorteia o intervalo de desbloqueio (dias) uma única vez — pedido explícito
            // do usuário em 26/08/2026: "não precisa ser semanal, pode ser aleatório
            // entre 1 e 30 dias, desde que não seja curto demais". 4–12 dias fica dentro
            // dessa faixa sem ficar frustrante nem óbvio demais.
            if (prefs.gabiUnlockIntervalDays.first() == 0) {
                prefs.setGabiUnlockIntervalDays((4..12).random())
            }
            val savedOutfitId = prefs.gabiSelectedOutfitId.first()
            _selectedOutfit.value = Outfits.all.firstOrNull { it.id == savedOutfitId }
        }
    }

    private data class BaseState(
        val lastFed: Long,
        val firstUse: Long,
        val intervalDays: Int,
        val eating: Boolean,
        val outfit: Outfit?
    )

    val uiState: StateFlow<RoomUiState> = combine(
        combine(
            prefs.gabiLastFedAt,
            prefs.gabiFirstUseAt,
            prefs.gabiUnlockIntervalDays,
            _eatingOverride,
            _selectedOutfit
        ) { lastFed, firstUse, intervalDays, eating, outfit ->
            BaseState(lastFed, firstUse, intervalDays, eating, outfit)
        },
        // Reaproveita o mesmo acesso de notificações que a Gabi já tem pra saber se o
        // Spotify está tocando agora — usado pra animar a caixinha de som no Quarto.
        JavisNotificationListenerService.spotifyPlaying
    ) { base, spotifyPlaying ->
        val unlocked = unlockedCount(base.firstUse, base.intervalDays)
        val nextIn = daysUntilNextUnlock(base.firstUse, base.intervalDays, unlocked)
        // "Rotina do dia" (outfit == null) é a opção padrão/explícita no guarda-roupa —
        // qualquer look selecionado (inclusive o vestido vermelho, índice 0) vale por si
        // só, sem cair de volta pra rotina por horário.
        val state = when {
            base.eating -> GabiRoomState.Eating
            base.outfit != null -> GabiRoomState.WearingOutfit(base.outfit)
            else -> GabiRoomState.Routine(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
        }
        RoomUiState(
            state = state,
            lastFedAt = base.lastFed,
            unlockedCount = unlocked,
            nextUnlockInDays = nextIn,
            selectedOutfit = base.outfit,
            isSpotifyPlaying = spotifyPlaying
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RoomUiState())

    /** Quantos looks já estão desbloqueados agora (sempre pelo menos 1 — o vestido vermelho). */
    private fun unlockedCount(firstUseAt: Long, intervalDays: Int): Int {
        if (firstUseAt == 0L || intervalDays <= 0) return 1
        val daysPassed = (System.currentTimeMillis() - firstUseAt) / (24 * 60 * 60 * 1000L)
        val extraUnlocked = (daysPassed / intervalDays).toInt()
        return (1 + extraUnlocked).coerceAtMost(Outfits.all.size)
    }

    private fun daysUntilNextUnlock(firstUseAt: Long, intervalDays: Int, unlockedCount: Int): Int {
        if (firstUseAt == 0L || intervalDays <= 0 || unlockedCount >= Outfits.all.size) return 0
        val daysPassed = (System.currentTimeMillis() - firstUseAt) / (24 * 60 * 60 * 1000L)
        val daysIntoCurrentInterval = daysPassed % intervalDays
        return (intervalDays - daysIntoCurrentInterval).toInt()
    }

    fun feed() {
        viewModelScope.launch {
            prefs.setGabiLastFedAt(System.currentTimeMillis())
            _eatingOverride.value = true
            delay(GabiRoomState.Eating.DURATION_MS)
            _eatingOverride.value = false
        }
    }

    fun selectOutfit(outfit: Outfit?) {
        _selectedOutfit.value = outfit
        viewModelScope.launch { prefs.setGabiSelectedOutfitId(outfit?.id ?: "") }
    }
}
