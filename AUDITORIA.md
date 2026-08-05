# 🔍 Auditoria Completa — App Gabi (Android)

> Base: `agmanly597/javis` (MIT) + adaptações Gabi (pt-BR, voz feminina)
> Método: leitura do código-fonte real em `android/app/src/main/kotlin/` (verificado por busca, não por chute)
> Data: 2026-08-05

## Resumo executivo

| Status | Qtd |
|--------|-----|
| ✅ **Completa** | 15 |
| 🟡 **Parcial** | 8 |
| ❌ **Não implementada** | 14 |

**Pontos fortes reais:** arquitetura limpa (Hilt + Room), controle de sistema via Accessibility Service (abre apps, automatiza WhatsApp, lê notificações), voz completa em pt-BR (STT + TTS + ElevenLabs), memória persistente, multi-provedor de IA (Groq/DeepSeek).

**Lacunas importantes vs. "JARVIS":** **não há** Claude (usa Groq), Obsidian, geração de imagens/vídeos, clima, notícias, Bluetooth, Wi-Fi, Home Assistant nem Alexa. Vários desses existiam como *stub* no antigo app web (Next.js), mas **não vieram junto** na virada para Android nativo.

---

## Tabela detalhada

| # | Funcionalidade | Status | Observações (baseadas no código) | Prioridade |
|---|----------------|--------|----------------------------------|------------|
| 1 | Conversa por texto | ✅ Completa | `JavisRepository.sendMessage` → Groq/DeepSeek com fallback. Chat funcional. | — |
| 2 | Conversa por voz (STT) | ✅ Completa | `JavisSpeechRecognizer` (Android `SpeechRecognizer`), agora **pt-BR forçado**. | — |
| 3 | Resposta falada (TTS) | ✅ Completa | `AndroidTtsFallback` (pt-BR feminino) + `ElevenLabsTts` (Roberta pt-BR). | — |
| 4 | Claude como cérebro/orquestrador | ❌ Não implementada | **Zero** código Anthropic. Usa **Groq** (principal) + DeepSeek. Você optou pelos grátis, então isso é intencional. | 🔴 Baixa |
| 5 | Sistema de roteamento de ferramentas | 🟡 Parcial | `CommandParser` faz roteamento por **palavras-chave/regex** (PT+EN), depois cai pro IA. **Não é** function-calling dinâmico (o LLM não escolhe ferramentas). | 🟠 Alta |
| 6 | Integração com Obsidian | ❌ Não implementada | Nenhum código (vault/markdown/export). O "6 arquivos" do scan era falso-positivo de `.md`. | 🔴 Baixa |
| 7 | Memória persistente | ✅ Completa | `MemoryManager` + Room (`UserMemory`: nome, preferências, hábitos). Sobrevive a reinícios. | — |
| 8 | Histórico de conversas | ✅ Completa | Room (`MessageDao`) — carrega últimas 16–20 msgs como contexto. | — |
| 9 | Sistema de habilidades/plugins | ❌ Não implementada | Comandos são **fixos** no `CommandParser`. Não há API de plugin nem registro dinâmico de skills. | 🔴 Baixa |
| 10 | Pesquisa na internet | ✅ Completa (básica) | `SEARCH_WEB` abre o **Google no navegador**. Sem resultados *dentro* do app. | 🟡 Média |
| 11 | Notícias atuais | ❌ Não implementada | Nenhum código de news. (Existia como stub no app web antigo.) | 🟡 Média |
| 12 | Consulta de clima | ❌ Não implementada | Nenhum código. **Mas é fácil**: o repo irmão `javis-os` tem `WeatherAgent` pronto (Open-Meteo, grátis, sem chave) — dá pra portar. | 🟠 Alta |
| 13 | Geração de imagens por IA | ❌ Não implementada | Nenhuma API de imagem (o scan casou `stability` num parâmetro de voz e `IMAGE_CAPTURE` da câmera). | 🔴 Baixa |
| 14 | Geração de vídeos (estrutura) | ❌ Não implementada | Nenhum código. | 🔴 Baixa |
| 15 | Compartilhamento pelo Android | ❌ Não implementada | Sem `ACTION_SEND`/`createChooser`. Fácil de adicionar. | 🟡 Média |
| 16 | Integração com WhatsApp | ✅ Completa | Abre chat por nome, **digita e envia** via Accessibility Service (`openWhatsAppChatByName` + `whatsAppSendMessage`). | — |
| 17 | Integração com Gmail | 🟡 Parcial | Só **abre o app** Gmail (alias em `CommandParser`). Não compõe/envia e-mail. | 🟡 Média |
| 18 | Integração com Agenda | 🟡 Parcial | Só **abre** o app Calendário/Relógio. Não cria eventos (`CalendarContract`). | 🟡 Média |
| 19 | Integração com contatos | ✅ Completa | `callContact` lê `ContactsContract` e liga por nome. | — |
| 20 | Controle por Bluetooth | ❌ Não implementada | Nenhum `BluetoothAdapter`. (A única menção é a palavra "bluetooth" no parser que abre Configurações.) | 🔴 Baixa |
| 21 | Controle de dispositivos Wi-Fi | ❌ Não implementada | Nenhum código de smart devices/IoT. | 🔴 Baixa |
| 22 | Integração com Home Assistant | ❌ Não implementada | Nenhum código. (Você optou por não usar.) | 🔴 Baixa |
| 23 | Integração com Alexa | ❌ Não implementada | Nenhum código no app Android. (Existia no app web antigo.) | 🔴 Baixa |
| 24 | Dashboard/HUD principal | 🟡 Parcial | A "home" **é o Chat**. Não há dashboard com cards/widgets/estados (como o SPEC web imaginava). | 🟡 Média |
| 25 | Configurações | ✅ Completa | `SettingsScreen`: provedor, chaves, voz, recursos, dados. | — |
| 26 | Gerenciamento de permissões | ✅ Completa | `PermissionsScreen` orienta mic/contatos/telefone/notificações/acessibilidade. | — |
| 27 | Funcionamento offline | 🟡 Parcial | Memória (Room) e TTS funcionam offline; **IA (Groq) precisa de internet**. Sem modelo on-device (o `javis-os` tem `OfflineAiEngine` — dá pra portar). | 🟡 Média |
| 28 | Múltiplos modelos de IA | ✅ Completa | `AiProvider` + Groq e DeepSeek, alternáveis em Ajustes, com fallback automático. | — |
| 29 | Arquitetura modular | ✅ Completa | Clean Architecture + Hilt DI (19 arquivos em `di/`), separação data/domain/ui. | — |
| 30 | Segurança (chaves/permissões) | 🟡 Parcial | Chaves em **DataStore (não criptografado)**; campos com máscara na UI. **Sem** `security-crypto`/`EncryptedSharedPreferences`. | 🟠 Alta |
| 31 | Sistema de automações | ❌ Não implementada | Sem motor de regras/gatilhos/`WorkManager`. (A automação via Accessibility é "ações em outros apps", item 34.) | 🔴 Baixa |
| 32 | Sistema de lembretes | 🟡 Parcial | Só `SET_ALARM` → abre o **app de relógio do sistema**. Sem lembretes próprios com notificação no app. | 🟠 Alta |
| 33 | Gerenciamento de arquivos | 🟡 Parcial | Há `FileHelper` + permissão de armazenamento, mas **sem gerenciador** (navegar/abir/organizar arquivos). | 🔴 Baixa |
| 34 | Ações em outros aplicativos | ✅ Completa | **Ponto forte.** `JavisAccessibilityService` lê tela, navega, automatiza apps (WhatsApp). | — |
| 35 | Registro de memória no Obsidian | ❌ Não implementada | Sem Obsidian. Memória fica só no Room local. | 🔴 Baixa |
| 36 | Sistema de contexto do usuário | ✅ Completa | `buildContextSummary()` injeta o que a Gabi sabe do usuário no prompt a cada mensagem. | — |
| 37 | Estrutura p/ expansão futura | ✅ Completa | Modular; adicionar provedor/voz/skill é isolar uma classe (interface `AiModelProvider`). | — |

---

## 🎯 Top recomendações (por custo × benefício)

### 🔴 Faça primeiro (alta prioridade, baixo esforço)
1. **Criptografar as chaves** (`security-crypto` / `EncryptedSharedPreferences`) — segurança das suas APIs. *(item 30)*
2. **Portar o clima** do `javis-os` (`WeatherAgent`, Open-Meteo **grátis, sem chave**) — já está pronto, é copiar. *(item 12)*
3. **Lembretes nativos** com notificação no app (`AlarmManager` + `Notification`) — hoje depende do app de relógio. *(item 32)*

### 🟠 Depois (alta/média prioridade)
4. **Roteamento de ferramentas com function-calling** — deixar o LLM chamar ferramentas dinamicamente em vez de regex fixo. *(item 5)*
5. **Compartilhamento Android** (`ACTION_SEND`) — trivial e útil. *(item 15)*
6. **Dashboard/HUD** com cards (clima, lembretes, atalhos) em vez de só chat. *(item 24)*

### 🟡 Opcionais (você decidiu usar só grátis — releve)
- Claude, Home Assistant, Alexa, Notícias: você optou pelos provedores grátis (Groq/ElevenLabs). Mantenho como **baixa** a menos que mude de ideia.
- Offline (IA on-device), Imagens/Vídeos, Obsidian, Bluetooth/IoT: mais trabalhosos ou pagos.

## ⚠️ Observação de processo
- **Não compilei aqui** (sem Android SDK). A auditoria é por leitura de código; qualquer mudança sugerida precisa ser validada no Android Studio.
- O `src/main/java/` duplicado e **não compilado** ainda está no projeto (herdado) — não afeta a auditoria, mas atrapalha a navegação.
