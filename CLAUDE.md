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

## Estado atual / bloqueios conhecidos (20/08/2026, fim do dia)

1. **App não roda**: falta a pasta `data/` para o arquivo SQLite. `src/db/index.ts`
   aponta para `./data/app.db`, e `better-sqlite3` não cria diretórios pai
   sozinho — qualquer rota que importe `db` quebra com `ENOENT`.
2. **`npm run typecheck` falha com 8 erros**, todos por `SpeechRecognition` /
   `SpeechRecognitionEvent` não declarados (API Web Speech não está nos tipos do
   projeto). Arquivos: `src/components/screens/ChatScreen.tsx`,
   `src/components/screens/HomeScreen.tsx`. `npm run build` também falha por
   causa disso.
3. **Git com rebase travado**: esta pasta herdou (via cópia) um
   `git pull --rebase origin main` iniciado em 16/08 e nunca concluído — o índice
   do git mostra ~30 arquivos como "unmerged", mas o conteúdo em disco já parece
   resolvido (sem marcadores `<<<<<<<` reais; `NexusApp.tsx` já foi removido a
   favor de `GabiApp.tsx`). Provavelmente resolve com
   `git add -A && git rebase --continue` após revisão — ou `git rebase --abort`
   se preferir recomeçar do commit local.
4. **Segurança (Android)**: chaves de API salvas em DataStore sem criptografia
   (`AUDITORIA.md` item 30) — deveria usar `EncryptedSharedPreferences`/
   `security-crypto`.

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
  criado. Nenhum dos bloqueios acima foi corrigido ainda — próxima sessão deve
  começar pela Etapa 1.
