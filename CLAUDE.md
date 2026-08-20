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
- Pacote Android continua `com.javis.assistant` internamente (nunca renomeado)
  — decisão pendente, não acidental: afeta ~120 arquivos Kotlin, baixo risco mas
  trabalho grande, adiado para depois do app web estar estável.

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

Pendente (não é mais bloqueio, mas ainda não commitado em `git`):
- `.claude/settings.json`, `src/components/screens/ChatScreen.tsx` e
  `src/components/screens/HomeScreen.tsx` estão modificados no working tree
  (as correções acima) mas ainda não commitados — decidir quando commitar.

Bloqueios que continuam (fora do escopo da Etapa 1):
6. **Segurança (Android)**: chaves de API salvas em DataStore sem criptografia
   (`AUDITORIA.md` item 30) — deveria usar `EncryptedSharedPreferences`/
   `security-crypto`. Fica para a Etapa 3.

## Voz do Android (TTS) — feita fora da sequência das 3 etapas

Motivado por reprovação de teste real do usuário ("voz robótica e arrastada").
Arquivos tocados: `android/app/src/main/kotlin/com/javis/assistant/voice/AndroidTtsFallback.kt`,
`android/app/src/main/kotlin/com/javis/assistant/storage/JavisPreferences.kt`,
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
- **Não validado por build** (sem Android SDK nesta máquina, mesma limitação já
  registrada na `AUDITORIA.md`) — validar no Android Studio/dispositivo real
  antes de considerar resolvido de verdade.

## Plano em 3 etapas (fazendo uma por vez)

- **Etapa 1 — Destravar o app web**: criar `data/`, declarar tipos de
  `SpeechRecognition`, resolver o rebase travado do git, remover strings
  "JARVIS" residuais. Objetivo: `npm run dev` e `npm run build` rodando limpo.
- **Etapa 2 — Organização do repositório**: decidir estratégia de git/remoto a
  partir desta pasta (é a nova cópia de trabalho), renomear pacote Android
  `com.javis.assistant` → Gabi se for pra frente, revisar `.gitignore`.
- **Etapa 3 — Fechar lacunas de produto/segurança**: criptografar chaves no
  Android, revisar os itens "não implementados"/"parciais" do `AUDITORIA.md`
  (roteamento por function-calling em vez de regex, dashboard, etc.) e decidir
  o que entra ou não no escopo atual.

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
  lá e no `.claude/settings.json` desta pasta. Próxima sessão: decidir sobre
  commitar as mudanças pendentes e começar a Etapa 2.
