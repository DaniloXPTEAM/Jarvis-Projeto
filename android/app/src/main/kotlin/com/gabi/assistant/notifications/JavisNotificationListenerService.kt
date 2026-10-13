package com.gabi.assistant.notifications

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import com.gabi.assistant.data.model.NotificationItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class JavisNotificationListenerService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private var instance: JavisNotificationListenerService? = null

        fun getInstance() = instance

        private val _newNotification = MutableSharedFlow<NotificationItem>(extraBufferCapacity = 20)
        val newNotification: SharedFlow<NotificationItem> = _newNotification

        /** Se o Spotify está tocando música AGORA — usado pra reagir visualmente no
         *  Quarto (pedido do usuário em 26/08/2026: caixinha de som com notas
         *  flutuando enquanto toca). Lê a `MediaSession` do Spotify pelo mesmo acesso
         *  de notificações que a Gabi já tem — não precisa de permissão nova. */
        private val _spotifyPlaying = MutableStateFlow(false)
        val spotifyPlaying: StateFlow<Boolean> = _spotifyPlaying.asStateFlow()

        private const val SPOTIFY_PACKAGE = "com.spotify.music"

        fun isEnabled() = instance != null

        /**
         * `instance` só reflete se o serviço está BOUND agora — o Android pode ter
         * matado/desconectado o processo sem revogar a permissão, deixando o app achar
         * que "não tem permissão" quando na verdade só precisa reconectar. Esta é a
         * fonte de verdade real: consulta o OS diretamente (mesma lista mostrada em
         * Configurações > Apps > Acesso especial > Acesso a notificações).
         */
        fun hasAccess(context: Context): Boolean =
            NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

        /** Pede ao OS pra rebindar o serviço quando a permissão está ok mas [instance] está nulo. */
        fun requestRebind(context: Context) {
            try {
                NotificationListenerService.requestRebind(
                    ComponentName(context, JavisNotificationListenerService::class.java)
                )
            } catch (_: Exception) {}
        }

        // Packages to ignore (system noise)
        private val IGNORE_PACKAGES = setOf(
            "android", "com.android.systemui", "com.android.settings",
            "com.google.android.gms", "com.android.vending"
        )
    }

    private var currentSpotifyController: MediaController? = null

    private val spotifyCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            _spotifyPlaying.value = state?.state == PlaybackState.STATE_PLAYING
        }
        override fun onSessionDestroyed() {
            _spotifyPlaying.value = false
        }
    }

    private val activeSessionsListener =
        MediaSessionManager.OnActiveSessionsChangedListener { attachToSpotifySession(it ?: emptyList()) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    /**
     * Só dá pra usar `MediaSessionManager` depois que o serviço está de fato
     * conectado/autorizado pelo sistema (não basta `onCreate`) — daí o listener é
     * registrado aqui, não em `onCreate`.
     */
    override fun onListenerConnected() {
        super.onListenerConnected()
        try {
            val manager = getSystemService(MediaSessionManager::class.java)
            val component = ComponentName(this, JavisNotificationListenerService::class.java)
            manager?.addOnActiveSessionsChangedListener(activeSessionsListener, component)
            attachToSpotifySession(manager?.getActiveSessions(component) ?: emptyList())
        } catch (_: SecurityException) {}
    }

    private fun attachToSpotifySession(controllers: List<MediaController>) {
        currentSpotifyController?.unregisterCallback(spotifyCallback)
        val spotify = controllers.firstOrNull { it.packageName == SPOTIFY_PACKAGE }
        currentSpotifyController = spotify
        spotify?.registerCallback(spotifyCallback)
        _spotifyPlaying.value = spotify?.playbackState?.state == PlaybackState.STATE_PLAYING
    }

    override fun onDestroy() {
        currentSpotifyController?.unregisterCallback(spotifyCallback)
        try {
            getSystemService(MediaSessionManager::class.java)
                ?.removeOnActiveSessionsChangedListener(activeSessionsListener)
        } catch (_: Exception) {}
        _spotifyPlaying.value = false
        instance = null
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val pkg = sbn.packageName ?: return
        if (IGNORE_PACKAGES.contains(pkg)) return

        val extras = sbn.notification?.extras ?: return
        val title = extras.getString("android.title") ?: return
        val text = extras.getCharSequence("android.text")?.toString() ?: ""

        if (title.isBlank() && text.isBlank()) return

        val appName = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(pkg, 0)
            ).toString()
        } catch (e: Exception) { pkg }

        val item = NotificationItem(
            packageName = pkg,
            appName = appName,
            title = title,
            text = text,
            key = sbn.key
        )

        scope.launch {
            _newNotification.emit(item)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Can observe removals if needed
    }

    fun getFilteredNotifications(): List<NotificationItem> {
        return try {
            activeNotifications?.mapNotNull { sbn ->
                val pkg = sbn.packageName ?: return@mapNotNull null
                if (IGNORE_PACKAGES.contains(pkg)) return@mapNotNull null
                val extras = sbn.getNotification()?.extras ?: return@mapNotNull null
                val title = extras.getString("android.title") ?: return@mapNotNull null
                val text = extras.getCharSequence("android.text")?.toString() ?: ""
                val appName = try {
                    packageManager.getApplicationLabel(
                        packageManager.getApplicationInfo(pkg, 0)
                    ).toString()
                } catch (e: Exception) { pkg }
                NotificationItem(
                    packageName = pkg,
                    appName = appName,
                    title = title,
                    text = text,
                    key = sbn.key
                )
            } ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    fun dismissNotification(key: String) {
        try { cancelNotification(key) } catch (e: Exception) {}
    }
}
