# Análise dos 4 repositórios "JAVIS" candidatos

> Data: 2026-08-05 · Análise do código-fonte real (clone) + status de build no GitHub Actions

## TL;DR — veredito

Os 4 são **apps Android NATIVOS de verdade** (Kotlin + Jetpack Compose, Hilt, Room, Retrofit) — **muito mais funcionais** do que a PWA web do seu repo atual. Um app web (Next.js) **nunca** consegue fazer o que esses fazem: wake-word sempre ativo, serviço de acessibilidade (controla outros apps), ler/responder notificações, fazer chamadas/SMS, etc.

| # | Repo | Compila no CI? | APK pronto? | Licença | Veredito |
|---|------|----------------|-------------|---------|----------|
| 1 | **agmanly597/javis** | ✅ último run **success** | ✅ Release **v36** | ✅ **MIT** | 🏆 **Melhor base** |
| 2 | **redx87518-bot/javis-os** | ✅ runs recentes success | ❌ sem release | ⚠️ nenhuma | Mais "skills" (clima grátis, IA offline) |
| 3 | **manl244345-rgb/JAVIS-Launcher-OS** | ⚠️ último (V06) **falhou**, anteriores ok | ✅ Release v1.0.3 | ⚠️ nenhuma | É **launcher** (substitui a home) — mais features de UI |
| 4 | **agmanly597/JAVIS-Android** | ❌ **todos os runs falharam** | ❌ | ⚠️ nenhuma | ❌ Não compila — descartar como base |

## Detalhe por repo

### 🥇 agmanly597/javis — RECOMENDADO como base
- **Licença MIT** (único com licença — seguro pra fork/adaptar com atribuição).
- Compila no CI e **publica APK** (release `build-36`).
- Clean Architecture (data/domain/di), Groq + DeepSeek + ElevenLabs.
- WhatsApp via Accessibility, chamadas por voz, leitor de notificações, memória (Room), atalho global de acessibilidade.
- Arquivos sólidos (ChatViewModel 596 LOC, VoiceManager 237 LOC).

### 🥈 redx87518-bot/javis-os — rico em "agentes"
- Compila no CI (runs recentes ok), mas **sem licença** (uso/derivação legalmente arriscado).
- Tem **AgentRouter** com agentes: Clima (Open-Meteo **grátis, sem chave**), Alarme, Contatos, Resumo de notificações, App Launcher, Resposta automática.
- **IA offline** (`OfflineAiEngine`, 436 LOC) — funciona sem internet.
- Compose + Material 3. Inglês/Nigéria (precisa localizar p/ pt-BR).

### 🥉 manl244345-rgb/JAVIS-Launcher-OS — o "launcher"
- Substitui a tela inicial do celular (gestos: swipe up/left/right/down).
- Mais features de UI: Image Studio, Video Studio, Mission Control, Command Log, Alarme, Memory.
- **Multi-provider IA** (OpenRouter/Groq/DeepSeek/Together/Fireworks) — e o OpenRouter vem com modelo **`free` que não precisa de chave**.
- XML clássico (não Compose). Sem licença. Último build quebrou (mas já teve APK v1.0.3).

### ❌ agmanly597/JAVIS-Android
- **Não compila no CI** (todos os runs falharam, até `startup_failure`). Não serve de base.

---

## Resposta direta às suas perguntas

**"Podemos usar parte deles pra montar o nosso?"**
Sim. Os componentes são modulares e portáveis (Hilt DI, packages separados). Dá pra reaproveitar, p.ex.: `WeatherAgent` (clima grátis), `WakeWordService` ("Hey Javis"), serviços de Acessibilidade, `AIProviderManager`, memória (Room). **Mas** precisa de projeto Android (Kotlin) — não dá pra misturar dentro do seu Next.js atual.

**"Usar um mais funcional e por nossos dados?"**
O mais funcional/seguro é o **agmanly597/javis** (MIT + compila + APK). Porém — **atenção**: as credenciais que você tem (Anthropic/Claude, Alexa, Home Assistant, NewsAPI) **não são o que esses apps usam**. Eles usam Groq/DeepSeek/OpenRouter/ElevenLabs — que têm **tier grátis** (até rodam sem chave). Pra usar suas chaves, eu adiciono Anthropic/H-A/News como novos providers.

## ⚠️ Caveats importantes (honesto)
1. **Decisão de plataforma:** isso é **nativo Android**, não web. É uma virada de chave em relação ao repo atual. Se a meta é "assistente que mexe no celular", o nativo é o caminho.
2. **Eu não compilo Android aqui** (sem Android SDK no sandbox). Posso ler/escrever/adaptar o Kotlin, mas o **APK sai no Android Studio** ou pelo workflow de CI deles (já existe).
3. **Licença:** só o `javis` (MIT) é livre. Os outros 3 não têm licença → legalmente "all rights reserved".
4. **Localização:** todos são inglês ("Sir", cidades da Nigéria). Precisa traduzir p/ pt-BR.

## Recomendação
Fazer um **fork do `agmanly597/javis`** → localizar p/ pt-BR → adicionar seus providers (Anthropic, Home Assistant, NewsAPI, Alexa) → você builda o APK no Android Studio (ou via CI).
