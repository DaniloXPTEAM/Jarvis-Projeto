# alexa-referencia — pacote para melhorar a voz da Gabi

Tudo que foi extraído do app do Alexa (com o REA) e do código da Gabi, mais o plano para diagnosticar e alterar.
**Nenhum código da Gabi foi alterado. Nenhum código do Alexa foi copiado**: são descrições, valores e ideias.

## Ordem de leitura
| # | Arquivo | Para quê |
|---|---|---|
| 1 | `README.md` | este índice |
| 2 | `BRIEFING_PARA_CLAUDE_VSCODE.md` | **comece por aqui no VS Code**: regras e as 15 tarefas |
| 3 | `DIAGNOSTICO_VOZ_GABI.md` | onde está o atraso e por que ela não ouve enquanto fala (lido do código da Gabi) |
| 4 | `PARAMETROS_DE_VOZ.md` | valores reais de áudio do Alexa: 16 kHz, mono, PCM 16 bits, buffer de 3 s, AEC, foco de áudio |
| 5 | `PLANO_DE_ALTERACOES.md` | **passo a passo**: Fase 0 medir → 1 correções pequenas → 2 falar por frases → 3 uma chamada de IA → 4 ouvir enquanto fala → 5 palavra de ativação |
| 6 | `ALEXA_PERMISSOES_REA.md` | permissões e módulos internos (inclui o que é exclusivo da Amazon, só como rótulo de referência) |
| 7 | `EVIDENCIAS_E_AMBIENTE.md` | ids das evidências do REA e o que foi instalado/criado no PC |
| 8 | `ESTRUTURA_CLASSES_AUDIO.md` | campos e métodos das 7 classes de áudio do Alexa (só estrutura, sem código) |

## Resumo em 6 linhas
1. A Gabi só volta a ouvir **depois** de terminar de falar (fluxo em sequência), usa o reconhecedor do Android (uma frase por vez) e não tem cancelamento de eco nem foco de áudio.
2. Atraso: espera fixa de ~1,8-2,5 s no fim da fala + 1 ou 2 chamadas de IA sem streaming (modelo grande) + voz que só toca com o MP3 inteiro pronto.
3. Bug: áudio com mais de 20 s é cortado no meio.
4. O Alexa capta a 16 kHz mono, guarda 3 s num buffer circular, envia o áudio em blocos enquanto a pessoa fala, tem AEC opcional e tratamento de foco de áudio por tipo de perda.
5. **Meça antes de mexer** (Fase 0). A ordem de importância no diagnóstico é estimativa.
6. Item 7 do briefing (biometria) está **descartado**.

## Regras
- Uma fase por vez, um commit por fase. Nunca `git add -A` (o repositório tem mudanças do Dannilo não commitadas).
- Não use nomes `com.amazon.*` no código da Gabi nem integre com serviços da Amazon.
- Confirme na documentação oficial antes de usar qualquer API ou biblioteca citada como "a verificar".
- Compile (`gradlew.bat assembleDebug`) antes de dizer que está pronto.
