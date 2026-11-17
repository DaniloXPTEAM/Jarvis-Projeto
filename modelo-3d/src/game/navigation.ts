// ============================================================
// Navegação: pontos oficiais, obstáculos e pathfinding A*.
// Layout atual (fiel às referências): cama à esquerda com cabeceira
// na parede verde; janela central na parede do fundo; porta do
// banheiro à direita da janela (mesma parede); escrivaninha na
// parede direita; guarda-roupa na parede direita (sul); tapete
// redondo no centro; mesinha de alimentação à frente.
// Sala 6.4m (X) x 5.2m (Z). Paredes em x=±3.2, z=±2.6.
// ============================================================

import type { NavPoint, NavPointId, Vec2 } from "./types";
import { SIMULATION_CONFIG } from "../config/simulation";

export const NAV_POINTS: Record<NavPointId, NavPoint> = {
  BED_POINT: {
    id: "BED_POINT",
    approach: { x: -1.3, z: -1.28 },
    placement: { x: -2.35, z: -1.28 },
    facing: Math.PI / 2, // deitada com a cabeça na cabeceira (oeste)
    label: "Cama",
  },
  DESK_POINT: {
    id: "DESK_POINT",
    approach: { x: 1.6, z: -0.3 },
    placement: { x: 2.15, z: -0.3 },
    facing: Math.PI / 2,
    label: "Escrivaninha",
  },
  CHAIR_POINT: {
    id: "CHAIR_POINT",
    approach: { x: 1.6, z: -0.3 },
    placement: { x: 2.12, z: -0.3 },
    facing: Math.PI / 2, // sentada virada para a mesa (leste)
    label: "Cadeira",
  },
  WARDROBE_POINT: {
    id: "WARDROBE_POINT",
    approach: { x: 1.75, z: 1.8 },
    placement: { x: 2.28, z: 1.8 },
    facing: Math.PI / 2,
    label: "Guarda-roupa",
  },
  FOOD_POINT: {
    id: "FOOD_POINT",
    approach: { x: -0.55, z: 1.25 },
    placement: { x: -0.55, z: 2.12 },
    facing: Math.PI, // sentada virada para a mesinha (norte)
    label: "Mesinha de alimentação",
  },
  PLAY_POINT: {
    id: "PLAY_POINT",
    approach: { x: 0.85, z: 0.95 },
    placement: { x: 0.05, z: 0.55 },
    facing: -2.2,
    label: "Tapete de brincadeiras",
  },
  BATHROOM_DOOR_POINT: {
    id: "BATHROOM_DOOR_POINT",
    approach: { x: 1.9, z: -1.55 },
    placement: { x: 1.9, z: -2.08 },
    facing: Math.PI, // olha para a porta (norte)
    label: "Porta do banheiro",
  },
  WINDOW_POINT: {
    id: "WINDOW_POINT",
    approach: { x: -0.32, z: -1.5 },
    placement: { x: -0.32, z: -2.02 },
    facing: Math.PI,
    label: "Janela",
  },
  IDLE_1: {
    id: "IDLE_1",
    approach: { x: -0.4, z: 0.1 },
    placement: { x: -0.4, z: 0.1 },
    facing: 0.4,
    label: "Centro",
  },
  IDLE_2: {
    id: "IDLE_2",
    approach: { x: -0.9, z: 0.95 },
    placement: { x: -0.9, z: 0.95 },
    facing: -0.6,
    label: "Beirada do tapete",
  },
  IDLE_3: {
    id: "IDLE_3",
    approach: { x: 1.0, z: 1.0 },
    placement: { x: 1.0, z: 1.0 },
    facing: 2.4,
    label: "Canto livre",
  },
  IDLE_4: {
    id: "IDLE_4",
    approach: { x: -1.75, z: 0.35 },
    placement: { x: -1.75, z: 0.35 },
    facing: 1.2,
    label: "Perto da cama",
  },
};

export interface Obstacle { minX: number; minZ: number; maxX: number; maxZ: number; label: string }

const R = SIMULATION_CONFIG.navigation.characterRadius;

export const OBSTACLES: Obstacle[] = [
  // Cama (cabeceira na parede esquerda)
  { minX: -3.1, minZ: -2.1, maxX: -1.5, maxZ: -0.45, label: "bed" },
  // Banquinho lateral
  { minX: -1.58, minZ: -2.22, maxX: -1.0, maxZ: -1.68, label: "nightstand" },
  // Guarda-roupa (parede direita, sul)
  { minX: 2.5, minZ: 1.05, maxX: 3.2, maxZ: 2.55, label: "wardrobe" },
  // Escrivaninha (parede direita)
  { minX: 2.5, minZ: -1.2, maxX: 3.2, maxZ: 0.6, label: "desk" },
  // Cadeira de rodinhas
  { minX: 1.85, minZ: -0.62, maxX: 2.48, maxZ: 0.02, label: "chair" },
  // Mesinha de alimentação
  { minX: -0.98, minZ: 1.62, maxX: -0.12, maxZ: 2.08, label: "food-table" },
  // Banquinho da mesinha
  { minX: -0.8, minZ: 2.3, maxX: -0.3, maxZ: 2.62, label: "food-stool" },
  // Baú de brinquedos (frente-esquerda)
  { minX: -3.05, minZ: 1.5, maxX: -2.2, maxZ: 2.2, label: "toy-chest" },
  // Monstera em cesto
  { minX: -1.98, minZ: 2.02, maxX: -1.42, maxZ: 2.52, label: "plant" },
];

export const ROOM = {
  minX: -3.05 + R, maxX: 3.05 - R,
  minZ: -2.5 + R, maxZ: 2.5 - R,
  doorGap: { minZ: 0, maxZ: 0 },
};

function pointInObstacle(p: Vec2, inflate: number): boolean {
  for (const o of OBSTACLES) {
    if (
      p.x > o.minX - inflate && p.x < o.maxX + inflate &&
      p.z > o.minZ - inflate && p.z < o.maxZ + inflate
    ) return true;
  }
  return false;
}

export function isWalkable(p: Vec2): boolean {
  if (p.x < ROOM.minX || p.x > ROOM.maxX || p.z < ROOM.minZ || p.z > ROOM.maxZ) return false;
  return !pointInObstacle(p, 0.06);
}

interface Cell { ix: number; iz: number; f: number; g: number; parent: string | null }

function key(ix: number, iz: number) { return `${ix},${iz}`; }

export function findPath(from: Vec2, to: Vec2): Vec2[] {
  const cell = SIMULATION_CONFIG.navigation.cellSize;
  const nx = Math.ceil((ROOM.maxX - ROOM.minX) / cell);
  const nz = Math.ceil((ROOM.maxZ - ROOM.minZ) / cell);
  const toCell = (p: Vec2) => ({
    ix: Math.max(0, Math.min(nx - 1, Math.round((p.x - ROOM.minX) / cell))),
    iz: Math.max(0, Math.min(nz - 1, Math.round((p.z - ROOM.minZ) / cell))),
  });
  const toWorld = (ix: number, iz: number): Vec2 => ({
    x: ROOM.minX + ix * cell,
    z: ROOM.minZ + iz * cell,
  });
  const start = toCell(from);
  const goal = toCell(to);
  const radius = SIMULATION_CONFIG.navigation.characterRadius * 0.75;

  const blocked = (ix: number, iz: number) => {
    if (ix < 0 || iz < 0 || ix >= nx || iz >= nz) return true;
    return pointInObstacle(toWorld(ix, iz), radius);
  };

  let goalIx = goal.ix, goalIz = goal.iz;
  if (blocked(goalIx, goalIz)) {
    let found = false;
    for (let ring = 1; ring <= 6 && !found; ring++) {
      for (let dx = -ring; dx <= ring && !found; dx++) {
        for (let dz = -ring; dz <= ring && !found; dz++) {
          if (Math.max(Math.abs(dx), Math.abs(dz)) !== ring) continue;
          const cx = goal.ix + dx, cz = goal.iz + dz;
          if (!blocked(cx, cz)) { goalIx = cx; goalIz = cz; found = true; }
        }
      }
    }
    if (!found) return [to];
  }

  const open: Cell[] = [{ ...start, f: 0, g: 0, parent: null }];
  const cameFrom = new Map<string, string | null>();
  const gScore = new Map<string, number>();
  const closed = new Set<string>();
  gScore.set(key(start.ix, start.iz), 0);
  cameFrom.set(key(start.ix, start.iz), null);

  const h = (ix: number, iz: number) => Math.hypot(ix - goalIx, iz - goalIz);
  const dirs = [
    [1, 0, 1], [-1, 0, 1], [0, 1, 1], [0, -1, 1],
    [1, 1, 1.414], [1, -1, 1.414], [-1, 1, 1.414], [-1, -1, 1.414],
  ];
  let found = false;
  let guard = 0;
  while (open.length > 0 && guard++ < 4000) {
    let bi = 0;
    for (let i = 1; i < open.length; i++) if (open[i].f < open[bi].f) bi = i;
    const current = open.splice(bi, 1)[0];
    const ck = key(current.ix, current.iz);
    if (closed.has(ck)) continue;
    closed.add(ck);
    if (current.ix === goalIx && current.iz === goalIz) { found = true; break; }
    for (const [dx, dz, cost] of dirs) {
      const nix = current.ix + dx, niz = current.iz + dz;
      if (blocked(nix, niz)) continue;
      if (dx !== 0 && dz !== 0 && (blocked(current.ix + dx, current.iz) || blocked(current.ix, current.iz + dz))) continue;
      const nk = key(nix, niz);
      if (closed.has(nk)) continue;
      const tentative = (gScore.get(ck) ?? Infinity) + cost;
      if (tentative < (gScore.get(nk) ?? Infinity)) {
        gScore.set(nk, tentative);
        cameFrom.set(nk, ck);
        open.push({ ix: nix, iz: niz, f: tentative + h(nix, niz), g: tentative, parent: ck });
      }
    }
  }
  if (!found) return [from, to];

  const cells: Array<[number, number]> = [];
  let ck: string | null = key(goalIx, goalIz);
  while (ck) {
    const [ix, iz] = ck.split(",").map(Number);
    cells.push([ix, iz]);
    ck = cameFrom.get(ck) ?? null;
  }
  cells.reverse();
  let path = cells.map(([ix, iz]) => toWorld(ix, iz));
  path[path.length - 1] = { ...to };
  path = smoothPath(path);
  return path;
}

function smoothPath(path: Vec2[]): Vec2[] {
  if (path.length <= 2) return path;
  const out: Vec2[] = [path[0]];
  let anchor = 0;
  for (let i = 2; i < path.length; i++) {
    if (!lineWalkable(path[anchor], path[i])) {
      out.push(path[i - 1]);
      anchor = i - 1;
    }
  }
  out.push(path[path.length - 1]);
  return out;
}

function lineWalkable(a: Vec2, b: Vec2): boolean {
  const dist = Math.hypot(b.x - a.x, b.z - a.z);
  const steps = Math.max(2, Math.ceil(dist / 0.1));
  const radius = SIMULATION_CONFIG.navigation.characterRadius * 0.7;
  for (let i = 0; i <= steps; i++) {
    const t = i / steps;
    const p = { x: a.x + (b.x - a.x) * t, z: a.z + (b.z - a.z) * t };
    if (p.x < ROOM.minX || p.x > ROOM.maxX || p.z < ROOM.minZ || p.z > ROOM.maxZ) return false;
    if (pointInObstacle(p, radius)) return false;
  }
  return true;
}

export function validateConnectivity(): Array<{ from: NavPointId; to: NavPointId; ok: boolean; length: number }> {
  const ids = Object.keys(NAV_POINTS) as NavPointId[];
  const results: Array<{ from: NavPointId; to: NavPointId; ok: boolean; length: number }> = [];
  for (const a of ids) {
    for (const b of ids) {
      if (a === b) continue;
      const path = findPath(NAV_POINTS[a].approach, NAV_POINTS[b].approach);
      const last = path[path.length - 1];
      const dest = NAV_POINTS[b].approach;
      const ok = Math.hypot(last.x - dest.x, last.z - dest.z) < 0.5;
      results.push({ from: a, to: b, ok, length: path.length });
    }
  }
  return results;
}
