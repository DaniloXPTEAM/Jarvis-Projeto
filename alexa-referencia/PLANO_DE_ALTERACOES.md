# Plano de diagnóstico e alterações da voz da Gabi (passo a passo)

Para o Claude no VS Code. Leia antes: `README.md`, `BRIEFING_PARA_CLAUDE_VSCODE.md` (regras), `DIAGNOSTICO_VOZ_GABI.md` (o que já foi lido) e `PARAMETROS_DE_VOZ.md` (valores de referência).

**Regra de ouro:** uma fase por vez, um commit por fase, **medir antes e depois**. Nada de "já que estou aqui". Se uma fase não melhorar o número, **reverta** e diga ao Dannilo.
**Antes de começar:** pergunte ao Dannilo se quer uma branch nova (sugestão: `voz-latencia`) para poder voltar atrás. O repositório tem mudanças dele não commitadas (`.gitignore`, `CLAUDE.md`): **nunca `git add -A`**, adicione só os arquivos que você mudou.
**Build:** `cd android && .\gradlew.bat assembleDebug` (Android SDK em `C:\Users\Dannilo\AppData\Local\Android\Sdk`). Se não compilar, diga o erro; não diga "pronto".
Caminhos abaixo são relativos a `android/app/src/main/kotlin/com/gabi/assistant/`.

---

## FASE 0 — Medir (não muda comportamento)
**Objetivo:** descobrir em qual etapa o tempo vai, com números.
**O que fazer:** adicionar logs de tempo (tag única, ex.: `GabiLatency`) usando `SystemClock.elapsedRealtime()`. Pontos de medição, em ordem de um turno:

| # | Marcador | Onde |
|---|---|---|
| T0 | usuário parou de falar (`onEndOfSpeech`) | `voice/JavisSpeechRecognizer.kt` |
| T1 | resultado final do reconhecimento (`onResults`) | `voice/JavisSpeechRecognizer.kt` |
| T2 | início de `sendMessage` no repositório | `domain/JavisRepository.kt` (~linha 161) |
| T3 | fim de `routeWithAi` (chamada de IA 1) | `domain/JavisRepository.kt` (~linha 196) |
| T4 | fim de `primary.chat` (chamada de IA 2), se houver | `domain/JavisRepository.kt` (~linha 219) |
| T5 | resposta do ElevenLabs recebida (`bytes()` terminou) | `voice/ElevenLabsTts.kt` (~linha 75) |
| T6 | primeiro som (`OnPreparedListener` → `start()`) | `voice/ElevenLabsTts.kt` (~linha 139) |
| T7 | fim da fala e religação da escuta | `ui/viewmodel/ChatViewModel.kt` (`continueListeningIfContinuous`) |

Registre também: quantas vezes houve 429/402 (`withRateLimitRetry`), quantas vezes caiu no DeepSeek, e o tamanho do texto falado.
**Entrega:** uma tabela com **pelo menos 10 turnos reais** (perguntas simples, comandos, resposta longa) e a **mediana** de cada intervalo (T1−T0, T3−T2, T4−T3, T5−T3 ou T4, T6−T5). Mostre ao Dannilo.
**Pronto quando:** a tabela existe. **Só então** passe à Fase 1.

## FASE 1 — Correções pequenas e seguras
1. **Corte de 20 s.** `voice/ElevenLabsTts.kt` (`withTimeoutOrNull(20_000)`) e `voice/AndroidTtsFallback.kt` (`withTimeoutOrNull(20_000)`) cortam áudios longos. Trocar por um limite proporcional ao tamanho do texto ou muito maior, mantendo a rede de segurança contra travamento. **Teste:** uma resposta que dure bem mais de 20 s toca inteira.
2. **Espera de silêncio** (`voice/JavisSpeechRecognizer.kt`, `buildIntent`: 2500 ms e 1800 ms). Experimente valores menores **medindo T1−T0** e conferindo se corta pausas naturais (o histórico no comentário diz que 1500/1000 cortava). Confirme na documentação do Android se esses extras são respeitados por todos os reconhecedores; no aparelho do Dannilo (Xiaomi) **meça de verdade**.
**Pronto quando:** compila, o corte de 20 s sumiu e há antes/depois do T1−T0.

## FASE 2 — Falar por frases (maior ganho esperado; estimativa)
**Ideia:** não esperar a resposta inteira da IA nem o MP3 inteiro. Receber a IA em streaming, cortar a primeira frase pronta, mandar ao TTS e tocar numa fila enquanto o resto chega.
**Arquivos:** `ai/GroqProvider.kt` (o pedido tem `stream: Boolean = false`), `domain/JavisRepository.kt` (`sendMessage`, `speak`), `voice/ElevenLabsTts.kt`.
**Antes de implementar, confirmar na documentação oficial** (não verifiquei): como o Groq faz streaming (SSE) neste modelo; se o ElevenLabs tem endpoint de streaming e qual modelo é o de menor latência para a voz clonada.
**Cuidados:** `stopSpeaking()` precisa limpar a fila inteira; ordem das frases; erro no meio da fila cai no TTS nativo sem repetir frases; manter o comportamento atual quando a resposta é curta.
**Pronto quando:** T6−T3 (ou T4) cai de forma clara na tabela da Fase 0, e interromper a fala não deixa frase "fantasma" tocando.

## FASE 3 — Uma chamada de IA só, quando possível
**Hoje:** `routeWithAi` (ferramentas) e, em comandos sem resposta pronta, `primary.chat` de novo (2 round-trips seguidos). Falha → outro provedor inteiro; 429 → espera 1,5 s.
**Ideia:** a mesma chamada trazer a fala **e** a ferramenta, para falar enquanto a ação roda. Ver `ai/AiToolRouter.kt`, `ai/ToolCalling.kt` (**não li**; leia antes).
**Também avaliar (verificar disponibilidade):** modelo menor ou raciocínio baixo no roteamento; enviar só as ferramentas necessárias e menos histórico (hoje: 15 ferramentas + até 16 mensagens + prompt de sistema por chamada).
**Cuidado:** é mudança de comportamento. Teste todos os comandos existentes (abrir app, ligar, WhatsApp, alarme, lembrete, clima, Spotify, Bluetooth, calendário). Mantenha o fallback.
**Pronto quando:** T4−T3 some nos comandos, sem regressão nos comandos.

## FASE 4 — Ouvir enquanto fala (barge-in)
**Por que não ouve hoje:** fluxo em sequência; `SpeechRecognizer` de uma frase por vez; microfone fechado durante a fala; sem AEC, sem foco de áudio (detalhes em `DIAGNOSTICO_VOZ_GABI.md`).
**Plano:**
1. **Microfone próprio** com `AudioRecord`: 16 kHz, mono, PCM 16 bits (formato visto no Alexa; ver `PARAMETROS_DE_VOZ.md`, seção 1). Pedir **AEC** (`AcousticEchoCanceler`) com o `audioSessionId` e **checar `isAvailable()`**; se não houver, registrar e seguir (o Alexa também segue sem).
2. **Buffer circular de ~3 s** (48.000 amostras a 16 kHz) para não perder o começo da frase.
3. **Detector local leve** (VAD ou palavra de ativação; ver Fase 5) rodando **durante** a fala da Gabi.
4. Ao detectar a voz do usuário: **parar o `MediaPlayer`/TTS**, limpar a fila, soltar o foco de áudio e abrir a escuta.
5. **Foco de áudio** (`AudioManager`/`AudioFocusRequest`) ao falar e ao ouvir, reagindo aos 4 tipos de perda: ganho, perda transitória com abaixar volume, perda transitória (pausar), perda definitiva (parar). Ver `PARAMETROS_DE_VOZ.md`, seção 5.
**Atenção:** testar no aparelho real do Dannilo (Xiaomi). Disponibilidade e qualidade do AEC variam por aparelho. Se o AEC não funcionar nele, diga e proponha alternativa em vez de forçar.
**Pronto quando:** em teste real, falar por cima da Gabi interrompe a fala em ~1 s e a nova frase é reconhecida, sem ela se ouvir sozinha em loop.

## FASE 5 — Palavra de ativação real (item 1 do briefing)
Troca o filtro por texto (`ChatViewModel`: `containsWakeWord`) por um detector local. **Opções a avaliar** (não verificadas): Porcupine (exige chave pessoal do Dannilo; **pergunte, não crie conta**), openWakeWord (sem chave), Vosk. Mantenha o filtro de texto como fallback. O limiar `500` do Alexa é da escala do motor dele: **não aproveitar**.

## FASE 6 — Demais itens do briefing
Itens 2 (bateria), 3 (modo privacidade), 4 (voz do dono, **propor por escrito e esperar aprovação**), 5 (tela cheia), 6 (tipos de serviço), 8 a 15: ver `BRIEFING_PARA_CLAUDE_VSCODE.md`. Item 7 (biometria): **descartado**.

---

## Protocolo de teste em cada fase (no aparelho real)
Frases de teste, repetidas antes e depois:
1. Pergunta curta: "Gabi, que dia é hoje?"
2. Pergunta que gera resposta longa (mais de 20 s falada).
3. Comando: "Gabi, abre o YouTube."
4. Comando com confirmação: "Gabi, manda uma mensagem…" (e responder "sim").
5. Interromper a Gabi no meio da resposta.
6. Ambiente com TV ou música ao fundo.
Registre para cada uma: tempo até o **primeiro som** e se houve erro.

## O que reportar ao terminar cada fase
1. Arquivos alterados.
2. Números antes e depois (tabela da Fase 0).
3. Resultado de `assembleDebug`.
4. O que **não** foi testado.

## Perguntas para o Dannilo antes da Fase 4/5
- Modelo exato do Xiaomi (AEC varia por aparelho).
- Aceita criar uma conta gratuita no Picovoice (Porcupine) ou prefere uma opção sem chave?
- Quer manter o ElevenLabs como voz principal? (custo por caractere; confirmar o plano dele)
