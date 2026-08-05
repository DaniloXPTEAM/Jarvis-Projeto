'use client';
import { useEffect, useState, useCallback, useRef } from 'react';
import Orb from '@/components/Orb';

interface Mem { id: string; type: string; title: string; done: boolean; dueAt: string | null }
interface Props {
  onNav: (t: string) => void;
  alexaConnected: boolean;
  smartDevices: SmartDevice[];
  onToggleDevice: (id: string) => void;
}
interface SmartDevice { id: string; name: string; icon: string; on: boolean; type: string }

export default function HomeScreen({ onNav, alexaConnected, smartDevices, onToggleDevice }: Props) {
  const [listening,  setListening]  = useState(false);
  const [thinking,   setThinking]   = useState(false);
  const [transcript, setTranscript] = useState('');
  const [response,   setResponse]   = useState('');
  const [mems,       setMems]       = useState<Mem[]>([]);
  const [greeting,   setGreeting]   = useState('');
  const [tts,        setTts]        = useState(true);
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const recRef = useRef<any>(null);
  const pendRef = useRef('');

  useEffect(() => {
    const h = new Date().getHours();
    setGreeting(h < 12 ? '☀️ Bom dia' : h < 18 ? '🌤️ Boa tarde' : '🌙 Boa noite');
    fetch('/api/memories').then(r => r.json()).then((d: Mem[]) => setMems(Array.isArray(d) ? d : [])).catch(() => {});
  }, []);

  const speak = useCallback((text: string) => {
    if (!tts || typeof window === 'undefined') return;
    const u = new SpeechSynthesisUtterance(text.replace(/[*#`\[\]>_]/g, '').slice(0, 500));
    u.lang = 'pt-BR'; u.rate = 1.05; u.pitch = 1;
    window.speechSynthesis.cancel();
    window.speechSynthesis.speak(u);
  }, [tts]);

  const sendVoice = useCallback(async (text: string) => {
    if (!text.trim()) return;
    setThinking(true);
    setTranscript(text);
    setResponse('');
    try {
      const r = await fetch('/api/chat', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ content: text, history: [] }),
      });
      const d = await r.json();
      const reply: string = d.message?.content || 'Não entendi. Tente novamente.';
      setResponse(reply);
      speak(reply);
      // refresh mems
      fetch('/api/memories').then(r => r.json()).then((d: Mem[]) => setMems(Array.isArray(d) ? d : [])).catch(() => {});
    } catch { setResponse('Erro de conexão. Tente novamente.'); }
    finally { setThinking(false); }
  }, [speak]);

  const startListen = useCallback(() => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const SR = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    if (!SR) { alert('Use Chrome para reconhecimento de voz.'); return; }
    const rec = new SR(); rec.lang = 'pt-BR'; rec.interimResults = true; rec.continuous = false;
    recRef.current = rec; pendRef.current = '';
    rec.onstart  = () => setListening(true);
    rec.onend    = () => { setListening(false); if (pendRef.current) sendVoice(pendRef.current); };
    rec.onerror  = () => setListening(false);
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    rec.onresult = (e: any) => {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const t = Array.from(e.results as any[]).map((r: any) => r[0].transcript).join('');
      setTranscript(t);
      if (e.results[e.results.length - 1].isFinal) { pendRef.current = t; rec.stop(); }
    };
    rec.start();
  }, [sendVoice]);

  const stopListen = useCallback(() => { recRef.current?.stop(); }, []);

  const pending = mems.filter(m => m.type === 'reminder' && !m.done).length;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', padding: '0 20px 20px' }}>

      {/* Greeting */}
      <div className="safe-top fade-in" style={{ width: '100%', display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', paddingBottom: 8 }}>
        <div>
          <div style={{ fontSize: 13, color: 'var(--dim)', fontWeight: 500 }}>{greeting}</div>
          <div className="shimmer-text" style={{ fontSize: 26, fontWeight: 800, letterSpacing: '-0.03em', lineHeight: 1.2 }}>NEXUS AI</div>
        </div>
        <div style={{ display: 'flex', gap: 8, alignItems: 'center', marginTop: 4 }}>
          <button onClick={() => setTts(v => !v)} className="btn btn-icon" style={{ fontSize: 18 }}>
            {tts ? '🔊' : '🔇'}
          </button>
          {alexaConnected && (
            <div style={{ display: 'flex', alignItems: 'center', gap: 5, padding: '6px 12px', borderRadius: 20, background: 'rgba(56,189,248,.10)', border: '1px solid rgba(56,189,248,.25)' }}>
              <span className="dot dot-blue" style={{ width: 6, height: 6 }} />
              <span style={{ fontSize: 11, fontWeight: 700, color: 'var(--blue)' }}>Alexa</span>
            </div>
          )}
        </div>
      </div>

      {/* ORB */}
      <div style={{ position: 'relative', margin: '8px 0 4px' }}>
        {listening && (
          <>
            <div className="ring1" style={{ position: 'absolute', inset: -20, borderRadius: '50%', border: '1.5px solid rgba(0,255,136,.5)', pointerEvents: 'none' }} />
            <div className="ring2" style={{ position: 'absolute', inset: -20, borderRadius: '50%', border: '1px solid rgba(0,255,136,.3)', pointerEvents: 'none' }} />
            <div className="ring3" style={{ position: 'absolute', inset: -20, borderRadius: '50%', border: '1px solid rgba(56,189,248,.2)', pointerEvents: 'none' }} />
          </>
        )}
        <button
          onPointerDown={startListen} onPointerUp={stopListen}
          style={{ background: 'none', border: 'none', cursor: 'pointer', padding: 0, display: 'block', borderRadius: '50%' }}
        >
          <Orb listening={listening} thinking={thinking} size={200} />
        </button>
      </div>

      {/* Label */}
      <div style={{ fontSize: 12, fontWeight: 600, color: listening ? 'var(--green)' : 'var(--dim)', letterSpacing: '.08em', textTransform: 'uppercase', marginBottom: 8 }}>
        {listening ? '● OUVINDO — SOLTE PARA ENVIAR' : thinking ? '◌ PROCESSANDO...' : 'SEGURE PARA FALAR'}
      </div>

      {/* Audio bars when listening */}
      {listening && (
        <div style={{ display: 'flex', gap: 4, alignItems: 'flex-end', height: 32, marginBottom: 8 }}>
          {[20,32,24,40,28,36,22,34].map((h, i) => (
            <div key={i} className="abar" style={{ height: h, background: 'linear-gradient(to top, var(--green), var(--blue))' }} />
          ))}
        </div>
      )}

      {/* Transcript / Response */}
      {(transcript || response) && (
        <div className="fade-in glass" style={{ width: '100%', padding: '14px 16px', marginBottom: 12, display: 'flex', flexDirection: 'column', gap: 10 }}>
          {transcript && (
            <div style={{ display: 'flex', gap: 8, alignItems: 'flex-start' }}>
              <span style={{ fontSize: 11, fontWeight: 700, color: 'var(--blue)', flexShrink: 0, paddingTop: 2 }}>VOCÊ</span>
              <span style={{ fontSize: 14, color: 'var(--text)', lineHeight: 1.5 }}>{transcript}</span>
            </div>
          )}
          {response && (
            <div style={{ display: 'flex', gap: 8, alignItems: 'flex-start' }}>
              <span style={{ fontSize: 11, fontWeight: 700, color: 'var(--green)', flexShrink: 0, paddingTop: 2 }}>NEXUS</span>
              <span style={{ fontSize: 14, color: 'var(--text)', lineHeight: 1.6, whiteSpace: 'pre-wrap' }}>{response}</span>
            </div>
          )}
        </div>
      )}

      {/* Quick stat row */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3,1fr)', gap: 8, width: '100%', marginBottom: 16 }}>
        {[
          { label: 'Lembretes', val: pending, icon: '📅', col: 'var(--green)', action: () => onNav('vault') },
          { label: 'Dispositivos', val: smartDevices.filter(d => d.on).length, icon: '💡', col: 'var(--amber)', action: () => onNav('home') },
          { label: 'Conexões', val: alexaConnected ? 2 : 1, icon: '🔗', col: 'var(--blue)', action: () => onNav('alexa') },
        ].map(s => (
          <button key={s.label} className="card" onClick={s.action}
            style={{ padding: '12px 10px', textAlign: 'center', border: `1px solid ${s.col}18` }}>
            <div style={{ fontSize: 22 }}>{s.icon}</div>
            <div style={{ fontSize: 24, fontWeight: 800, color: s.col, lineHeight: 1.1 }}>{s.val}</div>
            <div style={{ fontSize: 10, color: 'var(--dim)', fontWeight: 600, marginTop: 2 }}>{s.label}</div>
          </button>
        ))}
      </div>

      {/* Smart devices quick toggle */}
      <div style={{ width: '100%', marginBottom: 16 }}>
        <div className="sec-hd">Casa Inteligente</div>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
          {smartDevices.slice(0, 4).map(d => (
            <div key={d.id} className="card" style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '12px 14px', border: d.on ? '1px solid rgba(0,255,136,.25)' : undefined, background: d.on ? 'rgba(0,255,136,.05)' : undefined }}>
              <span style={{ fontSize: 22 }}>{d.icon}</span>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: 13, fontWeight: 600, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{d.name}</div>
                <div style={{ fontSize: 11, color: d.on ? 'var(--green)' : 'var(--dim)' }}>{d.on ? 'Ligado' : 'Desligado'}</div>
              </div>
              <button className={`toggle ${d.on ? 'on' : ''}`} onClick={() => onToggleDevice(d.id)} />
            </div>
          ))}
        </div>
      </div>

      {/* Quick actions */}
      <div style={{ width: '100%' }}>
        <div className="sec-hd">Ações rápidas</div>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
          {[
            { icon: '🎨', label: 'Gerar Imagem', sub: 'Com IA', action: () => onNav('images') },
            { icon: '💬', label: 'WhatsApp', sub: 'Enviar mensagem', action: () => onNav('chat') },
            { icon: '📰', label: 'Notícias', sub: 'Resumo do dia', action: () => sendVoice('quais as notícias de hoje?') },
            { icon: '🍽️', label: 'O que comer?', sub: 'Sugestão agora', action: () => sendVoice('o que eu posso comer agora?') },
          ].map(a => (
            <button key={a.label} className="card" onClick={a.action} style={{ padding: '14px', textAlign: 'left' }}>
              <div style={{ fontSize: 28, marginBottom: 6 }}>{a.icon}</div>
              <div style={{ fontSize: 13, fontWeight: 700 }}>{a.label}</div>
              <div style={{ fontSize: 11, color: 'var(--dim)', marginTop: 2 }}>{a.sub}</div>
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}
