# Gabi — Assistente Pessoal (contexto do projeto)

Este arquivo é lido automaticamente no início de cada sessão. Mantenha-o atualizado
conforme o projeto evolui — ele existe para evitar ter que reler o código inteiro
para "redescobrir" o projeto a cada nova sessão.

## O que é

App de secretária pessoal com IA, nome **Gabi** (ex-"Jarvis"/"Nexus"). Dois códigos
distintos convivem neste repositório:

1. **App Web** (`src/`) — Next.js, é o foco ativo de desenvolvimento.
2. **App Android nativo** (`android/`) — fork de `agmanly597/javis` (MIT), Kotlin,
   arquitetura própria e independente do app web. Ver `AUDITORIA.md` (37 recursos
   auditados, 05/08/2026) antes de mexer nele.

Documentos de referência já existentes (não duplicar aqui, só consultar):
`SPEC.md` (spec visual original), `AUDITORIA.md` (auditoria do Android),
`ANALISE_REPOS.md`, `RENDER-GUIA.md`, `VERIFICACAO.md`.

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
