'use client';

interface Device { id: string; name: string; icon: string; on: boolean; type: string; room: string }
interface Props { devices: Device[]; onToggle: (id: string) => void; onNav: (t: string) => void }

const ROOM_ORDER = ['Sala', 'Quarto', 'Cozinha', 'Banheiro', 'Escritório'];

export default function SmartHomeScreen({ devices, onToggle, onNav }: Props) {
  const rooms = ROOM_ORDER.filter(r => devices.some(d => d.room === r));
  const activeCount = devices.filter(d => d.on).length;

  return (
    <div style={{ padding: '16px 16px 0' }}>
      <div className="safe-top" style={{ paddingBottom: 0 }}>
        <div style={{ marginBottom: 20 }}>
          <div style={{ fontSize: 11, fontWeight: 700, letterSpacing: '.12em', textTransform: 'uppercase', color: 'var(--dim)', marginBottom: 4 }}>Casa Inteligente</div>
          <div style={{ fontSize: 28, fontWeight: 800, letterSpacing: '-0.02em' }}>Minha Casa</div>
          <div style={{ fontSize: 13, color: 'var(--dim)', marginTop: 4 }}>
            {activeCount} de {devices.length} dispositivos ligados
          </div>
        </div>

        {/* All on/off */}
        <div className="glass-green" style={{ padding: '14px 16px', marginBottom: 20, display: 'flex', alignItems: 'center', gap: 14 }}>
          <div style={{ fontSize: 32 }}>🏠</div>
          <div style={{ flex: 1 }}>
            <div style={{ fontWeight: 700, fontSize: 15 }}>Controle geral</div>
            <div style={{ fontSize: 12, color: 'var(--dim)', marginTop: 2 }}>Liga ou desliga tudo</div>
          </div>
          <div style={{ display: 'flex', gap: 8 }}>
            <button className="btn btn-ghost" style={{ padding: '8px 14px', fontSize: 12 }}
              onClick={() => devices.filter(d => !d.on).forEach(d => onToggle(d.id))}>
              Ligar tudo
            </button>
            <button className="btn btn-danger" style={{ padding: '8px 14px', fontSize: 12 }}
              onClick={() => devices.filter(d => d.on).forEach(d => onToggle(d.id))}>
              Desligar
            </button>
          </div>
        </div>

        {/* Voice command hint */}
        <div className="glass" style={{ padding: '12px 14px', marginBottom: 20, display: 'flex', gap: 12, alignItems: 'center' }}>
          <span style={{ fontSize: 24 }}>🎙️</span>
          <div>
            <div style={{ fontSize: 13, fontWeight: 600 }}>Controle por voz</div>
            <div style={{ fontSize: 12, color: 'var(--dim)', marginTop: 2 }}>"Liga a luz da sala" · "Desliga o ventilador"</div>
          </div>
          <button className="btn btn-primary" style={{ padding: '8px 14px', fontSize: 12, flexShrink: 0 }} onClick={() => onNav('chat')}>
            Falar
          </button>
        </div>

        {/* Rooms */}
        {rooms.map(room => (
          <div key={room} style={{ marginBottom: 20 }}>
            <div className="sec-hd">{room}</div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              {devices.filter(d => d.room === room).map(d => (
                <div key={d.id} className="card" style={{
                  display: 'flex', alignItems: 'center', gap: 12, padding: '14px 16px',
                  border: d.on ? '1px solid rgba(0,255,136,.25)' : '1px solid rgba(255,255,255,.07)',
                  background: d.on ? 'rgba(0,255,136,.04)' : undefined,
                }}>
                  <div style={{
                    width: 48, height: 48, borderRadius: 14, fontSize: 24,
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    background: d.on ? 'rgba(0,255,136,.12)' : 'rgba(255,255,255,.05)',
                    border: d.on ? '1px solid rgba(0,255,136,.25)' : '1px solid rgba(255,255,255,.08)',
                    transition: 'all .2s',
                  }}>
                    {d.icon}
                  </div>
                  <div style={{ flex: 1 }}>
                    <div style={{ fontWeight: 700, fontSize: 15 }}>{d.name}</div>
                    <div style={{ fontSize: 12, color: d.on ? 'var(--green)' : 'var(--dim)', marginTop: 2, display: 'flex', alignItems: 'center', gap: 5 }}>
                      <span className={`dot ${d.on ? 'dot-green' : 'dot-off'}`} style={{ width: 6, height: 6 }} />
                      {d.on ? 'Ligado' : 'Desligado'}
                    </div>
                  </div>
                  <button className={`toggle ${d.on ? 'on' : ''}`} onClick={() => onToggle(d.id)} />
                </div>
              ))}
            </div>
          </div>
        ))}

        {/* Integration note */}
        <div className="glass" style={{ padding: '14px 16px', marginBottom: 20 }}>
          <div style={{ fontSize: 11, fontWeight: 700, color: 'var(--amber)', letterSpacing: '.1em', textTransform: 'uppercase', marginBottom: 8 }}>
            ⚡ Integrações disponíveis
          </div>
          {[
            { icon: '🔵', label: 'Alexa / Echo', desc: 'Controle por voz via Echo' },
            { icon: '🏠', label: 'Google Home', desc: 'Dispositivos Google Nest' },
            { icon: '💡', label: 'Philips Hue', desc: 'Lâmpadas inteligentes' },
            { icon: '🌡️', label: 'Termostato', desc: 'Temperatura automática' },
          ].map(i => (
            <div key={i.label} style={{ display: 'flex', gap: 10, alignItems: 'center', marginBottom: 10 }}>
              <span style={{ fontSize: 20 }}>{i.icon}</span>
              <div>
                <div style={{ fontSize: 13, fontWeight: 600 }}>{i.label}</div>
                <div style={{ fontSize: 11, color: 'var(--dim)' }}>{i.desc}</div>
              </div>
            </div>
          ))}
          <div style={{ fontSize: 12, color: 'var(--dim)', marginTop: 4 }}>
            Configure nas variáveis de ambiente: HOME_ASSISTANT_URL e HOME_ASSISTANT_TOKEN
          </div>
        </div>
      </div>
    </div>
  );
}
