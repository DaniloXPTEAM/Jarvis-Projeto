# KIRANA / GABI — DOSSIÊ TÉCNICO COMPLETO
## Estado do projeto, histórico de testes, benchmarks, candidatos, decisões e plano de continuidade

**Atualização consolidada:** 13/10/2026  
**Finalidade:** documento de continuidade para ChatGPT Work ou qualquer outro agente técnico assumir o projeto sem depender do histórico da conversa.

---

# 1. ESTADO ATUAL EM UMA FRASE

> **A voz mais aprovada até agora é a produzida pelo Pocket TTS `portuguese_24l` usando como referência os primeiros 20 segundos da gravação original da Gabi. O problema atual não é simplesmente encontrar outra voz: é reproduzir essa identidade de forma consistente e conseguir executá-la com runtime e streaming rápidos o suficiente no Android.**

---

# 2. OBJETIVO DO PROJETO

Kirana/Gabi é um projeto de agente de voz local em português brasileiro, com foco em:

- voz feminina jovem e natural;
- português brasileiro convincente;
- baixa latência;
- execução preferencialmente local/offline;
- streaming de áudio progressivo;
- Android;
- possibilidade de interrupção imediata da fala (barge-in);
- baixo consumo de RAM/CPU;
- ausência de dependência obrigatória de GPU dedicada;
- ausência de API paga como solução principal.

## Metas de desempenho

| Métrica | Meta |
|---|---:|
| Primeiro áudio — ideal | **≤ 500 ms** |
| Primeiro áudio — máximo aceitável | **≤ 800 ms** |
| RTF | **< 1; preferencialmente ≤ 0,8** |
| Barge-in / cancelamento | **100–300 ms** |
| Detecção de fim de fala | **400–800 ms** |
| Reprodução | PCM progressivo / streaming real |
| Execução | Preferencialmente local/offline |
| RAM/CPU | Sustentável no Android |
| Qualidade | Sem palavras comidas, chiado ou degradação perceptível |

É obrigatório distinguir:

- **first PCM:** instante em que o sintetizador produz o primeiro bloco PCM;
- **first audible sound:** instante em que o usuário realmente começa a ouvir o áudio.

O segundo é a métrica de experiência mais importante.

---

# 3. PRIVACIDADE E CONSENTIMENTO

O arquivo:

`Gabi Voz isolada.mp3`

contém a voz real da afilhada do usuário, aproximadamente 10 anos.

Segundo o usuário, a mãe da criança autorizou o uso da voz neste projeto pessoal.

## Regras obrigatórias

- Não publicar o áudio.
- Não colocar no GitHub.
- Não transformar em dataset público.
- Não compartilhar com terceiros.
- Não enviar para serviços externos sem autorização explícita.
- Preferir processamento local.
- Se alguma ferramenta exigir upload para nuvem/API, PARAR e informar isso antes de enviar.
- Clones sintéticos derivados dessa voz devem receber o mesmo cuidado.

Historicamente existia o objetivo de a assistente soar aproximadamente **12–13 anos**, mas isso NÃO autoriza modificar artificialmente pitch/formantes/timbre nesta etapa. Primeiro deve ser preservada/reproduzida a identidade vocal que já foi aprovada.

---

# 4. GRAVAÇÃO ORIGINAL

## Arquivo-fonte

`Gabi Voz isolada.mp3`

Características registradas:

- aproximadamente 44 segundos;
- voz real da Gabi;
- gravação um pouco abafada;
- pouco conteúdo de agudos;
- fala natural de criança;
- algumas palavras são pronunciadas de forma solta/rápida;
- isso faz parte da fala original e não deve automaticamente ser considerado defeito.

Foi também registrada uma transcrição do áudio, começando aproximadamente por:

> "Ontem o dia foi muito legal, eu fui para aniversário de duas meninas..."

---

# 5. SESSÃO ORIGINAL DE TESTES — 20/21 DE SETEMBRO DE 2026

Foram avaliados três caminhos principais gratuitos/localmente:

1. Pocket TTS;
2. OpenVoice V2;
3. OmniVoice.

Nenhum estava integrado ao aplicativo Android nessa etapa.

---

# 6. POCKET TTS — KYUTAI

## 6.1 Visão geral

Pocket TTS foi o caminho que recebeu a melhor avaliação subjetiva de identidade vocal.

Foram testadas duas variantes:

- `portuguese` — variante menor/mais rápida;
- `portuguese_24l` — variante de 24 layers, maior e mais pesada.

**Importante:** `24l` significa **24 layers**. Não representa idade, versão da Gabi ou qualquer característica vocal.

## 6.2 Como a referência foi preparada

O procedimento histórico está agora recuperado:

`Gabi Voz isolada.mp3`

→ primeiros **20 segundos**

→ conversão para **WAV mono / 24 kHz**

→ arquivo intermediário:

`ref20.wav`

→ Pocket TTS zero-shot.

O `ref20.wav` original não foi preservado, mas pode ser reconstruído deterministicamente a partir do MP3 original.

## 6.3 Origem exata do `gabi_24l.wav`

Modelo:

`Pocket TTS portuguese_24l`

Referência:

`ref20.wav`

Texto:

> "Oi! Eu sou a Gabi, sua assistente. Como posso te ajudar hoje?"

Resultado:

`gabi_24l.wav`

### Avaliação do usuário

> **"O melhor de todos. Mais parecido, sem pressa e sem comer muito gramática."**

Esse é o principal resultado auditivo aprovado até agora.

**Não houve alteração intencional de pitch, formantes, idade, EQ criativo ou time-stretch para criar esse resultado.**

---

# 7. TODOS OS TESTES POCKET TTS REGISTRADOS

| Arquivo | Texto / finalidade | Modelo | Avaliação |
|---|---|---|---|
| `gabi_24l.wav` | "Oi! Eu sou a Gabi, sua assistente. Como posso te ajudar hoje?" | `portuguese_24l` | **Melhor de todos. Mais parecido, sem pressa e sem comer muito gramática.** |
| `gabi_pequeno.wav` | mesma frase | `portuguese` | Não comentado isoladamente |
| `resposta_24l.wav` | "Claro! Já vou abrir o Spotify pra você. Qual música você quer ouvir?" | `portuguese_24l` | **Segundo melhor**, mas primeiros ~2 s parecem uma "gralha"; depois fica humano |
| `resposta_pequeno.wav` | mesma resposta | `portuguese` | ~1 s de ruído estranho; depois normaliza |
| `original_24l.wav` | primeira frase da transcrição original | `portuguese_24l` | Lembra um pouco, mas come palavras e não respeita bem acentuação |
| `original_pequeno.wav` | mesma frase | `portuguese` | Tom aceitável em último caso, mas repetiu aproximadamente 3 palavras |
| `pergunta_24l.wav` | pergunta coloquial | `portuguese_24l` | **"Ridículo. Um robô ridículo."** |
| `pergunta_pequeno.wav` | mesma pergunta | `portuguese` | **"Robô ridículo que come palavras e gramática."** |
| `frases_24l.wav` | transcrição completa ~40 s, gerada frase a frase | `portuguese_24l` | Teste técnico; sem avaliação isolada |
| `frases_pequeno.wav` | mesma sequência | `portuguese` | **Comeu palavras e apresentou muito chiado** |

---

# 8. HIPÓTESES OBSERVADAS NO POCKET TTS

Duas hipóteses surgiram, mas ainda NÃO foram comprovadas sistematicamente.

## 8.1 Português formal versus coloquial

Houve indício de que:

- `está` pode funcionar melhor que `tá`;
- `para` pode funcionar melhor que `pra`.

Frases coloquiais pareceram produzir resultados piores em alguns testes.

Isso precisa de A/B controlado.

## 8.2 Degradação nos primeiros 1–2 segundos

Várias gerações apresentaram problema no início:

- ruído;
- som descrito como "gralha";
- fala mais robótica;
- estabilização posterior.

Isso é especialmente importante porque frases curtas de assistente podem terminar antes de a voz "estabilizar".

Possíveis linhas de investigação:

- warm-up;
- prefill;
- cache de estado;
- contexto inicial;
- chunk inicial;
- segmentação;
- tamanho do buffer;
- geração de prefixo;
- comportamento do decoder no primeiro bloco.

Nenhuma dessas hipóteses deve ser considerada solução antes de teste A/B.

---

# 9. POCKET TTS PEQUENO — STATUS

A variante `portuguese` menor teve vantagens de velocidade em alguns testes, porém foi rejeitada qualitativamente.

Problemas relatados:

- palavras comidas;
- repetições;
- chiado;
- gramática/pronúncia degradada;
- comportamento robótico.

## Status

**Não usar como solução principal apenas porque é mais rápido.**

Prioridade: **baixa**, exceto como referência de desempenho.

---

# 10. OPENVOICE V2 — MYSHELL

## 10.1 Estratégia

OpenVoice V2 utiliza uma abordagem diferente.

Em vez de gerar diretamente a fala completa a partir da referência, o fluxo testado foi:

1. uma voz-base fala corretamente o texto;
2. OpenVoice troca o timbre dessa fala;
3. a referência da Gabi orienta o timbre final.

Foi utilizado o conversor OpenVoice V2/ONNX.

## 10.2 Resultados

A pronúncia era clara porque vinha da voz-base.

Porém, o timbre resultante foi percebido como:

> **infantil masculino**

Foram testadas várias estratégias.

| Arquivo / grupo | Estratégia | Resultado |
|---|---|---|
| `vc_pergunta_ref20.wav` / `vc_resposta_ref20.wav` | referência de 20 s | baseline |
| `vc_pergunta_ref44.wav` / `vc_resposta_ref44.wav` | referência completa ~44 s | não resolveu timbre |
| `fem1_clareada_leve_*` | referência clareada levemente | clareza um pouco melhor, timbre continuou infantil masculino |
| `fem2_clareada_forte_*` | clareamento forte | timbre continuou inadequado |
| `fem3_eq_formantes12_*` | EQ + formantes +12% | **"Robô horrível."** |
| `fem4_eq_formantes20_*` | EQ + formantes +20% | **"Robô horrível."** |
| `pk1_gabi24l_*` | `gabi_24l.wav` usado como referência | gerado, nunca avaliado |
| `pk2_frases_fim_*` | fim de `frases_24l.wav` como referência | gerado, nunca avaliado |

## Status

**DESCARTADO como caminho principal.**

Não repetir esses experimentos sem uma razão técnica nova.

---

# 11. OMNIVOICE — K2-FSA

## 11.1 Características

- clonagem zero-shot;
- suporte anunciado a centenas de idiomas;
- modelo consideravelmente mais pesado.

Arquivos registrados:

- `omni_gabi.wav`;
- `omni_pergunta.wav`;
- `omni_resposta.wav`.

## 11.2 Avaliação auditiva

O usuário avaliou a qualidade técnica/nitidez como muito boa.

Porém:

> parecia uma criança de aproximadamente **6–8 anos**.

Medições históricas registradas:

- pitch mediano do resultado: ~320 Hz;
- gravação original: ~295 Hz;
- faixa heurística que havia sido considerada para 12–13 anos: ~230–260 Hz.

Essa faixa é apenas uma referência técnica histórica e NÃO deve ser usada automaticamente para alterar a voz.

## 11.3 Performance

No PC utilizado:

**~7 minutos para gerar ~7 segundos de áudio.**

Aproximadamente:

**55–65× mais lento que tempo real.**

Sem GPU adequada, isso torna o modelo impraticável para o objetivo local/interativo.

## Status

Qualidade técnica interessante, mas:

- idade percebida inadequada;
- desempenho inviável no hardware disponível.

**Prioridade baixa / alternativa futura caso o contexto de hardware mude.**

---

# 12. BENCHMARK POSTERIOR — POCKET TTS 24L

Posteriormente houve benchmark mais específico do:

`portuguese_24l + Gabi`

em ambiente Linux/CPU.

Resultados registrados:

| Métrica | Resultado |
|---|---:|
| Carga do modelo | ~18,799 s |
| Primeiro PCM | ~578–733 ms |
| RTF sustentado | ~2,467 |
| RAM | ~1,9 GiB |
| Streaming | apresentou underruns |

## Interpretação

A voz 24L continuou sendo a preferida, porém o desempenho ficou muito abaixo do necessário.

RTF ~2,47 significa que a síntese não acompanha reprodução em tempo real de maneira sustentável.

**Este benchmark NÃO é um benchmark Android definitivo.**

Ele serve como baseline histórico.

---

# 13. PROBLEMA ATUAL REAL

A questão central deixou de ser:

> "Qual modelo consegue produzir qualquer voz?"

E passou a ser:

> **"Como preservar a identidade/qualidade que conseguimos com Pocket TTS `portuguese_24l` e executar rápido o suficiente para uma assistente Android em tempo real?"**

---

# 14. POCKET TTS OTIMIZADO PARA ARM / MIXED PRECISION

Durante a curadoria posterior foi identificado um caminho especialmente relevante envolvendo Pocket TTS otimizado para processadores ARM/mixed precision.

Foi discutido o pacote/projeto:

`Arm/pocket-tts-pt-mix-precision`

A documentação/material pesquisado mencionava tecnologias como:

- GGUF;
- ONNX;
- LiteRT;
- runtime/Compute Flow.

**A compatibilidade real precisa ser confirmada. Não inventar suporte.**

## Benchmarks externos discutidos

### Variante equivalente de 6 layers

| Métrica | Resultado externo |
|---|---:|
| Primeiro bloco | ~107 ms |
| RTF | ~0,222 |
| RAM | ~820 MB |

### Variante equivalente de 24 layers

| Métrica | Resultado externo |
|---|---:|
| Primeiro bloco | ~398 ms |
| RTF | ~0,582 |
| RAM | ~2,7 GB |

Contexto mencionado:

- Vivo X300;
- benchmark equivalente em inglês.

## ALERTA

Esses números:

- NÃO foram medidos com a Gabi;
- NÃO foram comprovados em PT-BR no projeto;
- NÃO foram medidos no aparelho Android do usuário;
- NÃO garantem preservação da identidade vocal.

Mesmo assim, são extremamente relevantes porque o **24L externo estaria teoricamente dentro das metas de latência e RTF**.

## Prioridade

**PRIORIDADE MÁXIMA DE INVESTIGAÇÃO.**

É atualmente a alternativa mais lógica porque tenta acelerar justamente a família/modelo cuja voz foi melhor avaliada.

---

# 15. SUPERTONIC 3

Outro candidato pesquisado posteriormente.

Características discutidas:

- execução local;
- múltiplos idiomas;
- português disponível;
- vozes femininas;
- recursos expressivos.

## Benchmark externo discutido

Galaxy S23 Ultra:

| Métrica | Resultado externo |
|---|---:|
| Primeiro áudio | ~1,1 s |
| RTF | ~0,34 |
| RAM | ~832 MB |

O RTF é excelente para nossa meta.

Porém, **~1,1 s até primeiro áudio ultrapassa nosso máximo desejado de 800 ms.**

Também foi observado durante a pesquisa que o repositório oficial havia sido arquivado em **09/09/2026**.

Foi citado benchmark de leitura em português:

- Supertonic 3: WER ~2,48;
- Supertonic 2: WER ~1,52.

Esses números são externos.

## Status

Interessante como comparação de:

- naturalidade;
- memória;
- throughput.

Mas não deve substituir Pocket imediatamente.

## Prioridade

**Secundária — A/B caso Pocket otimizado falhe ou para comparação de naturalidade.**

---

# 16. KOKORO

Kokoro foi discutido como referência de eficiência.

Benchmark externo short-turn citado:

| Métrica | Resultado externo |
|---|---:|
| RTF | ~0,75–0,88 |
| RAM | ~527 MB |
| Primeiro áudio | não estabelecido no projeto |

Contexto:

Galaxy S23 Ultra.

## Pontos positivos

- consumo de memória interessante;
- RTF próximo/dentro da meta;
- potencialmente adequado a dispositivos móveis.

## Pontos ainda não resolvidos

- identidade da Gabi;
- qualidade PT-BR específica;
- naturalidade;
- first audible sound.

## Prioridade

**Secundária / benchmark comparativo.**

---

# 17. SPEECH-ANDROID / SONIQO

Foi discutido como referência arquitetural de implementação Android.

Características observadas:

- Kotlin;
- C++;
- JNI;
- execução local;
- TTS;
- STT;
- VAD;
- interrupção/cancelamento.

Foi citado externamente um pipeline em torno de:

**~1,2 GB RAM**

Esse número NÃO foi medido no dispositivo do usuário.

## Utilidade para o projeto

Não necessariamente substituir o TTS.

Pode servir como referência para:

- arquitetura Kotlin/JNI;
- carregamento de modelo;
- streaming;
- controle de áudio;
- cancelamento;
- gerenciamento de recursos.

---

# 18. JOTA SPEAKER

Foi analisado como referência de arquitetura de streaming.

Características discutidas:

- PCM16;
- streaming;
- WebSocket;
- cancelamento sem destruir toda a sessão;
- descarte de áudio pendente.

Foi relatado externamente:

**interrupção <100 ms**

porém em teste/motor simulado.

Isso NÃO comprova <100 ms com inferência TTS real.

## Utilidade

Estudar:

- cancelamento;
- barge-in;
- descarte de chunks antigos;
- arquitetura de streaming.

Não introduzir servidor externo apenas por causa disso.

---

# 19. X2STREAMING-TTS

Paper discutido:

**X2Streaming-TTS**

Publicação mencionada: agosto de 2026.

A proposta é importante porque trata de **streaming de verdade**, consumindo texto incrementalmente em vez de esperar uma sentença inteira.

Conceitos relevantes:

- texto incremental;
- segmentos ambíguos;
- continuidade acústica;
- manutenção de contexto entre chunks.

O paper relatava aproximadamente:

**15,8 ms de mediana até primeiro token de áudio**

em seu cenário experimental.

Esse número NÃO deve ser comparado diretamente com latência end-to-end em Android.

## Aplicação conceitual

Investigar:

- segmentação incremental;
- geração por orações curtas;
- continuidade prosódica;
- não esperar resposta textual completa para começar TTS.

---

# 20. TABELA MESTRE DE CANDIDATOS

| Solução | Tipo | PT-BR | Identidade Gabi | First audio | RTF | RAM | Status | Prioridade | Origem |
|---|---|---|---|---:|---:|---:|---|---|---|
| Pocket `portuguese_24l` | zero-shot TTS | Sim | **Melhor avaliada** | ~578–733 ms first PCM no baseline Linux | ~2,467 | ~1,9 GB | Voz aprovada, lento | **Máxima** | Medido no projeto |
| Pocket `portuguese` | zero-shot TTS | Sim | Fraca/inconsistente | — | mais rápido que 24L | — | Rejeitado qualitativamente | Baixa | Projeto |
| Pocket ARM 6L otimizado | TTS otimizado | potencial | Não testada | ~107 ms | ~0,222 | ~820 MB | Não validado | Alta técnica | Externo |
| Pocket ARM 24L otimizado | TTS otimizado | potencial | **Candidato ideal a preservar 24L** | ~398 ms | ~0,582 | ~2,7 GB | Não validado | **Máxima** | Externo |
| OpenVoice V2 | voice conversion | Sim | Timbre inadequado | — | — | — | **Descartado** | Nenhuma | Projeto |
| OmniVoice | zero-shot | Sim | Nítida, mas jovem demais | extremamente lento neste PC | 55–65× pior que real | — | Impraticável local | Baixa | Projeto |
| Supertonic 3 | TTS local | Sim | Não testada | ~1,1 s | ~0,34 | ~832 MB | Candidato secundário | Média | Externo |
| Kokoro | TTS local | precisa validar | Não testada | — | ~0,75–0,88 | ~527 MB | Referência eficiente | Média | Externo |
| speech-android | arquitetura/SDK | depende do motor | N/A | — | — | ~1,2 GB pipeline citado | Referência Android | Arquitetural | Externo |
| Jota Speaker | arquitetura streaming | N/A | N/A | — | — | — | Referência de cancelamento | Arquitetural | Externo |
| X2Streaming-TTS | pesquisa | conceitual | N/A | ~15,8 ms token no paper | — | — | Referência de streaming | Pesquisa | Paper |

---

# 21. ORDEM ATUAL DE PRIORIDADE

## PRIORIDADE 0 — REPRODUZIR O `gabi_24l`

Antes de qualquer migração:

1. usar `Gabi Voz isolada.mp3`;
2. extrair exatamente os primeiros 20 s;
3. converter para mono / 24 kHz;
4. criar `ref20.wav`;
5. usar Pocket TTS `portuguese_24l`;
6. reproduzir o texto histórico.

### Teste A — controle

> "Oi! Eu sou a Gabi, sua assistente. Como posso te ajudar hoje?"

Objetivo:

reproduzir o experimento original.

### Teste B

> "Claro! Eu posso fazer isso para você. Só preciso que me diga por onde gostaria de começar."

### Teste C

> "Entendi. Então vamos fazer assim: primeiro eu verifico isso para você e depois seguimos para o próximo passo."

Depois:

**PARAR PARA APROVAÇÃO AUDITIVA DO USUÁRIO.**

Não otimizar antes disso.

---

# 22. PRIORIDADE 1 — RUNTIME POCKET 24L OTIMIZADO

Após aprovação da identidade:

investigar a execução do mesmo caminho com:

- ARM64;
- ONNX Runtime;
- LiteRT;
- mixed precision;
- runtime específico disponível;
- outras otimizações que não alterem perceptivelmente a saída.

Não assumir que uma conversão é compatível.

Validar.

## Regra

Se a otimização mudar perceptivelmente a voz:

**NO-GO**, mesmo que fique muito mais rápida.

---

# 23. PRIORIDADE 2 — CACHE E STREAMING

Evitar repetir trabalho a cada resposta.

Idealmente manter:

- modelo carregado;
- runtime inicializado;
- embedding/perfil da Gabi em cache;
- buffers preparados;
- AudioTrack aberto quando apropriado.

Implementar:

**texto → TTS → chunks PCM → AudioTrack**

sem esperar o WAV completo.

A reprodução deve começar enquanto o restante da frase ainda está sendo sintetizado.

---

# 24. PRIORIDADE 3 — BENCHMARK ANDROID REAL

O benchmark definitivo precisa ocorrer no aparelho real.

Medir:

1. cold model load;
2. warm model load;
3. memória antes/depois;
4. primeiro PCM;
5. primeiro som;
6. tempo total de geração;
7. duração do áudio;
8. RTF;
9. CPU;
10. pico de RAM;
11. tamanho dos chunks;
12. intervalo entre chunks;
13. underruns;
14. tempo em que geração termina;
15. tempo em que reprodução termina.

## Fórmula

`RTF = tempo_de_geração / duração_do_áudio`

### Interpretação

- RTF 0,5 = gera 2× mais rápido que reprodução;
- RTF 0,8 = margem aceitável;
- RTF 1,0 = limite;
- RTF >1 = síntese começa a ficar atrás da reprodução.

---

# 25. PRIORIDADE 4 — ROBUSTEZ PT-BR

Depois de preservar a voz e obter desempenho:

testar sistematicamente:

### Formal vs coloquial

- está / tá;
- para / pra;
- você / cê, se pertinente.

### Tipos de frase

- afirmação;
- pergunta;
- exclamação;
- instrução;
- resposta curta;
- resposta longa.

### Conteúdo

- números;
- datas;
- horários;
- siglas;
- nomes brasileiros;
- nomes estrangeiros;
- pontuação;
- acentos;
- palavras difíceis.

### Comprimento

- 3–5 palavras;
- 10–15 palavras;
- 20–30 palavras;
- parágrafo.

---

# 26. INVESTIGAR O DEFEITO DOS PRIMEIROS SEGUNDOS

Problema histórico:

**primeiros ~1–2 s podem soar piores que o restante.**

Não mascarar isso alterando a voz.

Testar individualmente:

- warm-up silencioso;
- geração descartável antes da primeira resposta;
- prefill;
- cache;
- contexto;
- tamanho do primeiro chunk;
- buffers;
- prefixo textual;
- segmentação;
- inicialização antecipada do decoder.

Cada técnica deve ter:

**baseline vs otimizado.**

---

# 27. PRIORIDADE 5 — ALTERNATIVAS SE POCKET FALHAR

Somente se o Pocket 24L não conseguir simultaneamente:

- voz aprovada;
- latência;
- RTF;
- memória;
- estabilidade;

partir para A/B estruturado.

## Ordem sugerida

1. Supertonic;
2. Kokoro;
3. outros modelos móveis comprovadamente PT-BR;
4. OmniVoice somente se hardware mudar.

OpenVoice permanece descartado, salvo avanço técnico significativo que resolva especificamente o problema observado.

---

# 28. STREAMING REAL

Não considerar streaming se o sistema:

1. gera WAV inteiro;
2. espera terminar;
3. toca depois.

Isso é apenas reprodução pós-síntese.

Streaming desejado:

`LLM/texto incremental`
→ `TTS incremental`
→ `PCM chunk`
→ `AudioTrack`
→ usuário escuta
→ geração continua em paralelo.

---

# 29. AUDIOTrack / ANDROID

Arquitetura desejada:

- PCM progressivo;
- buffer suficientemente pequeno para baixa latência;
- suficientemente grande para evitar underrun;
- cancelamento imediato;
- descarte de chunks antigos;
- geração cancelável;
- nova resposta não espera áudio antigo terminar.

O evento `onAudioStarted` deve representar de maneira clara o momento em que áudio começa efetivamente a ser entregue/tocado, não o fim da síntese.

---

# 30. BARGE-IN

Meta:

**100–300 ms**

Quando o usuário começa a falar:

1. detectar fala;
2. cancelar TTS;
3. interromper AudioTrack;
4. descartar PCM pendente;
5. cancelar geração antiga;
6. aceitar nova entrada.

Evitar aguardar o buffer inteiro.

---

# 31. DETECÇÃO DE FIM DE FALA

Faixa desejada:

**400–800 ms**

Testes históricos também compararam janelas maiores, como:

- 1800 ms;
- 2500 ms;

que deixam a interação lenta demais.

O objetivo é conversa responsiva sem cortar pausas naturais.

---

# 32. CRITÉRIOS GO / NO-GO

## Voz

### GO

Usuário considera a voz suficientemente próxima/igual à identidade aprovada.

### NO-GO

Otimização produz voz perceptivelmente diferente.

---

## Latência

### Excelente

≤500 ms até primeiro som.

### Aceitável

500–800 ms.

### Investigar

800–1000 ms.

### NO-GO para objetivo atual

>1 s sem perspectiva clara de otimização.

---

## RTF

### Excelente

≤0,5.

### Meta

≤0,8.

### Limite

~1,0.

### NO-GO

sustentadamente >1.

---

## Streaming

### GO

áudio começa antes da síntese completa e continua sem underruns perceptíveis.

### NO-GO

precisa esperar a geração completa.

---

# 33. FASES DE EXECUÇÃO

## FASE A — Reprodutibilidade vocal

Entregáveis:

- `ref20.wav`;
- `GABI_CONTROLE_A.wav`;
- `GABI_CONTROLE_B.wav`;
- `GABI_CONTROLE_C.wav`;
- configuração;
- versões;
- hashes.

**Gate:** aprovação auditiva.

---

## FASE B — Baseline

Executar sem otimização agressiva.

Medir:

- carga;
- RAM;
- first PCM;
- first sound;
- RTF;
- CPU;
- underruns.

---

## FASE C — Runtime otimizado

Testar ARM64/ONNX/LiteRT/mixed precision somente quando tecnicamente compatível.

Comparar áudio com baseline.

**Gate:** mesma identidade vocal.

---

## FASE D — Streaming

Implementar PCM progressivo.

Demonstrar com timestamps:

- início request;
- primeiro token/texto;
- primeiro PCM;
- primeiro write;
- primeiro áudio;
- fim da síntese;
- fim da reprodução.

---

## FASE E — Android real

Benchmark no dispositivo alvo.

Não usar resultado desktop como substituto.

---

## FASE F — Robustez PT-BR

Executar matriz formal/coloquial e diferentes tipos de frase.

---

## FASE G — Barge-in

Implementar interrupção/cancelamento.

Meta:

100–300 ms.

---

## FASE H — Decisão final

Escolher motor/runtime apenas após comparar:

- identidade;
- naturalidade;
- first sound;
- RTF;
- RAM;
- estabilidade;
- Android.

---

# 34. ENTREGÁVEIS OBRIGATÓRIOS

Manter no projeto:

- `ref20.wav`;
- três WAVs de controle;
- `environment.txt`;
- arquivo de dependências/lock;
- versão do Python;
- versão do Pocket TTS;
- identificação exata do modelo;
- parâmetros de inferência;
- seed, se existir;
- SHA-256 da referência;
- SHA-256 dos resultados;
- sample rate;
- duração;
- logs de benchmark;
- relatório baseline;
- relatório otimizado;
- relatório Android;
- tabela comparativa;
- logs de timestamps;
- RAM;
- CPU;
- RTF;
- first PCM;
- first audible sound;
- underruns;
- tamanho de chunks;
- decisão GO/NO-GO.

**Não apagar resultados ruins.**

Eles são evidência experimental.

---

# 35. O QUE NÃO FAZER

1. **Não trocar a voz silenciosamente.**
2. **Não trocar o modelo silenciosamente.**
3. Não aplicar pitch shift para "melhorar".
4. Não aplicar formant shift para "feminizar".
5. Não aplicar time-stretch para fingir desempenho.
6. Não aplicar EQ criativo para mudar identidade.
7. Não tentar "rejuvenescer" ou "envelhecer" a voz nesta fase.
8. Não usar Pocket pequeno apenas porque é mais rápido.
9. Não pular aprovação auditiva.
10. Não chamar benchmark externo de resultado do projeto.
11. Não chamar first PCM de first audible sound.
12. Não integrar no APK antes de medir.
13. Não enviar voz da criança para nuvem sem autorização.
14. Não publicar áudio/clones.
15. Não subir referências para GitHub.
16. Não repetir OpenVoice sem nova justificativa técnica.
17. Não afirmar que uma ferramenta/modelo suporta Android/ONNX/LiteRT sem verificar.
18. Não afirmar que "ouviu" um áudio caso o ambiente não permita reprodução/análise auditiva.
19. Não considerar uma voz aprovada apenas por métricas automáticas.
20. **A aprovação subjetiva final da voz é do usuário.**

---

# 36. PRIORIDADE FINAL DOS CAMINHOS

| Prioridade | Caminho | Motivo |
|---:|---|---|
| **0** | Reproduzir `gabi_24l` histórico | Confirmar que a voz aprovada é reproduzível |
| **1** | Pocket 24L otimizado ARM | Melhor chance de preservar voz e atingir velocidade |
| **2** | Cache + PCM streaming + AudioTrack | Necessário para interação real |
| **3** | Benchmark Android | Prova definitiva de desempenho |
| **4** | Robustez PT-BR / início ruim | Corrigir qualidade sem trocar identidade |
| **5** | Supertonic | Alternativa secundária |
| **6** | Kokoro | Alternativa eficiente/memória |
| **7** | OmniVoice | Só reconsiderar com hardware adequado |
| **DESCARTADO** | OpenVoice V2 | Timbre inadequado/robótico nos testes |

---

# 37. PRÓXIMA AÇÃO EXATA PARA O WORK

**Não começar pesquisando mais modelos.**

Primeiro:

1. localizar `Gabi Voz isolada.mp3`;
2. extrair exatamente os primeiros 20 segundos;
3. converter para mono 24 kHz;
4. salvar `ref20.wav`;
5. registrar SHA-256;
6. instalar/carregar exatamente o Pocket TTS `portuguese_24l`;
7. registrar versão e dependências;
8. gerar o Teste A com o texto histórico;
9. gerar B e C;
10. entregar os três WAVs ao usuário;
11. **PARAR e solicitar avaliação auditiva.**

Se A reproduzir adequadamente a identidade do antigo `gabi_24l`:

**GO para otimização.**

Depois:

1. estabelecer baseline;
2. investigar Pocket ARM/mixed precision;
3. verificar runtime real e compatibilidade;
4. preservar os mesmos pesos/identidade sempre que possível;
5. implementar cache;
6. implementar streaming PCM;
7. integrar AudioTrack;
8. benchmark Android;
9. medir first PCM e first sound separadamente;
10. comparar contra metas.

Se o Pocket otimizado não puder ser usado por limitação técnica/acesso:

**documentar exatamente o bloqueio antes de migrar para outro TTS.**

---

# 38. DECISÃO TÉCNICA ATUAL

Não há justificativa, neste momento, para abandonar imediatamente o Pocket TTS.

Ele possui o melhor resultado auditivo já obtido para a identidade da Gabi.

O problema principal é performance.

A investigação de Pocket TTS otimizado para ARM é especialmente importante porque benchmarks externos sugerem que uma variante 24L otimizada pode teoricamente atingir:

- first block ~398 ms;
- RTF ~0,582.

Se algo próximo disso puder ser reproduzido com PT-BR/Gabi no Android real, o principal gargalo do projeto pode ser resolvido **sem abandonar a voz que já foi aprovada**.

Isso precisa ser demonstrado experimentalmente.

---

# 39. REGRA DE OURO DO PROJETO

> **Qualidade vocal e desempenho são dois problemas separados. Primeiro preserve a voz aprovada. Depois acelere a execução sem mudar essa voz.**

Uma solução extremamente rápida com voz errada não resolve o projeto.

Uma voz perfeita com RTF 2,47 também não resolve.

O objetivo final é conseguir simultaneamente:

**Gabi reconhecível + PT-BR natural + primeiro áudio ≤500–800 ms + RTF ≤0,8 + streaming PCM + RAM sustentável + Android local.**

---

# 40. RESUMO EXECUTIVO PARA QUEM ASSUMIR O PROJETO

- A fonte original é `Gabi Voz isolada.mp3`.
- O melhor clone histórico foi `gabi_24l.wav`.
- Ele veio de primeiros 20 s → mono 24 kHz → `ref20.wav` → Pocket `portuguese_24l`.
- Pocket 24L ganhou em identidade/naturalidade, mas o baseline posterior ficou lento.
- Pocket pequeno foi rejeitado por qualidade.
- OpenVoice foi descartado.
- OmniVoice é nítido, mas jovem demais e impraticavelmente lento no hardware testado.
- Pocket ARM/mixed precision é atualmente a investigação de maior prioridade.
- Supertonic e Kokoro são planos B/comparadores.
- speech-android e Jota são referências arquiteturais.
- X2Streaming-TTS é referência conceitual para streaming incremental.
- Não integrar nada antes de reproduzir e aprovar novamente a voz.
- Não confundir benchmarks externos com medições do projeto.
- Não compartilhar a voz da criança.
- O próximo passo é gerar A/B/C com Pocket 24L e obter aprovação auditiva.
- Depois disso, atacar runtime, cache, streaming e Android.

**Fim do dossiê consolidado.**
