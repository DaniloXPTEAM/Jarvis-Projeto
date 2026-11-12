# Evidências do REA e o que foi mexido no PC do Dannilo

## 1. Evidências do REA (rastreabilidade)
Todas vêm de leituras estáticas do APK `com.amazon.dee.app` 2.2.675097.0 (SHA-256 `574919d5a769b92b6d3f8c892c49cc952dcbc2481601dedef8a65c3c83ed045a`, arquivo em `C:\w\Nova pasta\`).
Os `evidence_id` abaixo foram copiados dos resultados do REA nesta sessão. Se algum não bater ao reconsultar, o **conteúdo** descrito continua sendo o resultado da chamada indicada.

| Achado | Chamada do REA | evidence_id |
|---|---|---|
| Permissões, versão, 74.633 classes | `inspect_android_package` (summary) | `ev_9bf10f5689f3dae0c95314872cd39be7511580d8eb13259c3c50becf4082473d` |
| Config do detector: limiar 500, sem VAD, baixa latência desligada | `inspect_android_method` PryonConfigProvider.createPryonConfig | `ev_85dcd18142c2e5c404fa5f87a77fa3b3fb5d06f234ab3aa240ed502af49f373c` |
| Config "legada": baixa latência ligada | PryonConfigProvider.createPryonConfigUsingLegacySetting | `ev_0943a86ca4e7349bbc1705190cf3eef84e67a7d322e65443af3f5f56b8876c5f` |
| Amostras ÷ 16 = ms (16 kHz) | WakeWordDetectingAudioCapturer.convertSamplesToMilliseconds | `ev_f59660567d61cb49297087e84eb9d6e090f43e4baf3b05989fcfa933c3de8480` |
| Envio do áudio em thread própria a partir do buffer | WakeWordDetectingAudioCapturer.startTransferring | `ev_d220a30c6acee0d37473e4d4ffae71d65f37f9142dab8ff8674c2b2037addaa0` |
| Buffer circular de 48.000 amostras | BufferingWakeWordDetector.\<init\> (overload 1) | `ev_90563349b81e9eebc45fc27ba3aad93c2ad09181707b62b7951d1d217e6df0e3` |
| Captura criada com flag de AEC; supressor de ruído `false` | PryonRecordingRunnable.createAudioCapturer | `ev_730f757b611a0c13c2a96b57a551c948c745579669f4538799d0d4ccf35789c1` |
| Laço de captação e medição de inicialização do microfone | PryonRecordingRunnable.run | `ev_49ccd1467b8d5ff8582b4223194f5b4a5da2a7d587cd0220c32523ed504f43fd` |
| AudioRecord: fonte 1 ou 6, 16000 Hz, canal 16, formato 2 | AudioCapturer.buildAudioRecord | `ev_a1a866789f6d4d97f964e4c2042cb925039ca692353bdbf8dfa6d0155edfb287` |
| Construtor do capturador (AEC/NS opcionais, `startRecording`) | AudioCapturer.\<init\> (overload 1) | `ev_388c23c90af07f2686c12cda7864eb46e14c586d96cee7a47a4adb98194015a2` |
| AEC só se `isAvailable()`; segue sem ele se não houver | AudioCapturer.requestAcousticEchoCanceler | `ev_5055388357ec73acd6de617459849474ad45f5c40367f5c745b65afcae2c6764` |
| Pedido de foco: novo pedido antes de soltar o antigo | AudioFocusManager.requestAudioFocus | `ev_b0be27a9425c1c460aa3594f756aa797842fe667c4f95acbdb45ca8d87c8c136` |
| Reação aos 4 tipos de mudança de foco | AudioFocusManager.onAudioFocusChange | `ev_f9a5117ae6e9de32b0ba936ca9557cd15e26d2081e88ddb7a30763e947a88fbc` |
| Estrutura das classes (campos e métodos) | `inspect_android_class` ShortRingBuffer / WakeWordDetectingAudioCapturer / PryonConfigProvider / AudioFocusManager / PryonRecordingRunnable / BufferingWakeWordDetector / AudioCapturer | `ev_26ad4619…`, `ev_3ce9954f…`, `ev_76578979…`, `ev_9907c3e1…`, `ev_8aa90e8c…`, `ev_8d2d5421…`, `ev_144719e7…` |
| Módulos de palavra de ativação e verificação de falante | `search_android_classes` "alexa.wakeword" | `ev_1f5e6102d594fd1d589d03835e9c40a077b9355b44588a1ae637ff3619faab0e` |
| Classes de foco de áudio | `search_android_classes` "AudioFocus" | `ev_e22b029878f51121f5a25359a417d68ba64d39a509e803c2ba6bff93e1b80802` |
| Classes SIP / TURN / softap / Matter | `search_android_classes` "Endpoint" | `ev_0b66732dddd87dcba01a46bebbea45351d12252ca2746b32b3a3fcde5356384b` |
| **Sem** classe com `TextToSpeech` no nome | `search_android_classes` "TextToSpeech" | `ev_133e7db4b4f57c9aa5d4e1ec1fe64ad2d9d209c689909642e4e5f64c02e4f810` |
| **Sem** classe com `BargeIn` no nome (não prova que não existe a função) | `search_android_classes` "BargeIn" | `ev_44f81d1ead3ac45918eb0ec5611a221cd1267fa39f563b82f987b36b8e91f7ee` |
| Relatório de latência (só telemetria) | `search_android_classes` "handsfree.latency" | `ev_52176a3d2e337e2c904c4ea00f3642c21e69102595a83e0ba59d797b621cff21` |

### O que NÃO foi obtido (ficou em branco de propósito)
`samplesPerFrame` do motor, `PREROLL_SAMPLE_SIZE`, `ACTUAL_BUFFER_SIZE`, `OVERRIDE_FOCUS_DURATION_VALUE`, `DEFAULT_DETECTION_SENSITIVITY` (valor), lista de canais de áudio e prioridades, quem liga a flag de AEC, quem chama a pausa da detecção, método `transferAudio`. O motor de detecção é código nativo proprietário; os valores dele não foram lidos.

## 2. O que foi instalado ou criado no PC (registro completo)
| O quê | Onde | Observação |
|---|---|---|
| JDK 21 (Eclipse Temurin 21.0.12) | `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot` | via `winget`; o Java 8 antigo **continua instalado**, mas o 21 vem primeiro no PATH |
| `jadx-headless-mcp-0.7.1-all.jar` | `C:\tools\rea-android\` | SHA-256 conferido: `6e5eacf500b64292bfb73c49797c1958f6ee44646e43e868039ae7feb573ff75` |
| `apktool.jar` (3.0.3) e `apktool.bat` | `C:\tools\rea-android\` | |
| `apktool-rea.cmd` | `C:\tools\rea-android\` | lançador sem `pause`, criado porque o `apktool.bat` travava esperando tecla quando chamado pelo REA |
| Variáveis de ambiente do usuário | `JAVA_HOME`, `REA_JADX_MCP_JAR`, `REA_APKTOOL_COMMAND` | `REA_APKTOOL_COMMAND` aponta para `apktool-rea.cmd`. **O REA precisa ser reiniciado para ler essa última mudança**; `decode_android_resources` ainda não foi testado com ela |
| Pasta `alexa-referencia` | `C:\CRiando coisa nova\Projeto Gabi\` | os arquivos deste conjunto. **Não commitada** |
| `apkscan.py` | `%TEMP%` | script meu, de uma leitura inicial do APK **fora do REA**. Pode apagar |
| Pasta `VozApp` | `C:\AI test\morena\Nova pasta\` | criada e depois **apagada** a pedido (era só um esqueleto vazio) |

**Nada foi alterado no código da Gabi.**

## 3. Para reproduzir a análise
1. Ter o APK em `C:\w\Nova pasta\com.amazon.dee.app_2.2.675097.0-961109715_minAPI28(armeabi-v7a)(nodpi)_apkmirror.com.apk`.
2. Ter o REA conectado com as 3 variáveis acima.
3. Chamar `search_android_classes` (para achar classes) e `inspect_android_class` / `inspect_android_method` (para ler campos e corpos) com o caminho do APK.
