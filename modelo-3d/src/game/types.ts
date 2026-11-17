// ============================================================
// Tipos centrais do minigame da Gabi
// ============================================================

export type GabiState =
  | "IDLE" | "WALK" | "TURN" | "SIT" | "STAND_UP"
  | "EAT" | "DRINK" | "STUDY" | "READ" | "WRITE"
  | "PLAY" | "SLEEP" | "WAKE_UP" | "WARDROBE" | "CHANGE_CLOTHES"
  | "BATHROOM" | "HAPPY" | "TIRED" | "HUNGRY" | "BORED";

export type ActivityPhase = "PREPARE" | "NAVIGATE" | "EXECUTE" | "COMPLETE" | "NONE";

export type ActivityId =
  | "none" | "eat" | "drink" | "study" | "read" | "play" | "memory"
  | "sleep" | "bathroom" | "wardrobe" | "idle-wander" | "look-window";

export type NavPointId =
  | "BED_POINT" | "DESK_POINT" | "CHAIR_POINT" | "WARDROBE_POINT"
  | "FOOD_POINT" | "PLAY_POINT" | "BATHROOM_DOOR_POINT" | "WINDOW_POINT"
  | "IDLE_1" | "IDLE_2" | "IDLE_3" | "IDLE_4";

export interface Vec2 { x: number; z: number }

export interface NavPoint {
  id: NavPointId;
  /** ponto de aproximação (onde Gabi para antes de interagir) */
  approach: Vec2;
  /** ponto de posicionamento final (ex.: sentada na cadeira) */
  placement: Vec2;
  /** direção que Gabi deve olhar (radianos, 0 = +Z) */
  facing: number;
  label: string;
}

export type MarkerId =
  | "arrived" | "sat" | "grabbed" | "foodToMouth" | "doorOpened"
  | "entered" | "liedDown" | "finished" | "stoodUp";

export interface Needs {
  hunger: number; energy: number; hygiene: number;
  bathroom: number; fun: number; learning: number; mood: number;
}

export type OutfitId = "red-dress" | "pajama" | "blue-set" | "yellow-set";

export interface Outfit {
  id: OutfitId;
  label: string;
  icon: string;
  colors: { top: string; bottom: string; accent: string; boots: string; sleeves?: boolean };
  isPajama?: boolean;
}

export interface GameSnapshot {
  gameMinutes: number; // minutos desde 00:00 do dia atual (acumula dias)
  dayCount: number;
  needs: Needs;
  xp: number;
  level: number;
  outfit: OutfitId;
  daytimeOutfit: OutfitId;
  state: GabiState;
  activity: ActivityId;
  activityPhase: ActivityPhase;
  activityProgress: number; // 0..1 na fase EXECUTE
  activityLabel: string;
  position: Vec2;
  facing: number;
  isMoving: boolean;
  bubble: string | null;
  doorOpen: number; // 0 fechada .. 1 aberta
  gabiInsideBathroom: boolean;
  autonomyEnabled: boolean;
  lastCareAt: number;
}

export interface SaveData {
  version: 1;
  gameMinutes: number;
  dayCount: number;
  needs: Needs;
  xp: number;
  outfit: OutfitId;
  daytimeOutfit: OutfitId;
  state: GabiState;
  activity: ActivityId;
  activityPhase: ActivityPhase;
  position: Vec2;
  facing: number;
  lastRealTime: number;
}

export interface GameSettings {
  muted: boolean;
  reducedMotion: boolean;
  autonomy: boolean;
  clockSpeed: number;
}
