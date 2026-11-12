# Diagnóstico de voz da Gabi (leitura do código, nada foi executado nem alterado)

Arquivos lidos (todos em `android/app/src/main/kotlin/com/gabi/assistant/`):
`voice/JavisSpeechRecognizer.kt`, `voice/ElevenLabsTts.kt`, `voice/AndroidTtsFallback.kt`, `voice/JavisVoiceService.kt`,
`ui/viewmodel/ChatViewModel.kt`, `domain/JavisRepository.kt` (linhas 150-395), `ai/GroqProvider.kt`, `ai/AiProvider.kt`, trecho de `ai/DeepSeekProvider.kt`.

> **Limite:** foi só leitura. **Não medi tempo nenhum** e não rodei o app. A ordem de importância abaixo é **estimativa**. O primeiro passo de quem implementar é **medir** (ver seção 4).

## 1. Por que ela NÃO ouve enquanto fala (causa: é o desenho do fluxo)

1. **Fluxo estritamente em sequência.** `JavisRepository.sendMessage()` só devolve depois que `speak()` termina de tocar o áudio (`speak` é `suspend` até o fim). Só então `ChatViewModel.sendMessage()` cai no `finally` e chama `continueListeningIfContinuous()`, que religa o reconhecedor. O próprio código comenta isso de propósito: *"é seguro voltar a ouvir sem captar a própria voz dela"* (ChatViewModel.kt ~linha 219).
2. **O reconhecedor é o `SpeechRecognizer` do sistema**, usado em modo "uma frase por vez". Cada turno exige novo `startForegroundService` → `startListening`. Durante a fala da Gabi o microfone **nem está aberto**.
3. **Mesmo que estivesse aberto, ela ouviria a si mesma:** não há cancelamento de eco. Busquei no código ativo por `AudioRecord`, `AcousticEchoCanceler`, `VOICE_COMMUNICATION`, `AudioFocus`, `AudioAttributes` e **não achei nenhum**. O `SpeechRecognizer` também não deixa escolher a fonte de áudio nem ligar AEC.
4. **Não existe caminho de "interromper":** `stopSpeaking()` existe, mas só é chamado ao ligar o modo contínuo. Nada o chama quando o usuário começa a falar.
5. **A palavra "Gabi" é filtrada depois do reconhecimento** (texto), então cada frase falada fora da janela de 60 s precisa ser reconhecida inteira só para ser descartada.
6. **Sinal de instabilidade no aparelho:** o próprio código comenta que o serviço de voz do aparelho (Xiaomi/AGSA) devolve o erro `-1` "o tempo todo", mesmo com o usuário falando. Isso causa reinícios de escuta.

## 2. De onde vem o atraso (fatos do código, em ordem do que acontece)

| # | Etapa | O que o código faz | Efeito |
|---|---|---|---|
| 1 | Fim da fala | `EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS = 2500` e `..._POSSIBLY_COMPLETE_... = 1800` (JavisSpeechRecognizer.kt, `buildIntent`). O comentário diz que foram **aumentados** (antes 1500/1000) porque cortava a fala | Até **~1,8 a 2,5 s de espera fixa** depois que a pessoa para de falar (se o reconhecedor respeitar esses valores; alguns ignoram) |
| 2 | Preparo | grava a mensagem no banco, lê 20 mensagens, monta resumo de memória, lê preferências (`DataStore`) | pequeno, mas tudo em sequência |
| 3 | **IA, chamada 1** | `routeWithAi` → `chatWithTools` no Groq, modelo **`openai/gpt-oss-120b`**, sem streaming, `max_tokens = 1024`. Envia prompt de sistema + **15 ferramentas** (o comentário do código diz que isso passa de 1000 tokens fixos por chamada) + até 16 mensagens | Espera a resposta **inteira** antes de seguir. O pedido **não define** `reasoning`/esforço de raciocínio (busquei no código e não há) |
| 4 | **IA, chamada 2 (só em comandos)** | se a IA devolveu uma ferramenta e a ação não gera resposta pronta, chama `primary.chat(...)` **de novo** | Dois round-trips seguidos para pedidos do tipo "abre o YouTube" |
| 5 | Falhas | se o Groq falha, chama o **DeepSeek** (outro round-trip inteiro). Erro 429/402 → `delay(1500)` e tenta de novo | Cada falha soma segundos. O comentário do código diz que o plano grátis do Groq limita **8000 tokens/min**, e cada chamada já gasta muito disso |
| 6 | **Voz** | `ElevenLabsTts.speak`: **uma requisição só para o texto inteiro**, espera `response.body?.bytes()` (o MP3 completo), grava arquivo temporário, `MediaPlayer.prepareAsync()`, só então toca. Modelo `eleven_turbo_v2_5`, timeouts de conexão 20 s e leitura 30 s | Nada começa a tocar até o áudio **todo** estar pronto. Resposta longa = espera longa |
| 7 | Reprodução | `speak()` bloqueia até acabar de tocar | Atrasa também a volta da escuta |

## 3. Um bug encontrado de passagem (relatório; não corrigi)
- `ElevenLabsTts.speak` envolve **a reprodução inteira** em `withTimeoutOrNull(20_000)`. Se o áudio tiver **mais de 20 s**, o app corta a fala no meio (`stopSpeaking()`) e segue. Mesmo padrão em `AndroidTtsFallback.speak` (também 20 s). Como `max_tokens` é 1024, respostas longas são possíveis.

## 4. O que fazer, em ordem (propostas; nada foi aplicado)
**Passo 0 — medir** (obrigatório antes de otimizar). Registrar, por turno, com `SystemClock.elapsedRealtime()`: fim da fala do usuário → resultado final do reconhecimento → fim da chamada de IA 1 → (IA 2 se houver) → resposta do ElevenLabs recebida → primeiro som. Isso diz qual etapa mais pesa, em vez de chutar.

Depois, candidatos (cada um precisa ser **verificado na documentação oficial** antes de implementar):
1. **Falar por frases:** receber a resposta da IA em streaming, cortar na primeira frase pronta e já mandar ao ElevenLabs, tocando numa fila enquanto o resto chega. (Verificar o endpoint de streaming do ElevenLabs e a opção de streaming do Groq.)
2. **Uma chamada de IA só** quando possível: deixar a mesma chamada trazer fala + ferramenta, para evitar a chamada 2.
3. **Modelo/raciocínio mais leve no roteamento:** avaliar um modelo menor ou configuração de raciocínio baixo no Groq (verificar o que está disponível; **não confirmei**).
4. **Cortar tokens fixos:** só enviar as ferramentas necessárias e menos histórico no roteamento.
5. **Fim de fala mais rápido:** o valor de 2,5 s existe para não cortar pausas. Alternativa: detectar o fim da fala por conta própria (VAD) com `AudioRecord`, em vez de depender do `SpeechRecognizer`.
6. **Ouvir enquanto fala:** abrir o microfone próprio (`AudioRecord`, 16 kHz, mono, PCM 16 bits — o mesmo formato visto no Alexa, ver `PARAMETROS_DE_VOZ.md`) com **cancelamento de eco**, um detector local leve, e ao detectar voz: parar o `MediaPlayer`, soltar o foco de áudio e abrir a escuta. Hoje a Gabi **não trata foco de áudio**.
7. **Foco de áudio:** pedir e soltar foco ao falar/ouvir e reagir à perda de foco (modelo em `PARAMETROS_DE_VOZ.md`, seção 5).
8. **Corrigir o corte de 20 s** (seção 3).

## 5. Fora de escopo deste diagnóstico (não li)
`ai/AiToolRouter.kt`, `ai/ToolCalling.kt` (definição das 15 ferramentas), `ai/CommandParser.kt`, telas Compose, avatar/Orb. A estimativa da seção 2 não inclui nada disso.
