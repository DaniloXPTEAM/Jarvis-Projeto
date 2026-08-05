'use client';
import { useState, useRef, useEffect, useCallback } from 'react';

interface Msg { id: string; role: string; content: string; skillRouted?: string | null; createdAt: string }

const SKILL_CHIP: Record<string, { label: string; cls: string }> = {
  reminder: { label: '📅 Lembrete',   cls: 'chip-green'  },
  food:     { label: '🍽️ Comida',     cls: 'chip-amber'  },
  idea:     { label: '💡 Ideia',      cls: 'chip-purple' },
  news:     { label: '📰 Notícias',   cls: 'chip-blue'   },
  alexa:    { label: '🔊 Alexa',      cls: 'chip-blue'   },
  image:    { label: '🎨 Imagem',     cls: 'chip-purple' },
  whatsapp: { label: '💬 WhatsApp',   cls: 'chip-green'  },
  smarthome:{ label: '🏠 Casa',       cls: 'chip-amber'  },
  claude:   { label: '🤖 Claude AI',  cls: 'chip-purple' },
};

const SUGGESTIONS = [
  'Me lembra de tomar remédio às 20h',
  'O que eu como de jantar hoje?',
  'Quanto custa montar um PC para IA?',
  'Gera uma imagem de um pôr do sol',
  'Manda "oi" para o grupo da família no WhatsApp',
  'Liga a luz da sala',
  'Quais as notícias de tecnologia hoje?',
  'Como aumentar a VRAM para geração de imagens?',
];

export default function ChatScreen({ onListeningChange }: { onListeningChange: (v: boolean) => void }) {
  const [msgs,    setMsgs]    = useState<Msg[]>([]);
  const [input,   setInput]   = useState('');
  const [sending, setSending] = useState(false);
  const [listen,  setListen]  = useState(false);
  const [tts,     setTts]     = useState(true);
  const endRef  = useRef<HTMLDivElement>(null);
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const recRef  = useRef<any>(null);
  const pendRef = useRef('');

  useEffect(() => {
    fetch('/api/chat').then(r => r.json()).then((d: Msg[]) => {
      if (!Array.isArray(d) || d.length === 0) boot();
      else setMsgs(d);
    }).catch(boot);
  }, []);// eslint-disable-line react-hooks/exhaustive-deps

  const boot = () => setMsgs([{
    id: '0', role: 'assistant', skillRouted: null, createdAt: new Date().toISOString(),
    content: 'Olá! Sou o NEXUS, sua IA pessoal. 🌐\n\nPosso:\n• Responder qualquer pergunta\n• Criar lembretes e organizar seu dia\n• Controlar sua casa inteligente\n• Gerar imagens com IA\n• Enviar mensagens no WhatsApp\n• Dar sugestões de comida\n• Conectar com sua Alexa\n\nO que você quer fazer?',
  }]);

  useEffect(() => { endRef.current?.scrollIntoView({ behavior: 'smooth' }); }, [msgs, sending]);

  const speak = useCallback((t: string) => {
    if (!tts) return;
    const u = new SpeechSynthesisUtterance(t.replace(/[*#`[\]>_]/g, '').slice(0, 400));
    u.lang = 'pt-BR'; u.rate = 1.06;
    window.speechSynthesis.cancel(); window.speechSynthesis.speak(u);
  }, [tts]);

  const send = useCallback(async (txt?: string) => {
    const content = (txt ?? input).trim();
    if (!content || sending) return;
    setInput(''); setSending(true);
    const uMsg: Msg = { id: Date.now().toString(), role: 'user', content, createdAt: new Date().toISOString() };
    setMsgs(p => [...p, uMsg]);
    try {
      const hist = msgs.slice(-8).map(m => ({ role: m.role, content: m.content }));
      const r = await fetch('/api/chat', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ content, history: hist }) });
      const d = await r.json();
      const aMsg: Msg = d.message;
      setMsgs(p => [...p, aMsg]);
      speak(aMsg.content);
    } catch {
      setMsgs(p => [...p, { id: Date.now().toString(), role: 'assistant', content: 'Erro de conexão. Tente novamente.', createdAt: new Date().toISOString() }]);
    } finally { setSending(false); }
  }, [input, sending, msgs, speak]);

  const startListen = useCallback(() => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const SR = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    if (!SR) { alert('Use Chrome para voz.'); return; }
    const rec = new SR(); rec.lang = 'pt-BR'; rec.interimResults = true; rec.continuous = false;
    recRef.current = rec; pendRef.current = '';
    rec.onstart  = () => { setListen(true); onListeningChange(true); };
    rec.onend    = () => { setListen(false); onListeningChange(false); if (pendRef.current) { send(pendRef.current); pendRef.current = ''; } };
    rec.onerror  = () => { setListen(false); onListeningChange(false); };
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    rec.onresult = (e: any) => {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const t = Array.from(e.results as any[]).map((r: any) => r[0].transcript).join('');
      setInput(t);
      if (e.results[e.results.length - 1].isFinal) { pendRef.current = t; setInput(''); rec.stop(); }
    };
    rec.start();
  }, [send, onListeningChange]);

  const stopListen = useCallback(() => { recRef.current?.stop(); }, []);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: 'calc(100dvh - 64px)' }}>
      {/* Messages */}
      <div className="noscroll" style={{ flex: 1, overflowY: 'auto', padding: '16px 16px 8px', display: 'flex', flexDirection: 'column', gap: 14 }}>
        {msgs.map(m => (
          <div key={m.id} className="fade-in" style={{ display: 'flex', flexDirection: 'column', alignItems: m.role === 'user' ? 'flex-end' : 'flex-start', gap: 4 }}>
            {m.role !== 'user' && m.skillRouted && SKILL_CHIP[m.skillRouted] && (
              <span className={`chip ${SKILL_CHIP[m.skillRouted].cls}`}>{SKILL_CHIP[m.skillRouted].label}</span>
            )}
            <div style={{
              maxWidth: '88%', padding: '12px 15px', borderRadius: m.role === 'user' ? '18px 18px 4px 18px' : '4px 18px 18px 18px',
              background: m.role === 'user' ? 'linear-gradient(135deg, rgba(0,255,136,.18), rgba(56,189,248,.18))' : 'rgba(255,255,255,.05)',
              border: m.role === 'user' ? '1px solid rgba(0,255,136,.25)' : '1px solid rgba(255,255,255,.07)',
              fontSize: 14, lineHeight: 1.65, whiteSpace: 'pre-wrap', wordBreak: 'break-word',
            }}>
              {m.content}
            </div>
            <span style={{ fontSize: 10, color: 'var(--dim)', padding: '0 4px' }}>
              {new Date(m.createdAt).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })}
            </span>
          </div>
        ))}

        {/* Suggestions (first load) */}
        {msgs.length <= 1 && !sending && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 6, marginTop: 8 }}>
            <div className="sec-hd">Experimente perguntar:</div>
            {SUGGESTIONS.map((s, i) => (
              <button key={i} onClick={() => send(s)} style={{
                background: 'rgba(255,255,255,.03)', border: '1px solid rgba(255,255,255,.07)', borderRadius: 12,
                padding: '10px 14px', cursor: 'pointer', fontSize: 13, color: 'rgba(255,255,255,.65)',
                textAlign: 'left', fontFamily: 'inherit', transition: 'all .15s',
              }}
                onMouseEnter={e => { e.currentTarget.style.borderColor = 'rgba(0,255,136,.25)'; e.currentTarget.style.color = 'white'; }}
                onMouseLeave={e => { e.currentTarget.style.borderColor = 'rgba(255,255,255,.07)'; e.currentTarget.style.color = 'rgba(255,255,255,.65)'; }}
              >{s}</button>
            ))}
          </div>
        )}

        {sending && (
          <div style={{ display: 'flex', justifyContent: 'flex-start' }}>
            <div style={{ padding: '12px 16px', borderRadius: '4px 18px 18px 18px', background: 'rgba(255,255,255,.05)', border: '1px solid rgba(255,255,255,.07)' }}>
              <div style={{ display: 'flex', gap: 5 }}><span className="tdot" /><span className="tdot" /><span className="tdot" /></div>
            </div>
          </div>
        )}
        <div ref={endRef} />
      </div>

      {/* Input bar */}
      <div style={{ padding: '10px 14px max(env(safe-area-inset-bottom),14px)', borderTop: '1px solid rgba(255,255,255,.06)', background: 'rgba(6,8,16,.95)', backdropFilter: 'blur(20px)' }}>
        {/* Mic */}
        <div style={{ display: 'flex', justifyContent: 'center', marginBottom: 10 }}>
          <div style={{ position: 'relative', width: 58, height: 58 }}>
            {listen && (
              <>
                <div className="ring1" style={{ position: 'absolute', inset: 0, borderRadius: '50%', border: '1.5px solid rgba(0,255,136,.5)' }} />
                <div className="ring2" style={{ position: 'absolute', inset: 0, borderRadius: '50%', border: '1px solid rgba(0,255,136,.3)' }} />
              </>
            )}
            <button
              onPointerDown={startListen} onPointerUp={stopListen}
              style={{
                position: 'absolute', inset: 0, borderRadius: '50%', cursor: 'pointer', fontFamily: 'inherit',
                background: listen ? 'linear-gradient(135deg, var(--green), #00C96B)' : 'rgba(255,255,255,.07)',
                border: `2px solid ${listen ? 'var(--green)' : 'rgba(255,255,255,.12)'}`,
                color: listen ? '#060810' : 'var(--dim)', fontSize: 24,
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                boxShadow: listen ? '0 0 30px rgba(0,255,136,.5)' : 'none', transition: 'all .2s',
              }}
            >
              {listen
                ? <div style={{ display: 'flex', gap: 2, alignItems: 'center', height: 20 }}>
                    {[16,24,18,28,14,22,16,24].map((h, i) => <div key={i} className="abar" style={{ height: h, background: '#060810' }} />)}
                  </div>
                : '🎙️'}
            </button>
          </div>
        </div>

        <div style={{ display: 'flex', gap: 8 }}>
          <button onClick={() => setTts(v => !v)} className="btn btn-icon" style={{ fontSize: 18, flexShrink: 0, color: tts ? 'var(--green)' : 'var(--dim)' }}>
            {tts ? '🔊' : '🔇'}
          </button>
          <input className="inp" value={input} onChange={e => setInput(e.target.value)}
            onKeyDown={e => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); send(); } }}
            placeholder="Pergunte ou peça qualquer coisa..." style={{ flex: 1 }} />
          <button onClick={() => send()} className="btn btn-icon" disabled={!input.trim() || sending}
            style={{ flexShrink: 0, color: input.trim() ? 'var(--green)' : 'var(--dim)', opacity: !input.trim() || sending ? .4 : 1, fontSize: 20 }}>
            ➤
          </button>
        </div>
        <div style={{ textAlign: 'center', fontSize: 10, color: 'var(--dim)', marginTop: 6, fontWeight: 600, letterSpacing: '.06em' }}>
          SEGURE 🎙️ PARA FALAR · ENTER PARA ENVIAR
        </div>
      </div>
    </div>
  );
}
