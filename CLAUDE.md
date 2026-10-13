# Gabi — Assistente Pessoal (contexto do projeto)

Este arquivo é lido automaticamente no início de cada sessão. Mantenha-o atualizado
conforme o projeto evolui — ele existe para evitar ter que reler o código inteiro
para "redescobrir" o projeto a cada nova sessão.

## O que é

App de secretária pessoal com IA, nome **Gabi** (ex-"Jarvis"/"Nexus"). Dois códigos
distintos convivem neste repositório:

1. **App Web** (`src/`) — Next.js, SEM USO desde 20/08/2026; o usuário pediu para
   removê-lo (21/09) e a remoção ainda NÃO foi feita — ver "Sessão 20–21/09/2026" no fim.
2. **App Android nativo** (`android/`) — fork de `agmanly597/javis` (MIT), Kotlin,
   arquitetura própria e independente do app web. Ver `AUDITORIA.md` (37 recursos
   auditados, 05/08/2026) antes de mexer nele.

Documentos de referência já existentes (não duplicar aqui, só consultar):
`SPEC.md` (spec visual original), `AUDITORIA.md` (auditoria do Android),
`ANALISE_REPOS.md`, `RENDER-GUIA.md`, `VERIFICACAO.md`.

## Backlog / começar a próxima sessão por aqui

> **➡️ Sessão de 13/10/2026 (a mais recente):** auditoria do que mudou no app desde
> 25/08/2026 — achado trabalho real feito pelo usuário com **outro agente de IA
> (Codex/ChatGPT) entre 13 e 14/09/2026**, nunca documentado aqui antes. Também:
> **nenhum commit novo desde `b0ac7ba` (25/08)** — tudo depois disso (rodadas 7-12 do
> WhatsApp, Quarto da Gabi, pipeline de vídeo, e agora o trabalho do Codex) está só como
> mudança não commitada, recuperável mas sem ponto de restauração. Detalhes completos na
> seção **"Auditoria 13/10/2026 — trabalho do Codex + estado do git"** no FIM deste
> arquivo — leia-a antes de mexer em `HomeScreen.kt`, `RoomScreen.kt`, ou nos dois
> sistemas de animação do avatar que agora coexistem.

> **➡️ Sessão de 20–21/09/2026:** diagnóstico do "ela não ouve" com o
> celular real via ADB + busca de uma voz gratuita da afilhada para a Gabi. O estado real
> e os próximos passos estão na seção **"Sessão 20–21/09/2026"** no FIM deste arquivo —
> leia-a antes do resto. Nenhum código do app foi alterado nela.

> **📍 AUDITORIA — status real de cada função (25/08/2026, leia isto
> primeiro — supersede os status antigos abaixo).** Pedida pelo usuário
> depois de várias rodadas de bugs em sequência. Cada item marcado pelo
> nível de confiança real, não só "eu escrevi o código":
>
> **✅ Confirmado funcionando pelo usuário, no aparelho real:**
> - Buscar/tocar vídeo específico no YouTube por voz (`search_youtube`) —
>   funcionou "redondo" testando "trailer do Senhor dos Anéis".
> - WhatsApp: o app ABRE de verdade agora (fix do `<queries>` no
>   manifesto, 8ª rodada) — a parte de abrir está resolvida.
> - Onboarding, permissões básicas, navegação geral.
>
> **🔧 Corrigido nesta sessão, AINDA SEM confirmação do usuário** (ou
> confirmado que ainda falha depois do fix mais recente):
> - **WhatsApp — enviar mensagem**: 4 causas diferentes já corrigidas em
>   sequência (serviço de acessibilidade não avisado direito, campo de
>   busca não escrevendo de verdade, emoji no nome do contato, e clicar na
>   seção "Contato" em vez de "Conversa" na busca) — usuário reportou
>   "continua igual" depois do último fix (12ª rodada), então **ainda tem
>   pelo menos mais uma causa não identificada**. Sem `uiautomator dump`
>   ao vivo da tela de busca real do WhatsApp do usuário, o resto é
>   tentativa e erro — ver proposta de ADB sem fio mais abaixo.
> - **Trava geral do app** ("preciso limpar a conversa e reabrir")
>   depois de pedir pra tocar música ou mandar WhatsApp — causa raiz
>   quase certa: `ElevenLabsTts.speak()` (fix da 11ª rodada, que faz
>   `speak()` esperar o áudio terminar de verdade) podia ficar esperando
>   pra sempre um callback do MediaPlayer que às vezes não disparava,
>   travando a coroutine — e como isso está no meio do fluxo de TODA
>   mensagem, travava o app inteiro. Corrigido nesta rodada (25/08) com
>   timeout de 20s (`withTimeoutOrNull`) como rede de segurança, e
>   `prepare()` síncrono (podia travar a UI sozinho) trocado por
>   `prepareAsync()`. **Não confirmado ainda.**
> - Modo de escuta contínua (toque-pra-ativar, sem segurar) — implementado
>   (10ª rodada) e depois religado ao `JavisVoiceService` (foreground
>   service de microfone) pra sobreviver com outro app em primeiro plano
>   (11ª rodada, depois do usuário reportar que morria com o YouTube
>   aberto). Não confirmado se resolveu de vez.
> - Confirmação por voz ("sim"/"não" falado em vez de botão) — bug de
>   pontuação corrigido (11ª rodada: "Sim." não batia com "sim"). Não
>   confirmado se era essa a causa real do problema original.
> - "Gabi, vai dormir" (desliga a escuta contínua por voz, sem fechar o
>   app) — implementado, não testado.
> - Notificações "sem permissão" mesmo com permissão ativa — trocado
>   pra checar o OS de verdade (`NotificationManagerCompat`) em vez de só
>   uma variável interna (7ª rodada). Não testado depois do fix.
> - "Quarto da Gabi" (tela nova de ambientação, pose por horário do dia)
>   — implementada com os assets novos do usuário (25/08). Não visto
>   rodando ainda (sem emulador nesta sessão).
>
> **❌ Sabidamente ainda quebrado ou incompleto:**
> - **Spotify**: só abre busca (sem tocar sozinho), e mesmo isso é
>   instável — `spotify:search:` não é oficial e tem bug documentado pela
>   própria comunidade Spotify (tela em branco). Resolver de vez precisa
>   da Web API de busca deles (Client ID/Secret grátis) — **perguntei ao
>   usuário se quer criar essa chave, sem resposta ainda**.
> - **Bluetooth**: só enxerga aparelhos JÁ pareados
>   (`BluetoothController.bondedDeviceNames()`) — não tem
>   `startDiscovery()`/`BroadcastReceiver` pra achar aparelho novo por
>   voz. Não implementado.
> - **Cast/espelhamento de tela**: não implementado. Usuário foi
>   explícito — quer que a Gabi abra a tela NATIVA de Cast do Android
>   (tipo `Settings.ACTION_CAST_SETTINGS`), não que ela reimplemente
>   Chromecast.
> - **Palavra de ativação "Gabi" com o app fechado** (tipo Alexa/Siri):
>   não implementado. Picovoice (a opção mais fácil) **descontinuou o
>   plano grátis em 30/06/2026** e não vai ter mais nível não-comercial —
>   a alternativa de graça (openWakeWord) exige treinar um modelo
>   customizado do zero (bem mais trabalho).
> - **Câmera tirando foto sozinha**: só abre o app de câmera (limitação
>   de intent implícito) — precisaria CameraX pra captura programática.
>   Usuário decidiu deixar pra depois.
> - **Quarto da Gabi — mecânica de jogo** (alimentar, stats, trocar de
>   roupa de verdade): só a v1 de ambientação existe; interação real
>   ainda não começou, combinado explicitamente com o usuário.
>
> **Recomendação pra próxima sessão**: os bugs de WhatsApp (envio) e a
> trava geral são os únicos itens realmente urgentes agora — e ambos se
> beneficiariam MUITO de depuração ao vivo (ADB sem fio no celular real
> do usuário, sem precisar de emulador) em vez de mais rodadas de
> tentativa-e-erro cegas. Vale propor isso primeiro na próxima sessão.

> **📍 Status ao fim do dia 21/08/2026.** O dia inteiro
> foi de bug-hunting real (não construção de feature nova). Achados 3 bugs
> sérios em sequência, cada um mascarando o próximo, todos corrigidos e o
> último confirmado ao vivo num emulador Android rodando nesta máquina:
> 1. **Mensagens processadas duas vezes** (`sharedChatViewModel` — Home e
>    Chat brigando pelo mesmo `speechRecognizer`) — corrigido e confirmado.
> 2. **Histórico mandado pra IA em ordem invertida** (`getRecentMessages`
>    DESC + `.takeLast()` errado) — o bug raiz por trás da maioria dos "ela
>    não responde o que eu pergunto" do dia. Corrigido e confirmado.
> 3. **`save_memory` disparando em cima de perguntas**, não só afirmações —
>    prompt ajustado, confirmado.
> Também: modelo do Groq trocado (o antigo foi descontinuado), rate limit
> mitigado, corrida de inicialização no TTS nativo corrigida, fallback da
> ElevenLabs pro TTS nativo quando falha. Ver a seção **"Bug crítico de
> ordem de mensagens..."** mais abaixo pro relato completo com evidência.
> **✅ ElevenLabs resolvido de ponta a ponta** (era pendência, agora não é
> mais): plano grátis da ElevenLabs **não permite nenhuma voz pronta via
> API** (nem a Roberta nem a Rachel padrão — confirmado testando por HTTP
> direto, erro `payment_required`/`paid_plan_required`), então o usuário
> assinou o plano pago deles. Depois disso apareceu um bug engraçado mas
> real: a Gabi respondia em **espanhol**. Causa: `eleven_turbo_v2` só
> adivinha o idioma pelo texto (sem trava), e confunde pt/es em frases
> curtas — e nem aceita o parâmetro `language_code` pra travar. Trocado
> pra `eleven_turbo_v2_5` + `language_code: "pt"` (testado por HTTP antes
> de compilar, confirmado retornando áudio válido). Ver seção **"ElevenLabs
> — saga completa"** mais abaixo pro passo a passo com as duas pegadinhas
> reais (ID da chave vs chave de verdade; plano grátis não serve).
> **Emulador Android (`gabi_test`) foi encerrado a pedido do usuário**
> (antivírus confundiu com ameaça — falso positivo comum com emuladores,
> mas ele preferiu não arriscar). SDK/AVD continuam instalados
> (`C:\Users\Dannilo\AppData\Local\Android\Sdk`) caso queira subir de novo
> numa sessão futura — não precisa reinstalar nada, só rodar o comando de
> novo (ver seção do emulador). **Não foi testado no celular real ainda**
> depois dessas correções — esse é o próximo passo natural.

Sessão de 21/08/2026 (2º dia) com o app já rodando de verdade no aparelho do
usuário — primeira vez com feedback real, não só compilação.

**Resolvido nesse 2º dia:**
- ✅ **Bug de confirmação de ações não usando o roteamento por IA** — corrigido.
  `JavisRepository.sendMessage()` agora retorna `SendMessageOutcome(reply,
  pendingCommand)`; a decisão de exigir confirmação sai do roteamento por IA
  (ou do fallback regex), não de uma checagem paralela no `ChatViewModel`.
  De quebra, corrigido um segundo bug no mesmo trecho: as mensagens de status
  ("Ligando para X…", "Beleza, não vou fazer isso.") eram mandadas via
  `repository.sendMessage(...)`, que as tratava como se fossem **fala do
  usuário** e reprocessava tudo de novo pela IA — criado
  `repository.sayAsAssistant(texto, sessionId)` só pra isso.
- ✅ **Bug real do usuário testando no aparelho**: botões do app inacessíveis
  porque os botões padrão do Android (navegação) ficavam por cima. Causa:
  `PermissionsScreen.kt` era a única tela sem `Scaffold`/`navigationBarsPadding`
  (as outras 5 usam `Scaffold` + `JavisBottomBar`, que já trata isso sozinho) —
  corrigido com `.safeDrawingPadding()`.
- ✅ **Tela de Permissões órfã**: existia no código mas nada nunca navegava
  pra ela — usuário nunca via o pedido de contatos/agenda/acessibilidade.
  Criado `OnboardingViewModel.kt`; `JavisNavGraph` agora lê `userOnboarded`
  (`JavisPreferences`) e decide a `startDestination` dinamicamente
  (Permissões na primeira vez, Home depois); o botão "Continuar" marca
  `userOnboarded=true` e sai da pilha de navegação (`popUpTo inclusive`).
- Revisão geral pedida pelo usuário ("falta algo comum a esse tipo de
  projeto?") também conferiu: **backup rules já excluem sharedpref/database**
  (protege a criptografia das chaves de restaurar num aparelho diferente —
  não precisou mexer), canais de notificação já existem. **Zero testes
  automatizados** segue como gap conhecido, não urgente pro uso pessoal atual.
- ✅ **Reforma visual — mockup "Céu de Outono" aprovado por ora.** Personagem
  real (ruiva, vestido vermelho) trocada pela arte nova do usuário (pasta
  `Jarvis Projeto\Gabi\ganiavatar`, mais parecida com a afilhada) — já copiada
  pros assets do app (`gabi-idle/listening/talking.png`). Mockup final:
  https://claude.ai/code/artifact/2415bf5d-f3e5-49e3-b23b-cccc948062f9 (avatar
  grande, "toque na Gabi pra conversar", cards compactos, barra de baixo com
  o rosto real recortado via CSS, sem sobreposição). ✅ **Aplicado no código
  real**: `Theme.kt` (paleta dourado/teal — mesmos nomes de token
  Cyan/Orange/BlueAccent, só valores trocados, pra não precisar renomear em
  10 arquivos), `HomeScreen.kt` (gradiente de fundo, avatar maior, cards
  "vidro" com chip de ícone + barra de destaque usando o dado real do
  dashboard), `JavisBottomBar.kt` (novo `GabiFaceAvatar` em `GabiCharacter.kt`
  — recorte circular só do rosto — no lugar central da barra, onde antes
  era o ícone genérico de Chat). `assembleDebug` completo confirmado.
- ✅ **Dashboard com dado real na Home.** Os 3 `QuickCard` em `HomeScreen.kt`
  agora usam dado de verdade: clima via novo `WeatherAgent.getCurrentWeatherSummary()`
  (struct `WeatherSummary`, não só a frase falada), lembretes de hoje via nova
  query `ReminderDao.observeDueTodayCount()`, avisos não lidos via nova query
  `NotificationDao.observeUnreadCount()`. Tudo combinado em
  `HomeViewModel.dashboard: StateFlow<HomeDashboardState>`.
- ✅ **WhatsApp: fallback de busca.** `JavisAccessibilityService.openWhatsAppChatByName()`
  antes só achava contato já visível na lista de conversas recentes. Agora,
  se não achar, abre a busca do WhatsApp (`openWhatsAppSearch()`), digita o
  nome (`typeIntoWhatsAppSearch()`) e tenta de novo. Mesma assinatura pública,
  nenhum outro arquivo precisou mudar.

- ✅ **Gerenciamento de arquivos** (`AUDITORIA.md` #33) — antes só existia
  `FileHelper` morto (pacote `com.javis.assistant`, fora do sourceSet
  compilado, nunca migrado no rebranding). Construído do zero via Storage
  Access Framework (`ACTION_OPEN_DOCUMENT_TREE`, sem precisar de
  `MANAGE_EXTERNAL_STORAGE`): novo `FileManager.kt` (listar/abrir/renomear/
  apagar via `DocumentFile`), `FilesViewModel.kt` (pilha de pastas,
  breadcrumb), `FilesScreen.kt` (navegar, abrir arquivo, renomear, apagar
  com confirmação). Acesso por Ajustes → "Gerenciar arquivos" (não entrou na
  barra de baixo, que já tem os 5 slots do mockup ocupados). Pasta escolhida
  persiste entre reinícios (`JavisPreferences.filesTreeUri`).
- ✅ **Memória: Gabi agora aprende de verdade em conversa, em português.**
  O usuário percebeu que o "aprendizado automático" (`learnFromConversation`
  em `MemoryManager.kt`) usava regex **só em inglês** ("i like...", "every
  morning...") — silenciosamente não capturava nada em português, incluindo
  o caso de uso real: "quando eu disser bom dia Gabi, me diga a data, hora e
  clima". Confirmado que a Memória **é** injetada no prompt da IA a cada
  mensagem (`buildContextSummary()`), então o problema era só a escrita, não
  a leitura. Removido o regex quebrado; criada ferramenta de IA real
  `save_memory` (`ToolCalling.kt`/`AiToolRouter.kt`/`CommandType.SAVE_MEMORY`
  → `JavisRepository.resolveSkillReply`), com instrução no
  `JavisPersonality.SYSTEM_PROMPT` pra Gabi chamá-la sempre que o usuário
  ensinar algo sobre si (nome, preferência, rotina, hábito), sem precisar
  pedir "salva isso" explicitamente.
- ✅ **Auditoria leve pós-mudanças do dia** (compilação + `assembleDebug` já
  vinham passando, mas isso não pega bug de robustez em runtime). 3 achados
  reais, todos corrigidos:
  1. `FilesViewModel` chamava `fileManager.openTree(...)` (I/O real via
     `DocumentFile`/`ContentResolver`) direto na coroutine principal — agora
     em `withContext(Dispatchers.IO)` nos dois pontos (`init` e
     `onFolderPicked`).
  2. `sendWhatsAppMessage` chamava os métodos do `JavisAccessibilityService`
     (que usam `Thread.sleep` por dentro) direto na thread principal — com o
     novo fallback de busca isso podia travar a UI por até ~1s a mais.
     Agora envolvido em `withContext(Dispatchers.IO)`.
  3. `formatSize()` duplicado (`FileManager.kt` tinha uma cópia morta, não
     chamada por ninguém — `FilesScreen.kt` já tinha a própria). Removida a
     cópia morta.
  Também: `persistTreePermission` ganhou try/catch pra `SecurityException`
  (alguns provedores raros não suportam permissão persistente), e o import
  de `Screen.Files` em `SettingsScreen.kt` foi limpo (estava com FQN inline).
  `assembleDebug` final confirmado com os 3 assets da personagem
  corretamente empacotados no APK (verificado via `unzip -l`).

**Testando no aparelho real, não só compilando** — mudou nesta sessão: o
usuário já rodou o app de verdade e reportou o primeiro bug real (insets).
Esperar mais rodadas de teste real conforme features forem usadas de fato
(voz reconhecendo direito, chroma key/crossfade renderizando bem, etc.).

## Caçada de bugs reais no aparelho (21/08/2026, 3ª rodada)

Usuário instalou o APK de verdade e reportou "nada funciona": IA não
responde, sem som nenhum, botões de navegação sem efeito, avatar navegando
pra tela errada. Achados, todos confirmados e corrigidos:

1. ✅ **Causa raiz principal: modelo do Groq descontinuado.** A chave nativa
   (`gsk_TrOU...`) sempre foi válida — mas `llama-3.3-70b-versatile`
   (`GroqProvider.MODEL`) retornava 404 `model_not_found` em toda chamada,
   confirmado via `curl` direto na API (sem precisar do celular). Trocado
   pra `openai/gpt-oss-120b` (testado ao vivo: chat normal e tool-calling
   ok). Isso sozinho explicava a cascata: "não responde nada" → "sem som"
   (o `speak()` só roda depois de uma resposta de IA bem-sucedida).
2. ✅ **Rate limit do plano grátis (8000 tokens/min, a conta inteira).**
   Cada mensagem manda o prompt de sistema (~1730 chars) + as 15 definições
   de ferramentas (~7900 chars de código-fonte) — isso é peso FIXO por
   mensagem, não só uma vez. Duas mensagens seguidas bastam pra estourar.
   `x-ratelimit-reset-tokens` mostra recuperação em frações de segundo, não
   minutos — então: (a) `GroqProvider` ganhou retry automático em 429/402
   (`withRateLimitRetry`, 1 nova tentativa após 1.5s), (b) prompt de sistema
   enxugado de 1730→1043 chars, (c) descrições das 15 ferramentas enxugadas
   (arquivo 7907→6284 chars) — tudo preservando o comportamento, só cortando
   verbosidade que não ajudava o modelo.
3. ✅ **Bug real e sério: corrida de inicialização no TTS nativo.**
   `AndroidTtsFallback` inicia o motor de forma assíncrona (callback do
   Android); se `speak()` fosse chamado antes desse callback terminar — bem
   provável logo na saudação, ao abrir o app — o código antigo só checava um
   boolean (`isReady`) e desistia **em silêncio total, sem erro, sem
   exceção**. Trocado por `CompletableDeferred<Boolean>` que `speak()`
   espera de verdade (timeout de 5s) antes de desistir. Também: antes, se o
   pacote de idioma pt-BR não estivesse instalado no aparelho
   (`LANG_MISSING_DATA`), ficava mudo pra sempre — agora tenta falar mesmo
   assim (idioma padrão do motor é melhor que silêncio total).
4. ✅ **Erros da IA agora aparecem pro usuário.** `ChatViewModel.sendMessage`
   não tinha nenhum tratamento de falha — se `repository.sendMessage`
   falhasse ou explodisse sem passar por `Result`, a tela ficava travada em
   "Pensando…" pra sempre, sem pista nenhuma do motivo. Agora tem
   try/catch/finally: qualquer falha vira uma mensagem de erro visível (foi
   assim que se descobriu o 402 de rate limit, ao vivo, com o usuário).
5. ✅ **Comportamento padrão da Home redesenhado a pedido do usuário.**
   Antes, tocar no avatar da Home navegava pra tela de Chat escrito — o
   usuário queria que **segurar o avatar já iniciasse a escuta ali mesmo**
   (igual ao `MicButton` do Chat), reservando a tela de Chat escrito pro
   botão "Gabi" da barra de baixo. `HomeScreen` agora injeta `ChatViewModel`
   também e usa `detectTapGestures(onPress=startListening, tryAwaitRelease,
   onRelease=stopListening)`. O ícone genérico de robô no topo do Chat virou
   o rosto real (`GabiFaceAvatar`).
6. 🟡 **Ainda não confirmado**: o bug relatado de "botão Início/Gabi não
   navega, fica preso em Ajustes" — não achei nada no código que explique
   isso (a lógica de clique/navegação da `JavisBottomBar` não foi alterada
   nesta rodada). Pode ter sido um APK desatualizado sendo testado, ou
   precisa reproduzir com logs reais pra confirmar. Emulador Android sendo
   configurado (SDK/system-image baixados) pra investigar isso e outros
   comportamentos sem depender só do celular do usuário.
7. ✅ **Bug crítico encontrado depois, com o usuário já testando o APK
   corrigido**: `MessageDao.getRecentMessages()` não filtrava por
   `sessionId` — pegava as últimas N mensagens de **todas as conversas já
   feitas no app, desde sempre**, sem distinguir sessão. Confirmado com
   reprodução isolada via `curl` direto na API: com prompt real + as 18
   ferramentas reais + só a mensagem atual (sem histórico), o modelo
   respondia normal; o problema só aparecia com o histórico real do app
   (poluído por sessões antigas/quebradas de testes anteriores no mesmo
   dia). Isso explicava a Gabi repetindo pra sempre uma alegação estranha
   sobre "não ter acesso ao microfone" (herdada de alguma troca antiga) e
   ignorando perguntas novas — e por que trocar de chave/provedor (Groq,
   DeepSeek, ElevenLabs) não mudava nada: o contexto poluído é o mesmo
   pra qualquer IA. Corrigido: `getRecentMessages` agora exige `sessionId`
   (mesmo padrão de `getMessagesBySession`); 2 pontos de chamada em
   `JavisRepository.kt` atualizados (`sendMessage`, `resolveSkillReply`
   ganhou `sessionId` como parâmetro). **Usuário precisa limpar o histórico
   de conversas (Ajustes → Limpar histórico) depois de instalar esse APK**,
   pra sessão atual não continuar carregando o lixo acumulado durante o
   debug de hoje.
8. **DeepSeek e ElevenLabs**: sem chave nativa pra testar de ponta a ponta
   (só Groq tem chave embutida, por decisão de sessão anterior), mas os
   endpoints/formato de requisição de ambos foram testados com chave
   inválida — retornaram 401 (chave errada), não 404 (endpoint/modelo
   errado), confirmando que o código está estruturalmente correto pra
   quando o usuário configurar uma chave real. Nota sobre ElevenLabs: a voz
   padrão (`RGymW84CSmfVugnA5tvA`, Roberta) só funciona se o usuário
   primeiro visitar a Voice Library da ElevenLabs e clicar "Add to VoiceLab"
   pra essa voz — só ter a API key não basta, é um passo fácil de esquecer.

**Backlog de funcionalidades está zerado no fim do 2º dia** (só resta o gap
já aceito de zero testes automatizados, não urgente pro uso pessoal). O que
falta não é mais construir coisa — é **testar de verdade no aparelho tudo
que foi construído hoje e nunca rodou fora do build**: reforma visual
completa (Theme/HomeScreen/JavisBottomBar), dashboard com clima/lembretes/
avisos reais, gerenciador de arquivos (fluxo do seletor de pasta do Android,
nunca testado interativamente), fallback de busca do WhatsApp (depende da
versão do WhatsApp instalada no aparelho — nomes de view/ids do WhatsApp
podem variar), e a nova ferramenta `save_memory` (depende do Groq realmente
escolher chamar a ferramenta em conversas reais, não só compilar). Nota:
`AUDITORIA.md` é de 05/08/2026 e está desatualizado — vários itens marcados
🟡/❌ lá (roteamento por regex, segurança de chaves, lembretes, dashboard,
agenda, arquivos) já foram resolvidos desde então; não reler os % de lá como
verdade atual sem cruzar com este backlog.

## Continuação da caçada de bugs (21/08/2026, 4ª rodada) — EM ANDAMENTO

Depois da 3ª rodada (seção acima), o usuário testou de novo e achou dois
problemas novos, mais graves:

1. ✅ **Mensagens e respostas duplicadas — causa encontrada e corrigida,
   ainda NÃO confirmada pelo usuário.** Toda mensagem do usuário estava
   sendo processada DUAS VEZES (duas respostas parecidas mas não idênticas
   — confirmando serem 2 chamadas de IA independentes, não só duplicação
   visual). Causa: `HomeScreen.kt` ganhou sua própria instância de
   `ChatViewModel` (feature de segurar-o-avatar-pra-falar, rodada anterior),
   e como `hiltViewModel()` sem escopo explícito cria uma instância POR TELA
   (não compartilhada), a Home e o Chat ficaram cada uma com seu próprio
   `observeSpeech()`/`sendGreeting()` rodando em paralelo sobre o mesmo
   `speechRecognizer.events` compartilhado — toda fala/mensagem processada
   duas vezes, uma por cada instância. Corrigido criando
   `sharedChatViewModel()` (novo arquivo `ui/viewmodel/SharedViewModels.kt`)
   que escopa o `ChatViewModel` pra Activity inteira via
   `hiltViewModel(viewModelStoreOwner = activity)` — agora Home e Chat usam
   literalmente a mesma instância. **Usuário testou depois desse fix e
   reportou "mesmo erro, não mudou nada"** — não sabemos ainda se (a) ele
   testou um APK antigo / não limpou o histórico de novo, ou (b) o fix não
   resolveu de verdade e tem uma segunda fonte de duplicação ainda não
   encontrada. Precisa investigar com logs reais.
2. 🔴 **Qualidade das respostas ruim — não investigado ainda.** Usuário deixou
   claro que a duplicação é "metade do problema"; o outro lado é a Gabi não
   responder de forma coerente à pergunta feita (ex.: perguntado "roxo" e
   "quero é gritar" depois de falar sobre a voz dela, ela ficou repetindo
   variações de "minha voz é suave" ignorando completamente o que foi
   perguntado). Hipótese (não confirmada): efeito colateral do bug de
   duplicação poluindo o histórico com respostas repetitivas, fazendo o
   modelo "grudar" no último tema em vez de reagir à mensagem nova — precisa
   reavaliar depois que a duplicação estiver de fato resolvida.

**Ambiente de teste automatizado (emulador Android) — bloqueado, aguardando
o usuário.** A pedido do usuário ("crie um ambiente Android, quero que você
faça os testes sozinho"), tentei montar um emulador nesta máquina pra não
depender só do celular físico dele. SDK do emulador + system image
`android-34;google_apis;x86_64` já baixados e instalados
(`C:\Users\Dannilo\AppData\Local\Android\Sdk\emulator`,
`...\system-images\android-34\google_apis\x86_64`), AVD `gabi_test` já
criado. **Mas o emulador não sobe**: essa máquina Windows (CPU Intel) não
tem o "Plataforma de Hipervisor do Windows" (WHPX) habilitado — obrigatório
pra emulação x86_64 com aceleração de hardware. Habilitar isso exige
admin/UAC (não consegui de dentro do sandbox: erro "requer elevação" ao
tentar `Get-WindowsOptionalFeature`) — só o usuário consegue, e pede
reiniciar o PC depois. Ele está fazendo isso agora (ativando via "Recursos
do Windows" no painel de controle). **Próximo passo assim que ele confirmar
que reiniciou**: recriar/verificar o AVD `gabi_test`, subir o emulador
(`emulator -avd gabi_test -no-snapshot -no-audio -gpu swiftshader_indirect`,
`adb wait-for-device` + polling em `sys.boot_completed`), instalar o APK
(`adb install -r app-debug.apk`), e reproduzir os bugs de duplicação/
coerência interativamente via `adb shell input tap/text` + `adb logcat`,
em vez de depender só do relato do usuário.

## Emulador funcionando, testado ao vivo (21/08/2026, 5ª rodada) — RESULTADO

Usuário reiniciou o PC, WHPX ativou (log do emulador confirmou: "Windows
Hypervisor Platform accelerator is operational"). AVD `gabi_test` sobe
normalmente agora. Testei o APK direto no emulador via `adb shell input
tap/text` + `uiautomator dump` (pra achar coordenadas exatas dos elementos,
já que a barra de baixo e o campo de texto mudam de posição com o teclado
aberto/fechado) + screenshots. **Nota técnica**: `adb shell input text`
truncou uma frase longa no meio uma vez — sempre confirmar o conteúdo do
campo via `uiautomator dump` antes de assumir que o texto digitado chegou
inteiro. Também: `adb shell input keyevent 66` (Enter) inseriu quebra de
linha em vez de enviar (o campo aceita múltiplas linhas) — usar sempre o
ícone de enviar do próprio app, não o Enter do teclado.

**Resultados:**
1. ✅ **Duplicação de mensagens: CONFIRMADA CORRIGIDA.** Com histórico
   limpo, mandei várias mensagens de teste — cada uma gerou exatamente UMA
   bolha de usuário e UMA resposta da Gabi, nenhuma duplicata em nenhuma
   tentativa. O fix do `sharedChatViewModel()` (Activity-scoped) resolveu
   de verdade.
2. ✅ **Coerência das respostas: CAUSA CONFIRMADA — era mesmo a duplicação.**
   Testei "você lembra da minha última pergunta" (pergunta meta/ambígua,
   difícil até pra humano) e o modelo respondeu mal — parecia refutar a
   hipótese. Mas reproduzindo os cenários REAIS que o usuário reportou como
   "sem sentido" ("roxo" e "quero é gritar", depois de falar sobre a voz
   dela) com histórico LIMPO (sem duplicação): `openai/gpt-oss-120b` (o
   modelo atual, sem trocar nada) respondeu de forma perfeitamente coerente
   nos dois casos — "Entendi, roxo! Vou guardar que você curte essa cor...",
   "Entendo, às vezes a gente só quer soltar um grito, né". **Confirmado: a
   duplicação era mesmo a causa raiz da incoerência** — histórico com
   respostas repetidas/quase-idênticas fazia o modelo "grudar" no último
   tema. Não precisa trocar de modelo.
   **Modelos alternativos testados por HTTP direto (Groq), pra decidir se
   valia trocar:**
   - `groq/compound`/`groq/compound-mini`: melhor coerência bruta em teste
     isolado, mas **não suportam tool-calling** (`"tool calling" is not
     supported with this model`) — trocar quebraria abrir apps/WhatsApp/
     agenda/lembretes/save_memory. Descartados.
   - `qwen/qwen3.6-27b`: suporta tools, mas em resposta de texto puro (sem
     tool call) vaza o `<think>...</think>` bruto direto no campo `content`
     (não separado como o `reasoning` do gpt-oss) — apareceria pro usuário
     literalmente. Também estourou 300 tokens só pensando, sem nem chegar
     na resposta (`finish_reason: length`) — precisaria de budget de tokens
     bem maior, piorando o problema de rate limit. Descartado sem trabalho
     extra de parsing.
   - `openai/gpt-oss-20b`: suporta tools, mas errou o teste de "você lembra"
     de forma diferente ainda pior ("não lembro, pois ainda não houve uma
     pergunta" — negando que existia mensagem anterior). Não testado nos
     cenários reais (roxo/gritar) — pode valer revisitar se quiser um modelo
     mais rápido/barato, mas sem urgência já que o 120b atual funciona.
3. ⚠️ **Botões de navegação (Início/Ajustes/Continuar): flakiness
   intermitente, não determinística.** Em vários testes, o PRIMEIRO toque
   num botão às vezes não registrava nada (nem crash, nem log, nem erro —
   simplesmente nada acontecia), e um segundo toque idêntico funcionava. Mas
   isso NÃO foi consistente: outras vezes um toque único funcionou de
   primeira. Não dá pra confirmar se é um bug real do app (timing de
   recomposição do Compose?) ou só instabilidade da injeção de toque
   sintético do `adb shell input tap` (limitação conhecida dessa ferramenta,
   não necessariamente reflete o comportamento num toque físico real de
   usuário). Não resolvido — precisa de mais dados (logs do usuário no
   aparelho real) pra decidir se vale a pena investigar mais.
4. Avatar carrega certo no emulador (chroma key + crossfade funcionando),
   só demora alguns segundos pra processar as 3 poses — não é bug, é
   esperado.

**Emulador `gabi_test` continua rodando** nesta máquina (não foi encerrado)
— pode ser reusado em sessões futuras sem precisar recriar. Pra reconectar:
`adb devices` (`$SDK/platform-tools/adb.exe`) deve listar `emulator-5554`;
se não aparecer, subir de novo com o comando acima. Screenshots/dumps de UI
ficam em `C:\Users\Dannilo\AppData\Local\Temp\claude\...\scratchpad\`.

## Bug crítico de ordem de mensagens + fixes de voz e memória (21/08/2026, 6ª rodada)

Continuando a caçar "não faz sentido" no emulador, achado o bug real por
trás da maior parte disso — mais fundamental que a duplicação da rodada
anterior:

1. ✅ **`getRecentMessages` retornava DESC (mais nova primeiro), mas o
   código não invertia antes de mandar pra API.** `JavisRepository.sendMessage()`
   fazia `.takeLast(16)` numa lista já DESC — isso pega os itens MAIS
   ANTIGOS da janela buscada (não os mais novos) e, mesmo quando cabem
   todos, manda a conversa pra API na ORDEM ERRADA (mais nova primeiro, mais
   antiga por último). APIs de chat (Groq/OpenAI-compatível) esperam ordem
   cronológica com a mensagem atual por ÚLTIMO — de trás pra frente, o
   modelo "respondia" a uma mensagem antiga em vez da atual conforme a
   conversa crescia além de ~3-4 trocas. Reproduzido ao vivo no emulador:
   perguntei "qual a capital do brasil" → certo; ensinei "gosto de pizza" →
   salvou certo; perguntei "o que eu gosto de comer" → respondeu **"a
   capital do Brasil é Brasília"** (a primeira pergunta, não a atual).
   Corrigido: `.take(16).reversed()` no lugar de `.takeLast(16)` (a query
   em si continua DESC de propósito — outro uso, achar a última resposta
   pra compartilhar, depende disso; só a montagem do `aiMessages` mudou).
   **Esse era provavelmente o bug raiz por trás da maioria dos "ela não
   responde o que eu pergunto" reportados ao longo do dia.**
2. ✅ **`save_memory` sendo chamado até em perguntas.** Depois do fix acima,
   "o que eu gosto de comer" (uma pergunta) ainda respondia "Beleza, vou
   lembrar disso." em vez de responder a pergunta — o modelo tratava
   qualquer frase autorreferente como "ensinando algo", mesmo sendo uma
   pergunta pedindo pra ela LEMBRAR, não uma afirmação nova. Corrigido no
   `SYSTEM_PROMPT`: instrução agora distingue explicitamente afirmação
   ("eu gosto de X" → salva) de pergunta ("o que eu gosto?" → responde com
   o que já sabe, nunca salva). Testado ao vivo depois do fix: mesma
   pergunta, sessão nova (histórico zerado), sem re-ensinar nada → "Você
   adora pizza, né? 🍕" — puxou certo da Memória persistente entre sessões.
3. ✅ **ElevenLabs falhando em silêncio total — agora tem fallback e
   aparece o erro.** Usuário configurou a chave da ElevenLabs e ativou o
   toggle, mas a voz "Roberta" (sugerida por padrão) não funcionou — sem
   nenhum som, sem erro visível. Causa (já documentada no código antes,
   confirmada agora): vozes da Voice Library da ElevenLabs (Roberta,
   Raquel, Keren, Carla, Ana Dias) só funcionam com a chave do usuário se
   ele **primeiro visitar elevenlabs.io/app/voice-library e clicar "Add to
   VoiceLab"** pra aquela voz — só colar a API key não basta, é um passo
   fácil de perder. Quando isso falhava, o app só logava o erro e ficava
   mudo (`ElevenLabsTts.speak()` não retornava nada usável). Corrigido:
   `speak()` agora retorna `Result<Unit>`; `JavisRepository.speak()` cai
   pro TTS nativo do Android automaticamente se a ElevenLabs falhar, em vez
   de silêncio total. **Usuário ainda precisa fazer o passo manual (Add to
   VoiceLab) no site da ElevenLabs pra ouvir a voz Roberta de verdade** —
   isso não dá pra automatizar, é uma ação na conta dele.
4. ✅ **Pequeno polish**: `MemoryScreen.kt` mostrava o tipo da memória em
   inglês cru (`Preference`) — trocado por rótulos em português
   (Preferência, Hábito, Rotina, Fato, Nome, App favorito, Comando).

**Testes ao vivo confirmados no emulador nesta rodada** (não só teoria):
pergunta simples, ensinar preferência via `save_memory` (verificado direto
na tela de Memória, registro real no banco), pergunta de acompanhamento
puxando a memória corretamente numa sessão nova — tudo sem duplicação e
com respostas coerentes.

## Origem desta pasta

Esta pasta (`Projeto Gabi`) foi copiada **inteira** em 20/08/2026 a partir de
`C:\CRiando coisa nova\Jarvis Projeto\Jarvis-Projeto` (incluindo `node_modules`,
`.next`, `android` com build e `.git`, para não precisar regerar nada). A pasta
original foi mantida intacta como backup/referência — não é mais o local de
trabalho ativo.

## Stack (app web)

- Next.js 16.2.6 (App Router) + React 19.2.6 + TypeScript 5.9.3
- Tailwind CSS 4.1.17
- Drizzle ORM 0.45.2 + **better-sqlite3** (SQLite local — trocado de Postgres/`pg`
  em 20/08/2026 para não depender de infra externa)
- Framer Motion (animações), Lucide React (ícones)
- `alexa-remote2` (integração Alexa, parcial)

## Estrutura

```
src/
├── app/
│   ├── layout.tsx
│   ├── page.tsx              # exporta GabiApp
│   └── api/
│       ├── chat/route.ts
│       ├── claude/route.ts
│       ├── alexa/route.ts
│       └── health/route.ts
├── components/
│   ├── GabiApp.tsx            # shell principal (substitui o antigo NexusApp)
│   ├── Orb.tsx
│   └── screens/
│       ├── HomeScreen.tsx
│       ├── ChatScreen.tsx
│       ├── VaultScreen.tsx
│       └── SmartHomeScreen.tsx
└── db/
    ├── schema.ts
    └── index.ts                # drizzle + better-sqlite3, lê DATABASE_URL (default ./data/app.db)
```

## Comandos

```
npm run dev         # servidor de desenvolvimento
npm run build        # build de produção (falha se houver erro de TS)
npm run start        # roda o build
npm run lint          # eslint
npm run typecheck    # tsc --noEmit
```

Android (dentro de `android/`): `./gradlew build` (precisa Android Studio/SDK,
nunca foi validado por mim — sem SDK disponível nesta máquina).

## Decisões já tomadas

- **Nome**: unificado para "Gabi" no app web (`page.tsx` já exporta `GabiApp`).
  Ainda restam strings "JARVIS" em `src/app/api/alexa/route.ts` (User-Agent) e
  `src/app/api/health/route.ts` (campo `service`) — cosmético, não bloqueia nada.
- **Banco de dados**: SQLite local via `better-sqlite3`, não Postgres. Motivo:
  evitar depender de provisionar um Postgres externo (Render/Vercel) só para dev.
- **Provedor de IA**: prioridade para opções **gratuitas** (Ollama local, depois
  Gemini). Claude/Anthropic fica como fallback opcional pago — decisão consciente
  do usuário, documentada em `PROMPT-GABI-CONCLUSAO.md` (pasta original).
- **Android usa Groq/DeepSeek**, não Claude — mesma lógica de custo. Ver
  `AUDITORIA.md` item 4 antes de "corrigir" isso — é intencional.
- **Pacote Android renomeado** `com.javis.assistant` → `com.gabi.assistant`
  (21/08/2026, Etapa 2). Escopo intencionalmente limitado ao nome do pacote —
  nomes de classe (`JavisApplication`, `JavisRepository`, etc.), o tema
  `Theme.JAVIS`, o recurso `ic_javis_foreground` e a string `"javis_prefs"` do
  DataStore continuam como estão. Trocar isso também é opcional, fica pra
  quando quiser. A pasta legada `android/app/src/main/java/` (não compilada)
  também não foi tocada.
- **Git remoto**: por decisão do usuário, esta pasta (`Projeto Gabi`) só
  commita local por enquanto — nenhum push foi feito pro GitHub
  (`DaniloXPTEAM/...`). Revisar essa decisão quando o app estiver mais maduro.
- **Permissões do Claude Code**: `.claude/settings.json` desta pasta e da pasta
  `Jarvis Projeto` (raiz) usam `permissions.defaultMode: "dontAsk"` — qualquer
  ação não coberta pela lista `deny` (rm -rf, format etc.) é aprovada sem
  prompt. Autorização total dada explicitamente pelo usuário para este projeto.

## Estado atual / bloqueios conhecidos (atualizado 20/08/2026, noite — Etapa 1 concluída)

**Etapa 1 concluída e validada**: `npm run build` passa de ponta a ponta
(`✓ Compiled successfully`, typecheck limpo, 9 páginas/rotas geradas,
`data/app.db` criado de fato durante o build). Git limpo, sem rebase pendente.

Resolvido:
1. ~~App não rodava por falta da pasta `data/`~~ — pasta criada, `better-sqlite3`
   já grava normalmente em `./data/app.db` (confirmado: arquivo gerado no build).
2. ~~8 erros de TypeScript (`SpeechRecognition`)~~ — criado
   `src/types/speech-recognition.d.ts` com os tipos ambientes da Web Speech API
   (`SpeechRecognition`, `SpeechRecognitionEvent`, etc. + augmentação de
   `Window`). Também renomeada a variável local que sombreava o tipo global
   (`SpeechRecognitionCtor` em vez de `SpeechRecognition`) em
   `ChatScreen.tsx`/`HomeScreen.tsx`.
3. ~~Git com rebase travado~~ — resolvido com `git add -A` +
   `git -c core.editor=true rebase --continue` (evita abrir editor interativo).
   Rebase concluído em um passo só (era o último da fila). Commit final:
   `chore: finish Gabi setup` (hash `93a9c8b`), branch `main` limpa.
4. ~~Strings "JARVIS" residuais~~ — trocadas por "Gabi" em
   `src/app/api/alexa/route.ts` (User-Agent) e `src/app/api/health/route.ts`
   (campo `service`).
5. `data/` (e `*.db`/`*.db-journal`/`*.db-wal`/`*.db-shm`) adicionados ao
   `.gitignore` — banco local não deve ir pro git.

6. ~~**Segurança (Android)**: chaves de API em texto puro~~ — resolvido na
   Etapa 3 (21/08), ver seção própria abaixo.

## Android nunca compilava — descoberto e corrigido em 20/08/2026 (noite)

Ao validar a correção de voz (abaixo), percebi que havia SDK Android já instalado
nesta máquina (`ANDROID_HOME=C:\Users\Dannilo\AppData\Local\Android\Sdk`,
platform 34, build-tools 34.0.0, `android/local.properties` já aponta pra lá) e
rodei `./gradlew :app:compileDebugKotlin` pela primeira vez. **O app Android
nunca tinha compilado desde o merge malfeito de 16/08** — ninguém tinha
validado isso porque não havia SDK disponível até agora. Achado grave, corrigido:

1. **BOM UTF-8 no `android/gradle/libs.versions.toml`** quebrava o parser TOML
   do Gradle logo de cara. Removido (rescrito sem BOM).
2. **6 arquivos com pedaços inteiros de código faltando** (não só linhas soltas):
   `JavisBottomBar.kt`, `HomeScreen.kt`, `ChatViewModel.kt` (em
   `ui/viewmodel/`), `JavisSpeechRecognizer.kt`, `JavisVoiceService.kt`,
   `WeatherAgent.kt`. Causa: o merge de 16/08 deletou blocos inteiros (assinatura
   de função/classe) e deixou um resto de marcador de conflito colado no meio do
   código (ex.: `... {63378f4 (chore: finish Gabi setup)` literalmente dentro de
   uma linha Kotlin) — sem `<<<<<<<` visível, por isso passou despercebido antes.
   **Corrigido restaurando os 6 arquivos da versão íntegra pré-rebase**
   (`git checkout 63378f4 -- <arquivo>`); os 2 commits remotos que tinham tocado
   `HomeScreen.kt` (259cf14, 128b07c) só mexiam num import não utilizado, nada
   se perdeu.
3. **18 arquivos com texto em português corrompido por dupla-codificação UTF-8**
   (ex.: "amigável" virou "amigÃ¡vel", emojis viraram lixo) — bug antigo,
   independente do merge (já estava assim desde o commit que introduziu cada
   arquivo). Corrigido com um script que detecta trechos re-codificáveis em
   Windows-1252 e os decodifica de volta como UTF-8 (233 trechos corrigidos ao
   todo). Lista completa: `AiProvider.kt`, `ClaudeProvider.kt`,
   `CommandParser.kt`, `BluetoothController.kt`, `AppModule.kt`,
   `JavisRepository.kt`, `MemoryManager.kt`, `BootReceiver.kt`,
   `ReminderReceiver.kt`, `ReminderScheduler.kt`, `ChatScreen.kt`,
   `MemoryScreen.kt`, `NotificationsScreen.kt`, `PermissionsScreen.kt`,
   `SettingsScreen.kt`, `SettingsViewModel.kt`, `AndroidTtsFallback.kt`,
   `ElevenLabsTts.kt`.
   ⚠️ **A pasta legada `android/app/src/main/java/` (não compilada) tem os
   mesmos problemas de encoding em ~10 arquivos e não foi corrigida** — baixa
   prioridade por não afetar o app real, mas repare se algum dia reaproveitar
   código de lá.
4. Resultado: `./gradlew :app:compileDebugKotlin` agora dá
   **`BUILD SUCCESSFUL`** — primeira vez que isso acontece desde 16/08.

Comando pra rodar localmente (JDK 17 precisa ser setado explicitamente, o `java`
do PATH aqui é a versão 8):
```
cd android
$env:JAVA_HOME = "C:\Users\Dannilo\AppData\Local\jdk17\jdk-17.0.20+8"
.\gradlew.bat :app:compileDebugKotlin
```

## Voz do Android (TTS) — feita fora da sequência das 3 etapas

Motivado por reprovação de teste real do usuário ("voz robótica e arrastada").
Arquivos tocados (caminhos atualizados após o rename do pacote — ver Etapa 2
abaixo): `android/app/src/main/kotlin/com/gabi/assistant/voice/AndroidTtsFallback.kt`,
`android/app/src/main/kotlin/com/gabi/assistant/storage/JavisPreferences.kt`,
`android/app/src/main/AndroidManifest.xml`.

- **Diagnóstico**: por padrão (`useElevenLabs = false`), 100% dos usuários que não
  configuram a própria chave do ElevenLabs usam `AndroidTtsFallback`, que só
  escolhia a primeira voz pt-BR cujo nome batesse com "female"/"pt-br-x" —
  ignorando a qualidade da voz (`Voice.quality`) e o motor de TTS do aparelho.
  Motores de fabricante (Samsung etc.) tendem a soar robóticos/arrastados; o
  motor "Google Text-to-Speech" (`com.google.android.tts`) tem vozes neurais bem
  melhores quando instalado.
- **Fix**: `AndroidTtsFallback` agora (1) prefere explicitamente o motor Google
  TTS se instalado (checagem via `PackageManager.getPackageInfo`, exige
  `<queries><package android:name="com.google.android.tts"/></queries>` no
  manifest por causa da visibilidade de pacotes do Android 11+/API 30+, já
  adicionado); (2) ordena as vozes pt-BR disponíveis por `Voice.quality`
  (VERY_HIGH > HIGH > NORMAL > LOW > VERY_LOW) em vez de pegar a primeira que
  bate no nome — isso é o que realmente evita robotização; nome "female"/
  "pt-br-x" vira só critério de desempate. Pitch artificial reduzido de 1.15
  para 1.05 (não precisa mais forçar tom agudo pra parecer feminina, a seleção
  de voz já resolve isso — mudado tanto na classe quanto no default de
  `JavisPreferences.ttsPitch`, que é o valor que de fato vale em runtime).
- **ElevenLabs mantido intocado** por pedido explícito do usuário: só entra em
  jogo se o próprio usuário configurar a própria chave em Ajustes
  (`useElevenLabs = true` e `elevenLabsApiKey` não vazio) — continua 100%
  opcional, nunca é o padrão.
- **Validado por build** (não só leitura de código, como as vezes anteriores):
  `./gradlew :app:compileDebugKotlin` deu `BUILD SUCCESSFUL` depois dessa
  mudança (ver seção "Android nunca compilava" abaixo). Ainda não testado num
  dispositivo/emulador real — só validação de compilação.

## Criptografia das chaves de API (Android) — Etapa 3, 21/08/2026

Chaves do Groq, ElevenLabs, DeepSeek e Anthropic saíram do DataStore em texto
puro e foram pra `EncryptedSharedPreferences` (Android Keystore, AES-256).

- Novo arquivo `storage/SecureKeyStore.kt`: wrapper sobre
  `EncryptedSharedPreferences` (esquema `AES256_SIV` pra chave, `AES256_GCM`
  pra valor), com uma ponte pra `Flow` via `callbackFlow` +
  `OnSharedPreferenceChangeListener` — mantém o mesmo contrato reativo que o
  DataStore já tinha, então `SettingsViewModel` não precisou mudar nada.
- `JavisPreferences.kt` agora delega `groqApiKey`/`elevenLabsApiKey`/
  `deepSeekApiKey`/`anthropicApiKey` (leitura e escrita) pro `SecureKeyStore`;
  o resto (voice id, pitch, rate, provider, etc. — nada sensível) continua no
  DataStore como antes.
- Dependência `androidx.security:security-crypto:1.1.0-alpha06` adicionada
  (`libs.versions.toml` + `build.gradle.kts`).
- **Sem migração de dados**: como o app nunca tinha compilado antes desta
  sessão, não existe nenhuma instalação real com chaves salvas em texto puro
  pra migrar — não precisou de código de migração.
- Validado: `./gradlew :app:compileDebugKotlin` → `BUILD SUCCESSFUL`.

## Roteamento por function-calling (Android) — Etapa 3, 21/08/2026

`CommandParser` (regex) deixa de ser o classificador principal. Agora quem
decide qual ferramenta usar é a própria IA (Groq/DeepSeek, API compatível
com tools da OpenAI) — o regex vira só o **fallback offline** (sem
internet/sem chave).

- `ai/ToolCalling.kt`: modelos compartilhados do formato "tool calling" da
  OpenAI (`ToolDef`, `ToolCall`, `ToolMessage`, etc.) + `GabiTools.definitions`
  — a lista de ferramentas (open_app, call_contact, set_alarm, search_web,
  search_youtube, send_whatsapp_message, read_notifications, open_settings,
  take_photo, get_weather, set_reminder, share_content, bluetooth_connect,
  ask_claude), cada uma com JSON schema dos parâmetros.
- `GroqProvider`/`DeepSeekProvider` ganharam `chatWithTools(...)`, além do
  `chat()` normal que já existia (não mudou, outros usos continuam iguais).
- `ai/AiToolRouter.kt`: converte a escolha de ferramenta da IA de volta pro
  mesmo `ParsedCommand` que o `CommandParser` já produzia — por isso o resto
  do pipeline (`executeCommand`, `resolveSkillReply`, confirmação de ações
  sensíveis como ligar/mandar WhatsApp) não precisou de nenhuma mudança.
- `set_reminder` pede a data/hora em ISO-8601 (`trigger_at`) em vez de a IA
  calcular só em texto livre — o prompt de roteamento inclui a data/hora atual
  (`AiToolRouter.currentDateTimeContext()`) pra IA calcular certo horários
  relativos ("em 30 minutos", "amanhã às 9h").
- `JavisRepository.sendMessage()`: tenta `routeWithAi()` primeiro (Groq, com
  fallback pro DeepSeek); se os dois falharem (sem internet/sem chave), cai
  pro `commandParser.parse(userText)` de sempre — mesma robustez offline que
  já existia.
- **Decisão do usuário**: Groq continua sendo o provedor principal (não
  Ollama — Ollama não roda nativamente no Android, exigiria PC ligado na
  mesma rede, o que quebra a ideia de assistente sempre disponível no bolso).
- **Chave do Groq nativa no app**: o usuário forneceu uma chave gratuita do
  Groq criada especificamente para este app e pediu pra ela vir embutida,
  sem precisar configurar em Ajustes. Implementado via
  `android/local.properties` (nunca vai pro git — já está no `.gitignore` em
  3 níveis, confirmado com `git ls-files` que nunca foi commitado) +
  `build.gradle.kts` lendo esse arquivo de verdade (antes só lia
  `project.findProperty`, que não pega `local.properties` sozinho — corrigido)
  → `BuildConfig.GROQ_API_KEY`. `JavisRepository` usa
  `prefs.groqApiKey.first().ifBlank { BuildConfig.GROQ_API_KEY }` — chave do
  usuário em Ajustes tem prioridade se ele configurar uma própria depois.
  ⚠️ Ressalva de segurança: uma chave embutida no `BuildConfig` é
  extraível por quem descompilar o APK final — risco baixo pra uma chave
  gratuita de uso pessoal, mas não é "seguro" no sentido forte. Se decidir
  distribuir o APK publicamente, vale reconsiderar essa escolha.
- Validado: `./gradlew clean :app:compileDebugKotlin` → `BUILD SUCCESSFUL`,
  e conferido que `BuildConfig.java` gerado realmente contém a chave.

## Gmail (prévia) + Agenda (ler e criar) — Etapa 3, 21/08/2026

Fecha 2 dos 5 itens "parciais" restantes da `AUDITORIA.md` (#17 Gmail, #18
Agenda). Decisão do usuário: Gmail só prévia de notificação (não a API
completa do Gmail/OAuth — mais trabalho, outra conta externa pra manter) e
aviso sob demanda (não proativo).

- **Permissões novas**: `READ_CALENDAR`/`WRITE_CALENDAR` no
  `AndroidManifest.xml` + 2 linhas novas em `PermissionsScreen.kt` ("Agenda"
  e "Criar compromissos").
- **3 `CommandType` novos**: `READ_CALENDAR`, `CREATE_CALENDAR_EVENT`,
  `READ_GMAIL` — com tool correspondente em `GabiTools` (roteamento por IA) e
  detecção por regex em `CommandParser` (fallback offline), mesmo padrão dos
  outros comandos.
- **Ler Gmail**: reaproveita a `JavisNotificationListenerService` que já
  existia (mesma infra de "ler notificações"), filtrando só
  `com.google.android.gm`. Lê remetente/assunto/trecho — **não** o corpo
  completo do email (isso exigiria a API do Gmail de verdade).
- **Ler agenda**: consulta `CalendarContract.Instances` (próximos 7 dias,
  todos os calendários visíveis) e fala os compromissos.
- **Criar compromisso**: insere um evento de verdade via
  `CalendarContract.Events` (não é o lembrete interno do app — é a agenda do
  sistema mesmo). Acha o primeiro calendário gravável (`CAL_ACCESS_CONTRIBUTOR`
  ou melhor) automaticamente. `create_calendar_event` pede `start_at` em
  ISO-8601, mesmo esquema do `set_reminder`.
- **Bônus**: `CommandType.READ_NOTIFICATIONS` estava detectado pelo
  `CommandParser` mas nunca era executado de fato — nem em `executeCommand`
  nem em `resolveSkillReply`, caía num `else -> {}` silencioso. Corrigido
  junto (mesmo padrão dos novos comandos), já que usa a mesma infra que o
  `read_gmail` novo.
- Nenhum desses 3 comandos exige confirmação (mesmo nível de risco do
  `SET_REMINDER` — leitura ou escrita reversível/de baixo impacto).
- Validado: `./gradlew :app:compileDebugKotlin` → `BUILD SUCCESSFUL`.

**Sobre a integração com WhatsApp** (não mexida nesta sessão, mas veio à tona
numa pergunta do usuário): usa Accessibility Service com IDs internos do
WhatsApp (`com.whatsapp:id/entry`, `com.whatsapp:id/send`), não a API
oficial. Frágil por natureza — quebra se o WhatsApp mudar esses IDs numa
atualização, e `openWhatsAppChatByName` só acha contatos já visíveis na tela
de conversas recentes (não abre a busca se não achar). Nunca testado num
aparelho real. Se for mexer nisso: adicionar fallback pra busca é a melhoria
mais valiosa.

**Restam 3 itens "parciais" da AUDITORIA.md** (não pedidos ainda): #24
Dashboard/HUD na Home, #27 funcionamento offline (IA local), #33
gerenciamento de arquivos.

## Plano em 3 etapas (fazendo uma por vez)

- **Etapa 1 — Destravar o app web**: ✅ concluída (20/08, noite). Criar
  `data/`, declarar tipos de `SpeechRecognition`, resolver o rebase travado do
  git, remover strings "JARVIS" residuais. `npm run dev`/`npm run build`
  rodando limpo.
- **Etapa 2 — Organização do repositório**: ✅ concluída (21/08). Pacote
  Android renomeado `com.javis.assistant` → `com.gabi.assistant`. Decisão do
  usuário: git fica só local por enquanto, sem push pro remoto. `.gitignore`
  já estava adequado (revisado, sem mudanças necessárias). Além do escopo
  original da etapa, essa sessão também descobriu e corrigiu que o Android
  nunca tinha compilado (ver seção própria acima) e fechou a config de
  permissões do Claude Code (`defaultMode: dontAsk`).
- **Etapa 3 — Fechar lacunas de produto/segurança**: próxima. Criptografar
  chaves no Android, revisar os itens "não implementados"/"parciais" do
  `AUDITORIA.md` (roteamento por function-calling em vez de regex, dashboard,
  etc.) e decidir o que entra ou não no escopo atual.

## O que foi feito por sessão

- **20/08/2026 manhã** (na pasta original `Jarvis-Projeto`): criado
  `GabiApp.tsx`, `page.tsx` religado para ele, telas principais tocadas
  (Home/Chat/Vault/SmartHome), migração do banco de Postgres para SQLite
  (`db/schema.ts`, `drizzle.config.ts`, `db/index.ts`), `.env.local` e
  `package.json` atualizados (`npm install` rodado). App parou de funcionar no
  fim dessa sessão pelos motivos 1 e 2 listados acima.
- **20/08/2026 noite**: pasta `Projeto Gabi` criada como cópia integral da pasta
  original, para servir de local de trabalho daqui pra frente. Este CLAUDE.md
  criado. Em seguida, **Etapa 1 executada e validada**: pasta `data/` criada,
  tipos de `SpeechRecognition` declarados, rebase do git resolvido, strings
  "JARVIS" removidas. `npm run build` passa limpo. Também corrigido: o
  `.claude/settings.json` da raiz `Jarvis Projeto` não tinha `Projeto Gabi` em
  `additionalDirectories`, causando prompts de permissão repetidos — adicionado
  lá e no `.claude/settings.json` desta pasta.
- **20/08/2026, mais tarde**: correção da voz do TTS gratuito do Android
  (motor Google preferido + seleção por qualidade em vez de nome, pitch
  reduzido de 1.15→1.05). Ao tentar validar isso descobri SDK Android já
  instalado na máquina e rodei o primeiro build real desde 16/08 — **o app
  Android nunca tinha compilado**. Corrigido: BOM no `libs.versions.toml`, 6
  arquivos com blocos de código inteiros perdidos no merge de 16/08
  (restaurados do commit pré-rebase `63378f4`), e 18 arquivos com texto em
  português corrompido por dupla-codificação UTF-8 (233 trechos, bug anterior
  ao merge). `./gradlew :app:compileDebugKotlin` → `BUILD SUCCESSFUL`.
- **21/08/2026**: **Etapa 2**. Perguntei e o usuário decidiu: git só local por
  enquanto (sem push), e renomear o pacote Android agora (bom momento, app
  nunca foi instalado em aparelho real). Renomeado `com.javis.assistant` →
  `com.gabi.assistant` em 56 arquivos + `build.gradle.kts` + `AndroidManifest.xml`
  + `proguard-rules.pro`; validado com `./gradlew clean :app:compileDebugKotlin`
  → `BUILD SUCCESSFUL`. Também resolvido de vez o prompt de permissão repetido:
  `.claude/settings.json` (raiz `Jarvis Projeto` e `Projeto Gabi`) agora usa
  `permissions.defaultMode: "dontAsk"`, então qualquer ação não coberta pela
  lista `deny` é aprovada sem interromper — antes só "Bash"/"PowerShell"
  estavam liberados como ferramenta inteira, o que ainda deixava buracos
  (comandos compostos, outras ferramentas).
- **21/08/2026, Etapa 3**: chaves de API criptografadas
  (`EncryptedSharedPreferences`), roteamento por function-calling (Groq/
  DeepSeek escolhem a ferramenta em vez de regex), chave nativa do Groq
  embutida via `local.properties`, Gmail (prévia) + Agenda (ler e criar)
  implementados. Ver seções próprias acima pra cada um.
- **21/08/2026, fechamento da sessão**: avisos proativos de WhatsApp/Gmail
  (toggle em Ajustes) e a personagem ilustrada da Gabi (`GabiCharacter.kt` +
  assets reais da personagem já no projeto + Orb reativo de verdade a
  ouvir/falar) — ver seções próprias acima. Depois disso, revisão do que
  ainda falta junto com o usuário resultou no backlog priorizado no topo
  deste arquivo (bug de confirmação de ações, dashboard com dado real,
  fallback de busca do WhatsApp, reforma visual, gerenciamento de arquivos).
  **Sessão encerrada aqui — próxima sessão começa pelo item 1 do backlog.**

## Personagem da Gabi (GabiCharacter) + Orb reativo — Etapa 3, 21/08/2026

Pedido do usuário: em vez do Orb abstrato de partículas, mostrar uma
personagem ilustrada (inspirada na afilhada dele — é pra ela que o app está
sendo feito) com 3 poses trocando por crossfade conforme o estado da Gabi.

- **`ui/components/GabiCharacter.kt`** (novo): carrega 3 poses de
  `assets/gabi-idle.png`, `gabi-listening.png`, `gabi-talking.png` e faz
  crossfade suave entre elas conforme `GabiCharacterState`
  (IDLE/LISTENING/TALKING). Inclui remoção de fundo por chroma key (magenta)
  com suavização de borda por distância de cor + supressão de vazamento de
  cor — pensado originalmente pra caso a arte viesse com fundo magenta sólido
  em vez de canal alfa real, mas é inofensivo rodar em cima de um PNG já
  transparente (o caso real, ver abaixo), então funciona nos dois cenários.
  Se algum arquivo de pose não existir, cai pro `fallback` (o Orb) sem
  quebrar — não trava a tela esperando asset que não existe.
- **Assets de verdade recebidos e já embutidos**: o usuário gerou a
  personagem (menina ruiva de vestido vermelho, estilo 3D animado) em 3
  poses com **canal alfa real** (não precisou do chroma key na prática,
  mas o código já suporta os dois casos). Arquivos copiados de
  `%TEMP%\gabi-idle.png` (+ `-listening`, `-talking`) pra
  `android/app/src/main/assets/`. **Confirmado com `unzip -l` no APK final**
  que os 3 arquivos (768×768, ~570-620KB cada) estão de fato embutidos no
  build — não é só teoria, testei o pipeline completo.
- **Reatividade de verdade** (o pedido de duas mensagens atrás, "só tá bom se
  o Orb reagir de verdade"): antes o Orb só tinha uma animação decorativa
  contínua, sem relação com o estado real de voz.
  - `JavisRepository` ganhou `isSpeaking: StateFlow<Boolean>`, ligado/desligado
    em volta da chamada de `speak()` (funciona pros dois motores de TTS).
  - `JavisSpeechRecognizer.isListening` já existia como `StateFlow<Boolean>`
    reativo — só precisava ser consumido.
  - Novo `ui/viewmodel/HomeViewModel.kt`: combina os dois Singletons acima
    num único `GabiCharacterState` (LISTENING > TALKING > IDLE, nessa
    prioridade). Por serem Singletons, reflete o estado real do app inteiro,
    não importa qual tela disparou a escuta/fala.
  - `HomeScreen.kt` passa esse estado pro `GabiCharacter` (e pro `GabiOrb`
    como fallback). O Orb agora muda cor/velocidade de verdade: laranja calmo
    parado (IDLE) → dourado quase branco e mais rápido (LISTENING) → coral
    quente (TALKING) — com `animateColorAsState`/`animateFloatAsState` pra
    transição suave entre estados, não um corte seco.
- **Reforma visual mais ampla** (fundo/paleta/tipografia do app inteiro,
  saindo do visual "painel hacker" atual): ainda não decidida — mostrei 3
  direções num artefato visual (Estrela-Guia, Bolha de Sabão, Lanterna
  Mágica) antes da personagem entrar em cena; com a personagem definida
  agora, essas direções provavelmente precisam ser refeitas ao redor da
  paleta dela (vermelho do vestido, ruivo do cabelo) em vez de abstratas.
  **Pendente pra próxima sessão.**
- Validado: `./gradlew clean :app:compileDebugKotlin` **e**
  `./gradlew :app:assembleDebug` (o build completo do APK, não só
  compilação) → `BUILD SUCCESSFUL` nos dois, com verificação real de que os
  assets foram parar no APK.

## ElevenLabs — saga completa (21/08/2026, depois do emulador ser encerrado)

Sem emulador disponível (usuário pediu pra encerrar, ver rodada acima), o
diagnóstico daqui pra frente foi feito **testando a API da ElevenLabs
direto por `curl`**, com a chave real do usuário — mais rápido que
reinstalar o app a cada tentativa. Três problemas em sequência, cada um só
aparecendo depois do anterior ser resolvido:

1. **Usuário colou o ID da chave, não a chave de verdade.** A ElevenLabs
   mostra na listagem um identificador que PARECE a chave mas não é — a
   chave de verdade só aparece por completo no momento da criação (ou ao
   "Rotate key"), sempre começando com `sk_`. Erro retornado:
   `authentication_error` / `api_key_id_used_as_api_key`, com a mensagem
   já explicando isso. Resolvido: usuário girou a chave, pegou a `sk_...`
   certa.
2. **Plano grátis da ElevenLabs não permite NENHUMA voz pronta via API.**
   Com a chave certa, toda chamada de TTS (testado com a Roberta e até com
   a Rachel, a voz padrão mais básica que existe na plataforma) retornava
   HTTP 402 `payment_required` / `paid_plan_required`: *"Free users cannot
   use library voices via the API."* Isso é restrição comercial da própria
   ElevenLabs, documentada na resposta de erro — nada a ver com o app ou
   com "Add to VoiceLab" (aquele passo continua sendo necessário, mas
   sozinho não basta no plano grátis). Usuário assinou o plano pago
   (elevenlabs.io/pricing) — confirmado por HTTP que a mesma chave passou
   a funcionar (HTTP 200, MP3 válido) sem precisar gerar chave nova.
3. **Depois de pagar, a Gabi respondia em espanhol.** `eleven_turbo_v2` (o
   model_id que o app usava) detecta o idioma automaticamente pelo texto,
   sem nenhuma trava — e confunde português com espanhol facilmente em
   frases curtas. Tentei forçar com `language_code: "pt"` nesse modelo e a
   API recusou (`Model 'eleven_turbo_v2' does not support language_code`)
   — esse modelo simplesmente não aceita travar idioma. Trocado pra
   `eleven_turbo_v2_5`, que aceita `language_code`. Testado por HTTP com
   `model_id: eleven_turbo_v2_5` + `language_code: "pt"` → HTTP 200, áudio
   válido. Aplicado em `ElevenLabsTts.kt` (`speak()`), `assembleDebug`
   confirmado. **Ainda não confirmado pelo usuário ouvindo no aparelho** —
   só validado via chamada HTTP direta, não rodou dentro do app de verdade
   ainda (sem emulador disponível pra esse teste específico).

**Resumo pra quem for mexer em TTS de novo**: `eleven_turbo_v2` (mais
antigo/rápido) não trava idioma e é mais restrito no plano grátis;
`eleven_turbo_v2_5` aceita `language_code` e é o que está em uso agora. Se
a voz continuar saindo errada, o próximo passo é testar
`eleven_multilingual_v2` (mais lento, mas historicamente o mais preciso em
português da ElevenLabs) do mesmo jeito — por `curl` direto, sem precisar
reinstalar o app.

## 7ª rodada (22/08/2026) — WhatsApp, Bluetooth, Notificações e o bug real por trás dos 3

Usuário testou no aparelho de novo e reportou (por voz, mensagem meio
transcrita errado) três coisas quebradas: mandar WhatsApp não funciona
(ele tem WhatsApp normal E Business instalados), o Bluetooth não acha
nenhum dispositivo por perto, e ler notificações diz "não tem permissão"
mesmo com a permissão concedida de verdade em Configurações. Investigado
via agente Explore (leitura de código, sem emulador) e depois corrigido:

1. **WhatsApp: `WA_PACKAGE` fixo em `"com.whatsapp"` em tudo.**
   `JavisAccessibilityService.kt` usava esse valor fixo pra montar TODOS os
   view IDs (`$WA_PACKAGE:id/entry`, `:id/send`, etc.) — como esses IDs são
   sempre prefixados pelo package DONO da tela, quem tem só o Business (ou
   usa ele como principal) nunca batia em nenhum elemento, mesmo com o
   Business logado e em primeiro plano. `CommandParser.kt` e
   `JavisRepository.kt` (`launchApp("com.whatsapp")`, `openWhatsAppChat()`,
   `sendWhatsAppMessage()`) também apontavam fixo pro pacote normal, então
   nem abrir o app funcionava se só o Business estivesse instalado. Fix:
   - `JavisAccessibilityService`: novo `WA_PACKAGE_BUSINESS =
     "com.whatsapp.w4b"` + `currentWaPackage()` privado, que resolve pelo
     `_activePackage` (o app que está de fato em primeiro plano agora) e
     cai pro normal se nenhum dos dois estiver ativo. Todos os
     `findNodeByViewId("$WA_PACKAGE:id/...")` trocados pra
     `"${currentWaPackage()}:id/..."`.
   - `CommandParser`: novo `resolveWhatsAppPackage()` — prefere o WhatsApp
     normal se estiver instalado (`Context.isPackageInstalled()`, já
     existia em `utils/Extensions.kt` mas nunca tinha sido usado), cai pro
     Business se só ele existir. `getPackageName()` chama isso sempre que
     o alias resolve pra `WA_PACKAGE`.
   - `JavisRepository.openWhatsAppChat()` / `sendWhatsAppMessage()`: trocado
     `launchApp("com.whatsapp")` fixo por
     `launchApp(commandParser.resolveWhatsAppPackage())`.

2. **Notificações: `getInstance() == null` era tratado como "sem
   permissão", mas só significa "serviço não está bound AGORA".** O Android
   mata/desconecta o `NotificationListenerService` sem revogar nada (ex.:
   app muito tempo em segundo plano) — a permissão real continua concedida
   em Configurações, mas o app dizia "não tem permissão" porque só olhava
   uma variável estática local (`instance`). Fix: `JavisNotificationListenerService`
   ganhou `hasAccess(context)` (consulta o OS de verdade via
   `NotificationManagerCompat.getEnabledListenerPackages`, a mesma lista de
   Configurações) e `requestRebind(context)` (força o Android a reconectar
   o serviço). `JavisRepository.readNotifications()`/`readGmailPreview()`
   agora passam por um novo `resolveNotificationService()`: se `instance`
   for nulo mas `hasAccess()` for true, chama `requestRebind()` e devolve
   "só um segundo, deixa eu me reconectar" em vez do "não tem permissão"
   errado — só mostra a mensagem de permissão de verdade quando `hasAccess()`
   também é false.

3. **Bluetooth "não acha nenhum dispositivo": `BluetoothController.kt` não
   faz scan.** Confirmado (não corrigido ainda) — só tem
   `bondedDeviceNames()` (dispositivos JÁ pareados), sem `startDiscovery()`
   nem `BroadcastReceiver` pra `ACTION_FOUND`. Ou seja, a Gabi só "acha"
   caixa/fone que o usuário já parear manualmente pelo Bluetooth do
   Android — não faz descoberta de dispositivos novos por voz. Também
   faltavam as permissões `BLUETOOTH_CONNECT`/`BLUETOOTH_SCAN` (Android 12+)
   na tela de onboarding — sem elas pedidas, ligar Bluetooth/rotear áudio
   falhava em silêncio (`SecurityException` engolida por `runCatching`).
   Corrigido: `PermissionsScreen.kt` ganhou os dois `PermDef` novos, e
   `SettingsScreen.kt` ganhou um botão "Revisar todas as permissões" pra
   quem já passou do onboarding conseguir conceder essas duas sem
   reinstalar. **Scan de descoberta de dispositivos novos continua
   pendente — próxima sessão.**

4. **O bug real por trás do WhatsApp nunca funcionar: a confirmação de
   ações sensíveis (`requiresConfirmation = true`, ex.: mandar WhatsApp,
   alarme) só tinha botões Sim/Não na tela de Chat — a Home (tela
   principal, por voz) não tinha ESSE UI nenhum.** A Gabi perguntava em voz
   alta "Enviar 'X' pra Fulano no WhatsApp?" (isso já funcionava,
   `speak(outcome.reply)` roda pro `pendingCommand` também), mas a resposta
   falada do usuário ("sim", "pode mandar") ia pro `ChatViewModel.sendMessage()`
   como mensagem NOVA, sem checar `pendingCommand` — virava só mais uma
   pergunta pro Groq, o comando pendente nunca era confirmado nem
   executado. Isso explica por que "ela não tentou em nenhum" WhatsApp:
   travava sempre na confirmação, silenciosamente, pra QUALQUER ação que
   peça confirmação, não só WhatsApp. Corrigido em duas frentes:
   - `ChatViewModel.sendMessage()` agora intercepta ANTES de rotear pra IA:
     se existe `pendingCommand`, roda `matchesConfirmation(text)` (lista de
     palavras sim/não em pt-BR, normalizada sem acento) e chama
     `confirmPendingCommand(true/false)` direto. Resposta ambígua (não bate
     nem com sim nem com não) descarta o pendente em vez de travar a
     conversa pra sempre.
   - `HomeScreen.kt` ganhou os mesmos botões "Sim, fazer"/"Cancelar" que já
     existiam só no `ChatScreen.kt`, pro caso de toque em vez de voz.

Build: `./gradlew compileDebugKotlin` e `./gradlew assembleDebug` →
`BUILD SUCCESSFUL` nos dois. **Nenhuma das 4 correções foi testada num
aparelho/emulador de verdade ainda** — só compilação/build confirmados.
APK gerado em `android/app/build/outputs/apk/debug/app-debug.apk` e
enviado pro usuário instalar e testar.

**Pendente pra próxima sessão**: implementar o scan real de Bluetooth
(`startDiscovery()` + `BroadcastReceiver`), e a feature de Cast — usuário
foi explícito: **não** quer que a Gabi reimplemente Chromecast, quer que
ela abra a tela nativa de Cast/espelhamento do Android (a mesma que apps
tipo Prime Video usam) pro usuário escolher a TV de lá — provável
`Settings.ACTION_CAST_SETTINGS` ou intent equivalente do sistema, a
investigar.

## 8ª rodada (22/08/2026) — por que o WhatsApp NUNCA abria, modo de escuta contínua, e a Gabi "falando" por menos tempo do que fala de verdade

Usuário testou a 7ª rodada no aparelho e voltou com: WhatsApp continua sem
abrir (nem sequer abre o app, não é só não mandar mensagem), câmera abre
mas não tira foto sozinha, o avatar "ouvindo" some ~1s depois mesmo
falando sem parar, e um pedido de UX grande: apertar UMA VEZ pra conversar
sem precisar segurar o botão (igual modo de voz do ChatGPT).

1. **A causa raiz de tudo do WhatsApp: faltava `<queries>` no
   `AndroidManifest.xml`.** As correções da 7ª rodada (resolver
   `com.whatsapp` vs `com.whatsapp.w4b`) estavam corretas mas nunca tiveram
   chance de rodar — Android 11+/API 30+ bloqueia por padrão que um app
   VEJA se outro está instalado (`isPackageInstalled`/`getPackageInfo`
   lançam `NameNotFoundException`) ou RESOLVA o intent explícito de abrir
   (`getLaunchIntentForPackage` devolve `null`) pra qualquer package que não
   esteja declarado num bloco `<queries>` no manifesto — só
   `com.google.android.tts` estava lá. É por isso que câmera/discador
   abriam (usam intents IMPLÍCITOS tipo `ACTION_IMAGE_CAPTURE`, que o
   Android sempre resolve, não precisam de `<queries>`) mas abrir um app
   específico por nome de pacote (WhatsApp, Instagram etc.) nunca
   funcionava, não importa o que o código Kotlin fizesse. Corrigido:
   `AndroidManifest.xml` ganhou um `<package android:name="...">` pra cada
   package usado em `CommandParser.appAliases` (WhatsApp normal, Business,
   YouTube, Chrome, Instagram, Spotify, etc. — a lista inteira, já que
   todos sofriam do mesmo bug, só que só o WhatsApp tinha sido testado a
   ponto de reportar).

2. **A Gabi "para de falar" bem antes do áudio acabar de verdade —
   `ElevenLabsTts.speak()` não esperava o MediaPlayer terminar.** Ele
   chamava `mediaPlayer.start()` e retornava na hora (só INICIAVA o áudio),
   não quando `onCompletionListener` disparava — então
   `JavisRepository._isSpeaking` virava `false` quase instantaneamente,
   não quando ela realmente parava de falar. Isso não foi reportado
   diretamente pelo usuário, mas é pré-requisito pro modo contínuo (ver
   item 4): sem isso, voltar a ouvir "depois dela terminar de falar" na
   real reiniciava o microfone enquanto ela ainda estava no meio da frase,
   captando a própria voz dela pelo alto-falante. Corrigido com
   `suspendCancellableCoroutine`, igual o `AndroidTtsFallback.kt` já fazia
   certo (ele usa `UtteranceProgressListener.onDone`) — `speak()` agora só
   retorna quando o áudio realmente terminou (ou deu erro).

3. **Avatar "ouvindo" sumia ~1s depois: os timeouts de silêncio do
   reconhecedor eram curtos demais pra uma pausa natural de fala.**
   `EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS` estava em 1500ms —
   uma pausa comum no meio de uma frase (respirar, pensar) já bastava pro
   Android decidir que a fala tinha acabado (`onEndOfSpeech`), e o app
   virava `isListening = false` na hora, trocando o avatar pra pose idle
   mesmo com o usuário ainda segurando o botão e querendo continuar
   falando. Aumentado pra 2500ms (silêncio "completo") / 1800ms
   ("possivelmente completo") em `JavisSpeechRecognizer.kt`.

4. **Implementado o modo de escuta contínua pedido** ("aperto uma vez e
   ela entende que qualquer coisa que eu falar depois é pra ela, sem
   precisar segurar o botão"). Trocado o gesto de segurar (`onPress` +
   `tryAwaitRelease`) por um toque único que liga/desliga
   `ChatUiState.continuousMode`:
   - `ChatViewModel.toggleContinuousMode()`: no toque que liga, já chama
     `startListening()`; no toque que desliga, chama `stopListening()`.
   - Depois de cada resposta dela (fim de `sendMessage()` e de
     `confirmPendingCommand()`), se `continuousMode` estiver ligado, volta
     a chamar `startListening()` sozinha — só é seguro por causa do fix do
     item 2 acima (senão pegaria a própria voz dela).
   - Erros de "não ouvi nada"/timeout (códigos 6/7, o normal de ficar
     ouvindo silêncio ambiente o tempo todo) não derrubam mais o modo
     contínuo — só reiniciam a escuta sozinhos. Qualquer OUTRO erro (sem
     permissão, sem rede, etc.) desliga `continuousMode` de verdade em vez
     de tentar de novo escondido.
   - `MicButton.kt` (usado só no `ChatScreen`) e o toque no personagem
     (`HomeScreen`) foram trocados pro mesmo toggle — `onPress`/`onRelease`
     virou um `onClick` único.
   - **Ainda não implementado**: o usuário também mencionou (como ideia,
     "talvez") que ela só devia responder quando ouvisse o próprio nome
     ("Gabi"), perguntando "tá falando comigo?" em casos ambíguos, pra não
     responder a conversas de fundo que não eram com ela. Por ora, no modo
     contínuo, ELA RESPONDE A TUDO que reconhece enquanto está ligado (sem
     filtro de "wake word") — decisão consciente pra não inventar uma
     heurística de detecção de nome sem confirmar com o usuário primeiro
     se é isso mesmo que ele quer, já que errar pro lado errado (ignorar
     coisa que era pra ela responder, ou ficar perguntando "tá falando
     comigo?" toda hora) é pior que perguntar antes de construir.

5. **Câmera "abre mas não tira foto sozinha": limitação do Android, não
   bug.** `openCamera()` usa `MediaStore.ACTION_IMAGE_CAPTURE`, um intent
   IMPLÍCITO que só abre o app de câmera do sistema — ele nunca dispara o
   obturador sozinho, o usuário sempre precisa tocar. Tirar foto de
   verdade sem interação manual exigiria trocar pra CameraX/Camera2 com
   captura programática (pipeline de câmera dentro do próprio app, não só
   abrir outro app) — não implementado ainda, é bem mais trabalho que os
   outros itens desta rodada. **Perguntar ao usuário se vale a pena antes
   de começar.**

6. **Pedido novo, ainda maior, registrado mas não iniciado**: mini-jogo de
   bichinho virtual (tipo o Pou antigo) dentro do app, pro momento em que
   a Gabi não está sendo usada como assistente — personagem "mora" numa
   telinha, dá pra alimentar/interagir. Ideia legal mas é uma feature nova
   grande (economia de jogo, telas novas, estados persistentes) — fica pro
   backlog, não escopada ainda.

Build: `./gradlew compileDebugKotlin` e `./gradlew assembleDebug` →
`BUILD SUCCESSFUL` nos dois. APK novo gerado e enviado pro usuário.
**Nenhuma correção desta rodada foi confirmada rodando no aparelho ainda**
— o `<queries>` em especial é o tipo de fix que só se prova testando de
verdade (a teoria bate 100% com os sintomas relatados, mas teoria não é
o mesmo que confirmação).

## 9ª rodada (23-24/08/2026) — WhatsApp abre mas não digita/envia nada

Usuário confirmou: o fix do `<queries>` funcionou (WhatsApp agora ABRE).
Mas depois de aberto, a Gabi não escreve nem envia mensagem nenhuma — sem
emulador disponível pra depurar ao vivo, o app ficava mudo sobre o motivo
real (`sendWhatsAppMessage()` só devolvia `Boolean`, e todo caminho de
falha — serviço de acessibilidade desligado, contato não achado na busca,
não achou o campo de digitar/o botão enviar — virava o mesmo "não
consegui" genérico, quando não ficava simplesmente em silêncio).

**Suspeita mais provável, ainda não confirmada**: o "Serviço de
acessibilidade" da Gabi (`Configurações > Acessibilidade > Serviços
instalados > Gabi`) pode não estar ativado no aparelho — é um toggle
manual separado das permissões normais, então fácil de nunca ter sido
ligado. Se não estiver ligado, `JavisAccessibilityService.getInstance()`
é sempre nulo, o WhatsApp abre (isso não depende do serviço) mas nenhuma
automação de UI roda depois — bate 100% com o sintoma. Outras causas
possíveis: WhatsApp não achou o contato pela busca (nome digitado
diferente do salvo), ou os view IDs hardcoded
(`conversations_row_contact_name`, `entry`, `send`,
`menuitem_search`/`search_src_text`) mudaram numa atualização recente do
WhatsApp — não dá pra confirmar sem inspecionar a árvore de UI ao vivo
(`uiautomator dump`), que precisa de emulador/device conectado.

**Fix aplicado (diagnóstico, não a causa em si)**: `sendWhatsAppMessage()`
agora devolve um `WhatsAppSendResult` (`Success` /
`AccessibilityDisabled` / `ContactNotFound` / `SendFailed`) em vez de
`Boolean`. `executeConfirmedCommand()` (`JavisRepository.kt`) passou a
devolver `String?` — quando não-nulo, é a frase real que a Gabi fala no
lugar da genérica "Enviando para X…" que ela dizia ANTES de saber se
realmente ia dar certo (um bug em si: ela "confirmava" verbalmente uma
ação que ainda nem tinha rodado). Agora cada motivo de falha tem uma
frase específica e acionável (pedir pra ativar o serviço de
acessibilidade, avisar que não achou o contato, avisar que achou o chat
mas não conseguiu escrever) — a run de teste seguinte no aparelho vai
finalmente dizer qual dos 3 motivos é, em vez de só "não funcionou".
`ChatViewModel.confirmPendingCommand()` atualizado pra usar essa frase
quando ela existir.

Build: `compileDebugKotlin` + `assembleDebug` → `BUILD SUCCESSFUL`. APK
enviado. **Próximo passo assim que o usuário testar**: ouvir a frase
exata que a Gabi fala ao tentar mandar WhatsApp agora — ela mesma vai
dizer qual dos 3 motivos é, sem precisar mais adivinhar.

## 10ª rodada (24/08/2026) — a causa provável do WhatsApp: ACTION_SET_TEXT "funciona" sem escrever nada

Usuário testou com o serviço de acessibilidade JÁ ativo (descartando o
motivo #1 da 9ª rodada) e confirmou: ela abre o WhatsApp E o contato
certo (a busca/lista funciona — `openWhatsAppChatByName` está OK), mas
"não chega a enviar nem [mesmo] já deixando na própria conversa" — ou
seja, mesmo com a tela certa já aberta, escrever/mandar não funciona.

**Causa mais provável**: `AccessibilityNodeInfo.performAction(ACTION_SET_TEXT)`
e `ACTION_CLICK` no botão de enviar retornam `true` quando a AÇÃO é
despachada com sucesso — não quando ela realmente tem efeito na tela. O
campo de digitar do WhatsApp é um `EditText` customizado
(`MentionableEntry` ou similar), e é um problema conhecido desses
componentes ignorarem `ACTION_SET_TEXT` silenciosamente: o app achava que
tinha escrito e mandado (`sent = true`), mas nada mudava na tela de
verdade. Sem emulador/device conectado pra inspecionar a árvore de UI ao
vivo (`uiautomator dump`), não dá pra confirmar 100% que é isso — é a
explicação que mais bate com "abre a conversa certa mas não escreve
nada", meio a meio com a possibilidade de os view IDs
(`:id/entry`, `:id/send`) terem mudado numa atualização do WhatsApp.

**Fix aplicado** em `JavisAccessibilityService.whatsAppSendMessage()`:
1. Depois de tentar `ACTION_SET_TEXT`, RECONFERE lendo `node.text` de
   volta (com `node.refresh()` antes, senão a leitura pode vir de um
   cache velho) — só considera "escreveu" se o texto realmente apareceu.
2. Se não apareceu, cai num fallback por **clipboard**: copia a mensagem
   pro clipboard e usa `ACTION_PASTE` no campo — caminho de entrada
   diferente do `ACTION_SET_TEXT`, que costuma funcionar em composers
   customizados que ignoram o outro.
3. Depois de clicar em enviar, RECONFERE que o campo de digitar
   realmente esvaziou (o WhatsApp sempre limpa o campo após um envio
   bem-sucedido de verdade) — só devolve sucesso se isso for verdade.
4. Adicionado fallback de contentDescription "Enviar" (pt-BR) além de
   "Send"/"send" pro botão de enviar, já que o aparelho do usuário está
   em português.

Com isso, se ainda falhar, a Gabi agora vai devolver `SendFailed` de
verdade (em vez de um falso `Success`) — o que já é uma confirmação útil
por si só: se ela continuar dizendo "consegui" mas nada aparecer no
WhatsApp, o problema é outro (ex.: `:id/entry`/`:id/send` mudaram de
nome numa atualização recente do WhatsApp, e nem o clipboard-paste
alcança o campo certo) — nesse caso só um `uiautomator dump` ao vivo
resolve, o que precisa de ADB conectado no celular real (não precisa de
emulador — dá pra fazer direto no aparelho do usuário via USB ou
wireless debugging, é só uma ideia pra próxima sessão se isso persistir).

Sobre "Avisos" mostrando todas as notificações do WhatsApp (não só
lembretes pedidos): usuário confirmou que é assim mesmo que deveria
funcionar — não é bug, só uma dúvida que ficou esclarecida. Nenhuma
mudança de código necessária ali.

Build: `compileDebugKotlin` + `assembleDebug` → `BUILD SUCCESSFUL`. APK
enviado. Ainda não confirmado no aparelho.

## 11ª rodada (24/08/2026) — Spotify alucinado, escuta contínua morrendo em segundo plano, "vai dormir"

**IMPORTANTE — SendUserFile parou de funcionar nesta sessão** (igual
aconteceu com o PowerShell antes). APKs desta rodada em diante ficam só
no caminho local `android/app/build/outputs/apk/debug/app-debug.apk` —
avisar o usuário pra pegar pelo Explorador de Arquivos em vez de tentar
mandar como anexo.

1. **"Toca uma música" — Gabi alucinava a ação.** Ela respondia em texto
   como se fosse escolher/tocar uma música ("vou botar uma música pra
   você, prefere isso ou aquilo"), abria o Spotify na tela inicial, e
   parava aí — porque **não existia nenhuma ferramenta** de
   busca/reprodução de música, só `open_app` (abre o app, sem contexto
   nenhum). A IA estava narrando uma ação que não tinha como executar de
   verdade — mesma categoria de problema já visto no WhatsApp antes dos
   fixes de function-calling. Corrigido com uma ferramenta nova, seguindo
   o padrão que já existia pro YouTube (`search_youtube`):
   - `search_spotify` adicionada em `ToolCalling.kt`,
     `CommandType.SEARCH_SPOTIFY` em `CommandParser.kt`, mapeamento em
     `AiToolRouter.kt`.
   - `JavisRepository.searchSpotify(query)`: usa o deep link
     `spotify:search:<query>` (suportado pelo app do Spotify sem precisar
     de login OAuth/API) — abre direto nos resultados da busca. **Não é
     play automático** — mesma limitação já documentada da câmera, o
     usuário ainda toca pra tocar. Fallback pro site
     `open.spotify.com/search/...` se o app não estiver instalado.
   - Regra nova no `SYSTEM_PROMPT` (`AiProvider.kt`): proibe
     explicitamente a IA de dizer que já fez/vai fazer algo sem chamar a
     ferramenta correspondente NESTE turno, e manda ela admitir quando
     não existe ferramenta pro que foi pedido em vez de fingir.
   - **Validado em parte pelo usuário**: pedir um trailer específico do
     YouTube funcionou perfeitamente (achou e tocou sozinho) — o padrão
     `search_youtube`/`ACTION_SEARCH` funciona bem quando a busca é
     específica o bastante pro YouTube abrir direto no vídeo.

2. **Causa raiz real do "some quando outro app abre" (ex.: pedir outro
   trailer com o YouTube já em primeiro plano): o modo de escuta contínua
   nunca usava o foreground service que já existia pra isso.**
   `ChatViewModel.toggleContinuousMode()`/`continueListeningIfContinuous()`
   chamavam `speechRecognizer.startListening()` DIRETO — isso só tem
   garantia de funcionar com a Gabi em primeiro plano; assim que outro
   app assume a tela, o Android para de entregar áudio pro reconhecedor
   de um processo em segundo plano sem justificativa (a não ser que exista
   um foreground service do tipo microfone ativo). Só que **esse serviço
   já existia** (`JavisVoiceService`, `foregroundServiceType="microphone"`,
   com notificação própria e ações START/STOP) — só nunca tinha sido
   ligado ao fluxo de modo contínuo, ficava conectado só a um trigger de
   intent separado (`ACTION_VOICE_TRIGGER`, usado pelo `MainActivity`) que
   nada mais no app dispara. Corrigido: `toggleContinuousMode()`,
   `continueListeningIfContinuous()` e o restart automático depois de
   silêncio/timeout agora chamam `context.startForegroundService()`/
   `startService()` no `JavisVoiceService` (ações `ACTION_START_LISTENING`/
   `ACTION_STOP`) em vez de mexer no `speechRecognizer` direto —
   `ChatViewModel` ganhou `@ApplicationContext context: Context` injetado
   pra isso.

3. **"Gabi, vai dormir" — novo comando de voz pra desligar o modo
   contínuo sem fechar o app.** Pedido explícito do usuário: nada de
   precisar tocar na tela, só falar (mas também não deve ENCERRAR o app,
   só parar de ouvir). Implementado em `ChatViewModel.sendMessage()`,
   verificado localmente (sem chamar a IA — instantâneo e não depende do
   modelo interpretar certo): reconhece "vai dormir", "pode dormir", "se
   desliga", "desativa", "para/pare de ouvir/escutar", "boa noite" (sem
   acento/pontuação/maiúsculas, via novo `normalizeSpeech()`) e chama
   `toggleContinuousMode()` direto.

4. **Possível causa da "confirmação por voz não funcionava, precisava
   apertar botão" + "ela repetia a pergunta de novo, como se não visse
   que não deu certo"**: `matchesConfirmation()` só removia ACENTO do
   texto reconhecido, não PONTUAÇÃO — se o reconhecedor de voz devolvesse
   "Sim." ou "Sim!" (comum, o Android às vezes inclui pontuação no
   resultado), o match falhava (`"sim." != "sim"`), a resposta virava
   "ambígua", o `pendingCommand` era descartado sem confirmar nem negar, e
   o texto seguia como mensagem NOVA pra IA — que, vendo o pedido de
   WhatsApp ainda fresco na conversa, podia re-perguntar a mesma
   confirmação. Corrigido junto com o item 3: `normalizeSpeech()` agora
   tira toda pontuação (`filter { it.isLetter() || it.isWhitespace() }`),
   não só acento. **Não confirmado ainda que era essa a causa exata** —
   mas é a explicação mais provável dada a mecânica do bug, e o teste
   anterior a isso foi feito ainda sem o fix da rodada 9/10 (confirmado
   pela frase antiga "Enviando para Luciane" que o usuário reportou
   ver/ouvir, prova de build desatualizado nesse teste específico).

Build: `compileDebugKotlin` + `assembleDebug` → `BUILD SUCCESSFUL`. APK
em `android/app/build/outputs/apk/debug/app-debug.apk` — usuário avisado
pra pegar direto do Explorador de Arquivos (SendUserFile indisponível).
Nada desta rodada confirmado no aparelho ainda.

## 12ª rodada (25/08/2026) — WhatsApp abria o PERFIL do contato, não o chat

Usuário testou de novo: "continua sem conseguir ver o contato. ele abre a
imagem de perfil ou o perfil, mas não envia nada." Pista nova e decisiva.

**Causa raiz**: a busca do WhatsApp mostra o nome batido em VÁRIAS seções
(Contatos, Conversas, Mensagens...) — `tapChatRowByName` fazia um match
GENÉRICO de texto na árvore inteira (`findAllNodesWithText`/
`findAllNodesWithNormalizedText`), sem diferenciar em qual seção o nó
estava. Como a seção "Contatos" normalmente vem ANTES de "Conversas" na
árvore de acessibilidade, o primeiro match batia ali, e o clique abria o
PERFIL do contato — que não tem campo de mensagem nenhum — em vez do
chat de verdade.

**Fix em `JavisAccessibilityService.tapChatRowByName`**: agora tenta
PRIMEIRO a view ID específica `:id/conversations_row_contact_name` (a
mesma já usada em `readWhatsAppChats`, garantidamente da lista de
CHATS, não de contatos/perfis) antes de cair pro match genérico antigo
como fallback.

**Rede de segurança extra em `whatsAppSendMessage`**: se mesmo assim
não achar campo de digitar (`:id/entry`) na tela — sinal de ter caído
num perfil — tenta um resgate tocando no botão "Mensagem"/"Message" que
o WhatsApp mostra na tela de perfil de contato, que abre o chat de
verdade; só desiste se nem esse botão existir.

Build: `compileDebugKotlin` + `assembleDebug` → `BUILD SUCCESSFUL`. APK
gerado, usuário avisado pra pegar pelo Explorador de Arquivos. Ainda não
confirmado no aparelho.

## Spotify — "ainda não consegue tocar música" (25/08/2026)

Usuário testou a busca do Spotify da 11ª rodada e continua sem
funcionar. Pesquisei (WebSearch/WebFetch) e confirmei: `spotify:search:<query>`
NÃO é um esquema documentado oficialmente pela Spotify — é só um truque
conhecido da comunidade, com bug relatado no próprio fórum oficial da
Spotify (abre o app mas a tela de busca fica em branco). A documentação
oficial (`developer.spotify.com/documentation/android/tutorials/content-linking`)
só cobre link pra conteúdo JÁ IDENTIFICADO por ID
(`spotify:track:<id>`, `spotify:playlist:<id>`), não busca livre por
texto.

**Fix aplicado (melhoria parcial, não resolve de vez)**: trocada a ordem
— agora tenta primeiro o link universal `https://open.spotify.com/search/<query>`
(mais confiável que o esquema customizado) e só cai pro `spotify:search:`
quebrado como último recurso.

**Solução de verdade, ainda não implementada**: usar a Web API de busca
da própria Spotify (`api.spotify.com/v1/search`) pra resolver o texto num
ID de faixa real, e então linkar direto pro `spotify:track:<id>` (esse
sim documentado e confiável). Isso não precisa de login do usuário —
só um Client ID/Secret gerados de graça em
`developer.spotify.com/dashboard` (fluxo "Client Credentials", só
autentica o APP, não a conta pessoal) — só que precisa do usuário criar
essa chave, então fica pendente até ele decidir se quer fazer isso.
**Perguntei ao usuário se quer seguir esse caminho antes de implementar.**

## "Quarto da Gabi" — mini-jogo v1 (25/08/2026)

Usuário gerou (em outra ferramenta, a partir do prompt que passei antes)
o pacote completo de assets em `gabi-avatar-completo/` — bem mais
completo do que os 2 looks extras que eu tinha pedido: veio um cenário
de quarto (`room-bg.png`) e 4 poses de ROTINA por horário (comer 6-11h,
computador 11-17h, ler 17-21h, dormir resto do dia), além de versões
REGERADAS das 3 poses de assistente (idle/listening/talking) com melhor
alinhamento entre si (canvas 768x1024 consistente, "sem pulo" no
crossfade, conforme o `README.md` que veio junto no pacote).

**Aplicado**:
- As 3 poses de assistente foram SUBSTITUÍDAS pelas novas versões (mesmo
  personagem/roupa, só melhor alinhadas) em `assets/gabi-idle.png`,
  `gabi-listening.png`, `gabi-talking.png`.
- 4 poses de rotina + `room-bg.png` copiadas pra `assets/` também.
- Novo `GabiAssetImage` em `GabiCharacter.kt`: composable genérico pra
  carregar UM arquivo de assets pelo nome (com ou sem o processamento de
  chroma-key, que só faz sentido pras poses de personagem, não pro
  cenário opaco do quarto).
- Nova tela `RoomScreen.kt` (rota `Screen.Room`): mostra `room-bg.png`
  de fundo + a pose de rotina certa pro horário atual do aparelho.
  **V1 só de ambientação/presença** — sem mecânica de jogo ainda
  (alimentar, stats, moeda) — combinado explicitamente com o usuário
  ("começar pequeno") antes de partir pros assets.
- Entrada nova na Home: card "Quarto da Gabi" (🏠) abaixo dos cards de
  Lembretes/Avisos, leva pra `RoomScreen`.

Build: `compileDebugKotlin` + `assembleDebug` → `BUILD SUCCESSFUL`.
**Não testado visualmente** — sem emulador rodando nesta sessão (foi
encerrado por pedido do usuário numa rodada anterior, por questão de
antivírus), só validação por leitura de código + build bem-sucedido.
Usuário avisado dessa limitação.

**Pendente pra próxima sessão** (mecânica de jogo de verdade, se o
usuário quiser seguir): alimentar/interagir, algum tipo de stat
(fome/humor), talvez trocar de roupa de verdade (precisaria de mais
poses geradas com outfits diferentes, seguindo o mesmo padrão de
canvas/alinhamento que esse pacote já estabeleceu).

## Fechamento do dia 25/08/2026

Duas últimas coisas antes de encerrar a sessão:

1. **"Search_web sendo chamado pra perguntas normais"**: usuário pediu
   pra Gabi responder uma pergunta simples e ela abriu o navegador/Google
   em vez de responder ela mesma — sem sentido pra uma IA com
   conhecimento próprio. Ajustada a regra no `SYSTEM_PROMPT`
   (`AiProvider.kt`): agora deixa explícito que PERGUNTA (ex.: "quem foi
   Machado de Assis?") ela responde com o que sabe, e `search_web` só
   quando o usuário PEDIR EXPLICITAMENTE pra abrir uma busca na tela
   ("abre uma pesquisa sobre X"). Não testado ainda.

2. **Palavra de ativação "Oi Gabi" com o app fechado/celular bloqueado**
   (tipo Siri/Alexa): usuário reforçou que quer isso — registrado como
   pedido confirmado, NÃO iniciado hoje (a pedido dele, "vamos encerrar
   por hoje"). Contexto já levantado nas rodadas anteriores: Picovoice
   (caminho mais simples) não tem mais plano grátis desde 30/06/2026, só
   empresarial; a alternativa de graça (openWakeWord) exige treinar um
   modelo customizado do zero — trabalho considerável, provavelmente
   várias sessões. **Primeira coisa a discutir/escopar na próxima
   sessão.**

Build final do dia: `compileDebugKotlin` + `assembleDebug` →
`BUILD SUCCESSFUL`. APK em
`android/app/build/outputs/apk/debug/app-debug.apk`.

**Resumo do que fica pendente de teste real pro usuário confirmar
assim que possível** (nenhum destes foi validado no aparelho ainda):
trava geral do app (fix do `ElevenLabsTts` com timeout), envio de
WhatsApp (mais uma causa corrigida — seção errada da busca), modo de
escuta contínua sobrevivendo com outro app em primeiro plano, "Gabi vai
dormir", confirmação por voz, Quarto da Gabi, e agora o ajuste do
`search_web`. Sugestão registrada: ADB sem fio no celular real pra
depurar o WhatsApp com dado real em vez de mais tentativa-e-erro.

## Widget de atalho — alternativa rápida à palavra de ativação (26/08/2026)

Retomando o tema "Oi Gabi" com o app fechado: expliquei que o Picovoice
(caminho fácil) não tem mais plano grátis, e a alternativa de graça
(openWakeWord) exige treinar um modelo customizado — trabalho real, não
pra fazer numa sessão só. Ofereci 3 caminhos por `AskUserQuestion` e o
usuário escolheu o **atalho rápido sem wake word de verdade** (a opção
mais barata: um widget de tela inicial) em vez de pagar ou de encarar o
treino de modelo agora.

**Implementado**: widget de tela inicial (`GabiWidgetProvider.kt`,
`AppWidgetProvider`) — um toque abre a Gabi já no modo de escuta
contínua, sem precisar achar o ícone do app e tocar no avatar/botão de
mic manualmente. **Ainda exige desbloquear o aparelho** (limitação de
segurança do Android pra lançar uma Activity — não tem como fugir disso
sem ser um serviço 100% em segundo plano, que é exatamente a feature de
wake word maior, ainda não feita).

Detalhe técnico que valeu a pena corrigir enquanto mexia nisso: já
existia um mecanismo de trigger por intent (`ACTION_VOICE_TRIGGER`,
craqueado desde sessões anteriores mas nunca conectado a nada) que
chamava `JavisVoiceService`/`speechRecognizer` DIRETO do
`MainActivity.onCreate`/`onNewIntent` — isso tinha uma corrida real: se
o app abrisse do zero (cold start) e o usuário falasse rápido, o
resultado do reconhecimento podia chegar ANTES do `ChatViewModel` existir
e começar a observar `speechRecognizer.events` (é um `StateFlow`, só
entrega o valor mais recente pra quem se inscreve DEPOIS — um resultado
que já passou se perde pra sempre). Corrigido: `MainActivity` agora só
marca uma intenção (`autoStartListening`, um `MutableState<Boolean>`
hospedado na Activity, passado por `JavisNavGraph` → `HomeScreen`), e um
`LaunchedEffect` no `HomeScreen` chama `chatViewModel.toggleContinuousMode()`
assim que a tela sobe de verdade — mesmo caminho testado do toque manual,
sem a corrida.

**Como o usuário ativa o widget** (não aparece sozinho — precisa
adicionar manualmente, é assim que todo widget Android funciona):
segurar o dedo na tela inicial vazia → "Widgets" → achar "Gabi" → arrastar
pro lugar desejado. Avisar isso explicitamente na resposta, senão o
usuário vai achar que "não apareceu nada".

Build: `compileDebugKotlin` (resources + kapt/ksp também rodaram limpo,
confirma que os XML novos do widget — layout, widget-info, drawable — 
estão válidos) + `assembleDebug` → `BUILD SUCCESSFUL`. Não testado no
aparelho ainda (nem o widget em si, nem a correção da corrida).

## WhatsApp — causa raiz achada e CONFIRMADA AO VIVO via ADB (26/08/2026)

Usuário conectou o celular real por USB (autorizou depuração USB) a
pedido meu, depois de repetidas tentativas cegas de corrigir o WhatsApp
sem conseguir ver a tela de verdade. Com o aparelho conectado, reproduzi
a busca do WhatsApp diretamente via `adb shell uiautomator dump` e achei
a causa raiz de verdade, definitivamente:

**A causa raiz**: `tapChatRowByName()` tinha um fallback genérico que
buscava o nome ("Luciane") em QUALQUER node de texto da tela inteira,
não só nas linhas de conversa. Na tela inicial do WhatsApp (antes de
qualquer busca), esse fallback batia num node
`com.whatsapp:id/msg_from_tv` — o rótulo de "quem mandou por último"
dentro da prévia de um GRUPO qualquer onde a Luciane tinha mandado
mensagem recentemente. O código tocava o pai desse node, abrindo esse
GRUPO ERRADO em vez da conversa individual da Luciane — **e reproduzi
isso ao vivo, mandando de fato uma mensagem de teste ("oin") que foi
PARAR NO GRUPO "Home Sweet Home 🏠"** (família do usuário — ele já sabe,
avisado). A via de busca (`openWhatsAppSearch`/`typeIntoWhatsAppSearch`)
também estava quebrada por conta própria: os IDs que o código procurava
(`menuitem_search`, `search_src_text`, contentDesc "Pesquisar"/"Search")
são de uma versão ANTIGA do WhatsApp — a versão atual integrou Meta AI
e mudou pra `search_bar_inner_layout`/`my_search_bar` e `search_input`.
Como o fallback genérico "funcionava" (errado) primeiro, a busca de
verdade nunca era nem tentada.

**Fix**: removido por completo o fallback genérico de texto em
`tapChatRowByName` (e as funções que ele usava,
`findAllNodesWithText`/`findAllNodesWithNormalizedText`, deletadas por
ficarem sem uso) — agora só bate via `conversations_row_contact_name`
(a view ID que garantidamente é linha de CONVERSA, testada e confirmada
tanto na lista principal quanto nos resultados de busca). Se não achar
por essa via, devolve `false` e deixa a busca de verdade rodar — que
agora usa os IDs certos (`search_bar_inner_layout`/`my_search_bar` pra
abrir, `search_input` pra digitar).

**CONFIRMADO AO VIVO, com prova real**: reinstalei o APK corrigido no
aparelho (`adb install -r`) e repeti o EXATO mesmo teste — "manda
mensagem pra Luciane dizendo teste2n" — e a mensagem foi entregue na
conversa individual certa (tela mostrando "Luciane ❣️" no topo, mensagem
com dois tiques). **Esta é a primeira correção de todo o arco do
WhatsApp confirmada de verdade, não só por teoria.**

**Nota de metodologia pra próximas sessões com ADB**: `adb shell input
tap X Y` às vezes não registra em `IconButton`s do Compose (mesmo
mirando exatamente no centro das bounds reportadas pelo
`uiautomator dump`) — aconteceu repetidas vezes tentando tocar no botão
de enviar do chat da própria Gabi. `adb shell input swipe X Y X Y
<duração_ms>` (um "swipe" de distância zero, ex. 150ms) simula um toque
com duração real e foi muito mais confiável pra elementos Compose do
que o `tap` instantâneo. Preferir swipe-zero em vez de tap pra UI
Compose daqui pra frente. **Também**: sempre re-dumpar a tela ANTES de
cada toque, nunca reusar coordenadas de um dump anterior — o teclado
abrindo/fechando ou o texto mudando de tamanho desloca a linha de
input inteira, e um toque na coordenada "certa" de um estado anterior
pode acabar caindo em cima do próprio teclado (aconteceu: um "toque no
botão enviar" virou sem querer a tecla "N" do teclado, grudando um "n"
extra no texto).

## WhatsApp — 2º bug real achado ao vivo: falso negativo no "não consegui enviar" (26/08/2026)

Continuando a auditoria ao vivo (usuário pediu "sim" pra eu continuar
testando outras funções com o celular conectado): reenviei a MESMA
mensagem de teste que confirmei funcionando (rodada anterior) e, dessa
vez, a Gabi disse **"Abri a conversa com Luciane mas não consegui
escrever ou enviar a mensagem"** — só que a mensagem TINHA chegado de
verdade (confirmado de novo na tela do WhatsApp, com tique de
entregue). Ou seja: o envio funciona, mas o CÓDIGO QUE CONFERE SE
FUNCIONOU estava mentindo.

**Causa raiz, achada com um dump direto no campo vazio de verdade**: o
campo de digitar do WhatsApp, quando vazio, devolve o texto do HINT
("Mensagem") como `text` na árvore de acessibilidade — não devolve
null nem string vazia. As duas verificações que eu tinha escrito na
9ª/10ª rodada (`isNullOrBlank()` depois de enviar, `isNotBlank()`
depois de digitar) partiam do pressuposto errado de que um campo
"vazio" devolve texto branco/nulo. Resultado: a checagem de ENVIO
sempre falhava (o hint "Mensagem" não é `isNullOrBlank()`), e a
checagem de DIGITAÇÃO sempre "passava" na hora (o hint também não é
`isNullOrBlank()` — o fallback de clipboard nunca era exercitado de
verdade, mascarado o tempo todo).

**Fix**: as duas checagens em `JavisAccessibilityService.kt` agora
comparam contra o TEXTO REAL que a gente mandou/digitou, em vez de
"ficou em branco":
- `setTextAndVerify`: `node.text.contains(text)` (o texto que mandamos
  aparece?) em vez de `isNotBlank()`.
- `whatsAppSendMessage` (pós-envio): `!text.contains(message)` (o texto
  que mandamos SUMIU?) em vez de `isNullOrBlank()`.

**CONFIRMADO AO VIVO de novo**: reinstalei, mandei outra mensagem de
teste ("teste4") pra Luciane, e desta vez a Gabi disse corretamente
"Pronto! Mandei a mensagem pra Luciane no WhatsApp." — testado, chegou
no WhatsApp de verdade E a Gabi relatou certo. **O arco inteiro do
WhatsApp (abrir → achar o contato certo → escrever → enviar → relatar
certo) está confirmado funcionando de ponta a ponta agora**, não só em
partes isoladas.

**Outras correções da mesma sessão de auditoria ao vivo**:
- `readNotifications()` (`JavisRepository.kt`): reduzido de 10 pra 5
  notificações e cada texto cortado em ~80 caracteres — confirmado ao
  vivo que sem isso ela lia um MURO de spam de grupo de venda de conta/
  chip do WhatsApp, inútil de ouvir em voz alta.
- Spotify: confirmado ao vivo que agora abre certinho na tela de
  resultados de busca (ex.: "Evidências Chitãozinho e Xororo" trouxe
  vários resultados prontos pra tocar) — o link `open.spotify.com/search`
  tentado primeiro (trocado na rodada anterior) resolveu o problema.
- Bluetooth: confirmado que "liga o bluetooth" executa e responde
  certo, mas — como já documentado — só rotea áudio pro que já está
  pareado, não faz descoberta de aparelho novo (scan ainda não
  implementado, backlog conhecido).
- Anti-alucinação do Spotify (rodada anterior) confirmado funcionando
  ao vivo: pedir "toca uma música" sem especificar qual fez ela
  perguntar "Qual música ou artista você quer ouvir?" em vez de fingir
  que ia tocar algo.

Build: `compileDebugKotlin` + `assembleDebug` → `BUILD SUCCESSFUL` em
todas as rodadas desta sessão de auditoria. Todas as correções desta
seção foram confirmadas ao vivo no aparelho real do usuário, não só
por teoria — é a primeira vez neste projeto inteiro que isso acontece
de forma sistemática.

## Bluetooth (scan de verdade), Spotify (Web API) e palavra de ativação (26/08/2026)

Três pedidos novos do usuário na mesma sessão de auditoria ao vivo,
implementados em sequência:

**1. Bluetooth — busca de aparelho novo de verdade.** Faltava desde
sempre (documentado como limitação conhecida) — `BluetoothController`
só sabia listar/rotear pra aparelhos JÁ pareados. Adicionado
`discoverDevices()`: `BluetoothAdapter.startDiscovery()` de verdade,
ouvindo os broadcasts `ACTION_FOUND` (um por aparelho achado) e
`ACTION_DISCOVERY_FINISHED` (fim do scan, ~15s de timeout de
segurança) via `BroadcastReceiver` + `suspendCancellableCoroutine`.
`neverForLocation` já declarado no manifesto pro `BLUETOOTH_SCAN`
dispensa localização em Android 12+; ler o nome dos aparelhos achados
usa `BLUETOOTH_CONNECT` (já concedido). `JavisRepository`: se já tem
algo pareado, mantém o caminho rápido de sempre (rotear áudio
direto); se NÃO tem nada pareado, escaneia, fala os nomes achados por
voz, e abre a tela nativa de Bluetooth (`openBluetoothSettings()`)
pro usuário tocar e parear — parear programaticamente pediria PIN/
confirmação do sistema de qualquer jeito, não vale a pena tentar
automatizar isso. **Não testado ao vivo de ponta a ponta** — o
aparelho do usuário já tem 3 dispositivos pareados (AL-5115, M-F8,
Alaxe Pro Max), então o caminho de scan não tinha como ser exercitado
sem desparear algo de verdade dele; só validado por compilação +
revisão de código.

**2. Spotify — Web API de verdade (opcional).** Usuário pediu
explicitamente pra resolver a limitação de "só abre busca, não toca
sozinha". Novo `skills/SpotifyClient.kt`: fluxo OAuth "Client
Credentials" (só autentica o APP, sem login/senha do usuário) —
`POST accounts.spotify.com/api/token` com Basic Auth do Client ID/
Secret, token cacheado até expirar, depois
`GET api.spotify.com/v1/search` pra resolver o texto num
`spotify:track:<id>` de verdade (esse formato É documentado
oficialmente e abre/toca a faixa direto, diferente do link de busca).
Novos campos de Client ID/Secret em Ajustes (`SecureKeyStore`/
`JavisPreferences`/`SettingsViewModel`/`SettingsScreen`, mesmo padrão
das outras chaves de API). `JavisRepository.searchSpotify()` agora
tenta a Web API primeiro (se tiver chave configurada) e só cai pro
link de busca de sempre se não tiver chave ou não achar nada — sem
quebrar quem não configurar. **Pendente**: usuário ainda precisa criar
o app grátis em `developer.spotify.com/dashboard` e colar o Client
ID/Secret em Ajustes > Spotify — sem isso, comportamento continua
igual ao de antes (só busca).

**3. Palavra de ativação DENTRO do modo contínuo (diferente da "Oi
Gabi com o app fechado", que segue no backlog maior).** Pedido
explícito: "só inicie a responder de fato quando der um comando,
tipo chamar o nome dela... pra não interferir em conversas que
esteja fazendo com outras pessoas perto dela". Implementado em
`ChatViewModel.sendMessage()`: novo parâmetro `fromVoice: Boolean`
(true só quando vem do `SpeechEvent.Result` do reconhecedor de voz,
false pra texto digitado no chat — digitar já é intencional, não
precisa "chamar" por escrito). Quando `fromVoice` é true e o texto
NÃO contém "gabi" como palavra inteira, ela ignora silenciosamente
(sem processar, sem falar nada, sem gastar chamada de IA) e continua
ouvindo. Janela de conversa ativa de 20s (`CONVERSATION_WINDOW_MS`)
depois de cada interação real — não precisa repetir o nome toda vez
num vai-e-vem de conversa, só depois de um silêncio (mesmo padrão
Alexa/Google "follow-up"). O nome é removido do início da frase antes
de mandar pra IA (`stripWakeWord`), pra não confundir o pedido de
verdade. **Não testado ao vivo** — não dá pra simular fala real via
ADB (só texto digitado, que por design não passa por esse filtro);
só validado por compilação + revisão de código. Confirmado por teste
manual que o caminho digitado continua funcionando normal, sem ser
afetado pelo filtro.

Build: `compileDebugKotlin` + `assembleDebug` → `BUILD SUCCESSFUL`.
APK instalado no aparelho do usuário via `adb install -r`.

## Spotify — chaves reais configuradas + descoberta de nova restrição da Spotify (26/08/2026)

Usuário decidiu deixar a API "nativa" do app (sem precisar digitar em
Ajustes) — adicionado `SPOTIFY_CLIENT_ID`/`SPOTIFY_CLIENT_SECRET` como
`buildConfigField` (mesmo padrão do `GROQ_API_KEY`, lido de
`local.properties`), com `JavisRepository.searchSpotify()` preferindo a
chave do usuário (Ajustes) mas caindo pra essa nativa se estiver vazia.
Removidos os campos de Ajustes (ficaram redundantes, a pedido do
usuário). Chaves reais do usuário já estão em `local.properties`
(gitignored, não vai pro repo).

**Descoberta real, confirmada ao vivo com logs**: a Spotify mudou a
regra em fevereiro de 2026 — a busca (mesmo via Client Credentials, sem
login de usuário nenhum) agora exige que a conta DONA do app no
Developer Dashboard tenha Premium ativo, senão devolve
`403 Active premium subscription required for the owner of the app`.
Confirmado testando 2 vezes: a primeira chave (conta gratuita) deu
esse erro; o usuário criou um app novo com uma conta Premium pessoal
dele, e o token passou a sair certo (log confirma: "Token Spotify
obtido com sucesso") mas a BUSCA continuou dando o mesmo 403 — a
própria mensagem de erro avisa que pode levar "algumas horas" pro
sistema da Spotify reconhecer o status Premium depois de mudar de
dono. **Ainda não confirmado se resolveu** — só o tempo dirá. Enquanto
isso, cai pro link de busca de sempre (`open.spotify.com/search`),
que já está confirmado funcionando.

Detalhe de segurança discutido com o usuário: o Client Secret fica
embutido no APK compilado (igual toda outra chave do projeto) — se
alguém decompilar o app, só consegue abusar da cota de busca no
catálogo público, não tem acesso a conta pessoal, playlist, pagamento
ou qualquer dado do dono da conta (Client Credentials é só busca
pública, não é OAuth de usuário). Risco baixo pra um app pessoal não
distribuído.

## Quarto da Gabi — alimentar + guarda-roupa por tempo de uso (26/08/2026)

Depois de uma conversa longa sobre escopo (usuário sugeriu Sims-style
com autonomia completa e necessidades tipo banheiro — combinado que é
grande demais pra uma sessão, tratado como visão de longo prazo, não
v1) e pesquisa no GitHub (nenhum projeto achado era reaproveitável —
tecnologia errada, sem licença, ou fora do tema — só a IDEIA do padrão
State de um trabalho de faculculdade em Java foi aproveitada, não o
código), implementado e **confirmado ao vivo, ponta a ponta**:

1. **Alimentar**: botão no Quarto, `RoomViewModel.feed()` grava o
   timestamp e ativa um estado temporário `GabiRoomState.Eating` (pose
   comendo + "Nham nham! 😋") por 3s, depois volta pro estado normal.
   Indicador de texto mostra "Alimentada há Xmin/h" (ou "Ainda não foi
   alimentada"). **Testado ao vivo**: pose mudou, reação apareceu,
   indicador atualizou certo.

2. **Guarda-roupa por tempo de uso**: usuário pediu explicitamente que
   NÃO precisava ser semanal — "pode ser aleatório entre 1 e 30 dias,
   desde que não seja curto demais". Implementado: no primeiro uso,
   sorteia um intervalo fixo entre 4–12 dias (`(4..12).random()`,
   guardado uma vez só, não resorteia) e marca a data de "nascimento"
   do Quarto. A cada intervalo completo desde essa data, libera 1 look
   novo (`Outfits.all`, 4 looks no total: vestido vermelho já
   desbloqueado + 3 looks futuros sem arte ainda — só o vestido tem
   imagem de verdade). Diálogo "Guarda-roupa" (ícone de cabide, canto
   superior direito) mostra os 4 slots: desbloqueados com imagem real,
   desbloqueados sem imagem ainda ("Arte pendente"), e trancados com
   cadeado + "Próximo look em X dias". **Testado ao vivo**: diálogo
   abriu, vestido vermelho aparece liberado com a imagem certa, os 3
   looks futuros aparecem trancados com o cadeado, contagem regressiva
   mostrando corretamente ("Próximo look em 10 dias").

3. **Padrão State (Kotlin sealed class)**: `GabiRoomState` — `Routine`
   (pose pelo horário, como já existia), `Eating` (transição temporária
   depois de alimentar), `WearingOutfit` (look diferente selecionado no
   guarda-roupa, sem variação por horário ainda — só existe 1 pose por
   look extra, não um conjunto de rotina completo). Cada estado sabe
   sua própria pose/texto; a tela só pergunta "qual é o estado atual".

**Pendente**: os 3 looks extra (`gabi-look-casual.png`,
`gabi-look-festa.png`, `gabi-look-pijama.png`) não têm arte ainda —
aparecem como "Arte pendente" quando desbloqueados. Quando o usuário
gerar essas imagens (mesmo processo/prompt já usado pro pacote de
rotina), é só soltar em `assets/` com esses nomes exatos que aparecem
automaticamente, sem precisar mexer em código. Mecânica de necessidade
de verdade (fome caindo sozinha, autonomia tipo Sims) continua no
roadmap de longo prazo, não implementada — combinado explicitamente
como próxima etapa, não desta rodada.

Build: `compileDebugKotlin` + `assembleDebug` → `BUILD SUCCESSFUL`.
Instalado e testado ao vivo no aparelho — feed + guarda-roupa
confirmados funcionando de ponta a ponta com prova visual (screenshots).

## Quarto da Gabi v3 — fusão do guarda-roupa com a rotina, respiração e caixinha de som reativa ao Spotify (26/08/2026)

Depois do teste ao vivo da v2, usuário achou confuso o guarda-roupa
mostrar "Vestido Vermelho" como desbloqueado enquanto o Quarto exibia
uma pose completamente diferente (moletom, da rotina por horário) —
pediu explicitamente pra "juntar os dois sistemas". Também pediu
animação (respirar, em vez de imagem estática parada) e uma caixinha
de som que reage quando o Spotify está tocando. Implementado e
**confirmado ao vivo** (com uma ressalva sobre o Spotify, ver abaixo):

1. **Guarda-roupa e rotina viraram um sistema só**: antes, escolher um
   look com `unlockIndex == 0` (o vestido vermelho) fazia
   `RoomViewModel` cair de volta pro estado `Routine` por horário — ou
   seja, a seleção do vestido nunca aparecia de verdade, e o guarda-roupa
   mentia. Corrigido: qualquer look selecionado (`_selectedOutfit`, agora
   incluindo o vestido) vira `GabiRoomState.WearingOutfit` de verdade;
   "Rotina do dia" virou uma opção EXPLÍCITA no guarda-roupa (primeiro
   slot da grade, ícone de casa) que mapeia pra `selectOutfit(null)` — é
   o padrão inicial (sem seleção salva). Slot selecionado agora tem borda
   destacada (cyan) no diálogo. **Testado ao vivo**: abrir o guarda-roupa
   mostra "Rotina do dia" com a borda de seleção ativa (é o estado atual,
   correto), "Vestido Vermelho" desbloqueado com "Arte pendente" (nome
   do arquivo mudou, ver Pendente), e "Moletom e Calça Preta" (ex-"Look
   Casual", renomeado pra bater com o que a roupa realmente é) aparece
   como 2º look, trancado.

2. **Nomes de arquivo dos looks 1 e 2 mudaram** (pedido explícito do
   usuário — o vestido precisa de uma pose própria em vez de reusar
   `gabi-idle.png`, e o moletom vira look 2 de verdade em vez de ficar
   preso à pose de rotina "mexendo no computador"):
   - `vestido_vermelho`: `gabi-idle.png` → `gabi-look-vestido-vermelho.png`
   - `look_casual` → renomeado pra `moletom_preto` ("Moletom e Calça
     Preta"): `gabi-look-casual.png` → `gabi-look-moletom.png`
   Nenhum dos dois arquivos existe ainda — aparecem como "Arte pendente"
   no guarda-roupa até o usuário gerar e soltar em `assets/` com esses
   nomes exatos (prompts pedidos pelo usuário, ainda não enviados —
   pendência da próxima resposta, não desta rodada de código).

3. **Respiração (animação nativa do Compose, sem asset novo)**:
   `BreathingCharacter` em `RoomScreen.kt` usa
   `rememberInfiniteTransition` pra pulsar escala (1.0↔1.015) e flutuar
   levemente no eixo Y (0↔-4dp) em loop de 1.8s com easing suave — dá a
   sensação de "ela está viva" sem precisar de GIF/vídeo (que o usuário
   pediu como visão maior, mas concordamos ser um projeto futuro maior,
   não desta rodada). Confirmado que roda ao vivo de um jeito indireto
   curioso: o `uiautomator dump` do dispositivo passou a falhar com
   "could not get idle state" na tela do Quarto — o Android considera a
   UI "ocupada" porque a animação infinita nunca para, o que é evidência
   de que ela está de fato rodando continuamente.

4. **Caixinha de som reativa ao Spotify**: `JavisNotificationListenerService`
   ganhou acesso a `MediaSessionManager`/`MediaController` (reaproveitando
   o MESMO binding de notificações que a Gabi já tinha — nenhuma
   permissão nova) — expõe `spotifyPlaying: StateFlow<Boolean>` no
   companion object, atualizado via `onListenerConnected()` +
   `OnActiveSessionsChangedListener` + `MediaController.Callback`
   filtrado pro pacote `com.spotify.music`. `RoomViewModel.uiState`
   incorpora esse flow (`isSpotifyPlaying`); `RoomScreen.kt` mostra uma
   caixinha de som (ícone `Speaker`) no canto inferior esquerdo do
   Quarto — cinza quando não tem nada tocando, verde-Spotify com 3
   notinhas (`MusicNote`) flutuando/sumindo em loop quando
   `isSpotifyPlaying == true`. **Testado ao vivo só a parte "sem
   música"**: a caixinha renderiza certo no canto, cinza/inativa (sem
   crash) — não foi possível confirmar a reação verde+notinhas ao vivo
   porque nenhuma música estava tocando no aparelho durante o teste.
   Logicamente deveria reagir corretamente na próxima vez que o usuário
   testar com o Spotify tocando algo, mas fica como pendência de
   confirmação, não como "implementado e testado".

**Pendente**:
- Enviar os prompts de geração de imagem pedidos pelo usuário —
  `gabi-look-vestido-vermelho.png` (vestido vermelho como pose própria,
  substituindo o reuso de `gabi-idle.png`) e `gabi-look-moletom.png`
  (moletom + calça preta, hoje só existe "emprestado" da pose de rotina
  `gabi-computador.png`) — corrigindo a proporção cabeça/corpo e o
  comprimento do cabelo pra bater com o "personagem principal", conforme
  a crítica de arte que o usuário fez. Ainda não escrito.
- Confirmar ao vivo a reação da caixinha de som com o Spotify realmente
  tocando algo.
- Animação "de verdade" (digitando, mexendo no cabelo, vídeo/GIF) segue
  como visão de longo prazo, não desta rodada — só a respiração sutil
  foi implementada agora.
- Remover os campos de Client ID/Secret do Spotify em
  `SettingsScreen.kt`/`SettingsViewModel.kt` (redundantes agora que é
  nativo via `BuildConfig`) — combinado, ainda não feito (baixa
  prioridade, inofensivo deixar como está).

Build: `compileDebugKotlin` + `assembleDebug` → `BUILD SUCCESSFUL`.
Instalado e testado ao vivo no aparelho: guarda-roupa fundido com a
rotina confirmado (seleção "Rotina do dia" destacada corretamente,
"Moletom e Calça Preta" aparece como look próprio trancado), caixinha
de som renderiza sem crash no estado inativo. Respiração e reação ativa
do Spotify ainda sem confirmação visual direta (ver pendências acima).

## Animação de vídeo de verdade — pipeline de composição com ffmpeg (26/08/2026)

Depois da imagem estática + respiração via Compose, o usuário pediu pra ir
direto pra animação de vídeo de verdade (gerada com Magnific): confirmado
explicitamente via pergunta ("Partir pra animação de vídeo agora"). Ele
gerou os clipes e passou pra eu montar.

**Arquitetura técnica descoberta e implementada nesta rodada**:

1. **`res/raw/` (não `assets/`)**: novo composable `LoopingSceneVideo.kt`
   toca vídeo via `media3-exoplayer`/`media3-ui` (já eram dependências do
   projeto — nenhuma dependência nova) usando `RawResourceDataSource`
   (`android.resource://<package>/<id>`), muted, `REPEAT_MODE_ONE`, sem
   controles. `rememberRawVideoResId(nome)` resolve o recurso pelo nome e
   retorna `null` se não existir — é a base do fallback automático (cena
   sem vídeo cai pra pose estática + respiração, sem quebrar nada).
2. **Sem canal alfa de vídeo no Android**: confirmado ao inspecionar os
   clipes do Magnific — eles vêm com fundo **preto sólido**, não
   transparência real. Compositar vídeo com alfa em tempo real no Android
   exigiria `RenderEffect` (API 31+, o app mira a API 26) ou um shader GL
   próprio via Media3 Effects — complexidade desnecessária pro caso. A
   solução adotada: **assar tudo num único MP4 opaco por fora do app**,
   usando `ffmpeg` (instalado nesta sessão — nem winget nem o pacote npm
   `@ffmpeg-installer/ffmpeg` sozinhos bastaram: o winget trouxe a versão
   nova (9.0.1, com o filtro `xfade`) mas não atualiza o `PATH` sem
   reiniciar o shell, então o binário foi chamado pelo caminho completo em
   `AppData\Local\Microsoft\WinGet\Packages\...\ffmpeg.exe`).
3. **Pipeline por cena** (roteiro reaproveitável pras próximas cenas):
   - Normaliza os clipes (`scale`+`pad` quando a resolução não bate,
     sempre 1080×1920/24fps/sem áudio).
   - Concatena os 2 clipes da cena com um `xfade` de 0.5s no meio (pedido:
     "junte elas pra ficar seguidas").
   - Fecha o loop: extrai os primeiros 0.5s do resultado concatenado e
     aplica um segundo `xfade` do final de volta pro início — reduz (não
     elimina 100%) o "pulo" quando o vídeo repete, sem precisar que o
     prompt original já tivesse sido gerado com frame inicial = frame
     final.
   - Remove o preto: filtro `colorkey=0x000000:0.06:0.04` (similaridade
     BAIXA de propósito — testado com 0.15 primeiro e as botas/calça preta
     dela também ficaram parcialmente transparentes junto com o fundo,
     porque são cores próximas do preto puro; 0.06 preserva roupa escura e
     ainda remove o fundo).
   - Sobrepõe (`overlay`) no fundo do quarto animado (também fornecido
     pelo usuário, em loop via `-stream_loop -1`) pra cena do Quarto, OU
     numa cor sólida `0x0A0F16` (a mesma do `skyGradient` da Home, região
     onde a personagem fica) pra cena da Home — resolve o problema de
     "onde vai a transparência" sem precisar de alfa de verdade.
4. **Cena "Home" nova** (não existia sistema de vídeo lá ainda — só o
   Quarto tinha): `HomeScreen.kt` agora checa `rememberRawVideoResId("home_idle")`
   e, se existir E o estado for `GabiCharacterState.IDLE`, mostra o vídeo
   dentro de um `Box` com `Modifier.background(Color(0xFF0A0F16))` (a
   mesma cor "assada" no vídeo, evita borda visível onde o vídeo opaco
   encontra o gradiente) usando `resizeMode = FIT` (não `ZOOM`, que é o
   padrão do Quarto) — o vídeo é 9:16 mas a caixa da Home é quadrada
   (260dp), `FIT` evita cortar a personagem. Só o estado IDLE tem vídeo
   por enquanto; ouvindo/falando continuam no `GabiCharacter` de sempre
   (crossfade de PNG). `LoopingSceneVideo` ganhou o parâmetro
   `resizeMode` pra suportar os dois casos (ZOOM/FIT).
5. **Arquivos gerados e testados**: `scene_computador.mp4` (Quarto, junção
   dos 2 clipes de "escrevendo no PC" + fundo do quarto animado) e
   `home_idle.mp4` (Home, junção dos 2 clipes do "idle padrão" numa cor
   sólida).

**Confirmado ao vivo no aparelho**: `home_idle.mp4` — **funcionando
perfeitamente**. Screenshot pegou ela no meio de um gesto (mão erguida,
diferente da pose estática parada), prova de que o vídeo está tocando de
verdade; cor de fundo bate exatamente com o gradiente, sem borda visível.

**`scene_computador.mp4` — implementado e verificado por frame extraído,
mas AINDA NÃO confirmado rodando dentro do app**: só ativa na janela de
horário 11h–16h (rotina "Mexendo no computador"), e o teste ao vivo caiu
fora dessa janela (17h, mostrou "Lendo um livro" corretamente, sem vídeo
ainda pra essa cena — confirma que o fallback funciona sem crash, mas não
prova o vídeo do computador em si). Vai tocar sozinho na próxima vez que o
horário cair nessa faixa, ou o usuário pode conferir manualmente nesse
período.

**Problema de qualidade real encontrado (não é bug de composição, é do
material-fonte)**: no clipe do computador, a calça/legging preta dela
ficou borrada/manchada depois da composição — não é o chroma key
removendo a roupa (isso foi corrigido, ver item 3 acima), é o PRÓPRIO
vídeo do Magnific que comprimiu mal uma região grande de preto-sobre-preto
(roupa preta colada no fundo preto = artefato de compressão H.264 clássico
em áreas de baixo contraste). **Recomendação pro usuário pra próximas
gerações**: evitar fundo preto quando a roupa da cena também tiver preto
grande (calça, bota grande) — verde ou magenta puro como fundo (chroma key
clássico) evita esse problema porque nunca é uma cor que aparece na roupa
dela.

**Também restaurado nesta rodada**: as 8 imagens estáticas
(`gabi-comer.png`, `gabi-computador.png`, `gabi-dormir.png`,
`gabi-idle.png`, `gabi-ler.png`, `gabi-listening.png`, `gabi-talking.png`,
`room-bg.png`) foram substituídas pelas versões melhoradas que o usuário
gerou (mais 3D, pose um pouco de lado, melhor distribuídas no tamanho de
tela) — confirmado ao vivo que a arte nova está em uso (pose "Lendo um
livro" mostrou o vestido roxo da versão nova).

Build: `assembleDebug` → `BUILD SUCCESSFUL` (APK maior agora por causa dos
vídeos em `res/raw/`, ~9MB adicionais). Instalado e testado ao vivo.

**Pendente**: confirmar `scene_computador.mp4` rodando de verdade dentro
da janela 11h–16h; decidir se vale regerar o clipe do computador com fundo
verde/magenta pra resolver o borrão da calça; aplicar o mesmo pipeline
pras próximas cenas (deitar, ler, festa, pijama) conforme o usuário for
gerando mais clipes — o roteiro do item 3 acima é reaproveitável passo a
passo.

## Vídeo assado → sequência de PNG com alfa real (28/08/2026) — usuário reclamou do fundo preto

Usuário testou os arquivos `home_idle.mp4`/`scene_computador.mp4` da rodada
anterior e reportou dois problemas reais (confirmados ao reabrir os arquivos
e conferir): **(1)** o vídeo aparecia com fundo preto sólido em vez de "sem
fundo" como as imagens — porque a solução da rodada passada "assava" uma cor
fixa no vídeo (jeito mais simples de contornar a falta de vídeo com alfa
nativo no Android), e isso fica óbvio quando o arquivo é aberto fora do app;
**(2)** "quadrados nas pernas" na cena do computador — confirmado com um
close-up, artefato de compressão do próprio clipe original do Magnific
(preto sobre preto comprime mal), não da minha composição.

Perguntei ao usuário se valia a pena resolver a transparência de verdade
(mais trabalho) ou manter o jeito mais simples — escolheu resolver de
verdade.

**Solução implementada — sequência de PNGs com canal alfa real, não vídeo**:
avaliei duas rotas pra transparência de vídeo de verdade no Android
(`RenderEffect`, API 31+, não serve pro minSdk 26 do app; ou um shader GL
customizado via `Media3 Effects`/`setVideoEffects()`, tecnicamente possível
mas com superfície de API incerta o suficiente pra ser arriscado sem
conseguir testar incrementalmente) e escolhi uma terceira, mais simples e
confiável: exportar cada clipe já com chroma-key aplicado (mesmo ffmpeg da
rodada anterior) como uma **sequência de arquivos PNG individuais, cada um
já com canal alfa de verdade** (não precisa remover cor em tempo de
execução — o alfa já vem pronto do ffmpeg), tocados em loop dentro do
Compose. Isso reaproveita 100% da infraestrutura de imagem que já existia
(mesma ideia do `GabiAssetImage`) em vez de mexer em API de vídeo/GL
experimental.

1. **`ffmpeg -vf "fps=8,scale=320:-1"`** exporta os clipes já com alfa
   (`idle_keyed2.mov`/`pc_keyed2.mov`, os mesmos intermediários com chroma
   key já ajustado da rodada anterior) como PNGs numerados —
   deliberadamente reduzido pra 8fps/320px de largura (em vez do 24fps/1080px
   original) pra manter a memória num teto razoável: ~86–92 frames por cena,
   ~topo de 60-70MB decodificados em RAM enquanto a tela está visível, longe
   dos 300MB+ que o full-res/full-fps custaria.
2. **`assets/anim/<nome-da-cena>/f001.png`, `f002.png`...** — pasta nova
   dentro de `assets/` (não `res/raw/`, que agora só guarda vídeo opaco sem
   personagem).
3. **`FrameSequenceAnimation.kt`** (novo): `FrameSequenceAnimation(folder,
   frameCount, fps=8, modifier)` carrega todos os frames em `Dispatchers.IO`
   (não trava a UI) e cicla via `LaunchedEffect` com `delay`.
   `rememberAnimFrameCount(folder)` conta quantos arquivos existem em
   `assets/anim/<folder>/` via `context.assets.list(...)` — não precisa
   saber o número de frames de antemão nem recompilar quando o usuário
   adicionar mais; pasta ausente = `null` = cai pro fallback estático, igual
   ao padrão já estabelecido com `rememberRawVideoResId`.
4. **Quarto e personagem viraram camadas independentes** (antes eram um
   único vídeo "assado" junto): `RoomScreen.kt` agora tem **duas** camadas —
   fundo (`res/raw/room_bg_loop.mp4`, quarto animado, opaco, sem
   personagem, tocado com `LoopingSceneVideo`/ZOOM de sempre) + personagem
   (`FrameSequenceAnimation` com alfa real, por cima). Isso é estritamente
   melhor que a v1: o quarto anima o tempo todo agora (mesmo em cenas cuja
   personagem ainda não tem animação própria — antes o quarto só animava
   quando a cena INTEIRA já vinha assada), e a personagem flutua de verdade
   por cima de qualquer fundo, sem precisar bater cor.
5. **`GabiRoomState.videoResName` renomeado pra `animFolder`** (mesmo
   conceito, nome mais preciso já que agora é uma pasta de PNGs, não mais
   um recurso de vídeo).
6. **Home**: trocado o `Box` com `Modifier.background(0xFF0A0F16)` +
   `LoopingSceneVideo` pela `FrameSequenceAnimation` direto (sem nenhum
   fundo sólido por trás) — o gradiente `skyGradient` já existente aparece
   por trás dela de verdade agora, não uma aproximação de cor.
7. **Limpeza**: removidos `res/raw/scene_computador.mp4` e
   `res/raw/home_idle.mp4` (os vídeos "assados" antigos, mortos agora);
   `room_norm.mp4` da rodada anterior virou `res/raw/room_bg_loop.mp4`
   (reaproveitado como está — só o quarto, sem personagem, não precisava
   de nenhuma mudança). Dependência `media3-effect` foi adicionada e depois
   REMOVIDA de novo no mesmo build (cheguei a cogitar o caminho do shader
   GL antes de decidir pela sequência de PNG — ficou sem uso, tirei pra não
   inflar o app à toa).

Build: `compileDebugKotlin` (17min — primeira resolução das dependências
media3 novas foi lenta) + `assembleDebug` → `BUILD SUCCESSFUL` nos dois.
**Não testado ao vivo ainda** — celular não estava disponível pra conectar
no fim desta rodada; usuário disse que testa por conta própria mais tarde.
Preciso confirmar na próxima sessão (ou quando o usuário reportar): Home
com o gradiente de verdade atrás dela (sem nenhuma borda/caixa visível),
Quarto com o chão animado + personagem por cima sem artefato de borda
visível, e que a troca pra sequência de PNG não introduziu travamento/jank
perceptível ao trocar de tela (é o principal risco dessa abordagem —
decodificar 86-92 bitmaps de uma vez).

**Ainda pendente** (não mudou desta rodada): o borrão de compressão nas
pernas da cena do computador continua lá (é do material-fonte, não da
composição) — resolver de vez precisa que o usuário regere aquele clipe
específico com fundo verde/magenta em vez de preto.

## Sessão 20–21/09/2026 — "ela não ouve" + busca de uma voz gratuita para a Gabi

**Nenhum código do app foi alterado nesta sessão.** Só diagnóstico, pesquisa e testes de
voz (feitos fora do projeto, na pasta temporária da sessão, e copiados para `voz-teste/`).

### 1. App web — remoção pedida, NÃO feita
O usuário disse "pode tirar" o app web (Next.js). O comando foi negado na hora e nunca foi
refeito (regra global: `rm` exige confirmação dupla explícita). Só do web: `src/ public/
data/ node_modules/ package.json package-lock.json next.config.ts next-env.d.ts
postcss.config.mjs eslint.config.mjs tsconfig.json tsconfig.tsbuildinfo drizzle.config.ts
render.yaml scripts/ RENDER-GUIA.md`. Manter: `android/`, `AUDITORIA.md`, `ANALISE_REPOS.md`
(é sobre repos Android), `gabi-avatar-completo/`, `CLAUDE.md`. `SPEC.md` e `VERIFICACAO.md`
são históricos/ambíguos. Os itens rastreados no git são reversíveis.

### 2. Diagnóstico do "ela não ouve / erro de fala"
Celular real via ADB: Xiaomi 2311DRK48G, Android 15, HyperOS, chip MediaTek.
- Reconhecedor padrão do sistema: `com.google.android.tts` (`GoogleTTSRecognitionService`).
  No log, em 18 de 18 tentativas (~77 s) ele detectou fala (`onStartOfSpeech`) mas devolveu
  resultado VAZIO (`NO_SPEECH_DETECTED`, "code -1"). Nenhum texto chegou e o app nem chamou
  a IA. O microfone e a escuta funcionam; falha a transcrição. Antes (YouTube por voz)
  funcionava, então algo mudou (serviço do Google no aparelho OU o pedido do app): NÃO
  determinado.
- **Teste pendente que separa as hipóteses:** ditado por voz do Gboard. Se o Gboard também
  falhar, é o serviço de fala do celular (atualizar/reinstalar "Serviços de fala do Google",
  baixar pt-BR offline). Se entender, o ajuste é no pedido do app.
- Bugs de app confirmados no código/log (NÃO corrigidos): (a) `JavisSpeechRecognizer.onError`
  reinicia sozinho após erro 5 (CLIENT) mesmo depois do usuário mandar parar (log:
  stopListening 17:07:17.679, "Erro transiente" 14 ms depois, nova sessão em seguida);
  (b) código -1 cai no `else` de `errorMessage()` ("Erro ao reconhecer a fala (código N)")
  e o `ChatViewModel` só trata 6/7 como esperado, então derruba o modo contínuo;
  (c) o filtro da palavra "Gabi" (`ChatViewModel.kt` ~L180) vale para TODA fala por voz, não
  só no modo contínuo: a 1ª frase da sessão sempre exige "gabi" exato, e "Gaby"/"Gabe" são
  descartados em silêncio; (d) `AndroidTtsFallback.speakInternal` só trata onDone/onError,
  sem `onStop` nem timeout: QUEUE_FLUSH ou `stopSpeaking()` deixam a corrotina pendurada e
  `_isSpeaking` preso (provável causa da "trava geral" já registrada); (e) `_isSpeaking` é um
  booleano compartilhado por `speak()` concorrentes (saudação/aviso proativo/resposta).
- Correções propostas ao usuário (aguardando o ok): registrar o código do erro e tratar -1
  como "não ouvi"; não reiniciar após parar; filtro "Gabi" só no modo contínuo; `onStop` +
  timeout no TTS nativo.
- Hipótese NÃO confirmada: "com Spotify/WhatsApp abertos ela para de ouvir" (Android 15
  restringe iniciar serviço de microfone em 2º plano). No teste de 4 min nenhum dos dois
  ficou em primeiro plano e mesmo assim ela não ouviu, então o reconhecimento vazio é
  independente disso.
- Dicas: `adb logcat -c` e captura em arquivo; `adb exec-out screencap -p` funciona onde
  `uiautomator dump` falha (telas com animação infinita).

### 3. Voz da afilhada para a Gabi (objetivo principal da sessão)
> **📄 Proveniência completa de cada áudio de teste (qual modelo gerou o quê,
> veredito do usuário por arquivo) está em `voz-teste/README.md`** — escrito
> pra ser lido sozinho, sem precisar do histórico da conversa (criado
> 13/10/2026 depois que outra IA, revisando um zip do projeto, não conseguiu
> reconstruir de onde veio `gabi_24l.wav` só pelo resumo abaixo).

Requisitos do usuário (a mãe da menina já autorizou): **gratuito**; conversa natural com
QUALQUER texto (nada de frases pré-gravadas/recortes); velocidade natural; a ElevenLabs
não será renovada (era o único caminho que funcionava; a "Roberta" era voz da biblioteca,
não um clone). Amostra: `Gabi Voz isolada.mp3` (44 s, criança de 10 anos, gravação abafada
com pouco agudo) + a transcrição que o usuário digitou. **Idade desejada da voz final:
~12–13 anos.** Todos os testes foram locais (nada enviado a serviços) e a avaliação é SÓ de
ouvido do usuário (eu não ouço áudio). Áudios em `voz-teste/`.
- **Pocket TTS (Kyutai, MIT, pt-BR, clona em CPU):** os pesos de clonagem exigem aceitar os
  termos em huggingface.co/kyutai/pocket-tts + token (conta Rafaelnobre; token fine-grained
  precisa do checkbox de repos restritos). Modelo grande `portuguese_24l`: melhor timbre
  (`gabi_24l` = o melhor de todos; `resposta_24l` em 2º), mas RTF ~1,9 neste PC (mais lento
  que a fala) e falhas: começo instável ("gralha" nos 2 primeiros segundos), frases curtas
  cortadas (3 de 8 precisaram de nova tentativa), texto longo numa chamada só perde fala
  (13 s de ~40 s), então gerar frase por frase. Hipótese NÃO confirmada: texto coloquial
  ("tá", "pra") piora (o teste `*_formal_*` foi gerado mas nunca ouvido). Modelo pequeno
  `portuguese` (RTF 0,62): come palavras e chia. Descartado.
- **OpenVoice V2 em ONNX (troca de timbre sobre uma voz-base clara em pt-BR, MIT):** fala
  clara e correta, mas "áudio sujo e timbre infantil masculino". A altura estava certa
  (~290 Hz); a referência é escura (brilho 1058 Hz, só 5% da energia acima de 4 kHz).
  Clarear a referência quase não mudou o timbre (`fem1` um pouco melhor); filtros de
  formantes na saída deixaram "robô horrível". `pk1/pk2` (referência = áudios do Pocket)
  foram gerados, sem avaliação do usuário. Requer gerar o ONNX com o código do
  myshell-ai/OpenVoice + checkpoint `myshell-ai/OpenVoiceV2/converter` (repo:
  santiquiroz/port-openvoice-onnx). Descartado por ora.
- **OmniVoice (k2-fsa, Apache-2.0, 600+ idiomas):** qualidade "muito boa, som muito nítido",
  mas soa como criança de 6–8 anos (altura mediana ~320 Hz; original 295 Hz; 12–13 anos ≈
  230–260 Hz). Em CPU deste PC leva ~7 min por frase de 7 s (RTF 55–65; download de 34
  min): só serve com placa de vídeo (anunciado RTF 0,025).
- Só pesquisados, NÃO testados: LuxTTS (ysharma3501, Apache-2.0, clona com 3 s, português
  não declarado), Qwen3-TTS, Chatterbox multilíngue (pt-BR), VoxCPM2, GPT-SoVITS. Os apps
  "alternativa ao ElevenLabs" (VoiceStudio etc.) são programas de PC sobre esses mesmos
  modelos e não rodam no Android.
- **Servidor:** hospedar o modelo e o app só mandar o texto e tocar o áudio é viável (é a
  arquitetura do ElevenLabs). Opção gratuita mais plausível: Modal (plano Starter com
  US$30/mês de crédito, T4 ≈ US$0,59/h ≈ 27 h/mês, cobra por ms; cold start de ~20 s ou mais
  após ocioso). Dados de sites de resenha, NÃO verificados na página oficial. HF ZeroGPU:
  ~5 min/dia de GPU via API (só teste). Oracle Always Free foi cortado em jun/2026 (2 OCPU,
  12 GB, sem GPU). O PC do usuário (i7-8550U, MX150 de 2 GB) é fraco e teria que ficar
  ligado. No app, o `speak()` viraria uma chamada HTTP que devolve áudio (parecido com a
  integração da ElevenLabs).
- Pagos pesquisados (fora do requisito): Fish Audio API (~US$15/1M bytes, clona com 15 s),
  ElevenLabs Starter (~US$6/mês), HeyGen API (pay-as-you-go; sugerido ao usuário, não
  avaliado a fundo).

### 4. Próximos passos (ordem sugerida)
1. **Ajustar a idade do OmniVoice:** baixar altura e formantes juntos (~−4 a −5 semitons,
   de ~320 para ~245 Hz) sobre os `omni_*.wav` já gerados (instantâneo; 1 passe do
   `rubberband` do ffmpeg com formant=shifted). Os testes de formante anteriores foram em
   saída ruim do OpenVoice; pode funcionar melhor em áudio limpo. Alternativa: "voice
   design" do OmniVoice (atributo de idade), mas só treinado em zh/en e instável em pt.
2. Teste do Gboard (microfone) e aplicar as correções da seção 2.
3. Decidir: servidor gratuito (Modal) com OmniVoice/Qwen3-TTS vs. gravação nova e limpa
   feita pela mãe (~40 s lendo devagar, perto da boca, sem barulho): melhora qualquer
   clonagem, e herdar o "jeitinho"/palavras comidas da referência é risco em qualquer
   modelo.
4. Só depois integrar no app.

### 5. Higiene
- NÃO rastreados no git: `Gabi Voz isolada.mp3`, `voz-teste/` (voz de uma criança, NÃO
  commitar nem subir ao GitHub) e `gabi-avatar-completo/`. Não entraram no `.gitignore`
  (o usuário não pediu).
- Sobras dos testes: ~6,4 GB de modelos em `~/.cache/huggingface/hub`, ~1,5 GB de ambientes
  Python na pasta temporária da sessão (`pocket-venv`, `omni-venv`, clone do OpenVoice) e o
  token do Hugging Face em `~/.cache/huggingface/token`. Limpar/revogar quando o usuário
  quiser.

### 6. Zip de backup gerado (21/09/2026, fim da sessão)
`C:\CRiando coisa nova\Projeto Gabi.zip` (113,7 MB) — cópia do projeto SEM `node_modules`,
`.next` e `android/app/build`/`.gradle` (regeneráveis, ~900 MB a menos). Inclui `.git`,
código-fonte dos dois apps, `gabi-avatar-completo/`, e **também `voz-teste/` e `Gabi Voz
isolada.mp3`** (voz da afilhada) — ok pra guardar localmente, mas NÃO compartilhar/enviar
esse zip a ninguém sem antes remover essas duas pastas/arquivo. Se pedir zip de novo, gerar
do mesmo jeito (copiar excluindo essas pastas antes de compactar, não zipar a pasta direto).

## Auditoria 13/10/2026 — trabalho do Codex + estado do git

Pedido pelo usuário ("faz uma auditoria completa, eu também mexi no Codex"). Tudo
abaixo é leitura do estado real dos arquivos (`git status`, timestamps, diffs) — não
é relato de uma sessão de trabalho minha, é um raio-x do que mudou desde 25/08/2026.

### 1. Nenhum commit novo desde `b0ac7ba` (25/08/2026)

`git log` confirma: o último commit é `b0ac7ba` ("fix(android): confirmacao via IA,
insets da tela de Permissoes, onboarding"). Tudo documentado nas seções abaixo dele
neste arquivo — rodadas 7 a 12 do WhatsApp, Bluetooth, Spotify, Quarto da Gabi
completo, pipeline de vídeo/PNG, widget de atalho, e agora o trabalho do Codex (ver
item 2) — está **só como mudança não commitada** na árvore de trabalho. Nada foi
perdido (os arquivos estão intactos no disco), mas não existe nenhum "ponto de
restauração" desde 25/08: se algo for sobrescrito por engano, não tem como voltar a
nenhum estado intermediário. Isso não envolve GitHub nem upload — commit aqui é só um
registro local, decisão já tomada antes de manter o git sem push pro remoto (ver
"Decisões já tomadas"). **Perguntar ao usuário se quer um commit de checkpoint com
tudo isso antes de continuar mexendo** — ainda não autorizado.

### 2. Trabalho do usuário com Codex/ChatGPT (13–14/09/2026), nunca documentado aqui

Achado por timestamp de arquivo (todos datados de 13-14/09, fora de qualquer sessão
minha documentada — minha última sessão de código Android antes disso foi 28/08, a
seguinte foi só pesquisa de voz em 20-21/09, sem mexer no app). Confirmado pela
autoria: os arquivos novos têm comentários em **inglês**, diferente do padrão em
português usado em todo o resto do projeto.

1. **Segundo sistema de animação do avatar, paralelo ao meu.** Novo
   `ui/components/AvatarAnimation.kt`: enum `AvatarState` (IDLE/LISTENING/THINKING/
   TALKING — o "THINKING" é um estado novo que eu nunca tinha), lê sequências de
   frames `.webp` de `assets/avatar/<estado>/` a 24fps (decodifica um frame por vez,
   sem guardar todos em memória — mais leve que o `FrameSequenceAnimation.kt` que eu
   fiz, que carrega tudo de uma vez). Ligado na **Home** (`HomeScreen.kt`), não no
   Quarto. **Meu `FrameSequenceAnimation.kt` (PNG a 8fps, pasta `assets/anim/`)
   continua em uso, mas só no Quarto (`RoomScreen.kt`)** — ou seja, agora há DOIS
   sistemas de animação de personagem coexistindo no mesmo app, cada um numa tela.
   Não está quebrado, mas é duplicação real — vale decidir com o usuário se um dos
   dois deveria ser descontinuado, ou se a divisão Home/Quarto é intencional.
2. **`ui/components/RoomIntroVideo.kt`** (novo): vídeo de entrada que toca uma
   única vez ao visitar o Quarto (`assets/avatar/room_intro.mp4`, ExoPlayer,
   `REPEAT_MODE_OFF`, sem áudio/foco de microfone), chamando `onFinished()` quando
   acaba. Ligado em `RoomScreen.kt` junto com o `FrameSequenceAnimation` de sempre
   (o intro toca primeiro, depois cai pra pose normal da rotina).
3. **Testes automatizados de verdade, pela primeira vez no projeto** — fecha a
   lacuna "zero testes" que eu vinha registrando como pendência desde 21/08:
   - `app/src/test/.../AvatarStateTest.kt` (unitário, JVM): testa
     `resolveAvatarState()` — prioridade falando > ouvindo > pensando > parado.
   - `app/src/androidTest/.../AvatarAnimationTest.kt` (instrumentado, roda num
     emulador/aparelho real): confere pixel a pixel que cada estado troca de frame
     de verdade e que o fundo fica transparente (compara contra magenta de fundo),
     e que o vídeo de entrada do Quarto dispara `onFinished` uma única vez.
   Nenhum dos dois foi rodado por mim ainda — não sei se passam.
4. **`BuildConfig.BUILD_TIME`** (novo, em `build.gradle.kts`): grava a hora exata
   de compilação (`dd/MM HH:mm`), mostrado em Ajustes — resolve de vez a confusão
   documentada antes ("testando um APK antigo achando que era o mais novo").
5. **`gabi-avatar-completo/` ganhou material novo**: `bible.png`, `gabinastelas.mp4`,
   `Girl_listening_intently_202609081538.mp4` (nome datado de 08/09), `idade1.mp4`,
   `idade2.mp4`, `melhorzinhoparadaidle.mp4`, `melhorzinhoparaescultando.mp4` —
   provavelmente o material-fonte usado pra gerar os `.webp` de `assets/avatar/`.
6. **`android/app/src/main/assets/avatar/`** (nova pasta): subpastas `idle/`,
   `listening/`, `talking/`, `thinking/` com os frames `.webp`, mais `room.webp` e
   `room_intro.mp4`.
7. `HomeScreen.kt` foi reescrito de forma ampla (196 linhas alteradas) além da
   troca de animação: ganhou os botões de confirmar/cancelar ação pendente que eu
   já tinha implementado antes só no Chat (parece já estar integrado certo com
   `chatViewModel.confirmPendingCommand`), um dashboard novo (`InfoCard` — clima,
   lembretes, avisos, card "Quarto da Gabi"), e o toque na personagem virou
   `pointerInput`/`detectTapGestures` em vez do `clickable` de antes.

**Nada disso foi testado/validado por mim** (nem compilação, nem rodar no
aparelho) — só lido o código. Próximo passo natural: rodar
`./gradlew compileDebugKotlin` pra confirmar que os dois sistemas de animação não
têm conflito de verdade, e perguntar ao usuário se os testes novos já passaram
quando ele rodou pelo Codex.
