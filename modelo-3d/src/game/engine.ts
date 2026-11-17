// ============================================================
// GAME ENGINE — relógio, necessidades, máquina de estados, autonomia.
// Separação: comportamento (aqui) / navegação (navigation.ts) /
// reprodução de animação (animationLayer via GameCanvas).
// A lógica NÃO depende de nomes de arquivo de animação.
// ============================================================

import { SIMULATION_CONFIG } from "../config/simulation";
import { NAV_POINTS, findPath } from "./navigation";
import { loadSave, saveGame } from "./persistence";
import { audio } from "./audio";
import type {
  ActivityId, GabiState, GameSnapshot, Needs,
  Outfit, OutfitId, SaveData, Vec2,
} from "./types";

export const OUTFITS: Outfit[] = [
  // Roupas fiéis às referências entregues
  { id: "red-dress", label: "Vestido Vermelho", icon: "👗", colors: { top: "#d92b2b", bottom: "#c1272d", accent: "#d92b2b", boots: "#262626" } },
  { id: "yellow-set", label: "Suéter Amarelo", icon: "🧶", colors: { top: "#e0a11f", bottom: "#2b2b2b", accent: "#e0a11f", boots: "#262626", sleeves: true } },
  { id: "blue-set", label: "Vestido Lilás", icon: "💜", colors: { top: "#b07fe0", bottom: "#a06fd0", accent: "#b07fe0", boots: "#262626" } },
  { id: "pajama", label: "Pijama de Estrelas", icon: "🌙", colors: { top: "#9cc3e8", bottom: "#8ab6e0", accent: "#ffffff", boots: "#cfe0f5", sleeves: true }, isPajama: true },
];

const clamp = (v: number, min = 0, max = 100) => Math.max(min, Math.min(max, v));

export interface ReconcileInfo {
  offlineRealMin: number;
  offlineGameMin: number;
  resumedInBed: boolean;
  message: string;
}

type Listener = (snap: GameSnapshot) => void;

function hourOf(gameMinutes: number): number {
  return Math.floor(gameMinutes / 60) % 24;
}
function minOf(gameMinutes: number): number {
  return Math.floor(gameMinutes % 60);
}

export function phaseOf(gameMinutes: number): { key: string; label: string; icon: string } {
  const h = hourOf(gameMinutes);
  const phases = SIMULATION_CONFIG.clock.phases;
  for (const key of Object.keys(phases)) {
    const p = phases[key];
    if (h >= p.from && h < p.to) return { key, label: p.label, icon: p.icon };
  }
  return { key: "night", label: "Noite", icon: "🌙" };
}

export function formatClock(gameMinutes: number): string {
  const h = hourOf(gameMinutes).toString().padStart(2, "0");
  const m = minOf(gameMinutes).toString().padStart(2, "0");
  return `${h}:${m}`;
}

export class GameEngine {
  snap: GameSnapshot;
  private listeners = new Set<Listener>();
  private path: Vec2[] = [];
  private pathIndex = 0;
  private activityToken = 0;
  private lockUntil = 0;
  private execTimer = 0;
  private execDuration = 0;
  private markersFired = new Set<string>();
  private bubbleTimer = 0;
  private autonomyTimer = 0;
  private lastIdleSwitch = 0;
  private elapsed = 0;
  private autosaveTimer = 0;
  private doorTarget = 0;
  private pendingFood: string | null = null;
  private studyKind: "study" | "read" = "study";
  reconcileInfo: ReconcileInfo | null = null;
  reducedMotion = false;
  clockSpeed = 1;
  /** UI flags observáveis */
  ui = { memoryOpen: false, wardrobeOpen: false, foodOpen: false, studyOpen: false, playOpen: false, helpOpen: false, inspectOpen: false };
  uiVersion = 0;
  currentFoodId(): string { return this.pendingFood ?? "meal"; }
  bumpUI() { this.uiVersion++; this.emit(); }

  constructor() {
    const saved = loadSave();
    if (saved) {
      this.snap = this.fromSave(saved);
      this.reconcileInfo = this.reconcile(saved);
    } else {
      this.snap = {
        gameMinutes: SIMULATION_CONFIG.clock.startHour * 60,
        dayCount: 1,
        needs: { hunger: 78, energy: 85, hygiene: 80, bathroom: 75, fun: 70, learning: 20, mood: 80 },
        xp: 0, level: 0,
        outfit: "red-dress", daytimeOutfit: "red-dress",
        state: "IDLE", activity: "none", activityPhase: "NONE",
        activityProgress: 0, activityLabel: "",
        position: { ...NAV_POINTS.IDLE_1.approach },
        facing: 0.4, isMoving: false, bubble: "Oi! Eu sou a Gabi! 💛",
        doorOpen: 0, gabiInsideBathroom: false,
        autonomyEnabled: true, lastCareAt: 0,
      };
      this.bubbleTimer = 5;
    }
    this.autonomyTimer = 20;
  }

  // ---------- persistência ----------
  private fromSave(s: SaveData): GameSnapshot {
    return {
      gameMinutes: s.gameMinutes, dayCount: s.dayCount,
      needs: { ...s.needs }, xp: s.xp,
      level: this.levelForXp(s.xp),
      outfit: s.outfit, daytimeOutfit: s.daytimeOutfit,
      state: "IDLE", activity: "none", activityPhase: "NONE",
      activityProgress: 0, activityLabel: "",
      position: { ...s.position }, facing: s.facing,
      isMoving: false, bubble: null,
      doorOpen: 0, gabiInsideBathroom: false,
      autonomyEnabled: true, lastCareAt: 0,
    };
  }

  private reconcile(saved: SaveData): ReconcileInfo {
    const now = Date.now();
    const elapsedRealSec = Math.max(0, (now - saved.lastRealTime) / 1000);
    const gameMinPerRealSec = (24 * 60) / (SIMULATION_CONFIG.clock.realMinutesPerGameDay * 60);
    const rawGameMin = elapsedRealSec * gameMinPerRealSec;
    const capMin = SIMULATION_CONFIG.needs.maxOfflineGameHours * 60;
    const appliedGameMin = Math.min(rawGameMin, capMin);
    // Aplica tempo
    let total = saved.gameMinutes + appliedGameMin;
    const daysPassed = Math.floor(total / 1440);
    this.snap.dayCount = saved.dayCount + daysPassed;
    this.snap.gameMinutes = total % (1440 * 365);
    // Deterioração limitada
    const hours = appliedGameMin / 60;
    const decay = SIMULATION_CONFIG.needs.decayPerGameHour;
    const n = this.snap.needs;
    n.hunger = clamp(n.hunger - decay.hunger * hours);
    n.energy = clamp(n.energy - decay.energy * hours);
    n.hygiene = clamp(n.hygiene - decay.hygiene * hours);
    n.bathroom = clamp(n.bathroom - decay.bathroom * hours);
    n.fun = clamp(n.fun - decay.fun * hours);
    n.mood = clamp(n.mood - decay.mood * hours * 0.7);
    // learning não decai offline
    // Retomada noturna: madrugada (0-5h) -> cama, pijama, SLEEP
    const h = hourOf(this.snap.gameMinutes);
    let resumedInBed = false;
    if (h >= 0 && h < 5) {
      this.snap.position = { ...NAV_POINTS.BED_POINT.placement };
      this.snap.facing = NAV_POINTS.BED_POINT.facing;
      if (this.snap.outfit !== "pajama") {
        this.snap.daytimeOutfit = this.snap.outfit;
        this.snap.outfit = "pajama";
      }
      this.snap.state = "SLEEP";
      this.snap.activity = "sleep";
      this.snap.activityPhase = "EXECUTE";
      this.snap.activityLabel = "Dormindo";
      resumedInBed = true;
      this.snap.bubble = "💤 …";
    } else if (saved.activity === "sleep" && h >= 5 && h < 8) {
      // Mantém dormindo no início da manhã se estava dormindo
      this.snap.position = { ...NAV_POINTS.BED_POINT.placement };
      this.snap.state = "SLEEP";
      this.snap.activity = "sleep";
      this.snap.activityPhase = "EXECUTE";
      this.snap.activityLabel = "Dormindo";
      resumedInBed = true;
    } else {
      this.snap.bubble = this.welcomeBack(elapsedRealSec);
      this.bubbleTimer = 6;
    }
    const offlineRealMin = elapsedRealSec / 60;
    return {
      offlineRealMin, offlineGameMin: appliedGameMin, resumedInBed,
      message: resumedInBed
        ? `Era madrugada no quarto — Gabi continuou dormindo. (${this.fmtOffline(offlineRealMin)})`
        : `Bem-vinda de volta! Passaram-se ${this.fmtOffline(offlineRealMin)} fora.`,
    };
  }

  private fmtOffline(realMin: number): string {
    if (realMin < 1) return "alguns segundos";
    if (realMin < 60) return `${Math.floor(realMin)} min`;
    const h = Math.floor(realMin / 60);
    if (h < 24) return `${h}h`;
    return `${Math.floor(h / 24)}d ${h % 24}h`;
  }

  private welcomeBack(elapsedSec: number): string {
    if (elapsedSec < 120) return "Você voltou! 🥰";
    if (elapsedSec < 3600) return "Que bom te ver! O que vamos fazer? ✨";
    return "Senti sua falta! Vamos brincar? 💛";
  }

  save(): SaveData {
    const s = this.snap;
    const data: SaveData = {
      version: 1, gameMinutes: s.gameMinutes, dayCount: s.dayCount,
      needs: { ...s.needs }, xp: s.xp, outfit: s.outfit,
      daytimeOutfit: s.daytimeOutfit,
      state: s.state === "WALK" || s.state === "TURN" ? "IDLE" : s.state,
      activity: s.activityPhase === "EXECUTE" && (s.activity === "sleep") ? "sleep" : "none",
      activityPhase: s.activity === "sleep" && s.activityPhase === "EXECUTE" ? "EXECUTE" : "NONE",
      position: { ...s.position }, facing: s.facing,
      lastRealTime: Date.now(),
    };
    saveGame(data);
    return data;
  }

  // ---------- reatividade ----------
  subscribe(fn: Listener): () => void {
    this.listeners.add(fn);
    return () => { this.listeners.delete(fn); };
  }
  private emit() {
    for (const fn of this.listeners) fn(this.snap);
  }
  get snapshot(): GameSnapshot { return this.snap; }

  setBubble(text: string | null, secs = 4) {
    this.snap.bubble = text;
    this.bubbleTimer = text ? secs : 0;
  }

  levelForXp(xp: number): number {
    const lv = SIMULATION_CONFIG.evolution.xpLevels;
    let level = 0;
    for (let i = 0; i < lv.length; i++) if (xp >= lv[i]) level = i;
    return level;
  }
  phaseName(): string {
    return SIMULATION_CONFIG.evolution.phases[this.snap.level] ?? "Autônoma";
  }

  private addXp(amount: number) {
    const before = this.snap.level;
    this.snap.xp += amount;
    this.snap.level = this.levelForXp(this.snap.xp);
    if (this.snap.level > before) {
      this.setBubble(`Subi de fase: ${this.phaseName()}! 🌟`, 5);
      audio.play("success");
    }
  }

  // ---------- comandos ----------
  /** Guarda anti-clique-duplo e anti-incompatibilidade */
  private canStart(activity: ActivityId): { ok: boolean; reason?: string } {
    const now = this.elapsed;
    if (now < this.lockUntil) return { ok: false, reason: "lock" };
    const s = this.snap;
    // Banheiro em execução não pode ser interrompido
    if (s.activity === "bathroom" && s.activityPhase === "EXECUTE") {
      return { ok: false, reason: "Gabi está no banheiro… 🚪" };
    }
    // Dormindo: só acordar
    if (s.activity === "sleep" && s.activityPhase === "EXECUTE" && activity !== "sleep") {
      return { ok: false, reason: "Gabi está dormindo. Toque em Acordar primeiro. 💤" };
    }
    if (s.activity === activity && s.activityPhase !== "NONE" && s.activityPhase !== "COMPLETE") {
      return { ok: false, reason: "lock" };
    }
    return { ok: true };
  }

  private beginNavigation(activity: ActivityId, label: string, dest: Vec2, state: GabiState = "WALK") {
    this.activityToken++;
    this.markersFired.clear();
    this.snap.activity = activity;
    this.snap.activityPhase = "NAVIGATE";
    this.snap.activityLabel = label;
    this.snap.activityProgress = 0;
    this.snap.state = state;
    this.path = findPath(this.snap.position, dest);
    this.pathIndex = 0;
    this.lockUntil = this.elapsed + 0.6;
    this.emit();
  }

  /** Ponto de interação por atividade (APPROACH). Placement é aplicado na chegada. */
  requestActivity(activity: ActivityId, opts?: { food?: string; study?: "study" | "read" }): boolean {
    if (activity === "read") activity = "study"; // leitura é variação do estudo (studyKind)
    const check = this.canStart(activity);
    if (!check.ok) {
      if (check.reason && check.reason !== "lock") {
        this.setBubble(check.reason, 3);
        audio.play("error");
      }
      return false;
    }
    // Cancela painéis
    this.ui.foodOpen = false; this.ui.studyOpen = false;
    const s = this.snap;
    switch (activity) {
      case "eat":
      case "drink": {
        const foodId = opts?.food ?? (activity === "drink" ? "water" : "meal");
        if (s.needs.hunger > SIMULATION_CONFIG.needs.thresholds.full && !SIMULATION_CONFIG.foods[foodId]?.isDrink) {
          this.setBubble("Estou satisfeita! 🥰", 3);
          audio.play("error");
          return false;
        }
        this.pendingFood = foodId;
        this.beginNavigation(activity, activity === "drink" ? "Beber" : "Comer", NAV_POINTS.FOOD_POINT.approach);
        this.setBubble(activity === "drink" ? "Vou beber algo! 💧" : "Oba, comida! 🍽️", 3);
        audio.play("tap");
        return true;
      }
      case "study": {
        this.studyKind = opts?.study ?? "study";
        if (s.needs.energy < 12) {
          this.setBubble("Estou cansada demais… 😮‍💨", 3);
          audio.play("error");
          return false;
        }
        this.beginNavigation("study", this.studyKind === "read" ? "Ler" : "Estudar", NAV_POINTS.CHAIR_POINT.approach);
        this.setBubble("Hora de estudar! 📚", 3);
        audio.play("tap");
        return true;
      }
      case "play":
      case "memory": {
        this.beginNavigation(activity, activity === "memory" ? "Jogo da memória" : "Brincar", NAV_POINTS.PLAY_POINT.approach);
        this.setBubble("Vamos brincar! 🧸", 3);
        audio.play("tap");
        return true;
      }
      case "sleep": {
        if (s.activity === "sleep" && s.activityPhase === "EXECUTE") return false;
        this.beginNavigation("sleep", "Dormir", NAV_POINTS.BED_POINT.approach);
        this.setBubble("Vou descansar… 🥱", 3);
        audio.play("tap");
        return true;
      }
      case "bathroom": {
        this.beginNavigation("bathroom", "Banheiro", NAV_POINTS.BATHROOM_DOOR_POINT.approach);
        this.setBubble("Preciso ir ao banheiro! 🚻", 3);
        audio.play("tap");
        return true;
      }
      case "wardrobe": {
        this.beginNavigation("wardrobe", "Trocar roupa", NAV_POINTS.WARDROBE_POINT.approach);
        this.setBubble("Vou escolher uma roupa! 👗", 3);
        audio.play("tap");
        return true;
      }
      case "look-window": {
        this.beginNavigation("look-window", "Olhar pela janela", NAV_POINTS.WINDOW_POINT.approach);
        audio.play("tap");
        return true;
      }
      case "idle-wander": {
        const idles = [NAV_POINTS.IDLE_1, NAV_POINTS.IDLE_2, NAV_POINTS.IDLE_3, NAV_POINTS.IDLE_4];
        const pick = idles[Math.floor(Math.random() * idles.length)];
        this.beginNavigation("idle-wander", "Passear", pick.approach);
        return true;
      }
      default:
        return false;
    }
  }

  /** Inspeção: manda Gabi ociosa caminhar até um ponto (testa contato/circulação). */
  debugGoto(pointId: import("./types").NavPointId): boolean {
    const s = this.snap;
    if (s.activity !== "none" || s.state === "WALK" || s.state === "TURN") return false;
    const pt = NAV_POINTS[pointId];
    if (!pt) return false;
    this.beginNavigation("idle-wander", `Inspeção: ${pt.label}`, pt.approach);
    return true;
  }

  /** Toque em objeto do cenário */
  tapObject(objectId: string): void {
    switch (objectId) {
      case "bed": this.requestActivity("sleep"); break;
      case "desk": case "chair": this.ui.studyOpen = true; this.bumpUI(); audio.play("tap"); break;
      case "wardrobe": this.requestActivity("wardrobe"); break;
      case "food-table": this.ui.foodOpen = true; this.bumpUI(); audio.play("tap"); break;
      case "rug": case "toy-chest": this.requestActivity("play"); break;
      case "bathroom-door": this.requestActivity("bathroom"); break;
      case "window": this.requestActivity("look-window"); break;
      case "gabi": this.pokeGabi(); break;
    }
  }

  private pokeGabi() {
    const s = this.snap;
    if (s.activityPhase === "NAVIGATE" || s.activityPhase === "EXECUTE") return;
    const lines = ["Oi! 🥰", "Hehe! ✨", "Vamos brincar? 🧸", "Eu gosto de você! 💛"];
    this.setBubble(lines[Math.floor(Math.random() * lines.length)], 2.5);
    s.state = "HAPPY";
    audio.play("pop");
    this.lastIdleSwitch = this.elapsed;
  }

  wakeUp(): void {
    const s = this.snap;
    if (s.activity !== "sleep" || s.activityPhase !== "EXECUTE") return;
    s.state = "WAKE_UP";
    this.execTimer = 0;
    this.execDuration = 2.2;
    s.activityPhase = "COMPLETE";
    s.activityLabel = "Acordando";
    audio.play("wake");
    // Volta roupa diurna
    if (s.outfit === "pajama") s.outfit = s.daytimeOutfit;
    this.setBubble("Bom dia! ☀️", 4);
  }

  cancelToIdle(reason?: string) {
    const s = this.snap;
    if (s.activity === "bathroom" && s.activityPhase === "EXECUTE") return;
    if (s.activity === "sleep" && s.activityPhase === "EXECUTE") return;
    s.activity = "none"; s.activityPhase = "NONE";
    s.state = "IDLE"; s.activityProgress = 0; s.activityLabel = "";
    s.isMoving = false;
    this.path = [];
    if (reason) this.setBubble(reason, 3);
    this.emit();
  }

  chooseOutfit(id: OutfitId): void {
    const s = this.snap;
    if (s.activity !== "wardrobe") return;
    s.state = "CHANGE_CLOTHES";
    s.activityPhase = "EXECUTE";
    this.execTimer = 0;
    this.execDuration = SIMULATION_CONFIG.activities.wardrobe.durationSec;
    this.pendingFood = id; // reuso como "pendente genérico"
    this.setBubble("Só um minutinho… ✨", 2.5);
    this.ui.wardrobeOpen = false;
    audio.play("pop");
  }

  completeMemory(won: boolean, pairs: number): void {
    const s = this.snap;
    if (s.activity !== "memory") return;
    const cfg = SIMULATION_CONFIG.activities.memoryGame;
    if (won) {
      s.needs.fun = clamp(s.needs.fun + cfg.funGain);
      s.needs.learning = clamp(s.needs.learning + cfg.learningGain);
      s.needs.mood = clamp(s.needs.mood + cfg.moodGain);
      s.needs.energy = clamp(s.needs.energy - cfg.energyCost);
      this.addXp(SIMULATION_CONFIG.evolution.xpPerCare);
      s.state = "HAPPY";
      this.setBubble(`Você achou ${pairs} pares! Incrível! 🎉`, 4);
      audio.play("success");
    } else {
      s.needs.fun = clamp(s.needs.fun + 8);
      s.needs.mood = clamp(s.needs.mood + 4);
      s.state = "IDLE";
      this.setBubble("Quase! Vamos de novo? 🃏", 3);
    }
    s.lastCareAt = this.elapsed;
    this.finishActivity();
    this.ui.memoryOpen = false;
  }

  private finishActivity() {
    const s = this.snap;
    s.activity = "none";
    s.activityPhase = "NONE";
    s.activityProgress = 0;
    s.activityLabel = "";
    if (s.state !== "HAPPY" && s.state !== "IDLE") s.state = "IDLE";
    s.isMoving = false;
    this.path = [];
    this.autonomyTimer = this.autonomyInterval() * 0.5;
    this.emit();
  }

  private autonomyInterval(): number {
    const arr = SIMULATION_CONFIG.evolution.autonomyIntervalSec;
    return arr[Math.min(this.snap.level, arr.length - 1)];
  }

  // ---------- update ----------
  update(dtReal: number): void {
    const s = this.snap;
    this.elapsed += dtReal;
    const cfg = SIMULATION_CONFIG;

    // Relógio: 1 dia de jogo a cada N min reais
    const gameMinPerRealSec = (24 * 60) / (cfg.clock.realMinutesPerGameDay * 60);
    const dGameMin = dtReal * gameMinPerRealSec * this.clockSpeed;
    s.gameMinutes += dGameMin;
    if (s.gameMinutes >= 1440 * 365) s.gameMinutes %= 1440 * 365;
    const newDay = Math.floor((s.gameMinutes / 1440)) + 1;
    if (newDay !== s.dayCount) {
      s.dayCount = newDay;
      // Novo dia: pequena recuperação de higiene/banheiro (rotina matinal implícita)
      s.needs.hygiene = clamp(s.needs.hygiene + 10);
    }

    // Decaimento das necessidades
    const dHours = dGameMin / 60;
    const sleeping = s.activity === "sleep" && s.activityPhase === "EXECUTE";
    const decay = cfg.needs.decayPerGameHour;
    const hungerMult = sleeping ? cfg.activities.sleepTick.hungerDecayMult : 1;
    s.needs.hunger = clamp(s.needs.hunger - decay.hunger * dHours * hungerMult);
    s.needs.energy = clamp(s.needs.energy - (sleeping ? 0 : decay.energy * dHours));
    s.needs.hygiene = clamp(s.needs.hygiene - decay.hygiene * dHours);
    s.needs.bathroom = clamp(s.needs.bathroom - decay.bathroom * dHours);
    s.needs.fun = clamp(s.needs.fun - decay.fun * dHours);
    s.needs.learning = clamp(s.needs.learning - decay.learning * dHours * 0.5);
    s.needs.mood = clamp(s.needs.mood - decay.mood * dHours * this.moodPressure());
    // Humor deriva das demais
    const avg = (s.needs.hunger + s.needs.energy + s.needs.fun + s.needs.bathroom) / 4;
    if (avg < 30) s.needs.mood = clamp(s.needs.mood - dHours * 3);
    else if (avg > 70) s.needs.mood = clamp(s.needs.mood + dHours * 2);

    // Sono recupera energia
    if (sleeping) {
      s.needs.energy = clamp(s.needs.energy + cfg.activities.sleepTick.energyPerSec * dtReal);
      if (s.needs.energy >= 100) {
        // Acorda sozinha quando descansada (fora da madrugada profunda)
        const h = hourOf(s.gameMinutes);
        if (h >= 5) this.wakeUp();
      }
    }
    // Madrugada: tende a dormir sozinha
    const h = hourOf(s.gameMinutes);
    if (h >= 0 && h < 5 && s.activity === "none" && s.state === "IDLE") {
      this.autonomyTimer -= dtReal * 3; // acelera decisão de dormir
    }

    // Bolha
    if (this.bubbleTimer > 0) {
      this.bubbleTimer -= dtReal;
      if (this.bubbleTimer <= 0) { s.bubble = null; }
    }

    // Porta do banheiro (animação suave; reduzida se reducedMotion)
    const doorSpeed = this.reducedMotion ? 10 : 2.2;
    const cur = s.doorOpen;
    const tgt = this.doorTarget;
    if (Math.abs(cur - tgt) > 0.001) {
      s.doorOpen = clamp(cur + Math.sign(tgt - cur) * doorSpeed * dtReal, 0, 1);
    }

    // Navegação
    if (s.activityPhase === "NAVIGATE") this.updateNavigation(dtReal);

    // Execução
    if (s.activityPhase === "EXECUTE") this.updateExecute(dtReal);
    if (s.activityPhase === "COMPLETE") this.updateComplete(dtReal);

    // Autonomia acolhedora
    if (s.autonomyEnabled && this.ui) {
      this.autonomyTimer -= dtReal;
      if (this.autonomyTimer <= 0) {
        this.autonomyTimer = this.autonomyInterval();
        this.autonomyTick();
      }
    }

    // Estados reativos visuais quando ociosa (fome/cansaço/tédio)
    if (s.activity === "none" && s.state === "IDLE" && this.elapsed - this.lastIdleSwitch > 6) {
      if (s.needs.hunger < 18) { s.state = "HUNGRY"; this.setBubble("Estou com fome… 🍎", 3); this.lastIdleSwitch = this.elapsed; }
      else if (s.needs.energy < 18) { s.state = "TIRED"; this.setBubble("Bocejo… preciso descansar 🥱", 3); this.lastIdleSwitch = this.elapsed; }
      else if (s.needs.fun < 18) { s.state = "BORED"; this.setBubble("Estou entediada… 🧸", 3); this.lastIdleSwitch = this.elapsed; }
    }
    // Volta de reação para IDLE
    if ((s.state === "HUNGRY" || s.state === "TIRED" || s.state === "BORED" || s.state === "HAPPY") && s.activity === "none") {
      if (this.elapsed - this.lastIdleSwitch > 3.2) s.state = "IDLE";
    }

    // Autosave
    this.autosaveTimer += dtReal;
    if (this.autosaveTimer > cfg.save.autosaveSec) {
      this.autosaveTimer = 0;
      this.save();
    }

    this.emit();
  }

  private moodPressure(): number {
    const s = this.snap;
    let p = 1;
    if (s.needs.bathroom < 20) p += 1.2;
    if (s.needs.hunger < 20) p += 0.8;
    return p;
  }

  private updateNavigation(dt: number) {
    const s = this.snap;
    const speed = SIMULATION_CONFIG.navigation.walkSpeed * (this.reducedMotion ? 1.15 : 1);
    if (this.pathIndex >= this.path.length) {
      this.onArrived();
      return;
    }
    s.isMoving = true;
    const target = this.path[this.pathIndex];
    const dx = target.x - s.position.x;
    const dz = target.z - s.position.z;
    const dist = Math.hypot(dx, dz);
    // Giro: se muito desalinhada, vira no lugar (estado TURN) antes de andar
    const desired = Math.atan2(dx, dz);
    let diff = desired - s.facing;
    while (diff > Math.PI) diff -= Math.PI * 2;
    while (diff < -Math.PI) diff += Math.PI * 2;
    if (Math.abs(diff) > 0.85 && dist > 0.25) {
      s.state = "TURN";
      s.facing = this.turnToward(s.facing, desired, SIMULATION_CONFIG.navigation.turnSpeed * 1.2 * dt);
      return;
    }
    s.state = "WALK";
    s.facing = this.turnToward(s.facing, desired, SIMULATION_CONFIG.navigation.turnSpeed * dt);
    const step = speed * dt;
    if (dist <= Math.max(step, SIMULATION_CONFIG.navigation.arriveDistance)) {
      s.position = { ...target };
      this.pathIndex++;
      if (this.pathIndex % 3 === 0) audio.play("step");
      if (this.pathIndex >= this.path.length) this.onArrived();
    } else {
      s.position.x += (dx / dist) * step;
      s.position.z += (dz / dist) * step;
    }
  }

  private turnToward(from: number, to: number, maxStep: number): number {
    let d = to - from;
    while (d > Math.PI) d -= Math.PI * 2;
    while (d < -Math.PI) d += Math.PI * 2;
    if (Math.abs(d) <= maxStep) return to;
    return from + Math.sign(d) * maxStep;
  }

  /** Marcador "arrived" — preparação da interação por atividade */
  private onArrived() {
    const s = this.snap;
    s.isMoving = false;
    this.fireMarker("arrived");
    switch (s.activity) {
      case "eat":
      case "drink": {
        s.activityPhase = "EXECUTE";
        s.state = "SIT";
        // posiciona no banquinho da mesinha
        s.position = { ...NAV_POINTS.FOOD_POINT.placement };
        s.facing = NAV_POINTS.FOOD_POINT.facing;
        const food = SIMULATION_CONFIG.foods[this.pendingFood ?? "meal"];
        this.execDuration = food?.durationSec ?? 6;
        this.execTimer = 0;
        audio.play("pop");
        break;
      }
      case "study": {
        s.activityPhase = "EXECUTE";
        s.state = "SIT";
        s.position = { ...NAV_POINTS.CHAIR_POINT.placement };
        s.facing = NAV_POINTS.CHAIR_POINT.facing;
        const kind = this.studyKind;
        this.execDuration = kind === "read"
          ? SIMULATION_CONFIG.activities.read.durationSec
          : SIMULATION_CONFIG.activities.study.durationSec;
        this.execTimer = 0;
        this.fireMarker("sat");
        audio.play("page");
        break;
      }
      case "play": {
        s.activityPhase = "EXECUTE";
        s.state = "SIT";
        s.position = { ...NAV_POINTS.PLAY_POINT.placement };
        s.facing = NAV_POINTS.PLAY_POINT.facing;
        this.execDuration = SIMULATION_CONFIG.activities.playFree.durationSec;
        this.execTimer = 0;
        this.fireMarker("sat");
        break;
      }
      case "memory": {
        s.activityPhase = "EXECUTE";
        s.state = "SIT";
        s.position = { ...NAV_POINTS.PLAY_POINT.placement };
        s.facing = NAV_POINTS.PLAY_POINT.facing;
        this.fireMarker("sat");
        this.ui.memoryOpen = true; // modal do jogo da memória
        this.execTimer = 0;
        this.execDuration = 1e9; // aguarda conclusão via completeMemory
        break;
      }
      case "sleep": {
        s.activityPhase = "EXECUTE";
        s.state = "SLEEP";
        s.position = { ...NAV_POINTS.BED_POINT.placement };
        s.facing = NAV_POINTS.BED_POINT.facing;
        if (s.outfit !== "pajama") {
          s.daytimeOutfit = s.outfit;
          s.outfit = "pajama";
        }
        this.fireMarker("liedDown");
        this.setBubble("Boa noite… 💤", 4);
        audio.play("sleep");
        this.execTimer = 0;
        this.execDuration = 1e9; // até acordar
        break;
      }
      case "bathroom": {
        // Sequência com porta: alcançar -> abrir -> entrar
        s.activityPhase = "EXECUTE";
        s.state = "BATHROOM";
        s.position = { ...NAV_POINTS.BATHROOM_DOOR_POINT.placement };
        s.facing = NAV_POINTS.BATHROOM_DOOR_POINT.facing;
        this.execTimer = 0;
        this.execDuration = SIMULATION_CONFIG.activities.bathroom.durationSec + 4.5; // inclui porta
        this.doorTarget = 1;
        audio.play("door");
        this.setBubble(null);
        break;
      }
      case "wardrobe": {
        s.activityPhase = "EXECUTE";
        s.state = "WARDROBE";
        s.position = { ...NAV_POINTS.WARDROBE_POINT.placement };
        s.facing = NAV_POINTS.WARDROBE_POINT.facing;
        this.execTimer = 0;
        this.execDuration = 1e9; // aguarda escolha
        this.ui.wardrobeOpen = true;
        audio.play("open");
        break;
      }
      case "look-window": {
        s.activityPhase = "EXECUTE";
        s.state = "IDLE";
        s.position = { ...NAV_POINTS.WINDOW_POINT.placement };
        s.facing = NAV_POINTS.WINDOW_POINT.facing;
        this.execTimer = 0;
        this.execDuration = 6;
        const ph = phaseOf(s.gameMinutes);
        this.setBubble(ph.key.includes("night") || ph.key === "night" || ph.key === "lateNight" ? "Olha as estrelas! ✨" : "Que dia lindo! 🌈", 4);
        break;
      }
      case "idle-wander": {
        this.finishActivity();
        break;
      }
      default:
        this.finishActivity();
    }
    this.emit();
  }

  private fireMarker(id: string) {
    if (this.markersFired.has(id)) return; // cada efeito ocorre uma única vez
    this.markersFired.add(id);
  }

  private updateExecute(dt: number) {
    const s = this.snap;
    this.execTimer += dt;
    const t = this.execTimer;
    const dur = this.execDuration;
    s.activityProgress = dur > 1e8 ? 0 : clamp(t / dur, 0, 1);

    switch (s.activity) {
      case "eat": {
        s.state = "EAT";
        if (t > 0.6) this.fireMarker("grabbed");
        if (t > 1.4) this.fireMarker("foodToMouth");
        if (Math.floor(t * 1.2) !== Math.floor((t - dt) * 1.2)) audio.play("eat");
        if (t >= dur) {
          const food = SIMULATION_CONFIG.foods[this.pendingFood ?? "meal"];
          if (food) {
            s.needs.hunger = clamp(s.needs.hunger + (food.hunger ?? 0));
            s.needs.energy = clamp(s.needs.energy + (food.energy ?? 0));
            s.needs.mood = clamp(s.needs.mood + (food.mood ?? 0));
            s.needs.fun = clamp(s.needs.fun + (food.fun ?? 0));
            if (food.hygiene) s.needs.hygiene = clamp(s.needs.hygiene + food.hygiene);
            this.addXp(SIMULATION_CONFIG.evolution.xpPerCare);
            s.lastCareAt = this.elapsed;
          }
          this.setBubble("Hmm, delícia! 😋", 3);
          audio.play("success");
          s.state = "STAND_UP";
          s.activityPhase = "COMPLETE";
          this.execTimer = 0; this.execDuration = 0.9;
        }
        break;
      }
      case "drink": {
        s.state = "DRINK";
        if (t > 0.6) this.fireMarker("grabbed");
        if (t > 1.2) this.fireMarker("foodToMouth");
        if (t >= dur) {
          const food = SIMULATION_CONFIG.foods[this.pendingFood ?? "water"];
          if (food) {
            s.needs.hunger = clamp(s.needs.hunger + (food.hunger ?? 0));
            s.needs.energy = clamp(s.needs.energy + (food.energy ?? 0));
            s.needs.mood = clamp(s.needs.mood + (food.mood ?? 0));
            this.addXp(Math.floor(SIMULATION_CONFIG.evolution.xpPerCare / 2));
            s.lastCareAt = this.elapsed;
          }
          this.setBubble("Refrescante! 💧", 3);
          audio.play("success");
          s.state = "STAND_UP";
          s.activityPhase = "COMPLETE";
          this.execTimer = 0; this.execDuration = 0.9;
        }
        break;
      }
      case "study": {
        const kind = this.studyKind;
        s.state = kind === "read" ? "READ" : (Math.floor(t / 4) % 2 === 0 ? "WRITE" : "STUDY");
        if (Math.floor(t / 5) !== Math.floor((t - dt) / 5)) audio.play("page");
        if (t >= dur) {
          const cfg = kind === "read" ? SIMULATION_CONFIG.activities.read : SIMULATION_CONFIG.activities.study;
          s.needs.learning = clamp(s.needs.learning + cfg.learningGain);
          s.needs.energy = clamp(s.needs.energy - cfg.energyCost);
          s.needs.mood = clamp(s.needs.mood + cfg.moodGain);
          this.addXp(SIMULATION_CONFIG.evolution.xpPerCare);
          s.lastCareAt = this.elapsed;
          this.setBubble(kind === "read" ? "Que história legal! 📖" : "Aprendi tanto! ⭐", 3.5);
          audio.play("success");
          s.state = "STAND_UP";
          s.activityPhase = "COMPLETE";
          this.execTimer = 0; this.execDuration = 0.9;
        }
        break;
      }
      case "play": {
        s.state = "PLAY";
        if (t >= dur) {
          const cfg = SIMULATION_CONFIG.activities.playFree;
          s.needs.fun = clamp(s.needs.fun + cfg.funGain);
          s.needs.mood = clamp(s.needs.mood + cfg.moodGain);
          s.needs.energy = clamp(s.needs.energy - cfg.energyCost);
          this.addXp(SIMULATION_CONFIG.evolution.xpPerCare);
          s.lastCareAt = this.elapsed;
          s.state = "HAPPY";
          this.setBubble("Isso foi divertido! 🎉", 3.5);
          audio.play("success");
          s.activityPhase = "COMPLETE";
          this.execTimer = 0; this.execDuration = 1.6;
        }
        break;
      }
      case "memory": {
        s.state = "PLAY";
        // Aguarda UI chamar completeMemory. Impede travamento: nada expira.
        s.activityProgress = 0;
        break;
      }
      case "sleep": {
        s.state = "SLEEP";
        s.activityProgress = s.needs.energy / 100;
        break;
      }
      case "bathroom": {
        s.state = "BATHROOM";
        // Linha do tempo: 0-1.1 abrir | 1.1-2.3 entrar | 2.3-(dur-2.1) oculto | fim sair+fechar
        const openEnd = 1.1, enterEnd = 2.3;
        const exitStart = dur - 2.1, closeStart = dur - 0.9;
        if (t >= openEnd) this.fireMarker("doorOpened");
        if (t >= enterEnd && !s.gabiInsideBathroom) {
          s.gabiInsideBathroom = true;
          this.fireMarker("entered");
          this.doorTarget = 0; // fecha durante a atividade (privacidade)
          audio.play("door");
          this.setBubble("🚪 …", 2);
        }
        if (t >= exitStart && s.gabiInsideBathroom) {
          s.gabiInsideBathroom = false;
          this.doorTarget = 1;
          audio.play("door");
        }
        if (t >= closeStart && this.doorTarget === 1) {
          this.doorTarget = 0;
        }
        s.activityProgress = clamp(t / dur, 0, 1);
        if (t >= dur) {
          const cfg = SIMULATION_CONFIG.activities.bathroom;
          s.needs.bathroom = clamp(s.needs.bathroom + cfg.bathroomRestore);
          s.needs.hygiene = clamp(s.needs.hygiene + cfg.hygieneRestore);
          s.needs.mood = clamp(s.needs.mood + 6);
          this.addXp(SIMULATION_CONFIG.evolution.xpPerCare);
          s.lastCareAt = this.elapsed;
          this.fireMarker("finished");
          this.setBubble("Lavei as mãozinhas! 🧼✨", 3.5);
          audio.play("success");
          // volta ao ponto de aproximação
          s.position = { ...NAV_POINTS.BATHROOM_DOOR_POINT.approach };
          s.state = "IDLE";
          this.finishActivity();
        }
        break;
      }
      case "wardrobe": {
        if (s.state === "CHANGE_CLOTHES") {
          s.activityProgress = clamp(t / dur, 0, 1);
          if (t >= dur) {
            const id = (this.pendingFood as OutfitId | null) ?? "red-dress";
            s.outfit = id;
            if (!OUTFITS.find((o) => o.id === id)?.isPajama) s.daytimeOutfit = id;
            s.needs.mood = clamp(s.needs.mood + 8);
            this.addXp(Math.floor(SIMULATION_CONFIG.evolution.xpPerCare / 2));
            s.lastCareAt = this.elapsed;
            this.setBubble("Como estou? ✨", 3.5);
            audio.play("success");
            s.state = "IDLE";
            this.finishActivity();
          }
        } else {
          // aguardando escolha no painel (sem timeout — jogador decide)
          s.activityProgress = 0;
        }
        break;
      }
      case "look-window": {
        s.state = "IDLE";
        if (t >= dur) {
          s.needs.mood = clamp(s.needs.mood + 3);
          this.finishActivity();
        }
        break;
      }
    }
  }

  private updateComplete(dt: number) {
    const s = this.snap;
    // Fase de conclusão: levantar / acordar
    if (s.activity === "sleep" && s.state === "WAKE_UP") {
      this.execTimer += dt;
      s.activityProgress = clamp(this.execTimer / this.execDuration, 0, 1);
      if (this.execTimer >= this.execDuration) {
        s.position = { ...NAV_POINTS.BED_POINT.approach };
        s.state = "IDLE";
        this.finishActivity();
      }
      return;
    }
    this.execTimer += dt;
    if (this.execTimer >= this.execDuration) {
      this.fireMarker("stoodUp");
      s.state = "IDLE";
      // Afasta-se do móvel para o ponto de aproximação correspondente
      const back: Partial<Record<ActivityId, Vec2>> = {
        eat: NAV_POINTS.FOOD_POINT.approach,
        drink: NAV_POINTS.FOOD_POINT.approach,
        study: NAV_POINTS.CHAIR_POINT.approach,
        play: NAV_POINTS.PLAY_POINT.approach,
      };
      const dest = back[s.activity];
      if (dest) s.position = { ...dest };
      this.finishActivity();
    }
  }

  // ---------- autonomia ----------
  private autonomyTick() {
    const s = this.snap;
    if (!s.autonomyEnabled) return;
    if (s.activity !== "none" || s.state === "WALK") return;
    if (this.ui.memoryOpen || this.ui.wardrobeOpen || this.ui.foodOpen) return;
    // Não interrompe jogador recente (60s de sossego após cuidado)
    if (this.elapsed - s.lastCareAt < 8) return;
    const chance = SIMULATION_CONFIG.evolution.autonomyChance[Math.min(s.level, 3)];
    if (Math.random() > chance) return;

    const h = hourOf(s.gameMinutes);
    const n = s.needs;
    // Madrugada -> dormir
    if (h >= 0 && h < 5) {
      this.requestActivity("sleep");
      return;
    }
    // Prioridades por necessidade
    const options: Array<{ score: number; act: ActivityId; opts?: { food?: string } }> = [];
    if (n.bathroom < 30) options.push({ score: 100 - n.bathroom + 30, act: "bathroom" });
    if (n.hunger < 45) options.push({ score: 100 - n.hunger + 10, act: "eat", opts: { food: "meal" } });
    if (n.energy < 25) {
      if (h >= 20 || h < 7) options.push({ score: 100 - n.energy + 25, act: "sleep" });
      else options.push({ score: 60 - n.energy, act: "sleep" });
    }
    if (n.fun < 40) options.push({ score: 90 - n.fun, act: "play" });
    if (n.energy > 40 && n.fun > 30 && (h >= 8 && h <= 18)) {
      options.push({ score: 30 + (50 - n.learning) * 0.4, act: "study" });
    }
    if (n.mood < 50) options.push({ score: 55 - n.mood, act: "look-window" });
    // Passeio ocioso com peso baixo (variedade controlada)
    options.push({ score: 12, act: "idle-wander" });

    options.sort((a, b) => b.score - a.score);
    const best = options[0];
    if (best && best.score > 14) {
      this.requestActivity(best.act, best.opts);
    } else if (Math.random() < 0.3) {
      this.requestActivity("idle-wander");
    }
  }

  needStatus(v: number): "ok" | "low" | "critical" {
    const t = SIMULATION_CONFIG.needs.thresholds;
    if (v < t.critical) return "critical";
    if (v < t.low) return "low";
    return "ok";
  }

  needsList(): Array<{ key: keyof Needs; label: string; icon: string; value: number }> {
    const n = this.snap.needs;
    return [
      { key: "hunger", label: "Fome", icon: "🍎", value: n.hunger },
      { key: "energy", label: "Energia", icon: "⚡", value: n.energy },
      { key: "hygiene", label: "Higiene", icon: "🧼", value: n.hygiene },
      { key: "bathroom", label: "Banheiro", icon: "🚻", value: n.bathroom },
      { key: "fun", label: "Diversão", icon: "🧸", value: n.fun },
      { key: "learning", label: "Estudo", icon: "📚", value: n.learning },
      { key: "mood", label: "Humor", icon: "💛", value: n.mood },
    ];
  }
}

// Labels de estado para inspeção
export const STATE_LABELS: Record<GabiState, string> = {
  IDLE: "Parada", WALK: "Andando", TURN: "Virando", SIT: "Sentada",
  STAND_UP: "Levantando", EAT: "Comendo", DRINK: "Bebendo",
  STUDY: "Estudando", READ: "Lendo", WRITE: "Escrevendo", PLAY: "Brincando",
  SLEEP: "Dormindo", WAKE_UP: "Acordando", WARDROBE: "No guarda-roupa",
  CHANGE_CLOTHES: "Trocando roupa", BATHROOM: "No banheiro",
  HAPPY: "Feliz", TIRED: "Cansada", HUNGRY: "Faminta", BORED: "Entediada",
};
