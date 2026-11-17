// ============================================================
// CATÁLOGO DE ANIMAÇÕES — Manifesto de integração externa
// Cada clipe tem: identificador, arquivo esperado, duração, repetição,
// transições, marcadores e disponibilidade.
// HOJE: todos "pending" (provisório procedural). Importar GLB = atualizar
// este arquivo + colocar o .glb em public/animations/. Nenhuma lógica de
// atividade depende de nomes de arquivo — apenas de clipId.
// ============================================================

import type { GabiState, MarkerId } from "./types";

export interface AnimClip {
  /** Identificador estável usado pela AnimationLayer e pela simulação */
  id: string;
  group: "rest" | "locomotion" | "posture" | "feeding" | "study" | "play" | "sleep" | "wardrobe" | "bathroom" | "reactions";
  label: string;
  /** Arquivo GLB/glTF esperado (relativo a public/). null = ainda não produzido */
  file: string | null;
  /** Nome do take dentro do GLB (quando houver) */
  take?: string;
  /** Duração em segundos (provisória até o clipe real) */
  duration: number;
  loop: boolean;
  /** Para qual clipe transiciona automaticamente */
  next?: string;
  /** Tempo de crossfade (s) */
  fadeIn: number;
  fadeOut: number;
  /** Estados da máquina que usam este clipe */
  states: GabiState[];
  /** Marcadores disparados pelo clipe */
  markers: Array<{ at: number; marker: MarkerId | "custom"; note: string }>;
  /** Disponibilidade */
  status: "ready" | "pending";
  /** Adaptações necessárias */
  adaptations?: string[];
  /** Notas de produção externa */
  notes?: string;
}

export const ANIMATION_MANIFEST: AnimClip[] = [
  // ---- REPOUSO ----
  { id: "idle_breathe", group: "rest", label: "Respirar parada", file: null, duration: 3.2, loop: true, fadeIn: 0.4, fadeOut: 0.3, states: ["IDLE"], markers: [], status: "pending", notes: "In-place. Loop perfeito. Escala do peito sutil." },
  { id: "idle_blink", group: "rest", label: "Piscar", file: null, duration: 0.18, loop: false, fadeIn: 0.05, fadeOut: 0.05, states: ["IDLE"], markers: [], status: "pending", notes: "Camada aditiva sobre qualquer idle. A cada 2–5s." },
  { id: "idle_look_around", group: "rest", label: "Olhar ao redor", file: null, duration: 4.0, loop: false, next: "idle_breathe", fadeIn: 0.4, fadeOut: 0.4, states: ["IDLE", "BORED"], markers: [], status: "pending" },
  { id: "idle_fix_hair", group: "rest", label: "Ajustar cabelo", file: null, duration: 2.6, loop: false, next: "idle_breathe", fadeIn: 0.3, fadeOut: 0.3, states: ["IDLE"], markers: [], status: "pending", adaptations: ["penteado"] },
  { id: "idle_fix_dress", group: "rest", label: "Ajustar vestido", file: null, duration: 2.4, loop: false, next: "idle_breathe", fadeIn: 0.3, fadeOut: 0.3, states: ["IDLE"], markers: [], status: "pending", adaptations: ["roupa: saia/vestido vs. conjunto"] },
  { id: "idle_check_nails", group: "rest", label: "Observar unhas", file: null, duration: 3.0, loop: false, next: "idle_breathe", fadeIn: 0.3, fadeOut: 0.3, states: ["IDLE", "BORED"], markers: [], status: "pending" },

  // ---- LOCOMOÇÃO (todos in-place; deslocamento pela navegação) ----
  { id: "walk", group: "locomotion", label: "Caminhar", file: null, duration: 0.9, loop: true, fadeIn: 0.25, fadeOut: 0.25, states: ["WALK"], markers: [], status: "pending", notes: "Ciclo de 2 passos in-place, 1.35 m/s. Sincronizar footstep L/R." },
  { id: "walk_start", group: "locomotion", label: "Iniciar caminhada", file: null, duration: 0.45, loop: false, next: "walk", fadeIn: 0.15, fadeOut: 0.1, states: ["WALK"], markers: [], status: "pending" },
  { id: "walk_stop", group: "locomotion", label: "Parar caminhada", file: null, duration: 0.4, loop: false, next: "idle_breathe", fadeIn: 0.1, fadeOut: 0.2, states: ["IDLE"], markers: [{ at: 0.4, marker: "arrived", note: "chegou ao destino" }], status: "pending" },
  { id: "turn_left", group: "locomotion", label: "Virar à esquerda", file: null, duration: 0.5, loop: false, next: "idle_breathe", fadeIn: 0.15, fadeOut: 0.15, states: ["TURN"], markers: [], status: "pending" },
  { id: "turn_right", group: "locomotion", label: "Virar à direita", file: null, duration: 0.5, loop: false, next: "idle_breathe", fadeIn: 0.15, fadeOut: 0.15, states: ["TURN"], markers: [], status: "pending" },

  // ---- POSTURA ----
  { id: "sit_down", group: "posture", label: "Sentar", file: null, duration: 0.9, loop: false, next: "sit_idle", fadeIn: 0.2, fadeOut: 0.15, states: ["SIT"], markers: [{ at: 0.9, marker: "sat", note: "sentou" }], status: "pending", adaptations: ["móvel: cadeira vs. cama vs. tapete (altura do assento)"] },
  { id: "sit_idle", group: "posture", label: "Sentada (respirar)", file: null, duration: 3.0, loop: true, fadeIn: 0.3, fadeOut: 0.3, states: ["SIT", "STUDY", "READ", "WRITE", "EAT", "DRINK"], markers: [], status: "pending", adaptations: ["roupa: ajustar saia ao sentar"] },
  { id: "stand_up", group: "posture", label: "Levantar", file: null, duration: 0.9, loop: false, next: "idle_breathe", fadeIn: 0.2, fadeOut: 0.2, states: ["STAND_UP"], markers: [{ at: 0.9, marker: "stoodUp", note: "levantou" }], status: "pending", adaptations: ["móvel: altura do assento"] },

  // ---- ALIMENTAÇÃO ----
  { id: "grab_food", group: "feeding", label: "Pegar alimento", file: null, duration: 0.6, loop: false, next: "eat", fadeIn: 0.2, fadeOut: 0.15, states: ["EAT", "DRINK"], markers: [{ at: 0.6, marker: "grabbed", note: "pegou objeto (fixar na mão R)" }], status: "pending", adaptations: ["alimento: tamanho do objeto na mão", "mão: R dominante"] },
  { id: "eat", group: "feeding", label: "Levar à boca + mastigar", file: null, duration: 2.2, loop: true, fadeIn: 0.2, fadeOut: 0.2, states: ["EAT"], markers: [{ at: 0.8, marker: "foodToMouth", note: "levou alimento à boca" }], status: "pending", adaptations: ["alimento: pizza/fatia vs. copo vs. colher"] },
  { id: "drink", group: "feeding", label: "Beber", file: null, duration: 2.0, loop: true, fadeIn: 0.2, fadeOut: 0.2, states: ["DRINK"], markers: [{ at: 0.7, marker: "foodToMouth", note: "levou copo à boca" }], status: "pending", adaptations: ["recipiente: copo vs. caixinha com canudo"] },
  { id: "put_back", group: "feeding", label: "Devolver objeto à mesa", file: null, duration: 0.7, loop: false, next: "sit_idle", fadeIn: 0.15, fadeOut: 0.2, states: ["EAT", "DRINK"], markers: [{ at: 0.7, marker: "finished", note: "terminou" }], status: "pending" },

  // ---- ESTUDO ----
  { id: "open_book", group: "study", label: "Abrir livro", file: null, duration: 1.0, loop: false, next: "read", fadeIn: 0.2, fadeOut: 0.15, states: ["STUDY", "READ"], markers: [{ at: 1.0, marker: "grabbed", note: "livro aberto sobre a mesa" }], status: "pending" },
  { id: "read", group: "study", label: "Ler", file: null, duration: 4.0, loop: true, fadeIn: 0.3, fadeOut: 0.3, states: ["READ"], markers: [], status: "pending", notes: "Cabeça levemente baixa, olhos no livro." },
  { id: "turn_page", group: "study", label: "Virar página", file: null, duration: 1.1, loop: false, next: "read", fadeIn: 0.15, fadeOut: 0.15, states: ["READ"], markers: [], status: "pending" },
  { id: "write", group: "study", label: "Escrever", file: null, duration: 3.0, loop: true, fadeIn: 0.3, fadeOut: 0.3, states: ["WRITE", "STUDY"], markers: [], status: "pending", adaptations: ["objeto: lápis fixado na mão R"] },
  { id: "think", group: "study", label: "Pensar (lápis no queixo)", file: null, duration: 3.2, loop: false, next: "write", fadeIn: 0.3, fadeOut: 0.3, states: ["STUDY"], markers: [], status: "pending" },

  // ---- BRINCADEIRA ----
  { id: "play_toy", group: "play", label: "Manipular brinquedo", file: null, duration: 3.5, loop: true, fadeIn: 0.3, fadeOut: 0.3, states: ["PLAY"], markers: [], status: "pending", adaptations: ["brinquedo: cubo vs. boneca vs. bola"] },
  { id: "celebrate", group: "play", label: "Comemorar", file: null, duration: 2.0, loop: false, next: "idle_breathe", fadeIn: 0.2, fadeOut: 0.3, states: ["HAPPY", "PLAY"], markers: [{ at: 2.0, marker: "finished", note: "terminou" }], status: "pending" },

  // ---- SONO ----
  { id: "yawn", group: "sleep", label: "Bocejar", file: null, duration: 2.4, loop: false, next: "sit_on_bed", fadeIn: 0.3, fadeOut: 0.2, states: ["TIRED", "SLEEP"], markers: [], status: "pending" },
  { id: "sit_on_bed", group: "sleep", label: "Sentar na cama", file: null, duration: 1.0, loop: false, next: "lie_down", fadeIn: 0.2, fadeOut: 0.15, states: ["SLEEP", "SIT"], markers: [{ at: 1.0, marker: "sat", note: "sentou na cama" }], status: "pending", adaptations: ["roupa: pijama"] },
  { id: "lie_down", group: "sleep", label: "Deitar", file: null, duration: 1.4, loop: false, next: "sleep_breathe", fadeIn: 0.2, fadeOut: 0.3, states: ["SLEEP"], markers: [{ at: 1.4, marker: "liedDown", note: "deitou" }], status: "pending" },
  { id: "sleep_breathe", group: "sleep", label: "Dormir respirando", file: null, duration: 4.0, loop: true, fadeIn: 0.5, fadeOut: 0.4, states: ["SLEEP"], markers: [], status: "pending", notes: "Respiração lenta e profunda. Loop perfeito." },
  { id: "wake_up", group: "sleep", label: "Acordar (espreguiçar)", file: null, duration: 2.6, loop: false, next: "sit_on_bed", fadeIn: 0.3, fadeOut: 0.2, states: ["WAKE_UP"], markers: [], status: "pending" },
  { id: "get_off_bed", group: "sleep", label: "Sair da cama", file: null, duration: 1.0, loop: false, next: "idle_breathe", fadeIn: 0.2, fadeOut: 0.2, states: ["STAND_UP", "WAKE_UP"], markers: [{ at: 1.0, marker: "stoodUp", note: "levantou" }], status: "pending" },

  // ---- GUARDA-ROUPA ----
  { id: "wardrobe_open", group: "wardrobe", label: "Abrir guarda-roupa", file: null, duration: 1.0, loop: false, next: "wardrobe_browse", fadeIn: 0.2, fadeOut: 0.15, states: ["WARDROBE"], markers: [], status: "pending", notes: "Porta do móvel anima junto (prop animation)." },
  { id: "wardrobe_browse", group: "wardrobe", label: "Escolher roupa", file: null, duration: 2.5, loop: true, fadeIn: 0.2, fadeOut: 0.2, states: ["WARDROBE"], markers: [], status: "pending" },
  { id: "wardrobe_close", group: "wardrobe", label: "Fechar guarda-roupa", file: null, duration: 0.9, loop: false, next: "idle_breathe", fadeIn: 0.15, fadeOut: 0.2, states: ["WARDROBE"], markers: [{ at: 0.9, marker: "finished", note: "terminou" }], status: "pending" },
  { id: "change_clothes", group: "wardrobe", label: "Trocar roupa (privado)", file: null, duration: 2.0, loop: false, next: "idle_breathe", fadeIn: 0.3, fadeOut: 0.3, states: ["CHANGE_CLOTHES"], markers: [], status: "pending", notes: "Executada com fade/privacidade — sem nudez. Troca de material do vestido." },

  // ---- BANHEIRO ----
  { id: "reach_handle", group: "bathroom", label: "Alcançar maçaneta", file: null, duration: 0.7, loop: false, next: "bath_open", fadeIn: 0.2, fadeOut: 0.1, states: ["BATHROOM"], markers: [], status: "pending", notes: "Mão R à altura da maçaneta (0.95m)." },
  { id: "bath_open", group: "bathroom", label: "Abrir porta", file: null, duration: 1.1, loop: false, next: "bath_enter", fadeIn: 0.15, fadeOut: 0.15, states: ["BATHROOM"], markers: [{ at: 1.1, marker: "doorOpened", note: "abriu porta" }], status: "pending", notes: "Folha da porta anima junto (prop)." },
  { id: "bath_enter", group: "bathroom", label: "Entrar", file: null, duration: 1.2, loop: false, next: "bath_idle_hidden", fadeIn: 0.2, fadeOut: 0.2, states: ["BATHROOM", "WALK"], markers: [{ at: 1.2, marker: "entered", note: "entrou" }], status: "pending" },
  { id: "bath_idle_hidden", group: "bathroom", label: "Dentro (fora de vista)", file: null, duration: 8.0, loop: true, fadeIn: 0.3, fadeOut: 0.3, states: ["BATHROOM"], markers: [], status: "pending", notes: "Personagem oculta. Porta fechada. Higiene via lavar-mãos." },
  { id: "bath_exit", group: "bathroom", label: "Sair", file: null, duration: 1.2, loop: false, next: "bath_close", fadeIn: 0.2, fadeOut: 0.15, states: ["BATHROOM", "WALK"], markers: [], status: "pending" },
  { id: "bath_close", group: "bathroom", label: "Fechar porta", file: null, duration: 0.9, loop: false, next: "idle_breathe", fadeIn: 0.15, fadeOut: 0.2, states: ["BATHROOM"], markers: [{ at: 0.9, marker: "finished", note: "terminou" }], status: "pending" },

  // ---- REAÇÕES ----
  { id: "happy", group: "reactions", label: "Alegria", file: null, duration: 2.2, loop: false, next: "idle_breathe", fadeIn: 0.2, fadeOut: 0.3, states: ["HAPPY"], markers: [], status: "pending" },
  { id: "hungry_react", group: "reactions", label: "Fome (mão na barriga)", file: null, duration: 2.0, loop: false, next: "idle_breathe", fadeIn: 0.2, fadeOut: 0.3, states: ["HUNGRY"], markers: [], status: "pending" },
  { id: "tired_react", group: "reactions", label: "Cansaço (esfregar olhos)", file: null, duration: 2.4, loop: false, next: "idle_breathe", fadeIn: 0.2, fadeOut: 0.3, states: ["TIRED"], markers: [], status: "pending" },
  { id: "bored_react", group: "reactions", label: "Tédio (suspirar)", file: null, duration: 2.6, loop: false, next: "idle_breathe", fadeIn: 0.2, fadeOut: 0.3, states: ["BORED"], markers: [], status: "pending" },
];

export const ANIM_GROUPS = [
  { id: "rest", label: "Repouso" },
  { id: "locomotion", label: "Locomoção" },
  { id: "posture", label: "Postura" },
  { id: "feeding", label: "Alimentação" },
  { id: "study", label: "Estudo" },
  { id: "play", label: "Brincadeira" },
  { id: "sleep", label: "Sono" },
  { id: "wardrobe", label: "Guarda-roupa" },
  { id: "bathroom", label: "Banheiro" },
  { id: "reactions", label: "Reações" },
] as const;

export function getClip(id: string): AnimClip | undefined {
  return ANIMATION_MANIFEST.find((c) => c.id === id);
}

export function pendingCount(): { total: number; pending: number; ready: number } {
  const total = ANIMATION_MANIFEST.length;
  const ready = ANIMATION_MANIFEST.filter((c) => c.status === "ready").length;
  return { total, pending: total - ready, ready };
}
