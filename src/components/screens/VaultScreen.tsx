'use client';
import { useEffect, useState } from 'react';

interface Mem { id: string; type: string; title: string; content: string; tags: string; done: boolean; dueAt: string | null; createdAt: string }

const TYPE_META: Record<string, { icon: string; color: string }> = {
  reminder: { icon: '📅', color: 'var(--green)' },
  idea:     { icon: '💡', color: 'var(--purple)' },
  note:     { icon: '📝', color: 'var(--blue)' },
  log:      { icon: '📋', color: 'var(--dim)' },
};

export default function VaultScreen() {
  const [items, setItems]   = useState<Mem[]>([]);
  const [filter, setFilter] = useState('all');
  const [show, setShow]     = useState(false);
  const [loading, setLoading] = useState(true);
  const [form, setForm]     = useState({ title: '', content: '', type: 'note', tags: '', dueAt: '', dueTime: '' });

  const load = () => {
    setLoading(true);
    fetch(filter === 'all' ? '/api/memories' : `/api/memories?type=${filter}`)
      .then(r => r.json()).then((d: Mem[]) => { setItems(Array.isArray(d) ? d : []); setLoading(false); })
      .catch(() => { setItems([]); setLoading(false); });
  };
  useEffect(load, [filter]); // eslint-disable-line

  const del    = async (id: string) => { await fetch(`/api/memories?id=${id}`, { method: 'DELETE' }); load(); };
  const toggle = async (m: Mem)     => { await fetch('/api/memories', { method: 'PATCH', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ id: m.id, done: !m.done }) }); load(); };
  const save   = async () => {
    if (!form.title) return;
    const dueAt = form.dueAt ? new Date(`${form.dueAt}T${form.dueTime || '09:00'}`).toISOString() : null;
    await fetch('/api/memories', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ ...form, dueAt }) });
    setForm({ title: '', content: '', type: 'note', tags: '', dueAt: '', dueTime: '' });
    setShow(false); load();
  };

  return (
    <div style={{ padding: '16px 16px 0' }}>
      <div className="safe-top" style={{ paddingBottom: 0 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 20 }}>
          <div>
            <div className="sec-hd" style={{ marginBottom: 2 }}>Memória</div>
            <div style={{ fontSize: 28, fontWeight: 800, letterSpacing: '-0.02em' }}>Vault</div>
            <div style={{ fontSize: 12, color: 'var(--dim)', marginTop: 3 }}>{items.length} registros salvos</div>
          </div>
          <button className="btn btn-primary" style={{ padding: '10px 18px', fontSize: 13 }} onClick={() => setShow(v => !v)}>
            {show ? '✕' : '+ Novo'}
          </button>
        </div>

        {show && (
          <div className="glass-green" style={{ padding: '16px', marginBottom: 20, display: 'flex', flexDirection: 'column', gap: 10 }}>
            <div style={{ display: 'flex', gap: 8 }}>
              {['note','reminder','idea'].map(t => (
                <button key={t} onClick={() => setForm(f => ({ ...f, type: t }))}
                  className={`btn ${form.type === t ? 'btn-primary' : 'btn-ghost'}`}
                  style={{ flex: 1, padding: '8px 4px', fontSize: 12, justifyContent: 'center' }}>
                  {TYPE_META[t]?.icon} {t === 'note' ? 'Nota' : t === 'reminder' ? 'Lembrete' : 'Ideia'}
                </button>
              ))}
            </div>
            <input className="inp" placeholder="Título *" value={form.title} onChange={e => setForm(f => ({ ...f, title: e.target.value }))} />
            <textarea className="inp" rows={2} placeholder="Detalhes" value={form.content} onChange={e => setForm(f => ({ ...f, content: e.target.value }))} />
            {form.type === 'reminder' && (
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
                <input className="inp" type="date" value={form.dueAt} onChange={e => setForm(f => ({ ...f, dueAt: e.target.value }))} />
                <input className="inp" type="time" value={form.dueTime} onChange={e => setForm(f => ({ ...f, dueTime: e.target.value }))} />
              </div>
            )}
            <button className="btn btn-primary" onClick={save} disabled={!form.title} style={{ width: '100%', justifyContent: 'center', opacity: form.title ? 1 : .5 }}>
              Salvar no Vault
            </button>
          </div>
        )}

        {/* Filters */}
        <div className="noscroll" style={{ display: 'flex', gap: 8, marginBottom: 16, overflowX: 'auto', paddingBottom: 4 }}>
          {[{ id: 'all', label: 'Todos' }, { id: 'reminder', label: '📅 Lembretes' }, { id: 'idea', label: '💡 Ideias' }, { id: 'note', label: '📝 Notas' }].map(f => (
            <button key={f.id} onClick={() => setFilter(f.id)}
              className={`btn ${filter === f.id ? 'btn-primary' : 'btn-ghost'}`}
              style={{ whiteSpace: 'nowrap', padding: '8px 16px', fontSize: 12, borderRadius: 20, flexShrink: 0 }}>
              {f.label}
            </button>
          ))}
        </div>

        {loading
          ? <div style={{ textAlign: 'center', padding: 40, color: 'var(--dim)' }}>Carregando…</div>
          : items.length === 0
          ? <div style={{ textAlign: 'center', padding: 48 }}>
              <div style={{ fontSize: 48, marginBottom: 12 }}>📭</div>
              <div style={{ color: 'var(--dim)', fontSize: 14 }}>Vault vazio — fale com o NEXUS para criar memórias.</div>
            </div>
          : <div style={{ display: 'flex', flexDirection: 'column', gap: 10, paddingBottom: 20 }}>
              {items.map(m => {
                const meta = TYPE_META[m.type] || TYPE_META.note;
                const due  = m.dueAt ? new Date(m.dueAt) : null;
                return (
                  <div key={m.id} className="card" style={{ opacity: m.done ? .45 : 1, borderLeft: `3px solid ${meta.color}40` }}>
                    <div style={{ display: 'flex', gap: 12, alignItems: 'flex-start' }}>
                      <div style={{ width: 40, height: 40, borderRadius: 12, fontSize: 20, display: 'flex', alignItems: 'center', justifyContent: 'center', background: `${meta.color}12`, border: `1px solid ${meta.color}28`, flexShrink: 0 }}>
                        {meta.icon}
                      </div>
                      <div style={{ flex: 1, minWidth: 0 }}>
                        <div style={{ fontSize: 14, fontWeight: 600, overflow: 'hidden', whiteSpace: 'nowrap', textOverflow: 'ellipsis', textDecoration: m.done ? 'line-through' : 'none', color: m.done ? 'var(--dim)' : undefined }}>
                          {m.title}
                        </div>
                        {due && <div style={{ fontSize: 11, color: 'var(--amber)', marginTop: 3 }}>⏰ {due.toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' })}</div>}
                        {m.content && <div style={{ fontSize: 12, color: 'var(--dim)', marginTop: 4, lineHeight: 1.5, overflow: 'hidden', display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical' }}>{m.content}</div>}
                      </div>
                      <div style={{ display: 'flex', gap: 4 }}>
                        {m.type === 'reminder' && (
                          <button className="btn btn-icon" style={{ width: 32, height: 32, borderRadius: 8, fontSize: 14, color: m.done ? 'var(--dim)' : 'var(--green)' }} onClick={() => toggle(m)}>
                            {m.done ? '↩' : '✓'}
                          </button>
                        )}
                        <button className="btn btn-icon" style={{ width: 32, height: 32, borderRadius: 8, fontSize: 14, color: 'var(--red)' }} onClick={() => del(m.id)}>✕</button>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
        }
      </div>
    </div>
  );
}
