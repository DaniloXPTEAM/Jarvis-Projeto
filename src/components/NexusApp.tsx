'use client';
import { useState, useCallback } from 'react';
import HomeScreen      from './screens/HomeScreen';
import ChatScreen      from './screens/ChatScreen';
import SmartHomeScreen from './screens/SmartHomeScreen';
import VaultScreen     from './screens/VaultScreen';

type Tab = 'home' | 'chat' | 'smarthome' | 'vault';

interface SmartDevice { id: string; name: string; icon: string; on: boolean; type: string; room: string }

const INITIAL_DEVICES: SmartDevice[] = [
  { id: '1', name: 'Luz da Sala',      icon: '💡', on: true,  type: 'light',   room: 'Sala' },
  { id: '2', name: 'TV',               icon: '📺', on: false, type: 'tv',      room: 'Sala' },
  { id: '3', name: 'Ventilador',       icon: '🌀', on: true,  type: 'fan',     room: 'Sala' },
  { id: '4', name: 'Luz do Quarto',    icon: '💡', on: false, type: 'light',   room: 'Quarto' },
  { id: '5', name: 'Ar Condicionado',  icon: '❄️', on: false, type: 'ac',      room: 'Quarto' },
  { id: '6', name: 'Luz da Cozinha',   icon: '💡', on: true,  type: 'light',   room: 'Cozinha' },
  { id: '7', name: 'Geladeira Smart',  icon: '🧊', on: true,  type: 'fridge',  room: 'Cozinha' },
  { id: '8', name: 'Roteador Wi-Fi',   icon: '📶', on: true,  type: 'router',  room: 'Escritório' },
];

const NAV = [
  { id: 'home',      icon: '◎',  label: 'Início'  },
  { id: 'chat',      icon: '💬', label: 'Chat IA' },
  { id: 'smarthome', icon: '🏠', label: 'Casa'    },
  { id: 'vault',     icon: '📦', label: 'Memória' },
];

export default function NexusApp() {
  const [tab,       setTab]       = useState<Tab>('home');
  const [listening, setListening] = useState(false);
  const [devices,   setDevices]   = useState<SmartDevice[]>(INITIAL_DEVICES);
  const [alexa,     setAlexa]     = useState({ connected: false, device: '' });

  const toggleDevice = useCallback((id: string) => {
    setDevices(prev => prev.map(d => d.id === id ? { ...d, on: !d.on } : d));
    // In real app: call Home Assistant or Alexa API here
  }, []);

  const onNav = useCallback((t: string) => setTab(t as Tab), []);

  return (
    <div className="wrap" style={{ background: '#060810' }}>
      {/* Background gradient */}
      <div style={{
        position: 'fixed', inset: 0, zIndex: 0, pointerEvents: 'none',
        background: 'radial-gradient(ellipse 80% 60% at 50% -5%, rgba(0,255,136,0.07) 0%, transparent 70%), radial-gradient(ellipse 50% 40% at 90% 70%, rgba(56,189,248,0.04) 0%, transparent 60%), #060810',
      }} />

      <main style={{ position: 'relative', zIndex: 1, paddingBottom: 80 }}>
        {tab === 'home'      && <HomeScreen onNav={onNav} alexaConnected={alexa.connected} smartDevices={devices} onToggleDevice={toggleDevice} />}
        {tab === 'chat'      && <ChatScreen onListeningChange={setListening} />}
        {tab === 'smarthome' && <SmartHomeScreen devices={devices} onToggle={toggleDevice} onNav={onNav} />}
        {tab === 'vault'     && <VaultScreen />}
      </main>

      {/* Bottom nav */}
      <nav className="bottom-nav">
        {NAV.map(n => (
          <button key={n.id} className={`nav-btn ${tab === n.id ? 'on' : ''}`} onClick={() => setTab(n.id as Tab)}>
            <span className="ni">{n.icon}</span>
            <span className="nl">{n.label}</span>
            {tab === n.id && <span style={{ width: 4, height: 4, borderRadius: '50%', background: 'var(--green)', boxShadow: '0 0 6px var(--green)', marginTop: 1 }} />}
          </button>
        ))}
      </nav>
    </div>
  );
}
