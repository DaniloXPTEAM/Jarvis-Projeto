// Painéis de interação: comida, estudo, guarda-roupa, memória, ajuda, inspeção.
import { useEffect, useMemo, useState } from "react";
import type { GameEngine } from "../game/engine";
import { OUTFITS, STATE_LABELS, phaseOf } from "../game/engine";
import { SIMULATION_CONFIG, type FoodId } from "../config/simulation";
import { ANIMATION_MANIFEST, ANIM_GROUPS, pendingCount } from "../game/animationManifest";
import { NAV_POINTS, validateConnectivity } from "../game/navigation";
import { clipForState } from "../game/animationLayer";
import type { GameSnapshot, OutfitId } from "../game/types";
import { audio } from "../game/audio";

function Sheet({ title, icon, onClose, children, wide }: { title: string; icon: string; onClose: () => void; children: React.ReactNode; wide?: boolean }) {
  return (
    <div className="absolute inset-0 z-30 flex items-end justify-center bg-black/45 p-2 sm:items-center" onClick={onClose}>
      <div
        onClick={(e) => e.stopPropagation()}
        className={`w-full ${wide ? "max-w-2xl" : "max-w-md"} max-h-[82vh] overflow-y-auto rounded-3xl bg-[#fff8ef] p-4 shadow-2xl sm:p-5`}
      >
        <div className="mb-3 flex items-center gap-2">
          <span className="text-2xl" aria-hidden>{icon}</span>
          <h2 className="font-display flex-1 text-xl font-extrabold text-stone-800">{title}</h2>
          <button onClick={onClose} className="rounded-full bg-stone-200 px-3 py-1.5 text-sm font-bold text-stone-600 transition hover:bg-stone-300 active:scale-95" aria-label="Fechar">
            ✕
          </button>
        </div>
        {children}
      </div>
    </div>
  );
}

export function FoodPanel({ engine, snap, onClose }: { engine: GameEngine; snap: GameSnapshot; onClose: () => void }) {
  const foods = SIMULATION_CONFIG.foods as Record<FoodId, { label: string; icon: string; hunger?: number; durationSec: number; isDrink?: boolean }>;
  const full = snap.needs.hunger > SIMULATION_CONFIG.needs.thresholds.full;
  return (
    <Sheet title="O que a Gabi vai comer?" icon="🍽️" onClose={onClose}>
      {full && <p className="mb-2 rounded-xl bg-emerald-100 px-3 py-2 text-sm text-emerald-800">Gabi está satisfeita! Só uma bebidinha, talvez? 💧</p>}
      <div className="grid grid-cols-2 gap-2">
        {(Object.keys(foods) as FoodId[]).map((id) => {
          const f = foods[id];
          const disabled = full && !f.isDrink;
          return (
            <button
              key={id}
              disabled={disabled}
              onClick={() => { audio.play("pop"); engine.ui.foodOpen = false; engine.bumpUI(); engine.requestActivity(f.isDrink ? "drink" : "eat", { food: id }); }}
              className={`flex items-center gap-2 rounded-2xl border-2 p-2.5 text-left transition active:scale-95 ${disabled ? "border-stone-200 bg-stone-100 opacity-50" : "border-amber-200 bg-white hover:border-amber-400 hover:shadow"}`}
            >
              <span className="text-3xl" aria-hidden>{f.icon}</span>
              <span>
                <span className="block text-sm font-bold text-stone-800">{f.label}</span>
                <span className="block text-xs text-stone-500">+{f.hunger ?? 0} saciedade{f.isDrink ? " · bebida" : ""}</span>
              </span>
            </button>
          );
        })}
      </div>
    </Sheet>
  );
}

export function StudyPanel({ engine, onClose }: { engine: GameEngine; onClose: () => void }) {
  const study = SIMULATION_CONFIG.activities.study;
  const read = SIMULATION_CONFIG.activities.read;
  return (
    <Sheet title="Hora de aprender!" icon="📚" onClose={onClose}>
      <div className="grid gap-2">
        <button
          onClick={() => { engine.ui.studyOpen = false; engine.bumpUI(); engine.requestActivity("study", { study: "study" }); }}
          className="flex items-center gap-3 rounded-2xl border-2 border-sky-200 bg-white p-3 text-left transition hover:border-sky-400 hover:shadow active:scale-95"
        >
          <span className="text-3xl" aria-hidden>✏️</span>
          <span>
            <span className="block text-sm font-bold text-stone-800">Estudar / Escrever ({study.durationSec}s)</span>
            <span className="block text-xs text-stone-500">+{study.learningGain} estudo · −{study.energyCost} energia</span>
          </span>
        </button>
        <button
          onClick={() => { engine.ui.studyOpen = false; engine.bumpUI(); engine.requestActivity("read", { study: "read" }); }}
          className="flex items-center gap-3 rounded-2xl border-2 border-violet-200 bg-white p-3 text-left transition hover:border-violet-400 hover:shadow active:scale-95"
        >
          <span className="text-3xl" aria-hidden>📖</span>
          <span>
            <span className="block text-sm font-bold text-stone-800">Ler historinha ({read.durationSec}s)</span>
            <span className="block text-xs text-stone-500">+{read.learningGain} estudo · −{read.energyCost} energia</span>
          </span>
        </button>
      </div>
    </Sheet>
  );
}

export function PlayPanel({ engine, onClose }: { engine: GameEngine; onClose: () => void }) {
  const cfg = SIMULATION_CONFIG.activities.playFree;
  const mem = SIMULATION_CONFIG.activities.memoryGame;
  return (
    <Sheet title="Vamos brincar!" icon="🧸" onClose={onClose}>
      <div className="grid gap-2">
        <button
          onClick={() => { engine.ui.playOpen = false; engine.bumpUI(); engine.requestActivity("memory"); }}
          className="flex items-center gap-3 rounded-2xl border-2 border-violet-200 bg-white p-3 text-left transition hover:border-violet-400 hover:shadow active:scale-95"
        >
          <span className="text-3xl" aria-hidden>🃏</span>
          <span>
            <span className="block text-sm font-bold text-stone-800">Jogo da memória (interativo)</span>
            <span className="block text-xs text-stone-500">+{mem.funGain} diversão · +{mem.learningGain} estudo ao vencer</span>
          </span>
        </button>
        <button
          onClick={() => { engine.ui.playOpen = false; engine.bumpUI(); engine.requestActivity("play"); }}
          className="flex items-center gap-3 rounded-2xl border-2 border-rose-200 bg-white p-3 text-left transition hover:border-rose-400 hover:shadow active:scale-95"
        >
          <span className="text-3xl" aria-hidden>🧸</span>
          <span>
            <span className="block text-sm font-bold text-stone-800">Brincar livre no tapete ({cfg.durationSec}s)</span>
            <span className="block text-xs text-stone-500">+{cfg.funGain} diversão · sem desafio</span>
          </span>
        </button>
      </div>
    </Sheet>
  );
}

export function WardrobePanel({ engine, snap, onClose }: { engine: GameEngine; snap: GameSnapshot; onClose: () => void }) {
  return (
    <Sheet title="Guarda-roupa da Gabi" icon="👗" onClose={onClose}>
      <p className="mb-2 text-xs text-stone-500">A troca acontece com privacidade (sem nudez) e é salva automaticamente. 🌙 O pijama é automático ao dormir.</p>
      <div className="grid grid-cols-2 gap-2">
        {OUTFITS.map((o) => {
          const active = snap.outfit === o.id;
          return (
            <button
              key={o.id}
              onClick={() => { engine.chooseOutfit(o.id as OutfitId); }}
              className={`flex items-center gap-2 rounded-2xl border-2 p-2.5 text-left transition active:scale-95 ${active ? "border-rose-400 bg-rose-50 shadow" : "border-stone-200 bg-white hover:border-rose-300"}`}
            >
              <span className="text-3xl" aria-hidden>{o.icon}</span>
              <span>
                <span className="block text-sm font-bold text-stone-800">{o.label}</span>
                <span className="block text-xs text-stone-500">{active ? "✨ vestindo agora" : o.isPajama ? "automático à noite" : "toque para vestir"}</span>
              </span>
            </button>
          );
        })}
      </div>
    </Sheet>
  );
}

const MEMORY_EMOJI = ["🌈", "🦄", "🍓", "🐱", "🚀", "🌼"];

export function MemoryPanel({ engine, onClose }: { engine: GameEngine; onClose: () => void }) {
  const deck = useMemo(() => {
    const d = [...MEMORY_EMOJI, ...MEMORY_EMOJI].map((e, i) => ({ id: i, emoji: e }));
    for (let i = d.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [d[i], d[j]] = [d[j], d[i]];
    }
    return d;
  }, []);
  const [open, setOpen] = useState<number[]>([]);
  const [found, setFound] = useState<number[]>([]);
  const [moves, setMoves] = useState(0);

  useEffect(() => {
    if (open.length === 2) {
      setMoves((m) => m + 1);
      const [a, b] = open;
      if (deck[a].emoji === deck[b].emoji) {
        audio.play("pop");
        const t = setTimeout(() => { setFound((f) => [...f, a, b]); setOpen([]); }, 450);
        return () => clearTimeout(t);
      } else {
        const t = setTimeout(() => setOpen([]), 800);
        return () => clearTimeout(t);
      }
    }
  }, [open, deck]);

  useEffect(() => {
    if (found.length === deck.length && deck.length > 0) {
      const t = setTimeout(() => engine.completeMemory(true, MEMORY_EMOJI.length), 700);
      return () => clearTimeout(t);
    }
  }, [found, deck.length, engine]);

  const flip = (idx: number) => {
    if (open.includes(idx) || found.includes(idx) || open.length >= 2) return;
    audio.play("tap");
    setOpen((o) => [...o, idx]);
  };

  return (
    <Sheet title={`Jogo da memória · ${moves} tentativas`} icon="🃏" onClose={onClose}>
      <p className="mb-2 text-xs text-stone-500">Encontre os 6 pares para ganhar diversão e estudo! Gabi está sentada no tapete brincando. 🧸</p>
      <div className="grid grid-cols-3 gap-2 sm:grid-cols-4">
        {deck.map((c, i) => {
          const face = open.includes(i) || found.includes(i);
          return (
            <button
              key={c.id}
              onClick={() => flip(i)}
              className={`flex aspect-square items-center justify-center rounded-2xl text-3xl transition active:scale-95 ${face ? "bg-white shadow-inner ring-2 ring-amber-300" : "bg-gradient-to-br from-rose-400 to-violet-500 text-white shadow hover:brightness-110"} ${found.includes(i) ? "opacity-60" : ""}`}
              aria-label={face ? c.emoji : "carta virada"}
            >
              {face ? c.emoji : "✨"}
            </button>
          );
        })}
      </div>
      <button
        onClick={() => engine.completeMemory(false, found.length / 2)}
        className="mt-3 w-full rounded-2xl bg-stone-200 py-2 text-sm font-bold text-stone-600 transition hover:bg-stone-300 active:scale-95"
      >
        Guardar o jogo ({found.length / 2}/6 pares)
      </button>
    </Sheet>
  );
}

export function HelpPanel({ onClose }: { onClose: () => void }) {
  const p = pendingCount();
  return (
    <Sheet title="Sobre o minigame da Gabi" icon="💛" onClose={onClose} wide>
      <div className="space-y-3 text-sm text-stone-700">
        <p>
          <strong>Como brincar:</strong> toque nos móveis do quarto (cama, escrivaninha, mesinha, tapete,
          guarda-roupa, porta do banheiro, janela) ou use a barra de ações. Cuide da fome, energia, higiene,
          banheiro, diversão, estudo e humor da Gabi.
        </p>
        <p>
          <strong>Relógio:</strong> 1 dia de jogo = {SIMULATION_CONFIG.clock.realMinutesPerGameDay} minutos reais.
          À noite Gabi fica cansada; de madrugada ela dorme. O progresso é salvo automaticamente e continua
          de onde parou — inclusive após dias longe.
        </p>
        <div className="rounded-2xl bg-amber-50 p-3 ring-1 ring-amber-200">
          <p className="font-bold text-amber-900">⚠️ Transparência visual</p>
          <p className="text-amber-800">
            A personagem 3D atual é <strong>provisória</strong> (malha procedural para validar escala, navegação
            e estados). A Gabi definitiva em GLB com esqueleto + {p.total} clipes de animação será importada
            depois, via manifesto — <strong>{p.pending} clipes pendentes</strong>, nenhum declarado pronto.
          </p>
        </div>
        <div className="rounded-2xl bg-sky-50 p-3 ring-1 ring-sky-200">
          <p className="font-bold text-sky-900">💾 Persistência local</p>
          <p className="text-sky-800">
            Este protótipo salva apenas no aparelho (localStorage). Para o app final, o formato de save
            documentado em <code>docs/INTEGRATION.md</code> pode ser enviado ao backend sem mudar o jogo.
          </p>
        </div>
        <p className="text-xs text-stone-500">
          Movimento reduzido 🚸 diminui balanços e acelera transições. O sino 🔇 silencia todos os sons.
          Autonomia pode ser ligada/desligada nas inspeções. Sem morte ou perda de progresso — sempre acolhedor.
        </p>
      </div>
    </Sheet>
  );
}

export function InspectPanel({
  snap, engine, fps, showNav, onToggleNav, autonomy, onToggleAutonomy, clockSpeed, onClockSpeed, onClose,
}: {
  snap: GameSnapshot; engine: GameEngine; fps: number;
  showNav: boolean; onToggleNav: () => void;
  autonomy: boolean; onToggleAutonomy: () => void;
  clockSpeed: number; onClockSpeed: (v: number) => void;
  onClose: () => void;
}) {
  const [tab, setTab] = useState<"estado" | "nav" | "anims">("estado");
  const [conn, setConn] = useState<null | { ok: number; total: number }>(null);
  const p = pendingCount();
  const phase = phaseOf(snap.gameMinutes);

  const runConnectivity = () => {
    const res = validateConnectivity();
    setConn({ ok: res.filter((r) => r.ok).length, total: res.length });
    audio.play(res.every((r) => r.ok) ? "success" : "error");
  };

  return (
    <div className="absolute bottom-24 left-2 right-2 z-30 mx-auto max-w-2xl sm:left-auto sm:right-3 sm:w-[380px]">
      <div className="overflow-hidden rounded-2xl bg-[#14101c]/95 text-xs text-violet-100 shadow-2xl ring-1 ring-violet-400/30 backdrop-blur">
        <div className="flex items-center gap-1 bg-violet-950/60 p-2">
          {(["estado", "nav", "anims"] as const).map((t) => (
            <button key={t} onClick={() => setTab(t)} className={`rounded-lg px-2.5 py-1 font-bold uppercase tracking-wide ${tab === t ? "bg-violet-500 text-white" : "text-violet-300 hover:bg-white/10"}`}>
              {t === "estado" ? "⚙️ Estado" : t === "nav" ? "🗺️ Nav" : `🎬 Anims ${p.ready}/${p.total}`}
            </button>
          ))}
          <span className="ml-auto tabular-nums text-violet-300">{fps} fps</span>
          <button onClick={onClose} className="rounded-lg bg-white/10 px-2 py-1 font-bold hover:bg-white/20">✕</button>
        </div>
        <div className="max-h-[38vh] overflow-y-auto p-2.5">
          {tab === "estado" && (
            <div className="space-y-1.5">
              <div className="grid grid-cols-2 gap-1 font-mono">
                <span>estado <b className="text-amber-300">{snap.state}</b> <span className="opacity-70">({STATE_LABELS[snap.state]})</span></span>
                <span>atividade <b className="text-amber-300">{snap.activity}.{snap.activityPhase}</b></span>
                <span>clipe <b className="text-sky-300">{clipForState(snap.state)}</b> <span className="opacity-70">(provisório)</span></span>
                <span>pos <b>[{snap.position.x.toFixed(2)}, {snap.position.z.toFixed(2)}]</b> θ {snap.facing.toFixed(2)}</span>
                <span>hora <b>{phase.icon} {(snap.gameMinutes / 60 % 24).toFixed(2)}h</b> dia {snap.dayCount}</span>
                <span>porta <b>{(snap.doorOpen * 100).toFixed(0)}%</b> {snap.gabiInsideBathroom ? "· dentro" : ""}</span>
                <span>roupa <b>{snap.outfit}</b></span>
                <span>xp <b>{snap.xp}</b> nv {snap.level}</span>
              </div>
              <div className="flex flex-wrap items-center gap-1.5 pt-1">
                <button onClick={onToggleNav} className={`rounded-lg px-2 py-1 font-bold ${showNav ? "bg-cyan-500 text-white" : "bg-white/10"}`}>
                  {showNav ? "Ocultar pontos" : "Mostrar pontos"}
                </button>
                <button onClick={onToggleAutonomy} className={`rounded-lg px-2 py-1 font-bold ${autonomy ? "bg-emerald-500 text-white" : "bg-white/10"}`}>
                  Autonomia {autonomy ? "ON" : "OFF"}
                </button>
                <span className="ml-1 opacity-70">vel. relógio</span>
                {[1, 4, 16].map((v) => (
                  <button key={v} onClick={() => onClockSpeed(v)} className={`rounded-lg px-2 py-1 font-bold tabular-nums ${clockSpeed === v ? "bg-amber-400 text-black" : "bg-white/10"}`}>{v}x</button>
                ))}
              </div>
              <p className="opacity-60">● ciano = approach · ● rosa = placement. Gabi nunca atravessa móveis (A* + raio 0.26m).</p>
            </div>
          )}
          {tab === "nav" && (
            <div className="space-y-1.5">
              <button onClick={runConnectivity} className="rounded-lg bg-cyan-500 px-2 py-1 font-bold text-white">▶ Validar 132 rotas (12 pontos)</button>
              {conn && <p className={conn.ok === conn.total ? "text-emerald-300" : "text-red-300"}>Rotas válidas: {conn.ok}/{conn.total}</p>}
              <div className="grid grid-cols-1 gap-1 font-mono">
                {Object.values(NAV_POINTS).map((pt) => (
                  <button
                    key={pt.id}
                    onClick={() => { if (!engine.debugGoto(pt.id)) audio.play("error"); else audio.play("tap"); }}
                    title="Mandar Gabi ociosa até este ponto"
                    className="rounded bg-white/5 px-2 py-1 text-left hover:bg-white/10"
                  >
                    <b className="text-cyan-300">{pt.id}</b> a:[{pt.approach.x.toFixed(2)},{pt.approach.z.toFixed(2)}] p:[{pt.placement.x.toFixed(2)},{pt.placement.z.toFixed(2)}]
                  </button>
                ))}
              </div>
            </div>
          )}
          {tab === "anims" && (
            <div className="space-y-2">
              <p className="opacity-70">Todos os clipes: <b className="text-amber-300">pending</b> (driver procedural). Importar GLB = atualizar manifesto. Nenhum efeito visual aqui altera a simulação.</p>
              {ANIM_GROUPS.map((g) => (
                <details key={g.id} className="rounded-lg bg-white/5">
                  <summary className="cursor-pointer px-2 py-1 font-bold text-violet-200">{g.label} ({ANIMATION_MANIFEST.filter((c) => c.group === g.id).length})</summary>
                  <div className="space-y-0.5 px-2 pb-2 font-mono">
                    {ANIMATION_MANIFEST.filter((c) => c.group === g.id).map((c) => (
                      <div key={c.id} className="flex items-center gap-1.5">
                        <span className="rounded bg-amber-500/20 px-1 text-amber-300">⏳</span>
                        <span><b>{c.id}</b> <span className="opacity-60">{c.duration}s{c.loop ? " ↻" : ""} → {c.next ?? "—"}</span></span>
                      </div>
                    ))}
                  </div>
                </details>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
