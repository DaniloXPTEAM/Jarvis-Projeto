# Referência: o que o REA extraiu do app do Alexa (Android)

Fonte: análise estática do APK feita com o REA (`inspect_android_package` e `search_android_classes`, motor jadx-headless-mcp 0.7.1).
O APK **nunca foi executado**. A assinatura **não foi verificada**. Permissão *declarada* não prova permissão *usada*. Nomes de classes são pistas, não prova do comportamento.

| Campo | Valor |
|---|---|
| Pacote | `com.amazon.dee.app` |
| Versão | 2.2.675097.0 (versionCode 961109715) |
| minSdk / targetSdk | 28 / 36 |
| Classes | 74.633 |
| SHA-256 do arquivo analisado | `574919d5a769b92b6d3f8c892c49cc952dcbc2481601dedef8a65c3c83ed045a` |
| Variante | `armeabi-v7a` (origem: APKMirror) |

> **Como usar:** os nomes `com.amazon.*` e `amazon.*` são **rótulos de referência** de como outro produto organiza o assunto. A Gabi **não** se integra a serviços da Amazon. Implemente a ideia com APIs do Android e código próprio, com nomes próprios.
> **Fora de escopo por decisão do Dannilo:** a loja de apps da Amazon (permissão `…venezia…app_launch`, omitida de propósito).
> **Marcado ★ = o Dannilo pediu para considerar.**

---

## 1. Permissões padrão do Android

**Áudio / voz**
`RECORD_AUDIO`, `MODIFY_AUDIO_SETTINGS`, `MODIFY_AUDIO_ROUTING`, `MEDIA_CONTENT_CONTROL`

**Serviços em primeiro plano (tipos declarados)**
`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MICROPHONE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`,
`FOREGROUND_SERVICE_CONNECTED_DEVICE`, `FOREGROUND_SERVICE_PHONE_CALL`, `FOREGROUND_SERVICE_LOCATION`,
`FOREGROUND_SERVICE_DATA_SYNC`, `FOREGROUND_SERVICE_MEDIA_PROJECTION`, `FOREGROUND_SERVICE_SYSTEM_EXEMPTED`

**Manter vivo / acordar**
`WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, `SCHEDULE_EXACT_ALARM`,
`START_ACTIVITIES_FROM_BACKGROUND`, `REQUEST_COMPANION_RUN_IN_BACKGROUND`, `REQUEST_COMPANION_USE_DATA_IN_BACKGROUND`

**Tela / alertas**
`USE_FULL_SCREEN_INTENT`, `SYSTEM_ALERT_WINDOW`, `POST_NOTIFICATIONS`, `POST_PROMOTED_NOTIFICATIONS`,
`BROADCAST_CLOSE_SYSTEM_DIALOGS`, `VIBRATE`

**Bluetooth / dispositivos próximos / Wi-Fi / rede**
`BLUETOOTH`, `BLUETOOTH_ADMIN`, `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, `BLUETOOTH_ADVERTISE`,
`NEARBY_DEVICES`, `NEARBY_WIFI_DEVICES`, `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE`, `CHANGE_WIFI_MULTICAST_STATE`,
`ACCESS_NETWORK_STATE`, `CHANGE_NETWORK_STATE`, `INTERNET`

**Telefone / contatos**
`CALL_PHONE`, `ANSWER_PHONE_CALLS`, `MANAGE_OWN_CALLS`, `READ_PHONE_STATE`, `READ_CONTACTS`

**Localização / contexto**
`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`, `ACCESS_MEDIA_LOCATION`,
`com.google.android.gms.permission.ACTIVITY_RECOGNITION`

**Câmera / mídia / armazenamento**
`CAMERA`, `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `READ_MEDIA_VISUAL_USER_SELECTED`, `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`

**Push (Google)**
`com.google.android.c2dm.permission.RECEIVE`

**Conta / credenciais / biometria**
`GET_ACCOUNTS`, `USE_CREDENTIALS`, `MANAGE_ACCOUNTS`, `AUTHENTICATE_ACCOUNTS`, `CREDENTIAL_MANAGER_QUERY_CANDIDATE_CREDENTIALS`,
`USE_BIOMETRIC`, `USE_FINGERPRINT` *(a **tarefa** de biometria continua descartada; aqui é só o registro do que existe)*

**Outras**
`WRITE_SETTINGS`, `INTERACT_ACROSS_USERS_FULL`

> Duas permissões que apareceram num scan antigo meu **fora do REA** (`BIND_NOTIFICATION_LISTENER_SERVICE`, `BIND_TELECOM_CONNECTION_SERVICE`) **não** estão aqui porque a lista do REA não as trouxe. Não confirmei se o app as usa.

---

## 2. Permissões próprias da Amazon (rótulos de referência)

### ★ Protocolo de fala (como o app conversa com o serviço de voz)
`amazon.speech.permission.SEND_DATA_TO_ALEXA`, `amazon.speech.permission.SEND_ALEXA_DIRECTIVE`,
`amazon.speech.permission.CONNECT_WITH_SIM`, `amazon.speech.sim.comms.readAccess`,
`com.amazon.alexa.externalnotifications.directivelistener`, `com.amazon.alexa.seamlessswitching.capability.agent.directivereceiver`
- **Ideia:** o servidor manda "diretivas" (ordens estruturadas: falar, tocar, abrir) e o app executa. Classes vistas: `amazon.speech.simclient.event.Endpoint`, e um cliente `alexaservice.base` com eventos e "estágio de endpoint" (`AVSEndpointStage`), o que sugere um serviço de voz na nuvem. *(interpretação pelo nome)*

### ★ Chamadas entre dispositivos
`com.amazon.deecomms.permission`, `com.amazon.rtcsc.permission.RTCSC`, `amazon.permission.USE_TCOMM`
- Classes vistas: `com.amazon.comms.calling.*`, `com.amazon.comms.models.sip.SIPProxyEndpoint`/`RDNEndpoint`, `org.pjsip.pjsua2.Endpoint`, `deecomms…GetEndpointsForTurnServer`.
- **Ideia:** chamada por **SIP/PJSIP com servidor TURN** (para atravessar NAT) e foco de áudio próprio (`RtcscAudioFocusConfig`). *(interpretação pelos nomes)*

### ★ Pareamento / configuração de dispositivos
`com.amazon.whisperjoin.deviceprovisioningservice.AllowListPolicyUpdatePermission`, `…ArcusUpdatePermission`, `…FFSServiceActionPermission`
- Classes vistas: `com.amazon.alexa.devicesetup.softap.*` (o aparelho vira um ponto de acesso Wi-Fi temporário durante a configuração), `LocaleAndEndpointConfigurator`, `com.amazon.alexa.matter.client.*` (padrão Matter de casa inteligente), `whisperjoin.credentiallocker.*` (guarda credenciais de Wi-Fi). *(interpretação pelos nomes)*

### ★ Módulos internos do Alexa (o que as classes mostram)
| Módulo | Classes vistas (exemplos) | O que sugere |
|---|---|---|
| **Palavra de ativação** | `wakeword.WakeWordDetectingAudioCapturer`, `BufferingWakeWordDetector`, `ShortRingBuffer`, `WakeWordArbitration`, `pryon.*` | captura contínua com detector e buffer circular dos últimos instantes |
| **Download de modelos** | `wakeword.davs.*` (`ArtifactManager`, `WakeWordModelDownloadTask`) | modelos baixados e atualizados do servidor |
| **Supressão de mídia** | `wakeword.pryon.mediasuppression.*` (`FingerprintArtifactInfo`, `WatermarkArtifactInfo`) | evitar que TV/música dispare a palavra |
| **Reconhecer o falante** | `wakeword.speakerverification.*` (`enrollment`, `profile`, `encryption`) | cadastro e verificação da voz do dono, com modelo criptografado |
| **Foco de áudio da conversa** | `alexaservice.base.interactions.AudioFocusManager`, `AudioFocusGainedEvent`, `AudioFocusLostEvent` | a conversa segura o foco de áudio e reage se perdê-lo |
| **Latência** | `handsfree.latencyreporter.*` | mede tempos (só telemetria) |
| **Alertas** | `alertsca.*` | alarmes e lembretes |
| **Visual** | `apl.*` | telas geradas por comando |
| **Acessórios** | `accessorykit.*`, `accessory.notificationpublisher.*` | fones e acessórios |
| **Extensões** | `com.amazon.alexa.extensions.permission.SKILL` | "skills" de terceiros |
| **Mídia externa** | `externalmediaplayer.*` | tocar de outros apps |
| **Janela flutuante** | `CONTROL_ALEXA_FLOATING_WINDOW` | overlay do assistente |
| **Modo privacidade** | `amazon.permission.PRIVACY_MODE` | mudo do microfone |
| **Cadastro de voz (telas)** | `fva.UVR_ENROLLMENT_SERVICE_PERMISSION`, `fva.UVR_SETTINGS_SERVICE_PERMISSION`, `…WAKEWORD_SETTINGS_SERVICE_PERMISSION` | configurações de voz e palavra |

### Outras (registro; o Dannilo não pediu para implementar)
- **Conta e login:** `com.amazon.dcp.sso.permission.*`, `com.amazon.identity.*`, `…USE_DEVICE_CREDENTIALS`, `…EXPIRE_ACCESS_TOKEN`.
- **Push da Amazon:** `com.amazon.device.messaging.permission.*`, `com.amazon.dee.app.permission.RECEIVE_ADM_MESSAGE`.
- **Telemetria:** `com.amazon.dcp.metrics.permission.METRICS_PERMISSION`, `amazon.permission.COLLECT_METRICS`, `…handsfree.metrics.IPC_METRIC_PERMISSION`.
- **Outras internas:** `com.amazon.tablet.voiceassistant.permission.START_ASSIST_SESSION`, `com.amazon.permission.RECORD_AUDIO_ACROSS_USER_PROFILES`, `com.amazon.permission.AUDIO_FORCE_USE`, `com.amazon.permission.ENABLE_KEYGUARD_FLAGS`, `com.amazon.alexa.permission.MODE_SWITCH`, `com.amazon.alexa.audio.permission.AUDIO_PROVIDER_MANAGER`, `com.amazon.alexa.ALEXA_AUDIO_PROVIDER_ACTION_PERMISSION`, `com.amazon.alexa.externalmediaplayer.permission.WAKE_UP`, `com.amazon.tahoe.permission.WAKE_WORD_CONTENT_PROVIDER_READ`, `com.amazon.settings.permission.ALEXA_SETTINGS_CONSUMER`, `com.amazon.dcp.settings.permission.READ_SETTINGS`, `com.amazon.dcp.config.permission.DYN_CONFIG_VALUES_UPDATED`, `com.amazon.alexa.accessory.ACCESSORY_BROADCAST_PERMISSION`.

---

## 3. Limites da evidência
- Análise **estática**; nada foi executado.
- Variante única (`armeabi-v7a`); splits e bibliotecas nativas **não** foram verificados pelo REA.
- Não foi verificado se o APK contém arquivos de áudio/voz. A busca por `TextToSpeech` nas classes não achou nada, e o desenho sugere voz gerada na nuvem.
- Interpretações marcadas como *(pelo nome)* são hipóteses.
