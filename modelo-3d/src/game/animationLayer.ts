// ============================================================
// CAMADA DE ANIMAÇÃO — independente da lógica das atividades.
// Hoje: driver procedural sobre a malha provisória (clipes "pending").
// Futuro: carregar GLB com esqueleto via loadGabiGLB() e tocar takes
// do manifesto — a simulação continua chamando apenas playState().
// Política de locomoção: animações NO LUGAR (in-place); deslocamento
// sempre controlado pela navegação. NUNCA usar root motion junto com
// navegação (evita deslocamento duplicado). Se um clipe GLB futuro
// contiver root motion, ele deve ser assado como in-place na importação.
// ============================================================

import * as THREE from "three";
import type { GabiRig } from "./gabiProvisional";
import type { GabiState } from "./types";
import { getClip } from "./animationManifest";

export interface AnimDriver {
  update(dt: number, time: number, state: GabiState, moving: boolean, reducedMotion: boolean): void;
  setLying(lying: boolean): void;
  setSitting(sitting: boolean): void;
  reset(): void;
}

const lerp = (a: number, b: number, t: number) => a + (b - a) * t;

/** Mapeia estado -> clipId do manifesto (camada única de verdade). */
export function clipForState(state: GabiState): string {
  switch (state) {
    case "IDLE": return "idle_breathe";
    case "WALK": return "walk";
    case "TURN": return "turn_left";
    case "SIT": case "STUDY": case "READ": case "WRITE": return "sit_idle";
    case "STAND_UP": return "stand_up";
    case "EAT": return "eat";
    case "DRINK": return "drink";
    case "PLAY": return "play_toy";
    case "SLEEP": return "sleep_breathe";
    case "WAKE_UP": return "wake_up";
    case "WARDROBE": return "wardrobe_browse";
    case "CHANGE_CLOTHES": return "change_clothes";
    case "BATHROOM": return "bath_idle_hidden";
    case "HAPPY": return "happy";
    case "TIRED": return "tired_react";
    case "HUNGRY": return "hungry_react";
    case "BORED": return "bored_react";
    default: return "idle_breathe";
  }
}

export class ProceduralDriver implements AnimDriver {
  private rig: GabiRig;
  private lying = false;
  private lyingBlend = 0;
  private sitting = false;
  private sitBlend = 0;
  private blinkTimer = 2;
  private blinkPhase = -1;
  private walkPhase = 0;
  private breathePhase = 0;

  constructor(rig: GabiRig) {
    this.rig = rig;
  }

  setLying(v: boolean) { this.lying = v; }
  setSitting(v: boolean) { this.sitting = v; }
  reset() {
    this.lying = false; this.lyingBlend = 0;
    this.sitting = false; this.sitBlend = 0;
  }

  update(dt: number, time: number, state: GabiState, moving: boolean, reducedMotion: boolean): void {
    const p = this.rig.parts;
    const amp = reducedMotion ? 0.35 : 1;

    // Blends de pose
    const blendSpeed = reducedMotion ? 8 : 4.5;
    this.lyingBlend = lerp(this.lyingBlend, this.lying ? 1 : 0, Math.min(1, dt * blendSpeed));
    this.sitBlend = lerp(this.sitBlend, this.sitting && !this.lying ? 1 : 0, Math.min(1, dt * blendSpeed));

    // Respiração
    this.breathePhase += dt * (state === "SLEEP" ? 1.6 : 2.6);
    const breathe = Math.sin(this.breathePhase) * (state === "SLEEP" ? 0.035 : 0.018) * amp;
    p.body.scale.set(1, 1 + breathe, 1);
    p.body.position.y = breathe * 0.6;

    // Piscar (camada aditiva)
    this.blinkTimer -= dt;
    if (this.blinkTimer <= 0 && state !== "SLEEP") {
      this.blinkPhase = 0;
      this.blinkTimer = 2 + Math.random() * 3;
    }
    if (this.blinkPhase >= 0) {
      this.blinkPhase += dt / 0.18;
      if (this.blinkPhase >= 1) this.blinkPhase = -1;
    }
    const eyeSY = state === "SLEEP" ? 0.08
      : this.blinkPhase >= 0 ? Math.max(0.08, Math.abs(Math.cos(this.blinkPhase * Math.PI)) * 1.25) : 1.25;
    p.eyeL.scale.set(1, eyeSY, 0.6);
    p.eyeR.scale.set(1, eyeSY, 0.6);
    const pupilsVisible = state !== "SLEEP" && this.blinkPhase < 0;
    p.pupilL.visible = pupilsVisible;
    p.pupilR.visible = pupilsVisible;

    // Caminhada / balanço
    if (moving && state === "WALK") {
      this.walkPhase += dt * 9;
      const sw = Math.sin(this.walkPhase) * 0.55 * amp;
      const sw2 = Math.sin(this.walkPhase + Math.PI) * 0.55 * amp;
      p.legL.rotation.x = lerp(p.legL.rotation.x, sw, 0.6);
      p.legR.rotation.x = lerp(p.legR.rotation.x, sw2, 0.6);
      p.armL.rotation.x = lerp(p.armL.rotation.x, sw2 * 0.7, 0.6);
      p.armR.rotation.x = lerp(p.armR.rotation.x, sw * 0.7, 0.6);
      this.rig.group.position.y = Math.abs(Math.sin(this.walkPhase)) * 0.03 * amp;
      p.body.rotation.y = Math.sin(this.walkPhase) * 0.04 * amp;
    } else {
      p.legL.rotation.x = lerp(p.legL.rotation.x, 0, Math.min(1, dt * 8));
      p.legR.rotation.x = lerp(p.legR.rotation.x, 0, Math.min(1, dt * 8));
      if (!this.sitting) {
        this.rig.group.position.y = lerp(this.rig.group.position.y, 0, Math.min(1, dt * 8));
      }
    }

    // Poses por estado (braços/cabeça)
    const k = Math.min(1, dt * 6);
    const idleSway = Math.sin(time * 1.2) * 0.03 * amp;
    switch (state) {
      case "HAPPY":
        p.armL.rotation.z = lerp(p.armL.rotation.z, -2.4, k);
        p.armR.rotation.z = lerp(p.armR.rotation.z, 2.4, k);
        p.armL.rotation.x = lerp(p.armL.rotation.x, 0, k);
        p.armR.rotation.x = lerp(p.armR.rotation.x, 0, k);
        this.rig.group.position.y = Math.abs(Math.sin(time * 8)) * 0.06 * amp;
        p.head.rotation.x = lerp(p.head.rotation.x, -0.15, k);
        break;
      case "EAT":
      case "DRINK": {
        // Braço direito leva à boca repetidamente
        const cyc = (Math.sin(time * 4.2) + 1) / 2;
        p.armR.rotation.x = lerp(p.armR.rotation.x, -1.9 + cyc * 0.5, k);
        p.armR.rotation.z = lerp(p.armR.rotation.z, 0.35, k);
        p.armL.rotation.x = lerp(p.armL.rotation.x, -0.5, k);
        p.head.rotation.x = lerp(p.head.rotation.x, 0.12 * cyc, k);
        break;
      }
      case "STUDY":
      case "WRITE":
        p.armL.rotation.x = lerp(p.armL.rotation.x, -1.1, k);
        p.armR.rotation.x = lerp(p.armR.rotation.x, -1.15 + Math.sin(time * 7) * 0.06 * amp, k);
        p.head.rotation.x = lerp(p.head.rotation.x, 0.32, k);
        break;
      case "READ":
        p.armL.rotation.x = lerp(p.armL.rotation.x, -1.0, k);
        p.armR.rotation.x = lerp(p.armR.rotation.x, -1.0, k);
        p.head.rotation.x = lerp(p.head.rotation.x, 0.3, k);
        break;
      case "PLAY":
        p.armL.rotation.x = lerp(p.armL.rotation.x, -1.2 + Math.sin(time * 5) * 0.35 * amp, k);
        p.armR.rotation.x = lerp(p.armR.rotation.x, -1.2 + Math.sin(time * 5 + 1.5) * 0.35 * amp, k);
        p.head.rotation.x = lerp(p.head.rotation.x, 0.25, k);
        break;
      case "TIRED":
        // Esfregar olho com mão direita
        p.armR.rotation.x = lerp(p.armR.rotation.x, -2.4, k);
        p.armR.rotation.z = lerp(p.armR.rotation.z, 0.5, k);
        p.head.rotation.z = lerp(p.head.rotation.z, 0.12, k);
        break;
      case "HUNGRY":
        // Mão na barriga
        p.armR.rotation.x = lerp(p.armR.rotation.x, -0.9, k);
        p.armL.rotation.x = lerp(p.armL.rotation.x, -0.9, k);
        p.head.rotation.x = lerp(p.head.rotation.x, 0.15, k);
        break;
      case "BORED":
        p.head.rotation.z = lerp(p.head.rotation.z, 0.18, k);
        p.armL.rotation.x = lerp(p.armL.rotation.x, -0.2, k);
        p.armR.rotation.x = lerp(p.armR.rotation.x, -0.2, k);
        break;
      case "WARDROBE":
      case "BATHROOM":
        p.armR.rotation.x = lerp(p.armR.rotation.x, -1.4, k);
        p.armL.rotation.x = lerp(p.armL.rotation.x, -0.3, k);
        break;
      case "SIT":
        p.armL.rotation.x = lerp(p.armL.rotation.x, -0.6, k);
        p.armR.rotation.x = lerp(p.armR.rotation.x, -0.6, k);
        p.head.rotation.x = lerp(p.head.rotation.x, 0.05, k);
        break;
      default:
        if (!moving) {
          p.armL.rotation.x = lerp(p.armL.rotation.x, idleSway, k);
          p.armR.rotation.x = lerp(p.armR.rotation.x, -idleSway, k);
          p.armL.rotation.z = lerp(p.armL.rotation.z, 0.18, k);
          p.armR.rotation.z = lerp(p.armR.rotation.z, -0.18, k);
          p.head.rotation.x = lerp(p.head.rotation.x, Math.sin(time * 0.7) * 0.05 * amp, k);
          p.head.rotation.y = lerp(p.head.rotation.y, Math.sin(time * 0.45) * 0.18 * amp, k);
          p.head.rotation.z = lerp(p.head.rotation.z, 0, k);
        }
        break;
    }

    // Sentar: dobra pernas, abaixa corpo
    if (this.sitBlend > 0.001) {
      const sb = this.sitBlend;
      p.legL.rotation.x = lerp(p.legL.rotation.x, -1.5, sb * 0.5);
      p.legR.rotation.x = lerp(p.legR.rotation.x, -1.5, sb * 0.5);
      p.body.position.y = lerp(p.body.position.y, -0.22, sb * 0.4);
    }

    // Deitar: gira corpo -90° (cabeça para -Z local => travesseiro)
    const g = this.rig.group;
    if (this.lyingBlend > 0.001) {
      p.body.rotation.x = lerp(0, -Math.PI / 2 + 0.08, this.lyingBlend);
      p.body.position.y = lerp(p.body.position.y, 0.62, this.lyingBlend * 0.35);
      p.body.position.z = lerp(0, -0.18, this.lyingBlend);
      g.position.y = lerp(g.position.y, 0, 0.2);
    } else {
      p.body.rotation.x = lerp(p.body.rotation.x, 0, Math.min(1, dt * 6));
      if (!this.sitting) {
        p.body.position.z = lerp(p.body.position.z, 0, Math.min(1, dt * 6));
      }
    }
  }
}

// ---- Integração futura de GLB ----
// Especificação (ver CHARACTER-ANIMATION-BIBLE.md § importação):
// - Arquivo: public/animations/gabi.glb (Y-up, metros, frente +Z)
// - Takes nomeados pelos clipIds do manifesto
// - Ossos: Hips, Spine, Chest, Neck, Head, Shoulder_L/R, UpperArm_L/R,
//   Forearm_L/R, Hand_L/R, Thigh_L/R, Shin_L/R, Foot_L/R
// - Sockets: Hand_R_Attach, Hand_L_Attach, Head_Attach
// Para ativar: chame loadGabiGLB(scene) e passe o mixer para GLBDriver.
// Nenhuma alteração na simulação é necessária — só atualizar o manifesto
// (status ready + file) e este loader.
export async function loadGabiGLB(): Promise<{ url: string } | null> {
  try {
    const res = await fetch("animations/gabi.glb", { method: "HEAD" });
    if (res.ok) return { url: "animations/gabi.glb" };
    return null;
  } catch {
    return null;
  }
}

export function describeCurrentClip(state: GabiState): string {
  const clip = getClip(clipForState(state));
  if (!clip) return "—";
  return `${clip.label} (${clip.status === "ready" ? "GLB" : "provisório"})`;
}

// Re-export para inspeção sem ciclos
export type { GabiRig };
export type ThreeGroup = THREE.Group;
