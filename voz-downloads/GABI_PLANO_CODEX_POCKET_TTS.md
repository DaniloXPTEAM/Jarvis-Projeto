# GABI --- Plano Codex para voz local com Pocket TTS

## Objetivo

Modificar **o projeto Android existente da Gabi**, sem reescrever o
aplicativo, para:

1.  reduzir fortemente a demora entre o fim da fala do usuário e o
    começo da resposta;
2.  substituir gradualmente o ElevenLabs por **Pocket TTS
    local/offline**;
3.  reutilizar a voz de referência da Gabi já presente no projeto;
4.  manter **ElevenLabs como fallback** até o Pocket estar validado;
5.  preservar as animações e o comportamento atual do avatar;
6.  buscar **custo recorrente de TTS = R\$ 0**;
7.  priorizar resposta perceptivelmente imediata e áudio em streaming.

> **Regra principal para o Codex:** não faça uma grande refatoração de
> uma vez. Trabalhe em etapas pequenas, compiláveis e reversíveis. Não
> remova ElevenLabs até o Pocket TTS funcionar no aparelho real.

------------------------------------------------------------------------

## 1. O que já foi localizado no projeto

Projeto Android principal:

`android/`

Arquivos importantes do fluxo de voz:

-   `android/app/src/main/kotlin/com/gabi/assistant/voice/ElevenLabsTts.kt`
-   `android/app/src/main/kotlin/com/gabi/assistant/voice/JavisSpeechRecognizer.kt`
-   `android/app/src/main/kotlin/com/gabi/assistant/voice/JavisVoiceService.kt`
-   `android/app/src/main/kotlin/com/gabi/assistant/ui/viewmodel/ChatViewModel.kt`
-   `android/app/src/main/kotlin/com/gabi/assistant/ui/viewmodel/RoomViewModel.kt`
-   `android/app/src/main/kotlin/com/gabi/assistant/domain/JavisRepository.kt`
-   `android/app/src/main/kotlin/com/gabi/assistant/ui/components/AvatarAnimation.kt`
-   `android/app/src/main/kotlin/com/gabi/assistant/ui/components/FrameSequenceAnimation.kt`
-   `android/app/src/main/kotlin/com/gabi/assistant/ui/components/LoopingSceneVideo.kt`

### Gargalo confirmado no TTS atual

Em `ElevenLabsTts.kt`, o fluxo atual faz aproximadamente isto:

`texto -> HTTP ElevenLabs -> response.body.bytes() -> arquivo temporário -> MediaPlayer -> play`

O uso de `response.body?.bytes()` significa que o app espera o corpo do
áudio ser recebido antes de gravá-lo e iniciar a reprodução. Isso impede
uma experiência realmente progressiva/streaming.

### Gargalo confirmado no reconhecimento de fala

Em `JavisSpeechRecognizer.kt` existem atualmente:

-   `EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS = 2500`
-   `EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS = 1800`

Esses valores podem acrescentar uma espera perceptível antes mesmo de a
pergunta seguir para a IA.

Não assumir que todo aparelho/implementação do `SpeechRecognizer`
respeitará exatamente esses extras; medir no aparelho.

------------------------------------------------------------------------

## 2. Voz da Gabi encontrada no ZIP

Há uma pasta inteira de testes:

`voz-teste/`

Arquivos encontrados incluem:

-   `gabi_24l.wav`
-   `gabi_pequeno.wav`
-   `omni_gabi.wav`
-   `pk1_gabi24l_pergunta.wav`
-   `pk1_gabi24l_resposta.wav`
-   `pk2_frases_fim_pergunta.wav`
-   `pk2_frases_fim_resposta.wav`
-   `original_24l.wav`
-   `original_pequeno.wav`
-   `frases_24l.wav`
-   `frases_pequeno.wav`
-   vários testes `fem*`, `vc_*`, `pergunta_*` e `resposta_*`

Também existe na raiz:

`Gabi Voz isolada.mp3`

### Referência inicial recomendada

Começar o teste de clonagem com:

`Gabi Voz isolada.mp3`

Depois comparar objetivamente com:

`voz-teste/gabi_24l.wav`\
`voz-teste/gabi_pequeno.wav`\
`voz-teste/omni_gabi.wav`

Escolher a referência que preserve melhor timbre, dicção e naturalidade
em **pt-BR**. Não escolher apenas pelo nome do arquivo.

A referência de voz deve ser usada somente se houver autorização
legítima para seu uso/clonagem.

------------------------------------------------------------------------

## 3. Tecnologia alvo

Usar **Kyutai Pocket TTS**, preferencialmente com uma implementação
Android local baseada em ONNX/C++ que ofereça:

-   português;
-   inferência no aparelho;
-   voice cloning;
-   saída PCM progressiva/streaming;
-   cache do perfil/embedding da voz;
-   ARM64;
-   ausência de cobrança por minuto.

### Atenção

O repositório oficial do Pocket TTS é a referência do modelo, mas o
caminho Android pode exigir uma implementação comunitária/ONNX/JNI.

Antes de adicionar dependências:

1.  verificar licença do código;
2.  verificar licença dos pesos/modelos;
3.  fixar versão/commit utilizado;
4.  não baixar binários de procedência desconhecida;
5.  documentar no projeto de onde cada modelo veio.

------------------------------------------------------------------------

# 4. Arquitetura desejada

## Hoje

`microfone` → `Android SpeechRecognizer` → espera fim da fala → `IA` →
texto completo → `ElevenLabs` → download do áudio → arquivo temporário →
`MediaPlayer` → Gabi fala

## Alvo

`microfone` → detecção rápida de fim de turno → `IA` → texto/frases
disponíveis → `Pocket TTS local` → **PCM em streaming** → `AudioTrack` →
Gabi começa a falar imediatamente

O avatar deve entrar em `talking` **quando o primeiro áudio realmente
começar**, e não somente quando todo o áudio estiver pronto.

------------------------------------------------------------------------

# 5. Criar uma abstração de TTS

Não espalhar chamadas de Pocket TTS pela UI.

Criar algo semelhante a:

``` kotlin
interface GabiTtsEngine {
    suspend fun initialize(): Result<Unit>

    suspend fun speak(
        text: String,
        onAudioStarted: (() -> Unit)? = null,
        onDone: (() -> Unit)? = null,
        onError: ((Throwable) -> Unit)? = null
    )

    fun stop()
    fun isSpeaking(): Boolean
    fun isReady(): Boolean
}
```

Implementações:

``` text
ElevenLabsTtsEngine
PocketTtsEngine
AndroidTtsEngine (fallback final, se já for conveniente)
```

Criar um coordenador:

``` text
GabiTtsManager
```

Responsabilidade:

``` text
Pocket disponível? -> Pocket
Pocket falhou? -> ElevenLabs
ElevenLabs indisponível? -> Android TTS
```

Durante os testes, permitir selecionar manualmente o motor nas
configurações:

-   Automático
-   Pocket local
-   ElevenLabs
-   Android TTS

------------------------------------------------------------------------

# 6. PocketTtsEngine

Criar preferencialmente:

`android/app/src/main/kotlin/com/gabi/assistant/voice/PocketTtsEngine.kt`

Responsabilidades:

-   carregar runtime apenas uma vez;
-   carregar modelo português apenas uma vez;
-   carregar/cachear a voz da Gabi;
-   manter runtime aquecido enquanto a conversa estiver ativa;
-   receber texto;
-   gerar áudio progressivamente;
-   reproduzir PCM com `AudioTrack`;
-   cancelar geração imediatamente em `stop()`;
-   liberar recursos corretamente;
-   nunca bloquear a Main Thread.

## Cache da voz

Não recalcular a clonagem a cada frase.

Fluxo esperado:

``` text
primeira execução
-> referência de voz
-> encoder/conditioning
-> embedding/perfil da Gabi
-> salvar em armazenamento privado

execuções seguintes
-> carregar embedding/perfil salvo
-> falar
```

Se a implementação Android escolhida já possuir formato próprio de
cache/embedding, usar esse formato.

------------------------------------------------------------------------

# 7. Streaming de áudio

Evitar o padrão atual:

``` text
gerar tudo
-> salvar arquivo
-> MediaPlayer
```

Preferir:

``` text
Pocket gera chunk PCM
-> AudioTrack.write(...)
-> toca
-> próximo chunk
```

Objetivo: começar a ouvir a resposta assim que houver áudio suficiente
para reprodução estável.

Implementar buffer pequeno, mas evitar underrun.

Registrar:

-   tempo em que texto foi entregue ao TTS;
-   tempo do primeiro chunk;
-   tempo do primeiro som reproduzido;
-   duração total da síntese;
-   underruns, se disponíveis.

------------------------------------------------------------------------

# 8. Reduzir espera do SpeechRecognizer

Arquivo:

`JavisSpeechRecognizer.kt`

Hoje:

``` text
complete silence: 2500 ms
possibly complete: 1800 ms
```

Criar parâmetros centralizados e testar inicialmente algo próximo de:

``` text
complete silence: 800–1200 ms
possibly complete: 500–800 ms
```

Não aplicar cegamente.

Fazer teste A/B no aparelho porque implementações Android podem ignorar
ou interpretar esses parâmetros de maneira diferente.

### Meta

A Gabi deve perceber rapidamente que o usuário terminou, sem cortar
pessoas que fazem pequenas pausas naturais.

------------------------------------------------------------------------

# 9. IA: procurar chamadas duplicadas

Auditar:

-   `ChatViewModel.kt`
-   `RoomViewModel.kt`
-   `JavisRepository.kt`
-   providers em `ai/`
-   rotas de tools/comandos

Procurar situações em que uma pergunta normal resulte em:

``` text
requisição IA 1
-> processamento
-> requisição IA 2
-> TTS
```

Se a segunda chamada não for necessária, eliminar.

Não quebrar tool calling, comandos do aparelho ou memória.

Adicionar logs temporários:

``` text
[GABI_LATENCY] speech_final
[GABI_LATENCY] ai_request_start
[GABI_LATENCY] ai_first_result
[GABI_LATENCY] ai_complete
[GABI_LATENCY] tts_request
[GABI_LATENCY] tts_first_audio
[GABI_LATENCY] playback_start
[GABI_LATENCY] playback_end
```

------------------------------------------------------------------------

# 10. Resposta por frases

Se o provider de IA permitir streaming, não esperar necessariamente o
parágrafo inteiro.

Criar um pequeno segmentador.

Exemplo:

``` text
"Claro. Eu posso fazer isso para você. Primeiro vamos..."
```

Assim que existir uma frase suficientemente estável:

``` text
"Claro."
```

pode seguir para o TTS enquanto a IA continua gerando.

Depois:

``` text
"Eu posso fazer isso para você."
```

entra na fila.

### Regras

Não segmentar no meio de:

-   números decimais;
-   abreviações comuns;
-   URLs;
-   e-mails;
-   siglas;
-   valores monetários.

Não criar dezenas de pedaços minúsculos. Naturalidade é mais importante
que alguns milissegundos.

------------------------------------------------------------------------

# 11. Barge-in: deixar o usuário interromper a Gabi

Meta futura importante para sensação de conversa natural:

Enquanto a Gabi estiver falando, se o usuário começar a falar:

``` text
detectar voz
-> PocketTtsEngine.stop()
-> AudioTrack.flush()
-> avatar = listening
-> capturar nova fala
```

Não implementar isso antes de o Pocket básico estar funcionando.

Fazer como fase separada.

------------------------------------------------------------------------

# 12. Animações

Não remover as animações.

Foram encontrados componentes específicos de avatar/vídeo e os assets:

-   `gabi-idle.png`
-   `gabi-listening.png`
-   `gabi-talking.png`
-   sequências/MP4s da Gabi

A integração deve obedecer:

``` text
aguardando -> idle
usuário falando -> listening
IA processando -> thinking (se existir)
primeiro PCM tocando -> talking
áudio terminou -> idle/listening conforme o modo
```

### Regra

A animação **não pode bloquear a pipeline de áudio**.

Decodificação/preparação de vídeo não deve acontecer sincronamente no
caminho crítico:

``` text
speech -> IA -> TTS -> primeiro áudio
```

------------------------------------------------------------------------

# 13. Plano de implementação para o Codex

## Fase A --- Instrumentação

Antes de trocar tecnologia:

-   adicionar métricas `[GABI_LATENCY]`;
-   medir o app atual;
-   registrar baseline de 10 perguntas curtas.

Entregar tabela:

``` text
fim da fala -> IA
IA -> resposta
resposta -> TTS
TTS -> primeiro som
TOTAL
```

## Fase B --- SpeechRecognizer

-   diminuir experimentalmente o silêncio;
-   testar 10 frases;
-   confirmar que não corta fala.

## Fase C --- Abstração TTS

-   criar `GabiTtsEngine`;
-   adaptar ElevenLabs à interface;
-   garantir que o comportamento atual continua funcionando;
-   compilar e testar.

## Fase D --- Pocket local

-   integrar runtime Pocket TTS;
-   importar modelo pt/português;
-   criar `PocketTtsEngine`;
-   gerar uma frase simples com voz padrão;
-   somente depois integrar clonagem.

## Fase E --- Voz da Gabi

Testar referências:

1.  `Gabi Voz isolada.mp3`
2.  `voz-teste/gabi_24l.wav`
3.  `voz-teste/gabi_pequeno.wav`
4.  `voz-teste/omni_gabi.wav`

Gerar sempre as mesmas frases de avaliação:

``` text
"Oi, tudo bem? Eu sou a Gabi."
"Claro. Me conta o que aconteceu."
"Entendi. Vamos resolver isso juntos."
"Que bom falar com você novamente."
```

Escolher o melhor resultado e gerar/cachear o perfil de voz.

## Fase F --- Streaming

-   Pocket -\> PCM chunks -\> AudioTrack;
-   iniciar `talking` no primeiro áudio;
-   não criar WAV/MP3 temporário para cada resposta.

## Fase G --- IA em streaming

Somente depois de Pocket estável:

-   receber tokens/chunks;
-   detectar frases;
-   alimentar fila do TTS;
-   manter ordem;
-   permitir cancelamento.

## Fase H --- Barge-in

-   interrupção pelo usuário;
-   cancelar fala;
-   limpar fila;
-   voltar a ouvir.

------------------------------------------------------------------------

# 14. Metas de desempenho

Medir em aparelho real.

### Meta principal

`fim real da fala -> primeiro som da Gabi`

Alvos:

``` text
Excelente: < 1,0 s
Bom:       1,0–1,5 s
Aceitável: 1,5–2,0 s
Ruim:      > 2,0 s
```

Não maquiar métrica iniciando cronômetro depois do reconhecimento.

O cronômetro deve começar no momento em que o usuário efetivamente
termina de falar.

------------------------------------------------------------------------

# 15. Memória e bateria

Como o objetivo é uso prolongado, medir:

-   RAM do modelo;
-   RAM total do app;
-   CPU durante fala;
-   temperatura;
-   consumo de bateria;
-   tamanho dos modelos instalados;
-   tempo de cold start;
-   tempo de warm start.

Não manter processamento pesado ativo quando a conversa estiver ociosa
por muito tempo.

------------------------------------------------------------------------

# 16. Compatibilidade

Antes de fechar a implementação, documentar:

``` text
ABI suportada
Android mínimo
Android alvo
RAM mínima recomendada
espaço necessário para modelos
tempo de primeira instalação/download
comportamento offline
```

Se Pocket local não rodar adequadamente em determinado aparelho:

``` text
Pocket -> falha/indisponível -> ElevenLabs
```

A Gabi não deve ficar muda.

------------------------------------------------------------------------

# 17. Segurança do projeto

O `build.gradle.kts` atual possui chaves vindas de propriedades locais
para serviços como Groq, ElevenLabs e DeepSeek.

Durante esta alteração:

-   não colocar tokens no Git;
-   não imprimir chaves em Logcat;
-   não mover segredo para código-fonte;
-   não incluir token Hugging Face em APK distribuído;
-   para modelo gated, resolver aquisição/distribuição de forma
    compatível com a licença.

------------------------------------------------------------------------

# 18. O que NÃO fazer

Não:

-   apagar ElevenLabs no primeiro commit;
-   reescrever a UI;
-   remover as animações;
-   trocar simultaneamente STT + IA + TTS + avatar;
-   executar inferência pesada na Main Thread;
-   recalcular a clonagem da Gabi a cada resposta;
-   esperar áudio inteiro para começar playback;
-   criar arquivo temporário para cada frase se PCM streaming estiver
    disponível;
-   adicionar uma API paga sem necessidade;
-   afirmar que a otimização funcionou sem medir no aparelho.

------------------------------------------------------------------------

# 19. Critério de conclusão

Considerar a migração pronta quando:

-   [ ] Pocket TTS funciona offline;
-   [ ] português está natural o suficiente;
-   [ ] voz da Gabi está carregada a partir de perfil cacheado;
-   [ ] não há cobrança por minuto para o TTS local;
-   [ ] primeira fala começa progressivamente;
-   [ ] animação `talking` acompanha playback real;
-   [ ] `stop()` interrompe imediatamente;
-   [ ] ElevenLabs continua disponível como fallback;
-   [ ] app não trava durante síntese;
-   [ ] latência foi medida antes/depois;
-   [ ] teste de pelo menos 20 interações consecutivas passou;
-   [ ] cold start e warm start foram registrados;
-   [ ] modelo/licenças foram documentados.

------------------------------------------------------------------------

# 20. Primeira tarefa para o Codex

Execute primeiro **somente Fases A, B e C**.

1.  Analise o projeto existente.
2.  Adicione instrumentação de latência.
3.  Centralize os tempos de silêncio do reconhecimento.
4.  Crie a abstração `GabiTtsEngine`.
5.  Adapte o ElevenLabs existente sem alterar o resultado funcional.
6.  Compile.
7.  Corrija erros.
8.  Mostre quais arquivos foram modificados.
9.  Explique como medir a baseline no aparelho.
10. **Pare e aguarde validação antes de integrar Pocket TTS.**

Depois da validação, prossiga para Pocket local.

------------------------------------------------------------------------

## Resultado esperado

A arquitetura final deve privilegiar:

**Gabi rápida + voz local + offline + sem custo por minuto + animação
preservada + fallback seguro.**

O objetivo não é apenas "fazer o Pocket falar". O objetivo é transformar
a interação em uma conversa com baixa latência, na qual ouvir, pensar,
começar a falar e ser interrompida sejam partes de uma única pipeline
contínua.
