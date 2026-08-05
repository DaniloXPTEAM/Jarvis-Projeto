# 🎀 Gabi — Assistente Android (pt-BR)

App Android nativo da **Gabi**, adaptado de `agmanly597/javis` (MIT). Voz feminina, fala e entende português brasileiro.

## ✨ O que foi adaptado (resumo)

| Item | Antes | Agora |
|------|-------|-------|
| Nome do app | JAVIS | **Gabi** |
| Idioma da UI | Inglês | **Português (BR)** — todas as telas |
| Personalidade da IA | "JAVIS / J.A.R.V.I.S / Sir" | **"Você é a Gabi"** — calorosa, amigável, responde sempre em pt-BR |
| Voz (TTS sistema) | Inglês (US), tom masculino | **pt-BR**, tom feminino (pitch 1.15) + voz feminina se disponível |
| Voz (ElevenLabs) | "Adam" (masculino, EN) | **Roberta** (feminina, nativa pt-BR) + outras |
| Reconhecimento de fala | Locale do aparelho | **pt-BR forçado** |
| Comandos por voz | Só inglês ("open", "call") | **Bilíngue** ("abrir WhatsApp", "ligar para", "buscar", "mandar mensagem", "alarme"...) |

## 🎙️ Vozes femininas pt-BR (ElevenLabs) — troque se quiser

Já configuradas em `ElevenLabsTts.kt`:
- **Roberta** `RGymW84CSmfVugnA5tvA` ← padrão (conversacional, jovem, amigável)
- **Raquel** `GDzHdQOi6jjf8zaXhCYD` (forte, jovem, conversacional)
- **Keren** `33B4UnXyTNbgLmdEDh5P` (doce, vibrante)
- **Carla** `eVXYtPVYB9wDoz9NVTIy` (jovem, animada)
- **Ana Dias** `MZxV5lN3cv7hi1376O0m` (levemente grave)

> Para usar uma voz da Voice Library via API: acesse `elevenlabs.io/app/voice-library`, encontre a voz, clique em **"Add to VoiceLab"** (grátis) — só então o Voice ID funciona com sua chave. Também dá pra colar outro ID direto em **Ajustes → Voz → ID da voz** dentro do app.

## 🔑 Onde colocar as chaves (no app — nunca no chat nem no repo)

> ⚠️ Importante: o app lê as chaves **só da tela "Ajustes" dentro dele**. O `local.properties`/BuildConfig
> existe no Gradle, mas **não é usado em execução**. Então o jeito certo é colar a chave no app.

Depois de instalar o APK:
1. Abra a Gabi → toque no ícone de ⚙️ (Ajustes)
2. Em **Provedor de IA**, cole sua **Chave da API Groq** → toque em **Salvar ajustes**
3. (Opcional, voz premium) em **Voz**, ative **Usar voz IA da ElevenLabs**, cole a **Chave da API ElevenLabs** → Salvar

Sem a chave da Groq, ela não "pensa" (IA). Sem a ElevenLabs, ela usa o **TTS do próprio Android** — também em **pt-BR feminino** (já configurado), só um pouco mais robótico.

### Onde pegar as chaves grátis
- **Groq** (a "mente" — obrigatório): https://console.groq.com → API Keys → gerar (grátis, generoso)
- **ElevenLabs** (voz premium — opcional): https://elevenlabs.io → perfil → API Keys (plano grátis)

## 📦 Como gerar o APK

**Opção A — Android Studio (recomendado):**
1. Abra a pasta `android/` no Android Studio
2. Aguarde o Gradle sincronizar
3. **Run** num celular/emulador, ou **Build → Build APK**

**Opção B — GitHub Actions:** dê push num commit e o workflow `.github/workflows/build-apk.yml` builda o APK automaticamente (como no repo original).

## ⚠️ Observações
- **Eu não consigo compilar Android aqui** (sem SDK neste ambiente). As edições foram feitas no código; o APK sai no Android Studio/CI.
- Existe um diretório `src/main/java/` duplicado e **não compilado** (o Gradle usa só `src/main/kotlin/`). É código morto herdado — posso remover pra limpar se você quiser.
- O reconhecimento de voz e a maior parte das integrações precisam de **permissões** (mic, contatos, acessibilidade, leitor de notificações) — o app pede na primeira tela.
