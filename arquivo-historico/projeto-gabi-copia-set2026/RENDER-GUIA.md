# 🚀 Como configurar o Render (servidor da Gabi)

O Render hospeda o **servidor Next.js** (o "cérebro web") que dá pra Gabi os poderes que o celular não tem: **Claude, Alexa, notícias, casa inteligente**. As chaves ficam **todas no painel do Render** — nada no celular.

> ⚠️ Não cole chaves/cookies no chat nem no GitHub. Cole **só no painel do Render** (Environment).

---

## ✅ Pré-requisitos
- Conta no **GitHub** (você já tem — o repo é `DaniloXPTEAM/Jarvis-Projeto`)
- Conta no **Render** grátis → https://render.com (faça login com o GitHub)
- (Opcional) Sua **Chave da API Anthropic**, **cookie da Alexa**, **chave NewsAPI** — conforme for usar

---

## 🧱 Passo a passo (modo Blueprint — mais fácil)

Já existe um `render.yaml` no repo que cria tudo automaticamente.

1. No Render: **New +** → **Blueprint** → selecione o repositório `DaniloXPTEAM/Jarvis-Projeto`.
2. Ele detecta o `render.yaml` e mostra 2 recursos pra criar: **`gabi-server`** (Web Service) e **`gabi-db`** (PostgreSQL). Clique em **Apply**.
3. O Render instala, faz o build (`npm install && npm run build`) e sobe o serviço. Pode levar uns 3–5 min na 1ª vez.
4. Quando ficar **Live**, copie a **URL pública** (ex.: `https://gabi-server.onrender.com`).

> Sem Blueprint? Faça manual: **New + → PostgreSQL** (crie `gabi-db`); depois **New + → Web Service** → conecte o repo → runtime Node → build `npm install && npm run build` → start `npx next start -H 0.0.0.0 -p $PORT` → plano Free/Starter.

---

## 🔑 Variáveis de ambiente (painel → Environment)

Abra `gabi-server` → **Environment** e preencha (as marcadas `sync: false`):

| Variável | Obrigatória? | Onde pegar |
|----------|:--:|------|
| `DATABASE_URL` | ✅ sim | **automático** (vem do `gabi-db`) |
| `NEXT_PUBLIC_BASE_URL` | ✅ sim | a URL pública do Render (ex.: `https://gabi-server.onrender.com`) |
| `ANTHROPIC_API_KEY` | 🟡 opcional | console.anthropic.com → API Keys (Claude) |
| `ALEXA_COOKIE` | 🟡 opcional | cookie de `alexa.amazon.com.br` (ver abaixo) |
| `ALEXA_CSRF` | 🟡 opcional | vem junto do cookie (procure `csrf=` dentro dele) |
| `NEWS_API_KEY` | 🟡 opcional | newsapi.org (notícias) |

Salve → o Render faz **redeploy** automático.

---

## 🗄️ Criar as tabelas do banco (1 vez, após o 1º deploy)

O servidor só sobe com `DATABASE_URL` (✅ já vem do banco). Mas as **tabelas** precisam ser criadas:

1. No `gabi-server` → **Shell** (botão na barra lateral).
2. Rode:
   ```bash
   npx drizzle-kit push
   ```
3. Confirme. Pronto — tabelas criadas no PostgreSQL do Render.

---

## 🔌 Conectar o app da Gabi ao Render

No **app da Gabi** (celular) → ⚙️ Ajustes → campo **"URL do Render"**:
```
https://gabi-server.onrender.com
```
A partir daí, quando você disser *"fale com o Claude…"*, *"manda a Alexa…"*, etc., a Gabi chama o Render (que tem as chaves) — sem nada no celular.

> 📌 Esse campo será adicionado na próxima rodada (ponte Gabi ↔ Render). Por enquanto o servidor já funciona isolado.

---

## 🍪 Cookie da Alexa (pra "manda a Alexa desligar a luz")

No **navegador** (aba anônima):
1. Acesse `https://alexa.amazon.com.br/spa/index.html` e faça login.
2. **F12** → aba **Network/Rede** → recarregue (F5) → clique em `index.html`.
3. **Headers → Request Headers** → copie o valor inteiro da linha **`Cookie:`** → cole em `ALEXA_COOKIE`.
4. Dentro desse texto, procure `csrf=` → o número depois é o `ALEXA_CSRF`.

⚠️ Expira em dias/semanas. Quando a Gabi parar de falar com a Alexa, **repetir**.
Alternativa mais durável: `npx alexa-cookie-cli -p amazon.com.br -a pt-BR -L pt-BR` (gera um refresh token).

---

## 💸 Plano Free × Starter (importante pra Alexa)

| Plano | Sempre ligado? | Custo | Pra Alexa? |
|-------|:--:|------|------|
| **Free** | ❌ desliga após ~15 min parado (1º request demora ~30s) | R$ 0 | 🟡 funciona, mas com atraso no 1º uso |
| **Starter** | ✅ sempre ligado | ~US$ 7/mês | ✅ ideal (resposta na hora) |

A Alexa é chata com tempo de resposta. Pra *"Gabi, manda a Alexa…"* responder rápido, o **Starter (always-on)** é recomendado.

---

## 🎯 Depois que o Render estiver no ar

Próximas implementações (faço eu):
1. **Ponte Gabi ↔ Render** no app (campo "URL do Render" + chat/Claude/Alexa pelo servidor).
2. **`/api/alexa` controlando dispositivos** (desligar luz, etc.).
3. No app nativo: **Cast pra TV** (precisa do SDK do Google Cast — valido no Android Studio).
