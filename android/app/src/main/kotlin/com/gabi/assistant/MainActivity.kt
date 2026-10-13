package com.gabi.assistant

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.gabi.assistant.ui.navigation.JavisNavGraph
import com.gabi.assistant.ui.theme.JavisTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Compose observa isso pra decidir se liga o modo de escuta contínua sozinho
    // assim que a tela sobe — ver comentário em handleVoiceTriggerIntent().
    private val autoStartListening = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleVoiceTriggerIntent(intent)

        setContent {
            JavisTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    JavisNavGraph(autoStartListening = autoStartListening)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleVoiceTriggerIntent(intent)
    }

    /**
     * Disparado pelo widget de atalho da tela inicial (`GabiWidgetProvider`). Antes
     * chamava `JavisVoiceService`/`speechRecognizer` direto daqui — funcionava só às
     * vezes: se o app abria do zero (cold start), o resultado da fala podia chegar
     * ANTES do `ChatViewModel` existir e começar a observar `speechRecognizer.events`
     * (StateFlow só entrega o valor mais recente pra quem se inscreve depois — um
     * resultado que já passou se perde). Agora só marca a intenção aqui; quem liga o
     * modo contínuo de verdade é o `ChatViewModel`, através do mesmo caminho testado
     * do toque manual na Gabi — sem essa corrida.
     */
    private fun handleVoiceTriggerIntent(intent: Intent?) {
        if (intent?.action == ACTION_VOICE_TRIGGER) {
            autoStartListening.value = true
        }
    }

    companion object {
        const val ACTION_VOICE_TRIGGER = "com.gabi.assistant.ACTION_VOICE_TRIGGER"
    }
}
