// ============================================================
// Persistência local (localStorage).
// LIMITAÇÃO DOCUMENTADA: este projeto usa apenas persistência local.
// Não há autenticação remota nem sincronização em nuvem. Para integrar
// ao app real com backend, substitua load/save por chamadas à API do app
// mantendo o formato SaveData (ver docs/INTEGRATION.md).
// ============================================================

import { SIMULATION_CONFIG } from "../config/simulation";
import type { GameSettings, SaveData } from "./types";

const SAVE_KEY = SIMULATION_CONFIG.save.key;
const SETTINGS_KEY = SIMULATION_CONFIG.save.settingsKey;

export function loadSave(): SaveData | null {
  try {
    const raw = localStorage.getItem(SAVE_KEY);
    if (!raw) return null;
    const data = JSON.parse(raw) as SaveData;
    if (data.version !== 1) return null;
    return data;
  } catch {
    return null;
  }
}

export function saveGame(data: SaveData): void {
  try {
    localStorage.setItem(SAVE_KEY, JSON.stringify(data));
  } catch {
    // armazenamento indisponível — jogo continua sem salvar
  }
}

export function clearSave(): void {
  try { localStorage.removeItem(SAVE_KEY); } catch { /* noop */ }
}

export function loadSettings(): GameSettings {
  try {
    const raw = localStorage.getItem(SETTINGS_KEY);
    if (raw) return { muted: false, reducedMotion: false, autonomy: true, clockSpeed: 1, ...JSON.parse(raw) };
  } catch { /* noop */ }
  return { muted: false, reducedMotion: false, autonomy: true, clockSpeed: 1 };
}

export function saveSettings(s: GameSettings): void {
  try { localStorage.setItem(SETTINGS_KEY, JSON.stringify(s)); } catch { /* noop */ }
}
