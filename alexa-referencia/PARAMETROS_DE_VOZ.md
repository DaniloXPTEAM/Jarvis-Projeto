# Parâmetros de voz extraídos do app do Alexa (REA)

Fonte: leitura das classes de áudio do APK `com.amazon.dee.app` 2.2.675097.0 com o REA (`inspect_android_class` / `inspect_android_method`, motor jadx-headless-mcp 0.7.1).
SHA-256 do APK analisado: `574919d5a769b92b6d3f8c892c49cc952dcbc2481601dedef8a65c3c83ed045a`.

> **Isto é descrição de valores e de lógica, em palavras próprias. Não é código copiado.** Implemente na Gabi com APIs do Android e código próprio.
> **Análise estática:** o app nunca foi executado. Onde eu **inferi** algo, está escrito "inferência". Onde **não consegui** o valor, está em "Não obtido".

---

## 1. Captação do microfone (classe `voice.pryon.asr.AudioCapturer`)

| Parâmetro | Valor encontrado | Observação |
|---|---|---|
| Taxa de amostragem | **16.000 Hz** | Escrito direto no código |
| Canais | código `16` | Corresponde a **mono** (`CHANNEL_IN_MONO`) no Android. Tradução do número é conhecimento meu |
| Formato | código `2` | Corresponde a **PCM 16 bits** (`ENCODING_PCM_16BIT`). Tradução do número é conhecimento meu |
| Fonte de áudio | **`1` por padrão**, **`6`** quando a configuração "legada" está ligada | Corresponde a `MIC` (1) e `VOICE_RECOGNITION` (6). Tradução é conhecimento meu |
| Tamanho do buffer | `samplesPerFrame × 2` bytes | `samplesPerFrame` vem do motor de detecção. **Valor não obtido** |
| Cancelamento de eco (AEC) | **Opcional**, ligado por uma flag | Se ligada: checa `AcousticEchoCanceler.isAvailable()`, cria com o `audioSessionId` do `AudioRecord` e ativa. Se o aparelho não suportar, só registra no log e **segue sem AEC** |
| Supressor de ruído | **Desligado** no caminho da palavra de ativação | O construtor recebe `false` para o supressor de ruído |
| Início | `startRecording()` logo após criar | Se falhar: libera o `AudioRecord` e lança erro |

**Leitura prática:** a captação é a configuração clássica de reconhecimento de voz (16 kHz, mono, 16 bits), com **AEC como opção** e **sem** supressor de ruído.

## 2. Detector de palavra de ativação (classe `wakeword.pryon.PryonConfigProvider`)

Existem duas configurações. A diferença é só a flag de baixa latência.

| Parâmetro | Config atual | Config "legada" |
|---|---|---|
| Limiar de detecção (`detectThreshold`) | **500** | **500** |
| Detecção de atividade de voz (`useVad`) | **desligada** | **desligada** |
| Baixa latência (`lowLatency`) | **desligada** | **ligada** |
| Lista de impressões digitais (mídia) | nenhuma | nenhuma |

> **Atenção:** o motor de detecção (chamado "Pryon" nas classes) é **proprietário**. O número `500` é a escala **dele**. **Não serve para Porcupine, openWakeWord ou outro detector.** Serve só como informação de que ele opera com limiar numérico fixo e sem VAD.

## 3. Buffer circular de pré-gravação (classe `wakeword.ShortRingBuffer`)

| Parâmetro | Valor |
|---|---|
| Capacidade | **48.000 amostras** de 16 bits (criado no `BufferingWakeWordDetector`) |
| Duração | **3 segundos** a 16 kHz (48.000 ÷ 16.000). A classe converte amostras em ms dividindo por 16 (16 amostras por ms), o que confirma os 16 kHz |
| Estrutura | vetor de `short` + posição de escrita + posição de início, com leitores independentes (`RingBufferReader`) |

**Para que serve (pelo comportamento do método `startTransferring`):** quando a palavra é detectada, uma **thread separada** lê o buffer a partir do ponto em que a palavra começou (há um ajuste de índices da palavra, `adjustWakeWordIndices`) e **envia o áudio em blocos** para o destino, **enquanto a pessoa ainda está falando**. Resultado: **o começo da frase não se perde** e o áudio já sai em fluxo, sem esperar a fala terminar.

Não obtido: `PREROLL_SAMPLE_SIZE` (quanto de áudio *antes* da palavra é enviado) e `ACTUAL_BUFFER_SIZE` (tamanho de cada bloco enviado).

## 4. Fluxo de threads (classes `PryonRecordingRunnable` e `WakeWordDetectingAudioCapturer`)

1. Uma `Runnable` própria inicia: confere o `samplesPerFrame` do motor (se vier `-37`, aborta com erro), mede quanto o microfone demorou para inicializar e entra no laço de captação.
2. Os campos da classe (`ringBuffer`, `samplesPushedToPryon`, flags `capturing`, `detectingWakeWord`, `wakeWordDetectionPaused`) indicam que o áudio alimenta **o detector e o buffer circular ao mesmo tempo** (**inferência**: eu não li o método `transferAudio`).
3. **A detecção pode ser pausada e retomada sem parar a captação** (existem `pauseDetectingWakeWord` / `resumeDetectingWakeWord` e a flag `wakeWordDetectionPaused`). Isso permite continuar gravando enquanto não procura a palavra.
4. O envio do áudio depois da detecção roda numa **thread executora separada**, para não travar a captação.

## 5. Foco de áudio da conversa (classe `AudioFocusManager`)

**Ao pedir o foco (`requestAudioFocus`):**
- Antes de pedir, registra se **já havia música tocando** (só se ele ainda não tinha o foco) e se um **player externo** estava tocando. Isso é repassado na notificação de foco ganho, para depois saber o que retomar.
- **Cria um pedido novo, marca o antigo como inativo, pede o novo e só então solta o antigo, se o novo for concedido.** Ou seja: a troca de dono do foco **não deixa intervalo** sem foco.
- Se o sistema não conceder, retorna falha.

**Ao mudar o foco (`onAudioFocusChange`):** reage por código. Os nomes abaixo são os do Android (conhecimento meu); os números estão no código.

| Código | Nome no Android | O que o app faz |
|---|---|---|
| `1` | GAIN | marca que tem foco; **traz a interação do topo para a frente** e **manda as outras para segundo plano** |
| `-3` | LOSS_TRANSIENT_CAN_DUCK | marca que perdeu o foco; **manda todos os canais para segundo plano** (volume baixo) |
| `-2` | LOSS_TRANSIENT | marca que perdeu o foco; **pausa todos os canais** |
| `-1` | LOSS | avisa a perda, zera as flags de foco, **para e limpa** os canais que exigem foco de áudio |

A mudança é feita **dentro de um bloco sincronizado** sobre os canais (evita corrida entre threads).

**Estrutura de "canais":** o app mantém canais de áudio com prioridade e uma coleção ordenada por duração de foco (`audioMetadataByType`), e escolhe o "melhor" (`getBestAudioMetadata`). Não li as prioridades.

Não obtido: `OVERRIDE_FOCUS_DURATION_VALUE`, `OVERRIDE_FOCUS_POLICIES`, e a lista de canais e prioridades.

---

## 6. O que isto NÃO prova (importante)
- **Não verifiquei se o Alexa mantém a detecção ligada enquanto fala** (interrupção / "barge-in"). O que existe é: a flag de AEC, a pausa/retomada da detecção e o buffer circular, que são as **peças** que permitiriam isso. Quem chama a pausa e quando, **não li**.
- **Não verifiquei quem liga a flag de AEC** (em que situação ela vem `true`).
- **A voz de resposta não está aqui.** Esta análise é só da captação, do detector e do foco de áudio. Não analisei como a resposta falada é gerada ou tocada.
- Os números do motor de detecção (`500`, `samplesPerFrame`) não são portáveis.

## 7. Como isto se compara com os problemas da Gabi (hipóteses a CONFIRMAR lendo o código da Gabi)
| Problema do Dannilo | O que o Alexa faz (fato extraído) | Hipótese para a Gabi (não verificada) |
|---|---|---|
| Não ouve enquanto fala | Captação separada da fala, AEC opcional, detecção pausável sem parar a captação, buffer de 3 s | A Gabi talvez pare o microfone ou o reconhecedor durante a fala |
| Resposta lenta | Envia o áudio em blocos **enquanto a pessoa ainda fala**, por thread própria | A Gabi talvez só envie depois que a fala termine, e fale só depois da resposta completa |
| Perde o começo da frase | Pré-gravação de 3 s no buffer circular | A Gabi talvez só comece a gravar depois do gatilho |
| Conflito com música / chamadas | Foco de áudio por prioridade, troca sem intervalo, reação a 4 tipos de perda | A Gabi talvez não trate perda de foco |

**Próximo passo para quem implementar:** ler o código de voz da Gabi (`voice/`, `ChatViewModel`) e **medir** onde está o atraso **antes** de aplicar qualquer item acima.
