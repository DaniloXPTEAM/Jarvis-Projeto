package com.gabi.assistant.ui.viewmodel

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * `ChatViewModel` representa a ÚNICA conversa em andamento com a Gabi — sessão,
 * histórico, escuta de voz. Tem que ser a MESMA instância em qualquer tela que a
 * use (Home, que segura-e-fala; Chat, que mostra o histórico escrito), senão cada
 * tela roda seu próprio `observeSpeech()`/`sendGreeting()` em paralelo e cada fala
 * do usuário é processada duas vezes — foi exatamente esse bug (mensagens e
 * respostas duplicadas) que apareceu ao dar à Home seu próprio `hiltViewModel()`
 * sem escopo compartilhado. `hiltViewModel()` sem argumento escopa pro
 * NavBackStackEntry atual (uma instância por tela); passando a Activity como
 * dono, todas as telas caem na mesma instância, escopada pro app inteiro.
 */
@Composable
fun sharedChatViewModel(): ChatViewModel {
    val activity = LocalContext.current as ComponentActivity
    return hiltViewModel(viewModelStoreOwner = activity)
}
