# Origem dos áudios de teste de voz da Gabi

Este documento explica, sem exigir o histórico da conversa, de onde vem cada
arquivo `.wav` desta pasta — o que é, qual modelo gerou, qual foi a avaliação
do usuário, e o que **ainda não** foi decidido.

**Nenhum destes áudios é uma decisão final.** São testes de 21/09/2026 pra
comparar caminhos gratuitos de clonagem de voz. Nada foi integrado ao app
Android ainda.

## ⚠️ Conteúdo sensível — não compartilhar

Esta pasta contém a voz de uma criança (a afilhada do usuário, ~10 anos),
tanto a gravação original quanto clones sintéticos derivados dela. A mãe da
criança autorizou o uso pra este projeto pessoal. **Não subir esta pasta pro
GitHub, nem enviar pra terceiros, nem usar fora deste projeto.**

## A gravação original (a fonte de tudo)

- Arquivo: `../Gabi Voz isolada.mp3` (fora desta pasta, na raiz do projeto).
- 44 segundos, voz real da afilhada do usuário (~10 anos), gravação meio
  abafada, com pouco agudo. Ela fala naturalmente — solta, comendo algumas
  palavras — porque é assim que uma criança de 10 anos fala, não é defeito
  da gravação.
- O usuário também digitou a transcrição do que ela fala nesse áudio
  ("Ontem o dia foi muito legal, eu fui pra aniversário de duas meninas...").
- **Idade de voz desejada pro resultado final: ~12-13 anos** — mais velha do
  que a gravação original, porque é a idade que a Gabi (a assistente) deveria
  "soar".

## Os 3 caminhos testados (todos gratuitos, todos rodados localmente neste PC)

Nenhum áudio foi enviado pra nuvem/serviço externo em nenhum dos 3 — tudo
rodou num ambiente Python isolado, só neste computador.

### 1. Pocket TTS (Kyutai, MIT, clonagem zero-shot)

Modelo aberto da empresa francesa Kyutai. Dado um trecho de referência de
alguém falando, ele gera qualquer texto novo imitando aquele timbre. Testado
em 2 variantes:
- `portuguese` ("pequeno", menos camadas, mais rápido)
- `portuguese_24l` ("24l" = 24 layers, variante maior/mais pesada — **não**
  tem relação com idade nem com nada da Gabi, é só o nome da variante do
  modelo)

Referência de timbre usada: os primeiros 20 segundos de
`Gabi Voz isolada.mp3`, convertidos pra WAV mono 24kHz (`ref20.wav` — esse
arquivo intermediário NÃO foi salvo nesta pasta, só os resultados finais).

**Arquivos e veredito do usuário** (ouvindo, eu não ouço áudio):
| Arquivo | Texto | Modelo | Veredito do usuário |
|---|---|---|---|
| `gabi_24l.wav` | "Oi! Eu sou a Gabi, sua assistente. Como posso te ajudar hoje?" | grande | **"O melhor de todos. Mais parecido, sem pressa e sem comer muito gramática."** |
| `gabi_pequeno.wav` | mesma frase | pequeno | não comentado isoladamente |
| `resposta_24l.wav` | "Claro! Já vou abrir o Spotify pra você. Qual música você quer ouvir?" | grande | "Segundo melhor, mas os primeiros 2s +- parece uma gralha, depois fica humano." |
| `resposta_pequeno.wav` | mesma frase | pequeno | "Mesmo defeito do outro, 1s de ruído esquisito depois normaliza." |
| `original_24l.wav` | 1ª frase da transcrição original ("Ontem o dia foi muito legal...") | grande | "Mais ou menos, lembra um pouco. Mas comeu um monte de coisas e não respeitou acentuação." |
| `original_pequeno.wav` | mesma frase | pequeno | "Se for um desespero e não de outra, essa é aceitável no tom. Mas repetiu umas 3 palavras." |
| `pergunta_24l.wav` | "Oi, Gabi! O que você tá achando dessa voz? Tá parecida com a sua?" (coloquial) | grande | **"Ridículo. Um robô ridículo."** |
| `pergunta_pequeno.wav` | mesma frase | pequeno | "Ridículo. Um robô ridículo que come palavras e gramática." |
| `frases_24l.wav` | transcrição completa (~40s), gerada frase por frase com pausas | grande | não comentado isoladamente (teste técnico de "texto longo perde fala") |
| `frases_pequeno.wav` | mesma, frase por frase | pequeno | "Comeu palavras e muito chiado." |

**Padrão observado**: frases em português coloquial ("tá", "pra") saíram
piores que frases em português formal ("está", "para") — hipótese levantada
mas nunca confirmada com certeza. O começo de cada geração (~1-2s) também
tende a sair pior ("gralha"/ruído), efeito mais visível em frases curtas.

### 2. OpenVoice V2 (MyShell, MIT, troca de timbre)

Caminho diferente: não gera a fala do zero, **troca o timbre** de um áudio
já falado por outra pessoa. Convertido pra formato ONNX (sem precisar de
PyTorch em produção) a partir do código oficial do MyShell
(`myshell-ai/OpenVoiceV2`), usando o checkpoint `converter` deles.

Fluxo: (1) uma "voz-base" fala o texto com pronúncia/ritmo corretos — nos
testes, usada uma voz feminina genérica via um serviço de TTS só pra esse
propósito de teste (`base_pergunta.wav`, `base_resposta.wav` = a voz-base,
ANTES de qualquer conversão); (2) o conversor troca o timbre dessa fala pelo
timbre extraído de um áudio de referência.

**Resultado principal**: a fala saiu clara e correta (herdou isso da
voz-base), mas o timbre ficou "infantil masculino" — problema nunca
resolvido nos testes.

| Arquivo | Referência de timbre usada | Veredito do usuário |
|---|---|---|
| `vc_pergunta_ref20.wav`, `vc_resposta_ref20.wav` | 20s de `Gabi Voz isolada.mp3` | (baseline, ver `fem1`/`fem2` abaixo pra comentário) |
| `vc_pergunta_ref44.wav`, `vc_resposta_ref44.wav` | os 44s inteiros de `Gabi Voz isolada.mp3` | idem |
| `fem1_clareada_leve_*.wav` | igual, mas com o áudio de referência "clareado" (mais agudo) antes de extrair o timbre, tentativa leve | "O timbre não mudou. Continua infantil masculino. A clareza leve tá um pouco melhor." |
| `fem2_clareada_forte_*.wav` | igual, clareamento forte | "O timbre não mudou. Continua infantil masculino." |
| `fem3_eq_formantes12_*.wav` | saída com equalização + deslocamento de formantes (+12%, tentando soar mais feminino/infantil) | **"Robô horrível."** |
| `fem4_eq_formantes20_*.wav` | igual, +20% | **"Robô horrível."** |
| `pk1_gabi24l_*.wav` | usa `gabi_24l.wav` (do Pocket TTS, já aprovado) como referência de timbre, em vez do áudio original | gerado, **nunca avaliado pelo usuário** |
| `pk2_frases_fim_*.wav` | usa o fim de `frases_24l.wav` como referência | gerado, **nunca avaliado pelo usuário** |

Conclusão: esse caminho foi **descartado** — mesmo variando a referência e
tentando "feminizar" o resultado, o timbre nunca convenceu.

### 3. OmniVoice (k2-fsa, Apache-2.0, clonagem zero-shot, 600+ idiomas)

Modelo maior e mais pesado que o Pocket TTS, do mesmo grupo por trás do
Kaldi. Mesmo princípio de clonagem zero-shot (dá um trecho de referência,
ele imita o timbre falando texto novo).

| Arquivo | Texto | Veredito do usuário |
|---|---|---|
| `omni_gabi.wav` | "Oi! Eu sou a Gabi..." | — |
| `omni_pergunta.wav` | "Oi, Gabi! O que você tá achando dessa voz?..." | — |
| `omni_resposta.wav` | "Claro! Já vou abrir o Spotify..." | — |

Veredito geral do usuário pros 3: **"Qualidade muito boa, som muito nítido,
mas parecem de uma criança muito nova, 6-8 anos."** (medido tecnicamente:
altura mediana ~320 Hz, contra ~295 Hz do áudio original e uma faixa alvo de
~230-260 Hz pra soar como 12-13 anos).

**Limitação prática séria**: neste PC (sem placa de vídeo dedicada
decente), o OmniVoice levou **~7 minutos pra gerar 7 segundos de áudio**
(55-65x mais lento que o tempo real). Só serve de verdade rodando numa
placa de vídeo de verdade (nuvem).

## O que ficou decidido, e o que não

- ✅ Entre os 3 caminhos, **Pocket TTS `portuguese_24l`** (o "gabi_24l")
  teve a melhor avaliação de timbre/naturalidade.
- ✅ **OmniVoice** teve a melhor avaliação de nitidez/qualidade técnica, mas
  soa jovem demais e é lento demais pra rodar neste PC.
- ✅ **OpenVoice** (troca de timbre) foi descartado.
- ❌ Nada foi integrado ao app Android — isso é só o estágio de "qual
  motor de voz vale a pena perseguir", não um resultado pronto pra usar.
- ❌ O problema de idade (soar mais velha, 12-13 anos) não foi resolvido em
  nenhum dos caminhos.
- ❌ Não foi decidido entre: (a) rodar o modelo escolhido num servidor
  gratuito (ex.: Modal, com placa de vídeo) e o app chamar por HTTP, ou
  (b) pedir uma gravação nova e mais limpa da criança (a mãe lendo um texto
  devagar, perto do microfone) pra melhorar a referência de qualquer modelo.

Para o relato completo da sessão (incluindo o diagnóstico separado do bug
de reconhecimento de voz do app, que não tem relação com este documento),
ver a seção **"Sessão 20–21/09/2026"** em `../CLAUDE.md`.
