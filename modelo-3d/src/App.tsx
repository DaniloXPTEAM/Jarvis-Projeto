// ============================================================
// APP — ponto de integração do minigame da Gabi.
// INTEGRAÇÃO: o botão "Cuidar da Gabi" (HomeScreen) abre o <GameView/>
// dentro do próprio app; "Voltar" salva e desmonta tudo (render,
// sons e temporizadores param no cleanup do GameCanvas).
// Para embutir em outro app: importe { GameView } de src/game/GameView.
// ============================================================

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import GameCanvas from "./game/GameCanvas";
import { GameEngine, formatClock, phaseOf } from "./game/engine";
import type { ActivityId, GameSnapshot, GameSettings } from "./game/types";
import { loadSave, loadSettings, saveSettings } from "./game/persistence";
import { audio } from "./game/audio";
import HUD from "./components/HUD";
import ActionBar from "./components/ActionBar";
import { FoodPanel, StudyPanel, WardrobePanel, MemoryPanel, PlayPanel, HelpPanel, InspectPanel } from "./components/Panels";

// ---------------- TELA INICIAL ----------------
function HomeScreen({ onPlay }: { onPlay: () => void }) {
  const save = useMemo(() => loadSave(), []);
  return (
    <div className="min-h-screen bg-gradient-to-b from-[#2b1f3d] via-[#3d2a4d] to-[#1d1526] text-white">
      <div className="mx-auto flex min-h-screen max-w-5xl flex-col items-center justify-center gap-6 p-6 lg:flex-row lg:gap-12">
        <div className="w-full max-w-sm shrink-0">
          <div className="overflow-hidden rounded-[2rem] shadow-2xl ring-4 ring-white/15">
            <img
              src="images/gabi-concept.jpg"
              alt="Arte conceitual da Gabi — menina ruiva de vestido vermelho"
              className="aspect-[4/5] w-full object-cover"
              onError={(e) => { (e.target as HTMLImageElement).style.display = "none"; }}
            />
          </div>
          <p className="mt-2 text-center text-xs text-white/50">Arte conceitual — personagem 3D definitiva em produção externa</p>
        </div>

        <div className="max-w-md text-center lg:text-left">
          <p className="mb-1 text-sm font-bold uppercase tracking-widest text-amber-300">Minigame de cuidados 💛</p>
          <h1 className="font-display text-5xl font-extrabold leading-[0.95] sm:text-6xl">
            Quarto da <span className="text-rose-400">Gabi</span>
          </h1>
          <p className="mt-3 text-white/80">
            Alimente, ensine, brinque, vista e coloque a Gabi para dormir em seu quartinho 3D aconchegante —
            com dia e noite, rotina e surpresas todos os dias.
          </p>

          <button
            onClick={onPlay}
            className="font-display mt-6 w-full rounded-3xl bg-rose-500 px-8 py-4 text-xl font-extrabold shadow-xl shadow-rose-900/50 ring-4 ring-rose-400/30 transition hover:-translate-y-0.5 hover:bg-rose-400 active:scale-95 sm:w-auto"
          >
            💛 Cuidar da Gabi
          </button>
          {save && (
            <p className="mt-2 text-sm text-white/70">
              Continuar do Dia {save.dayCount} · {formatClock(save.gameMinutes)} {phaseOf(save.gameMinutes).icon}
            </p>
          )}

          <div className="mt-6 grid grid-cols-2 gap-2 text-left text-sm">
            {[
              ["🛏️", "Rotina completa", "Comer, estudar, brincar, banho e sono"],
              ["🌙", "Dia e noite", "Quarto muda com o relógio do jogo"],
              ["🧭", "Ela anda sozinha", "Navega pelo quarto sem atravessar móveis"],
              ["💾", "Salva sozinho", "Pode sair e voltar quando quiser"],
            ].map(([icon, t, d]) => (
              <div key={t} className="rounded-2xl bg-white/10 p-3 ring-1 ring-white/10">
                <span className="text-xl" aria-hidden>{icon}</span>
                <p className="mt-1 font-bold">{t}</p>
                <p className="text-xs text-white/60">{d}</p>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

// ---------------- JOGO ----------------
export function GameView({ onBack }: { onBack: () => void }) {
  const engineRef = useRef<GameEngine | null>(null);
  if (!engineRef.current) engineRef.current = new GameEngine();
  const engine = engineRef.current;

  const [snap, setSnap] = useState<GameSnapshot>({ ...engine.snapshot });
  const [settings, setSettings] = useState<GameSettings>(() => loadSettings());
  const [showNav, setShowNav] = useState(false);
  const [fps, setFps] = useState(60);
  const [reconcileMsg, setReconcileMsg] = useState<string | null>(engine.reconcileInfo?.message ?? null);
  const lastUiVersion = useRef(engine.uiVersion);

  // Aplica preferências ao engine/áudio
  useEffect(() => {
    engine.reducedMotion = settings.reducedMotion;
    engine.clockSpeed = settings.clockSpeed;
    engine.snapshot.autonomyEnabled = settings.autonomy;
    audio.setMuted(settings.muted);
    saveSettings(settings);
  }, [settings, engine]);

  // Espelha o snapshot no React (150ms + imediato em mudanças de UI)
  useEffect(() => {
    let latest: GameSnapshot = engine.snapshot;
    const unsub = engine.subscribe((s) => { latest = s; });
    const id = window.setInterval(() => {
      if (engine.uiVersion !== lastUiVersion.current) {
        lastUiVersion.current = engine.uiVersion;
        setSnap({ ...latest });
      } else {
        setSnap((prev) => {
          // evita re-render se nada relevante mudou (resumo barato)
          if (
            prev.state === latest.state && prev.activity === latest.activity &&
            prev.activityPhase === latest.activityPhase && prev.bubble === latest.bubble &&
            Math.abs(prev.gameMinutes - latest.gameMinutes) < 0.05 &&
            prev.doorOpen === latest.doorOpen
          ) return prev;
          return { ...latest };
        });
      }
    }, 150);
    return () => { window.clearInterval(id); unsub(); };
  }, [engine]);

  // Some a mensagem de retorno após alguns segundos
  useEffect(() => {
    if (!reconcileMsg) return;
    const id = window.setTimeout(() => setReconcileMsg(null), 6);
    return () => window.clearTimeout(id);
  }, [reconcileMsg]);

  // Salva ao ocultar a aba
  useEffect(() => {
    const onHide = () => engine.save();
    document.addEventListener("visibilitychange", onHide);
    window.addEventListener("pagehide", onHide);
    return () => {
      document.removeEventListener("visibilitychange", onHide);
      window.removeEventListener("pagehide", onHide);
    };
  }, [engine]);

  const handleBack = useCallback(() => {
    audio.play("close");
    engine.save();
    saveSettings({ ...loadSettings(), muted: audio.muted });
    onBack();
  }, [engine, onBack]);

  const act = useCallback((a: ActivityId) => {
    if (a === "eat" || a === "drink") { engine.ui.foodOpen = true; engine.bumpUI(); }
    else if (a === "memory" || a === "play") { engine.ui.playOpen = true; engine.bumpUI(); audio.play("tap"); }
    else engine.requestActivity(a);
  }, [engine]);

  const ui = engine.ui;

  return (
    <div className="fixed inset-0 overflow-hidden bg-[#241d28] select-none">
      <GameCanvas engine={engine} inspect={{ showNav, showLabels: false }} onFps={setFps} />

      <HUD
        snap={snap}
        phaseName={engine.phaseName()}
        muted={settings.muted}
        reducedMotion={settings.reducedMotion}
        inspectOpen={ui.inspectOpen}
        onToggleMute={() => setSettings((s) => ({ ...s, muted: !s.muted }))}
        onToggleMotion={() => setSettings((s) => ({ ...s, reducedMotion: !s.reducedMotion }))}
        onToggleInspect={() => { engine.ui.inspectOpen = !engine.ui.inspectOpen; engine.bumpUI(); }}
        onOpenHelp={() => { engine.ui.helpOpen = true; engine.bumpUI(); }}
        onBack={handleBack}
      />

      {reconcileMsg && (
        <div className="absolute left-1/2 top-32 z-20 w-max max-w-[92%] -translate-x-1/2 rounded-2xl bg-violet-950/90 px-4 py-2 text-center text-sm text-violet-100 shadow-xl ring-1 ring-violet-400/40">
          {reconcileMsg}
        </div>
      )}

      <ActionBar
        snap={snap}
        onAction={act}
        onOpenFood={() => { engine.ui.foodOpen = true; engine.bumpUI(); audio.play("tap"); }}
        onOpenStudy={() => { engine.ui.studyOpen = true; engine.bumpUI(); audio.play("tap"); }}
        onWake={() => engine.wakeUp()}
        onCancel={() => engine.cancelToIdle("Ok, parei! ✨")}
      />

      {ui.foodOpen && <FoodPanel engine={engine} snap={snap} onClose={() => { engine.ui.foodOpen = false; engine.bumpUI(); }} />}
      {ui.studyOpen && <StudyPanel engine={engine} onClose={() => { engine.ui.studyOpen = false; engine.bumpUI(); }} />}
      {ui.playOpen && <PlayPanel engine={engine} onClose={() => { engine.ui.playOpen = false; engine.bumpUI(); }} />}
      {ui.wardrobeOpen && snap.activity === "wardrobe" && (
        <WardrobePanel engine={engine} snap={snap} onClose={() => { engine.ui.wardrobeOpen = false; engine.cancelToIdle(); engine.bumpUI(); }} />
      )}
      {ui.memoryOpen && snap.activity === "memory" && (
        <MemoryPanel engine={engine} onClose={() => engine.completeMemory(false, 0)} />
      )}
      {ui.helpOpen && <HelpPanel onClose={() => { engine.ui.helpOpen = false; engine.bumpUI(); }} />}
      {ui.inspectOpen && (
        <InspectPanel
          snap={snap} engine={engine} fps={fps}
          showNav={showNav} onToggleNav={() => setShowNav((v) => !v)}
          autonomy={settings.autonomy} onToggleAutonomy={() => setSettings((s) => ({ ...s, autonomy: !s.autonomy }))}
          clockSpeed={settings.clockSpeed} onClockSpeed={(v) => setSettings((s) => ({ ...s, clockSpeed: v }))}
          onClose={() => { engine.ui.inspectOpen = false; engine.bumpUI(); }}
        />
      )}
    </div>
  );
}

// ---------------- APP ----------------
export default function App() {
  const [screen, setScreen] = useState<"home" | "game">("home");
  return screen === "home"
    ? <HomeScreen onPlay={() => { audio.resume(); audio.play("open"); setScreen("game"); }} />
    : <GameView onBack={() => setScreen("home")} />;
}
