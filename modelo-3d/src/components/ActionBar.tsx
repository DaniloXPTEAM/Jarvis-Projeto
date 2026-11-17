// Barra de ações inferior — confortável para toque.
import type { ActivityId, GameSnapshot } from "../game/types";

interface Props {
  snap: GameSnapshot;
  onAction: (a: ActivityId) => void;
  onOpenFood: () => void;
  onOpenStudy: () => void;
  onWake: () => void;
  onCancel: () => void;
}

export default function ActionBar({ snap, onAction, onOpenFood, onOpenStudy, onWake, onCancel }: Props) {
  const busy = snap.activity !== "none";
  const sleeping = snap.activity === "sleep" && snap.activityPhase === "EXECUTE";
  const inBathroom = snap.activity === "bathroom" && snap.activityPhase === "EXECUTE";

  const btn = (active: boolean) =>
    `flex min-w-[62px] flex-1 flex-col items-center gap-0.5 rounded-2xl px-1 py-2 text-[11px] font-semibold transition active:scale-95 sm:text-xs ${
      active ? "bg-rose-500 text-white shadow-lg shadow-rose-500/30" : "bg-white/90 text-stone-700 shadow hover:bg-white"
    }`;

  if (sleeping) {
    return (
      <div className="absolute inset-x-0 bottom-0 z-20 p-2 sm:p-3">
        <div className="mx-auto flex max-w-xl items-center gap-2 rounded-3xl bg-black/55 p-2 backdrop-blur">
          <div className="flex-1 px-2 text-sm text-white">💤 Dormindo… energia {Math.round(snap.needs.energy)}%</div>
          <button onClick={onWake} className="rounded-2xl bg-amber-400 px-5 py-2.5 text-sm font-bold text-stone-900 shadow transition hover:bg-amber-300 active:scale-95">
            ☀️ Acordar
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="absolute inset-x-0 bottom-0 z-20 p-2 sm:p-3">
      <div className="mx-auto flex max-w-xl items-center gap-1.5 rounded-3xl bg-black/55 p-2 backdrop-blur">
        <button className={btn(snap.activity === "eat" || snap.activity === "drink")} onClick={onOpenFood}>
          <span className="text-xl" aria-hidden>🍽️</span>Comer
        </button>
        <button className={btn(snap.activity === "study")} onClick={onOpenStudy}>
          <span className="text-xl" aria-hidden>📚</span>Estudar
        </button>
        <button className={btn(snap.activity === "play" || snap.activity === "memory")} onClick={() => onAction("memory")}>
          <span className="text-xl" aria-hidden>🧸</span>Brincar
        </button>
        <button className={btn(snap.activity === "sleep")} onClick={() => onAction("sleep")}>
          <span className="text-xl" aria-hidden>🛏️</span>Dormir
        </button>
        <button className={btn(snap.activity === "bathroom")} onClick={() => onAction("bathroom")}>
          <span className="text-xl" aria-hidden>🚻</span>Banheiro
        </button>
        <button className={btn(snap.activity === "wardrobe")} onClick={() => onAction("wardrobe")}>
          <span className="text-xl" aria-hidden>👗</span>Roupas
        </button>
        {busy && !inBathroom && (
          <button onClick={onCancel} title="Interromper" className="flex min-w-[44px] flex-col items-center gap-0.5 rounded-2xl bg-white/20 px-1 py-2 text-[11px] font-semibold text-white transition hover:bg-white/30 active:scale-95">
            <span className="text-xl" aria-hidden>✋</span>Parar
          </button>
        )}
      </div>
      <p className="mt-1 text-center text-[11px] text-white/70 drop-shadow">Dica: toque nos móveis do quarto para interagir ✨</p>
    </div>
  );
}
