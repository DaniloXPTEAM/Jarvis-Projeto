# Briefing: melhorias da Gabi inspiradas na arquitetura do Alexa

Você (Claude no VS Code) vai implementar melhorias no app Android **Gabi** (`android/`, Kotlin + Compose + Hilt).
O dono é o Dannilo. Leia este arquivo inteiro e `ALEXA_PERMISSOES_REA.md` (mesma pasta) antes de mexer em qualquer coisa.

> **Pacote completo nesta pasta.** Leia `README.md` (índice) e siga `PLANO_DE_ALTERACOES.md` (fases 0 a 6, com como medir e como testar). Este briefing traz as regras e as 15 tarefas.

## Regras (valem para tudo)
1. **Só ideias, não código.** Não existe código de terceiros para copiar aqui. O arquivo `ALEXA_PERMISSOES_REA.md` tem nomes `com.amazon.*`/`amazon.*`: são **rótulos de referência** de como outro produto organiza o assunto. **Não use esses nomes no código da Gabi e não tente integrar com serviços da Amazon.** Implemente a ideia com APIs oficiais do Android, bibliotecas abertas e nomes próprios da Gabi.
2. **Não invente.** Antes de usar uma API, permissão, flag ou biblioteca, confirme que existe (doc oficial do Android ou o código do projeto). Se não confirmou, diga que não confirmou.
3. **Código ativo = `android/app/src/main/kotlin/com/gabi/assistant/`.** A pasta `android/app/src/main/java/com/javis/` é código morto herdado (o Gradle não compila). Não edite nem apague sem o Dannilo pedir.
4. **Mudança mínima por item.** Um item por vez, um commit por item. Nada de refactor "já que estou aqui". Se achar outro problema, anote no fim e pergunte.
5. **Sem biometria.** O item 7 (biometria para proteger chaves) foi **descartado** pelo Dannilo. Não implementar.
6. **Chaves de API** (Groq, ElevenLabs, Anthropic) ficam só na tela Ajustes do app. Nunca em arquivo versionado, log ou mensagem.
7. **Compilar antes de dar como pronto**: `cd android && .\gradlew.bat assembleDebug`. O Android SDK já existe nesta máquina em `C:\Users\Dannilo\AppData\Local\Android\Sdk` (platform 34, build-tools 34.0.0, adb, emulador). Se não compilar, diga o erro; não diga "pronto".
8. O repositório tem mudanças não commitadas do Dannilo (`.gitignore`, `CLAUDE.md`) e arquivos soltos. **Não faça `git add -A`.** Adicione só os arquivos que você mudou.
9. Ações irreversíveis (apagar arquivos, push, force) só com confirmação explícita do Dannilo.

## Estado atual da Gabi (lido do código, verificado)
- Stack: Kotlin, Jetpack Compose, Hilt (via KAPT), `compileSdk 34`, **`minSdk 26`**. Várias permissões novas só existem em Android 12/13/14; proteja com `Build.VERSION.SDK_INT` e `android:maxSdkVersion`/`tools:targetApi` quando aplicável.
- Manifest: `android/app/src/main/AndroidManifest.xml`. Já declara `RECORD_AUDIO`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MICROPHONE`, `CALL_PHONE`, `READ_CONTACTS`, `READ_PHONE_STATE`, `READ_CALENDAR`/`WRITE_CALENDAR`, Bluetooth (`CONNECT`, `SCAN`), `ACCESS_FINE_LOCATION`, `SCHEDULE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`, `POST_NOTIFICATIONS`, `VIBRATE`.
- **Não aparecem no manifest** (confirme com busca antes de assumir): `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, `USE_FULL_SCREEN_INTENT`, `SYSTEM_ALERT_WINDOW`, `CAMERA`, `ANSWER_PHONE_CALLS`, `BLUETOOTH_ADVERTISE`, `NEARBY_WIFI_DEVICES`, `ACCESS_BACKGROUND_LOCATION`, `ACTIVITY_RECOGNITION`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `FOREGROUND_SERVICE_CONNECTED_DEVICE`.
- Serviços no manifest: `notifications.JavisNotificationListenerService`, `accessibility.JavisAccessibilityService`, `voice.JavisVoiceService`.
- Palavra de ativação hoje: **filtro de texto** em `ui/viewmodel/ChatViewModel.kt` (`containsWakeWord`, `stripWakeWord`, janela `CONVERSATION_WINDOW_MS`, em torno das linhas 187-198 e 339-345). Ou seja, escuta tudo e descarta depois. O comentário em `widget/GabiWidgetProvider.kt` (linha ~15) já diz que o widget é um atalho "enquanto a palavra de ativação de verdade" não existe.

## Tarefas (em ordem de prioridade sugerida)

### 0. PRIORIDADE MÁXIMA: resposta lenta e não ouve enquanto fala
**Leia também `DIAGNOSTICO_VOZ_GABI.md` (mesma pasta):** já mapeia, no código da Gabi, onde está o atraso e por que ela não ouve enquanto fala (fluxo em sequência, `SpeechRecognizer` de uma frase por vez, 2,5 s de espera de silêncio, chamada de IA sem streaming, TTS que espera o MP3 inteiro, sem AEC nem foco de áudio). Comece pelo "Passo 0 — medir".
**Leia primeiro `PARAMETROS_DE_VOZ.md` (mesma pasta):** tem valores reais extraídos do Alexa (16 kHz mono PCM16, buffer circular de 3 s, AEC opcional, envio em blocos enquanto a pessoa fala, foco de áudio com troca sem intervalo) e uma tabela comparando com os problemas da Gabi. O que está lá é fato extraído; a coluna "hipótese para a Gabi" **não foi verificada**.
Queixa do Dannilo (dono): a Gabi **demora muito para responder** e, **enquanto fala, não para para ouvir**. A voz da Gabi é uma voz clonada na ElevenLabs (a Gabi já aceita colar um Voice ID em Ajustes → Voz).
**Antes de mudar qualquer coisa: LEIA e DIAGNOSTIQUE** `voice/` (`JavisVoiceService`, `ElevenLabsTts`), `ai/` (provedores) e `ui/viewmodel/ChatViewModel.kt`. Meça onde o tempo vai (reconhecer fala → LLM → TTS → tocar) e mostre os números ao Dannilo antes de otimizar.
Padrões usados por assistentes de voz grandes (ideias a verificar, **nada disto foi testado na Gabi**):
- **Responder em pedaços:** não esperar a resposta inteira do LLM. Receber o texto em streaming, cortar por frase e mandar cada frase ao TTS assim que fechar, tocando numa fila. A primeira frase sai enquanto as outras ainda são geradas.
- **TTS rápido:** conferir na doc da ElevenLabs se há endpoint de streaming e modelo de baixa latência para a voz clonada (não verifiquei o nome atual dos modelos).
- **Ouvir enquanto fala (barge-in):** manter um detector local leve ativo durante a fala, com cancelamento de eco (Android tem `AcousticEchoCanceler`; confirme o uso correto e a fonte de áudio adequada) para a Gabi não escutar a si mesma. Ao detectar a palavra "Gabi" ou fala do usuário: parar o áudio na hora, soltar o foco de áudio e abrir a escuta.
- **Foco de áudio:** tratar a conversa como "dona" do foco de áudio (`AudioManager`/`AudioFocusRequest`) e reagir à perda do foco parando de falar.
- **Buffer circular de áudio:** guardar os últimos instantes do microfone para não perder o começo da frase depois do gatilho.
- **Ruído de mídia:** evitar que TV/música dispare a palavra de ativação.
Pronto quando: tempo até a **primeira palavra falada** medido antes e depois; interromper a Gabi falando funciona em teste real; compila.

### 1. Palavra de ativação real (no aparelho)
- **Por quê:** o filtro de texto gasta bateria e envia áudio ao reconhecedor sem necessidade. Assistentes de voz grandes usam um detector de wake word separado do reconhecimento de fala.
- **O que fazer:** trocar o gatilho por um detector local que só acorda o reconhecimento quando ouvir "Gabi". Mantenha o filtro de texto como fallback.
- **Opções a avaliar (não verificadas, pesquise a licença e a versão atual antes):** Porcupine (exige AccessKey do Dannilo; **pergunte a ele, não crie conta**), openWakeWord, Vosk.
- **Pronto quando:** com o app em segundo plano, dizer "Gabi" inicia a escuta; falar sem a palavra não inicia; compila.

### 2. Ignorar otimização de bateria
- **Por quê:** o serviço de voz pode ser morto pelo sistema. O Alexa declara `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.
- **O que fazer:** declarar a permissão e pedir ao usuário, uma vez, com texto claro em pt-BR, na tela de onboarding ou Ajustes (intent de configurações do sistema; confirme o nome exato da action na doc). Nunca pedir sem explicar o motivo.
- **Pronto quando:** a Gabi detecta se já está isenta e só pergunta se não estiver.

### 3. Modo privacidade
- **Por quê:** assistentes de voz grandes têm um modo privacidade (mudo). A Gabi escuta continuamente.
- **O que fazer:** um interruptor visível (Home e notificação do serviço) que **para** captura de áudio de verdade (serviço de voz parado), com indicador claro de "microfone desligado". Estado persistido.
- **Pronto quando:** com o modo ligado, nenhum `AudioRecord`/`SpeechRecognizer` ativo (verifique no código).

### 4. Reconhecer a voz do dono (cadastro de voz)
- **Por quê:** assistentes de voz grandes cadastram a voz do dono. Sem isso, qualquer pessoa perto do celular comanda a Gabi.
- **Atenção:** é a tarefa mais difícil. Faça só depois da 1. Proponha primeiro uma abordagem por escrito (modelo de embedding de locutor no aparelho, limiar de aceitação, o que acontece com falso positivo) e **espere o Dannilo aprovar**.

### 5. Lembrete em tela cheia / janela flutuante
- **Por quê:** `USE_FULL_SCREEN_INTENT`, `SYSTEM_ALERT_WINDOW`. Hoje lembrete é só notificação.
- **O que fazer:** em `receiver/ReminderReceiver.kt`/`reminder/ReminderScheduler.kt`, lembretes marcados como importantes abrem uma tela cheia (estilo alarme). Prefira `USE_FULL_SCREEN_INTENT` ao overlay; confirme as regras do Android 14+ para essa permissão. `SYSTEM_ALERT_WINDOW` só se for realmente necessário.

### 6. Tipos de serviço em primeiro plano
- **Por quê:** o Alexa declara `MEDIA_PLAYBACK`, `CONNECTED_DEVICE`, `PHONE_CALL`. A Gabi só declara `MICROPHONE`.
- **O que fazer:** adicionar só os tipos que a Gabi **realmente usa** (ex.: `mediaPlayback` se tocar música/TTS longo; `connectedDevice` se manter conexão Bluetooth). Confirme qual `android:foregroundServiceType` combina com qual permissão. Não declarar tipo sem uso.

### 7. (DESCARTADO) Biometria — não fazer.

### 8. Descoberta e pareamento de dispositivos
- **Por quê:** `BLUETOOTH_ADVERTISE`, `NEARBY_WIFI_DEVICES`, `CHANGE_WIFI_MULTICAST_STATE`. A Gabi só escaneia/conecta Bluetooth.
- **O que fazer:** fluxo "adicionar dispositivo" por voz ("Gabi, conectar a caixa de som"). Comece pelo Bluetooth (já existe `CommandParser` com esse comando) e só depois pense em Wi-Fi.

### 9. Atender ligação por voz
- **Por quê:** `ANSWER_PHONE_CALLS`. A Gabi já liga (`CALL_PHONE`).
- **O que fazer:** comando "atender" / "recusar" via API oficial de telecom. Confirme as restrições por versão do Android.

### 10. Câmera ("o que é isso?")
- **Por quê:** `CAMERA` no Alexa. A Gabi não usa câmera.
- **O que fazer:** o código já tem `ClaudeProvider`, `GroqProvider` e `DeepSeekProvider` em `ai/`. Confirme, lendo cada um, qual aceita imagem (**não verifiquei**) e pergunte ao Dannilo qual usar antes de implementar.

### 11. Contexto por atividade e localização
- **Por quê:** `ACTIVITY_RECOGNITION`, `ACCESS_BACKGROUND_LOCATION`.
- **Atenção:** localização em segundo plano é restrita na Play Store e exige justificativa. Só implemente se o Dannilo confirmar que quer publicar ou que é só uso pessoal.

### 12. Protocolo de comandos estruturados ("diretivas")
- **Referência:** `ALEXA_PERMISSOES_REA.md`, seção "Protocolo de fala". O serviço manda ordens estruturadas (falar, tocar, abrir) que o app executa.
- **Na Gabi:** já existem `ai/AiToolRouter.kt` e `ai/ToolCalling.kt`. **Primeiro leia o que eles já fazem** e diga ao Dannilo se cobrem isso.
- **Ideia a avaliar:** a resposta da IA vir separada em "o que falar" e "que ação executar", para a Gabi **começar a falar enquanto a ação roda**. Isso também ajuda na latência (tarefa 0). Não invente formato: proponha por escrito e espere aprovação.

### 13. Chamadas entre dispositivos (Gabi ↔ Gabi)
- **Referência:** seção "Chamadas entre dispositivos". As classes sugerem SIP/PJSIP com servidor TURN.
- **Atenção:** isso exige servidor (sinalização e TURN) e é a tarefa mais cara de infraestrutura. **Não implemente sem o Dannilo decidir** o escopo. Alternativa mais simples a propor: interfone entre dois aparelhos na mesma rede. Confirme as opções atuais (WebRTC, etc.) na documentação antes de sugerir.

### 14. Pareamento / configuração de dispositivos novos
- **Referência:** seção "Pareamento". As classes sugerem configuração por ponto de acesso Wi-Fi temporário e suporte ao padrão Matter.
- **Na Gabi:** comece pelo Bluetooth (já existe o comando em `CommandParser`). Matter e provisionamento Wi-Fi só depois, e só após **confirmar na documentação oficial** o que o Android permite (não verifiquei).

### 15. Módulos internos do assistente (inspiração de arquitetura)
Veja a tabela de módulos em `ALEXA_PERMISSOES_REA.md`. Para a Gabi, os que mais importam, em ordem:
1. Palavra de ativação com buffer circular e **supressão de mídia** (tarefas 0 e 1).
2. **Foco de áudio da conversa** (tarefa 0).
3. Cadastro de voz do dono (tarefa 4).
4. Alertas/lembretes (a Gabi já tem; ver tarefa 5).
5. Janela flutuante (tarefa 5).
6. "Extensões" de terceiros e acessórios: **só depois**, e só se o Dannilo pedir.

## Fora de escopo
- **Loja de aplicativos da Amazon:** o Dannilo disse que não precisa.
- Login/conta, push da Amazon e telemetria: **não foram pedidos**. Não implemente sem o Dannilo pedir.
- Biometria (tarefa 7).

## Ao terminar cada item, reporte
1. O que mudou (arquivos).
2. Como você verificou (comando e resultado de `assembleDebug`).
3. O que **não** foi verificado (ex.: não testado em aparelho real).
