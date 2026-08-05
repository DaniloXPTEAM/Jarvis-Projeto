import { NextRequest, NextResponse } from 'next/server';

// Claude API integration
// Note: In production, use environment variable for API key
const CLAUDE_API_URL = 'https://api.anthropic.com/v1/messages';

export async function POST(request: NextRequest) {
  try {
    const { messages, systemPrompt } = await request.json();
    
    // Check if API key is available
    const apiKey = process.env.CLAUDE_API_KEY;
    
    if (!apiKey) {
      // Return mock response for demo purposes
      return NextResponse.json({
        content: "Olá! Sou a SecretaryAI. Para ativar a integração com o Claude, configure a variável CLAUDE_API_KEY no ambiente. Enquanto isso, posso ajudar com Lembretes, Atalhos e Sugestões! 📅",
        mock: true,
      });
    }
    
    const response = await fetch(CLAUDE_API_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-api-key': apiKey,
        'anthropic-version': '2023-06-01',
        'anthropic-dangerous-direct-browser-access': 'true',
      },
      body: JSON.stringify({
        model: 'claude-3-sonnet-20240229',
        max_tokens: 1024,
        system: systemPrompt || "Você é SecretaryAI, uma assistente pessoal inteligente. Você ajuda com lembretes, organização, ideias, sugestões de comida, e pode analisar ideias de projetos. Seja útil, amigável e concisa.",
        messages: messages.map((m: { role: string; content: string }) => ({
          role: m.role === 'assistant' ? 'assistant' : 'user',
          content: m.content,
        })),
      }),
    });
    
    if (!response.ok) {
      throw new Error(`Claude API error: ${response.status}`);
    }
    
    const data = await response.json();
    
    return NextResponse.json({
      content: data.content[0].text,
      mock: false,
    });
  } catch (error) {
    console.error('Claude API error:', error);
    
    return NextResponse.json(
      { error: 'Failed to connect to Claude' },
      { status: 500 }
    );
  }
}

export async function GET() {
  return NextResponse.json({
    status: 'ok',
    service: 'SecretaryAI Claude Integration',
    version: '1.0.0',
  });
}
