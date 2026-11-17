// ============================================================
// GABI MINIGAME — Configuração central da simulação
// Único ponto de ajuste para tempo, durações, necessidades e evolução.
// ============================================================

export const SIMULATION_CONFIG = {
  // ---- Relógio ----
  clock: {
    /** Minutos reais para 1 dia de jogo (24h). Padrão: 24 min */
    realMinutesPerGameDay: 24,
    /** Velocidade multiplicadora (1 = normal). Inspeção pode acelerar. */
    speed: 1,
    /** Hora inicial de um novo jogo (8h = manhã) */
    startHour: 8,
    /** Fases do dia (hora inicial, hora final) */
    phases: {
      dawn: { from: 5, to: 7, label: "Madrugada", icon: "🌅" },
      morning: { from: 7, to: 12, label: "Manhã", icon: "☀️" },
      day: { from: 12, to: 17, label: "Dia", icon: "🌤️" },
      evening: { from: 17, to: 20, label: "Fim de tarde", icon: "🌇" },
      night: { from: 20, to: 24, label: "Noite", icon: "🌙" },
      lateNight: { from: 0, to: 5, label: "Madrugada", icon: "🌌" },
    } as Record<string, { from: number; to: number; label: string; icon: string }>,
  },

  // ---- Navegação ----
  navigation: {
    /** Raio da personagem (m) — infla obstáculos */
    characterRadius: 0.26,
    /** Velocidade de caminhada (m/s) */
    walkSpeed: 1.35,
    /** Velocidade de giro (rad/s) */
    turnSpeed: 5.2,
    /** Tamanho da célula do grid A* (m) */
    cellSize: 0.2,
    /** Distância de chegada (m) */
    arriveDistance: 0.12,
  },

  // ---- Necessidades (0–100) ----
  needs: {
    /** Decaimento por hora de jogo */
    decayPerGameHour: {
      hunger: 4.2, // fome cai -> precisa comer
      energy: 3.4,
      hygiene: 2.2,
      bathroom: 5.0, // necessidade de banheiro sobe? modelado como "conforto" que cai
      fun: 5.2,
      learning: 1.1,
      mood: 2.8,
    },
    /** Limiares de alerta */
    thresholds: { low: 30, critical: 15, full: 85 },
    /** Cap de deterioração offline (horas de jogo simuladas no retorno) */
    maxOfflineGameHours: 10,
  },

  // ---- Alimentação ----
  foods: {
    pizza: { label: "Pizza", icon: "🍕", hunger: 28, energy: 6, mood: 10, fun: 4, durationSec: 6 },
    fruits: { label: "Frutas", icon: "🍎", hunger: 18, energy: 8, mood: 6, hygiene: 2, durationSec: 5 },
    sandwich: { label: "Sanduíche", icon: "🥪", hunger: 22, energy: 7, mood: 7, durationSec: 5 },
    meal: { label: "Refeição", icon: "🍲", hunger: 34, energy: 10, mood: 8, durationSec: 7 },
    cake: { label: "Bolo", icon: "🍰", hunger: 14, energy: 4, mood: 14, fun: 8, durationSec: 5 },
    water: { label: "Água", icon: "💧", hunger: 4, energy: 5, mood: 3, isDrink: true, durationSec: 3 },
    juice: { label: "Suco", icon: "🧃", hunger: 8, energy: 6, mood: 8, fun: 3, isDrink: true, durationSec: 3 },
  } as Record<string, { label: string; icon: string; hunger?: number; energy?: number; mood?: number; fun?: number; hygiene?: number; durationSec: number; isDrink?: boolean }>,

  // ---- Durações das atividades (segundos reais, provisórios) ----
  activities: {
    study: { durationSec: 20, energyCost: 12, learningGain: 22, moodGain: 4 },
    read: { durationSec: 12, energyCost: 5, learningGain: 12, moodGain: 5 },
    playFree: { durationSec: 15, energyCost: 8, funGain: 24, moodGain: 10 },
    memoryGame: { energyCost: 6, funGain: 26, learningGain: 10, moodGain: 12 },
    sleepTick: { energyPerSec: 2.2, hungerDecayMult: 0.4 },
    bathroom: { durationSec: 8, bathroomRestore: 70, hygieneRestore: 35 },
    wardrobe: { durationSec: 4 },
    markers: {
      // Durações provisórias dos marcadores (s) — substituídas por eventos de clipe GLB
      arrive: 0,
      sit: 0.9,
      grabObject: 0.6,
      foodToMouth: 0.8,
      openDoor: 1.1,
      enter: 1.2,
      lieDown: 1.4,
      finish: 0.4,
      standUp: 0.9,
    },
  },

  // ---- Evolução / Autonomia ----
  evolution: {
    xpPerCare: 20,
    xpLevels: [0, 100, 260, 480],
    phases: ["Dependente", "Curiosa", "Exploradora", "Autônoma"],
    /** Intervalo entre decisões autônomas (s reais) por fase */
    autonomyIntervalSec: [75, 55, 40, 30],
    /** Probabilidade de iniciar comportamento autônomo quando elegível */
    autonomyChance: [0.45, 0.6, 0.75, 0.9],
  },

  // ---- Persistência ----
  save: {
    key: "gabi-save-v1",
    settingsKey: "gabi-settings-v1",
    autosaveSec: 5,
  },
} as const;

export type FoodId = keyof typeof SIMULATION_CONFIG.foods;
export type ClockPhaseKey = keyof typeof SIMULATION_CONFIG.clock.phases;
