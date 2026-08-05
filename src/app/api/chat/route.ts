import { NextRequest, NextResponse } from 'next/server';
import { db } from '@/db';
import { chatMessages, memories, skillLogs } from '@/db/schema';
import { desc } from 'drizzle-orm';
import { randomUUID } from 'crypto';

type Hist = { role: string; content: string };

/* ═══════════════════════════════════════════════════════════════════
   SKILL ROUTER — detecta intenção e roteia para a skill certa
═══════════════════════════════════════════════════════════════════ */
interface Routed {
  skill: string;
  reply: string;
  saveVault?: { type: string; title: string; content: string; tags: string };
  alexaSpeak?: string;
}

function normalize(t: string) {
  return t.toLowerCase().normalize('NFD').replace(/\p{Diacritic}/gu, '');
}

function routeSkill(text: string): Routed | null {
  const t = normalize(text);

  /* ── SMART HOME ── */
  if (/\b(liga|desliga|acende|apaga|ligar|desligar|abre|fecha)\b.*(luz|lampada|ventilador|ar.?condicionado|tv|tomada|persiana|portao|geladeira|chuveiro)/.test(t) ||
      /\b(luz|ventilador|ar.?condicionado)\b.*(liga|desliga)/.test(t)) {
    const action = /liga|acende|abre/.test(t) ? 'ligar' : 'desligar';
    const deviceMatch = text.match(/(luz|lâmpada|ventilador|ar|tv|tomada|portão|geladeira)/i);
    const device = deviceMatch ? deviceMatch[0] : 'dispositivo';
    return {
      skill: 'smarthome',
      reply: `✅ Comando enviado: **${action.toUpperCase()} ${device}**\n\nPara integração real com sua casa, configure:\n• Home Assistant (homeassistant.local)\n• Ou Alexa Skill com os seus dispositivos\n\nNo app, acesse **Casa** para controlar manualmente.`,
    };
  }

  /* ── WHATSAPP ── */
  if (/whatsapp|whats.?app|zap|zap.?zap|manda.*(mensagem|msg|oi|texto)|envia.*(mensagem|msg)|wpp/.test(t)) {
    const msgMatch = text.match(/(?:manda|envia|fala|escreve)[^:]*?[:\s]+["']?(.+?)["']?$/i);
    const msg = msgMatch ? msgMatch[1] : null;
    const groupMatch = text.match(/(?:para|pro|pra)\s+(?:o\s+grupo\s+)?(.+?)(?:\s+no\s+whatsapp|$)/i);
    const dest = groupMatch ? groupMatch[1] : 'contato';
    return {
      skill: 'whatsapp',
      reply: `💬 **WhatsApp**\n\nPara: ${dest}\n${msg ? `Mensagem: "${msg}"` : ''}\n\n⚠️ Para envio real, configure a **WhatsApp Business API** ou use o **Evolution API** (self-hosted).\n\nVariáveis necessárias:\n• WA_API_URL\n• WA_API_TOKEN\n• WA_PHONE_NUMBER\n\nEnquanto isso, posso redigir a mensagem para você copiar!`,
    };
  }

  /* ── GERAR IMAGEM ── */
  if (/gera|cria|faz|desenha|produz|quero ver|me mostra.*(imagem|foto|figura|ilustracao|pintura|wallpaper|arte)/.test(t) ||
      /(imagem|foto|ilustracao|arte).*(de|do|da|com|um|uma)/.test(t)) {
    return {
      skill: 'image',
      reply: `🎨 **Geração de Imagem IA**\n\nSua solicitação: "${text}"\n\nPara gerar imagens, configure:\n• **OPENAI_API_KEY** → DALL-E 3\n• **STABILITY_API_KEY** → Stable Diffusion\n• **REPLICATE_API_TOKEN** → Modelos variados\n\nSem a chave, posso descrever a imagem em detalhes para você usar em outro gerador!`,
      saveVault: { type: 'idea', title: `Imagem: ${text.slice(0, 60)}`, content: text, tags: 'imagem,ia' },
    };
  }

  /* ── ALEXA ── */
  if (/alexa|echo|caixa de som|fala na|anuncia/.test(t)) {
    const msgMatch = text.match(/(?:fala na alexa|alexa fala|anuncia|diz)[:\s]+(.+)/i);
    const msg = msgMatch ? msgMatch[1] : text;
    return {
      skill: 'alexa',
      reply: `🔊 **Alexa**\n\nEnviando: "${msg}"\n\nPara integração real configure ALEXA_COOKIE. A Alexa vai falar isso em voz alta nos seus dispositivos Echo.`,
      alexaSpeak: msg,
    };
  }

  /* ── LEMBRETE ── */
  if (/\b(lembr|remind|nao esquec|agenda|marca|notifica)\b/.test(t)) {
    const timeMatch = text.match(/(\d{1,2})[h:](\d{0,2})?/);
    const timeStr = timeMatch ? `${timeMatch[1]}:${(timeMatch[2] || '00').padStart(2, '0')}` : null;
    const dateMatch = text.match(/(hoje|amanha|segunda|terca|quarta|quinta|sexta|sabado|domingo)/i);
    return {
      skill: 'reminder',
      reply: `✅ **Lembrete criado!**\n\n📅 ${text.replace(/me lembra|lembrar de|me lembre de/gi, '').trim()}\n${timeStr ? `⏰ ${timeStr}` : ''}${dateMatch ? ` · ${dateMatch[0]}` : ''}\n\nSalvo no seu Vault. Posso falar pela Alexa no horário!`,
      saveVault: { type: 'reminder', title: text.slice(0, 80), content: text, tags: 'voz,lembrete' },
    };
  }

  /* ── COMIDA / RECEITA ── */
  if (/\b(com[ae]r|comida|fome|almoç|jant|lanch|receita|ingrediente|culinaria|prato|refeicao|cardapio|calorias|dieta|emagrecer)\b/.test(t)) {
    const h = new Date().getHours();
    const opts =
      h < 10 ? ['🥞 Panquecas de banana com mel e canela', '🥚 Omelete de queijo com tomate', '🍌 Vitamina de banana com aveia'] :
      h < 15 ? ['🥗 Bowl de frango grelhado com arroz integral', '🥙 Wrap de atum com legumes', '🍝 Macarrão integral ao alho e óleo'] :
      h < 18 ? ['🍎 Maçã com pasta de amendoim', '🥜 Mix de castanhas e frutas secas', '🧀 Torrada integral com queijo'] :
               ['🍕 Pizza integral caseira', '🍜 Sopa de legumes com carne', '🥩 Frango com batata-doce assada'];
    return {
      skill: 'food',
      reply: `🍽️ **Sugestões para agora** (${new Date().toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })})\n\n${opts.map((o, i) => `${i+1}. ${o}`).join('\n')}\n\nQuer a receita completa de alguma? Ou prefere pedir delivery? Posso buscar restaurantes próximos!`,
    };
  }

  /* ── NOTÍCIAS ── */
  if (/\b(noticia|noticias|manchete|news|aconteceu|atualidade|hoje no mundo|mundo hoje)\b/.test(t)) {
    return {
      skill: 'news',
      reply: `📰 **Notícias do dia**\n\nPara notícias em tempo real, configure **NEWS_API_KEY** (newsapi.org — gratuito).\n\nPosso pesquisar sobre qualquer tema específico! O que você quer saber?\n\n• Tecnologia • Economia • Esportes • Entretenimento • Ciência`,
    };
  }

  /* ── HARDWARE / PC / GPU ── */
  if (/\b(gpu|placa de video|vram|memoria|computador|pc|notebook|processador|cpu|ram|ssd|montar|custaria|budget|orcamento)\b/.test(t)) {
    return {
      skill: 'claude',
      reply: null as unknown as string, // forward to Claude
    };
  }

  /* ── IDEIA ── */
  if (/\b(ideia|pensei|e se|quero criar|quero fazer|projeto|startup|negocio|inventar)\b/.test(t)) {
    return {
      skill: 'idea',
      reply: `💡 **Ideia capturada!**\n\nAdicionei ao seu Vault de ideias.\n\nQuer que eu:\n• **Analise a viabilidade** do projeto?\n• **Crie um plano de ação** passo a passo?\n• **Estime os custos** de desenvolvimento?\n• **Liste os concorrentes** existentes?`,
      saveVault: { type: 'idea', title: text.slice(0, 80), content: text, tags: 'voz,ideia,projeto' },
    };
  }

  return null; // → Claude / resposta geral
}

/* ═══════════════════════════════════════════════════════════════════
   CLAUDE
═══════════════════════════════════════════════════════════════════ */
async function askClaude(history: Hist[], userMsg: string): Promise<string> {
  const key = process.env.ANTHROPIC_API_KEY;
  if (!key) {
    // Smart fallback sem API
    const t = normalize(userMsg);
    if (/custo|preco|quanto|custaria|budget|orcamento/.test(t) && /gpu|placa|vram/.test(t)) {
      return `💻 **Ampliar VRAM / GPU para geração de imagens**\n\nOpções por orçamento:\n\n**Econômico (~R$800-1.500):**\n• GTX 1060 6GB (usado) — básico para SD 1.5\n• RX 6600 8GB — boa relação custo/benefício\n\n**Intermediário (~R$2.000-4.000):**\n• RTX 3060 12GB — excelente para SDXL\n• RTX 4060 8GB — DLSS 3, eficiente\n\n**Avançado (~R$4.000-8.000+):**\n• RTX 3090 24GB — referência para IA\n• RTX 4070 Ti 12GB — moderno e rápido\n\n**Computador dedicado (mini PC):**\n• Mac Mini M4 Pro — 24-48GB unificada ~R$8.000\n• Aceita modelos grandes localmente\n\nPara SDXL rodar bem: mínimo 8GB VRAM\nPara ComfyUI/Flux: 12GB+ recomendado`;
    }
    return `Entendi sua pergunta! Para respostas completas de IA, configure **ANTHROPIC_API_KEY** nas variáveis de ambiente.\n\nEnquanto isso, posso ajudar com:\n• Lembretes e organização\n• Sugestões de comida\n• Controle da casa inteligente\n• Capturar suas ideias\n• Conectar com a Alexa`;
  }

  const system = `Você é NEXUS, uma IA pessoal avançada integrada a um app mobile Android.
Você está conectado a: casa inteligente, Alexa, geração de imagens, WhatsApp, lembretes, vault de memória.
Responda em português brasileiro, seja direto e útil.
Para perguntas técnicas (hardware, GPU, IA), dê informações precisas com preços aproximados em R$.
Para pedidos criativos, seja detalhado. Máximo 6 parágrafos por resposta.`;

  const res = await fetch('https://api.anthropic.com/v1/messages', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'x-api-key': key, 'anthropic-version': '2023-06-01' },
    body: JSON.stringify({
      model: 'claude-3-haiku-20240307', max_tokens: 800, system,
      messages: [
        ...history.slice(-8).map(m => ({ role: m.role === 'assistant' ? 'assistant' : 'user', content: m.content })),
        { role: 'user', content: userMsg },
      ],
    }),
  });
  if (!res.ok) throw new Error(`Claude ${res.status}`);
  const d = await res.json();
  return d.content[0].text as string;
}

/* ═══════════════════════════════════════════════════════════════════
   HANDLERS
═══════════════════════════════════════════════════════════════════ */
export async function GET() {
  try {
    const rows = await db.select().from(chatMessages).orderBy(desc(chatMessages.createdAt)).limit(80);
    return NextResponse.json(rows.reverse());
  } catch { return NextResponse.json([]); }
}

export async function POST(req: NextRequest) {
  const { content, history } = await req.json() as { content: string; history: Hist[] };

  const userRow = { id: randomUUID(), role: 'user' as const, content, skillRouted: null as string | null, createdAt: new Date() };
  await db.insert(chatMessages).values(userRow);

  const routed = routeSkill(content);
  let reply: string;
  let skill: string;

  if (routed && routed.reply) {
    reply = routed.reply; skill = routed.skill;
    if (routed.saveVault) {
      await db.insert(memories).values({ id: randomUUID(), ...routed.saveVault, done: false, dueAt: null, createdAt: new Date(), updatedAt: new Date() });
    }
    if (routed.alexaSpeak) {
      // Fire-and-forget Alexa
      const base = process.env.NEXT_PUBLIC_BASE_URL || 'http://localhost:3000';
      fetch(`${base}/api/alexa`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ action: 'announce', text: routed.alexaSpeak }) }).catch(() => {});
    }
  } else {
    skill = 'claude';
    try { reply = await askClaude(history, content); }
    catch { reply = 'Erro de conexão com a IA. Tente novamente!'; }
  }

  const aiRow = { id: randomUUID(), role: 'assistant' as const, content: reply, skillRouted: skill, createdAt: new Date() };
  await db.insert(chatMessages).values(aiRow);
  await db.insert(skillLogs).values({ id: randomUUID(), skillName: skill, input: content, output: reply.slice(0, 200), success: true, executedAt: new Date() });

  return NextResponse.json({ message: aiRow, skill });
}
