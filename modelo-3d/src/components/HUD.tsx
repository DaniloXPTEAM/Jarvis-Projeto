// Barra superior: relógio, fase do dia, necessidades compactas, XP e controles.
import type { GameSnapshot } from "../game/types";
import { formatClock, phaseOf } from "../game/engine";
import { SIMULATION_CONFIG } from "../config/simulation";

interface Props {
  snap: GameSnapshot;
  phaseName: string;
  muted: boolean;
  reducedMotion: boolean;
  inspectOpen: boolean;
  onToggleMute: () => void;
  onToggleMotion: () => void;
  onToggleInspect: () => void;
  onOpenHelp: () => void;
  onBack: () => void;
}

const NEED_META: Array<{ key: keyof GameSnapshot["needs"]; icon: string; label: string }> = [
  { key: "hunger", icon: "🍎", label: "Saciedade" },
  { key: "energy", icon: "⚡", label: "Energia" },
  { key: "hygiene", icon: "🧼", label: "Higiene" },
  { key: "bathroom", icon: "🚻", label: "Banheiro" },
  { key: "fun", icon: "🧸", label: "Diversão" },
  { key: "learning", icon: "📚", label: "Estudo" },
  { key: "mood", icon: "💛", label: "Humor" },
];

function barColor(v: number): string {
  const t = SIMULATION_CONFIG.needs.thresholds;
  if (v < t.critical) return "bg-red-400";
  if (v < t.low) return "bg-amber-400";
  return "bg-emerald-400";
}

export default function HUD({ snap, phaseName, muted, reducedMotion, inspectOpen, onToggleMute, onToggleMotion, onToggleInspect, onOpenHelp, onBack }: Props) {
  const phase = phaseOf(snap.gameMinutes);
  const xpLevels = SIMULATION_CONFIG.evolution.xpLevels;
  const cur = xpLevels[snap.level] ?? 0;
  const next = xpLevels[snap.level + 1];
  const pct = next ? Math.min(1, (snap.xp - cur) / (next - cur)) : 1;

  return (
    <div className="pointer-events-none absolute inset-x-0 top-0 z-20 flex flex-col gap-1.5 p-2 sm:p-3">
      <div className="flex items-center gap-2">
        <button
          onClick={onBack}
          className="pointer-events-auto flex h-10 items-center gap-1.5 rounded-xl bg-black/55 px-3 text-sm font-semibold text-white backdrop-blur transition hover:bg-black/70 active:scale-95"
          aria-label="Voltar"
        >
          <span aria-hidden>‹</span> Voltar
        </button>
        <div className="pointer-events-auto flex h-10 items-center gap-2 rounded-xl bg-black/55 px-3 text-white backdrop-blur">
          <span className="text-lg" aria-hidden>{phase.icon}</span>
          <span className="font-display text-base font-bold tabular-nums">{formatClock(snap.gameMinutes)}</span>
          <span className="hidden text-xs opacity-80 sm:inline">Dia {snap.dayCount} · {phase.label}</span>
        </div>
        <div className="pointer-events-auto ml-auto flex h-10 items-center gap-1 rounded-xl bg-black/55 px-2 text-white backdrop-blur">
          <span className="px-1 text-xs font-semibold text-amber-200">Lv{snap.level + 1} {phaseName}</span>
          <div className="h-1.5 w-12 overflow-hidden rounded-full bg-white/20 sm:w-16">
            <div className="h-full rounded-full bg-amber-300 transition-all" style={{ width: `${pct * 100}%` }} />
          </div>
        </div>
      </div>

      <div className="flex items-center gap-1.5">
        <div className="pointer-events-auto flex flex-1 items-center gap-1 overflow-x-auto rounded-xl bg-black/55 p-1.5 backdrop-blur">
          {NEED_META.map((n) => {
            const v = Math.round(snap.needs[n.key]);
            return (
              <div key={n.key} title={`${n.label}: ${v}%`} className="flex min-w-[52px] flex-1 flex-col items-center gap-0.5 rounded-lg px-1 py-0.5">
                <span className="text-sm leading-none" aria-hidden>{n.icon}</span>
                <div className="h-1.5 w-full overflow-hidden rounded-full bg-white/20">
                  <div className={`h-full rounded-full transition-all ${barColor(v)}`} style={{ width: `${v}%` }} />
                </div>
              </div>
            );
          })}
        </div>
        <div className="pointer-events-auto flex gap-1">
          <button onClick={onToggleMute} title={muted ? "Ativar som" : "Silenciar"} className="flex h-9 w-9 items-center justify-center rounded-xl bg-black/55 text-base backdrop-blur transition hover:bg-black/70 active:scale-95">
            {muted ? "🔇" : "🔊"}
          </button>
          <button onClick={onToggleMotion} title="Movimento reduzido" className={`flex h-9 w-9 items-center justify-center rounded-xl text-base backdrop-blur transition active:scale-95 ${reducedMotion ? "bg-teal-500/80" : "bg-black/55 hover:bg-black/70"}`}>
            🚸
          </button>
          <button onClick={onToggleInspect} title="Modo de inspeção" className={`flex h-9 w-9 items-center justify-center rounded-xl text-base backdrop-blur transition active:scale-95 ${inspectOpen ? "bg-violet-500/80" : "bg-black/55 hover:bg-black/70"}`}>
            🔍
          </button>
          <button onClick={onOpenHelp} title="Sobre e ajuda" className="flex h-9 w-9 items-center justify-center rounded-xl bg-black/55 text-base backdrop-blur transition hover:bg-black/70 active:scale-95">
            ❔
          </button>
        </div>
      </div>

      {(snap.activity !== "none" || snap.bubble) && (
        <div className="flex justify-center">
          {snap.bubble && snap.activity === "none" && (
            <div className="pointer-events-auto max-w-[85%] rounded-2xl rounded-bl-md bg-white/95 px-3 py-1.5 text-center text-sm font-medium text-stone-800 shadow-lg">
              {snap.bubble}
            </div>
          )}
          {snap.activity !== "none" && (
            <div className="pointer-events-auto flex max-w-[92%] items-center gap-2 rounded-2xl bg-white/95 px-3 py-1.5 shadow-lg">
              <span className="text-sm font-semibold text-stone-800">{snap.bubble ?? snap.activityLabel}</span>
              {snap.activityProgress > 0 && (
                <div className="h-2 w-20 overflow-hidden rounded-full bg-stone-200 sm:w-28">
                  <div className="h-full rounded-full bg-rose-400 transition-all" style={{ width: `${snap.activityProgress * 100}%` }} />
                </div>
              )}
              {snap.activity === "sleep" && snap.activityPhase === "EXECUTE" && (
                <span className="text-xs text-stone-500">⚡ {Math.round(snap.needs.energy)}%</span>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
