# Mapa de materiais do Projeto Gabi

Índice curto pra achar as coisas sem precisar reler o `CLAUDE.md` inteiro (2800+ linhas).
Criado em 17/11/2026, depois de uma auditoria que consolidou material espalhado em
~6 pastas diferentes do PC dentro desta única pasta (`Projeto Gabi`).

## Onde está cada coisa

| O que é | Onde | Status |
|---|---|---|
| App Android (código real, em uso) | `android/` | Ativo. Branch `voz-latencia`, não mergeada na `main`. Ver CLAUDE.md pra detalhe linha a linha. |
| App Web (Next.js) | `src/` | Abandonado desde 20/08/2026, pedido de remoção nunca executado. |
| Plano de latência de voz (Alexa/REA) | `alexa-referencia/` (8 MDs) | Já executado em parte (Fases 0,1,3,4 + 3 provedores de IA). Base da arquitetura atual de voz (ElevenLabs na nuvem). |
| **Plano concorrente: voz 100% local (Pocket TTS)** | `KIRANA_GABI_DOSSIE_COMPLETO.md` (raiz, 1276 linhas) + `voz-downloads/GABI_PLANO_CODEX_POCKET_TTS.md` (694 linhas) | **Nunca reconciliado com o que foi implementado.** Outra sessão de IA (ChatGPT Work/Codex) projetou rodar a voz da Gabi local no Android via Pocket TTS, substituindo a ElevenLabs — objetivo diferente do que a branch `voz-latencia` fez. Ver seção abaixo. |
| Protótipos de minigame (Quarto da Gabi, virtual pet) | `minigame-prototipos/` (6 zips, não extraídos) | Protótipos isolados, nunca integrados ao app Android real. `v1`/`v2`/`v3` são builds sucessivas do mesmo conceito. |
| Modelo 3D da Gabi (projeto separado) | `modelo-3d/` | Projeto à parte, segundo o usuário "não está dando certo" — mantido como protótipo, sem mexer por ora. |
| Testes de voz antigos (Pocket TTS, OpenVoice, OmniVoice) | `voz-teste/` (raiz) | Sessão de 20-21/09/2026. Ver `voz-teste/README.md`. |
| Downloads soltos de voz recentes | `voz-downloads/*.wav` | Testes mais recentes (13/11/2026), proveniência não documentada ainda. |
| Cópias antigas/históricas | `arquivo-historico/` | `projeto-gabi-copia-set2026/` (cópia do app web de 15/09, sem a pasta `android/`) + `whatsapp-fotos-ago2026/` (fotos soltas resgatadas de um clone git esquecido em `Documents/GitHub/Jarvis Projeto`). |

## O que ainda NÃO foi comparado/decidido

- **Estratégia de voz dividida em 3 caminhos nunca reconciliados**: (1) ElevenLabs na nuvem — o que está implementado hoje; (2) Pocket TTS local no Android — plano detalhado em `KIRANA_GABI_DOSSIE_COMPLETO.md` + `GABI_PLANO_CODEX_POCKET_TTS.md`, nunca executado; (3) servidor próprio (Oracle Cloud) — tentado e bloqueado por falta de capacidade (ver CLAUDE.md, sessão 14-15/10). **Antes de investir mais em qualquer um dos três, vale decidir qual é o caminho de verdade** — os dois primeiros competem pelo mesmo objetivo (voz da Gabi) com arquiteturas incompatíveis.
- Os 6 zips de `minigame-prototipos/` nunca foram abertos/comparados entre si — não sei dizer qual é "o melhor", só a ordem cronológica dos nomes.
- `modelo-3d/` nunca foi avaliado tecnicamente por mim — só sei que existe e que o usuário disse que "não está dando certo".

## MDs da raiz — triagem (17/11/2026)

- ❌ `AUDITORIA.md` — **apagado** (usuário confirmou que estava desatualizado).
- ✅ `SPEC.md`, `VERIFICACAO.md` — mantidos (usuário decidiu não apagar, apesar de antigos).
- ❓ `RENDER-GUIA.md` (deploy do app web no Render) e `ANALISE_REPOS.md` (análise do repo-fonte `agmanly597/javis`) — usuário não decidiu, ficou em dúvida sobre o que cada um significa. Ainda presentes, sem decisão.

## Estado do GitHub (17/11/2026)

- Remoto real existe: `github.com/DaniloXPTEAM/Jarvis-Projeto` (muda a decisão antiga documentada de "só local"). **Repositório é PÚBLICO.**
- Branch `voz-latencia` local = `origin/voz-latencia` (sincronizado). `main` local e remoto divergiram há meses (coisa antiga, inofensiva).
- Usuário vai renomear o repo manualmente (quer tirar "Jarvis" do nome, já que existe um projeto Jarvis-base de verdade, separado, em `Documents/GitHub/Jarvis Projeto`).
- Pendência: comparar o que está local em `Projeto Gabi` com o que já foi pushado (`Gabi Up`, 12/11) antes de decidir se falta algum commit — ver CLAUDE.md seção mais recente.
- Decisão do usuário (17/11): tornar o repo privado só DEPOIS do checkpoint completo estar 100% no GitHub (o projeto vai se fundir com outro projeto/motor dele).
