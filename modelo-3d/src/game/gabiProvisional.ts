// ============================================================
// GABI PROVISÓRIA — representação temporária em malha procedural,
// agora alinhada às referências entregues: cabelo RUIVO LONGO
// ONDULADO até a cintura, rosto infantil com SARDAS e olhos grandes,
// vestido vermelho LISO sem mangas + botinhas pretas. Roupas
// alternativas das referências: pijama azul de estrelas (com mangas),
// vestido lilás, suéter amarelo com legging preta.
// ⚠️ NÃO É A GABI FINAL — a personagem definitiva em GLB com
// esqueleto será importada via manifesto (ver
// docs/CHARACTER-ANIMATION-BIBLE.md). Esta malha valida escala
// (≈1.05 m), circulação, câmera, luz e a máquina de estados.
// Unidades: metros. Eixo Y up. Frente: +Z local.
// ============================================================

import * as THREE from "three";

export interface OutfitLook {
  top: string;
  bottom: string;
  accent: string;
  boots: string;
  sleeves?: boolean;
}

export interface GabiRig {
  group: THREE.Group;
  parts: {
    head: THREE.Group;
    eyeL: THREE.Mesh; eyeR: THREE.Mesh;
    pupilL: THREE.Mesh; pupilR: THREE.Mesh;
    hair: THREE.Group;
    torso: THREE.Mesh;
    dress: THREE.Mesh;
    sleeveL: THREE.Mesh; sleeveR: THREE.Mesh;
    armL: THREE.Group; armR: THREE.Group;
    legL: THREE.Group; legR: THREE.Group;
    body: THREE.Group;
  };
  mats: {
    top: THREE.MeshStandardMaterial;
    bottom: THREE.MeshStandardMaterial;
    boots: THREE.MeshStandardMaterial;
  };
  anchors: {
    handR: THREE.Object3D;
    handL: THREE.Object3D;
    head: THREE.Object3D;
    mouth: THREE.Object3D;
  };
  height: number;
}

const SKIN = "#ffd9b8";
const HAIR = "#c94a1e";
const HAIR_DARK = "#a63a14";

export function buildProvisionalGabi(outfit: OutfitLook): GabiRig {
  const group = new THREE.Group();
  const body = new THREE.Group();
  group.add(body);

  const mat = (color: string, rough = 0.8) =>
    new THREE.MeshStandardMaterial({ color, roughness: rough, metalness: 0.02 });
  const skinMat = mat(SKIN, 0.65);
  const hairMat = mat(HAIR, 0.72);
  const hairDarkMat = mat(HAIR_DARK, 0.78);
  const topMat = mat(outfit.top, 0.85);
  const bottomMat = mat(outfit.bottom, 0.85);
  const bootsMat = mat(outfit.boots, 0.55);

  // ---- Pernas (pivot no quadril, y=0.42) ----
  const mkLeg = (side: number) => {
    const g = new THREE.Group();
    g.position.set(0.09 * side, 0.42, 0);
    const leg = new THREE.Mesh(new THREE.CylinderGeometry(0.05, 0.045, 0.3, 10), skinMat);
    leg.position.y = -0.15;
    g.add(leg);
    // Botinha preta de cano curto
    const boot = new THREE.Mesh(new THREE.CylinderGeometry(0.052, 0.055, 0.12, 10), bootsMat);
    boot.position.y = -0.27;
    g.add(boot);
    const foot = new THREE.Mesh(new THREE.SphereGeometry(0.058, 12, 10), bootsMat);
    foot.scale.set(1, 0.65, 1.35);
    foot.position.set(0, -0.33, 0.03);
    g.add(foot);
    g.traverse((o) => { if (o instanceof THREE.Mesh) { o.castShadow = true; } });
    group.add(g);
    return g;
  };
  const legL = mkLeg(-1);
  const legR = mkLeg(1);

  // ---- Vestido liso (cone) + corpinho ----
  const dress = new THREE.Mesh(new THREE.ConeGeometry(0.22, 0.36, 20), bottomMat);
  dress.position.y = 0.55;
  body.add(dress);
  const torso = new THREE.Mesh(new THREE.CylinderGeometry(0.105, 0.13, 0.22, 14), topMat);
  torso.position.y = 0.73;
  body.add(torso);

  // ---- Braços (sem mangas por padrão; mangas opcionais por roupa) ----
  const sleeveGeo = new THREE.CylinderGeometry(0.05, 0.048, 0.2, 10);
  const mkArm = (side: number) => {
    const g = new THREE.Group();
    g.position.set(0.145 * side, 0.78, 0);
    const arm = new THREE.Mesh(new THREE.CylinderGeometry(0.037, 0.032, 0.28, 10), skinMat);
    arm.position.y = -0.15;
    g.add(arm);
    const hand = new THREE.Mesh(new THREE.SphereGeometry(0.044, 10, 8), skinMat);
    hand.position.y = -0.3;
    g.add(hand);
    const anchor = new THREE.Object3D();
    anchor.position.set(0, -0.3, 0.03);
    g.add(anchor);
    // Manga opcional (suéter/pijama)
    const sleeve = new THREE.Mesh(sleeveGeo, topMat);
    sleeve.position.y = -0.11;
    sleeve.visible = !!outfit.sleeves;
    g.add(sleeve);
    g.rotation.z = side * -0.16;
    g.traverse((o) => { if (o instanceof THREE.Mesh) { o.castShadow = true; } });
    body.add(g);
    return { g, anchor, sleeve };
  };
  const aL = mkArm(-1);
  const aR = mkArm(1);

  // ---- Cabeça infantil grande ----
  const head = new THREE.Group();
  head.position.y = 1.02;
  body.add(head);
  const skull = new THREE.Mesh(new THREE.SphereGeometry(0.155, 20, 16), skinMat);
  skull.scale.set(1, 1.05, 0.95);
  skull.castShadow = true;
  head.add(skull);

  // Olhos grandes castanhos
  const eyeGeo = new THREE.SphereGeometry(0.043, 14, 12);
  const whiteMat = new THREE.MeshStandardMaterial({ color: "#ffffff", roughness: 0.25 });
  const eyeL = new THREE.Mesh(eyeGeo, whiteMat);
  eyeL.position.set(-0.063, 0.015, 0.124);
  eyeL.scale.set(1, 1.25, 0.6);
  head.add(eyeL);
  const eyeR = eyeL.clone();
  eyeR.position.x = 0.063;
  head.add(eyeR);
  const pupilMat = new THREE.MeshStandardMaterial({ color: "#5a3016", roughness: 0.2 });
  const pupilL = new THREE.Mesh(new THREE.SphereGeometry(0.022, 12, 10), pupilMat);
  pupilL.position.set(-0.063, 0.012, 0.154);
  head.add(pupilL);
  const pupilR = pupilL.clone();
  pupilR.position.x = 0.063;
  head.add(pupilR);
  const glintMat = new THREE.MeshBasicMaterial({ color: "#ffffff" });
  const glintL = new THREE.Mesh(new THREE.SphereGeometry(0.007, 6, 6), glintMat);
  glintL.position.set(-0.056, 0.022, 0.171);
  head.add(glintL);
  const glintR = glintL.clone();
  glintR.position.x = 0.07;
  head.add(glintR);
  // Sobrancelhas ruivas claras
  const browMat = mat("#b06a2a", 0.8);
  const browL = new THREE.Mesh(new THREE.BoxGeometry(0.05, 0.012, 0.015), browMat);
  browL.position.set(-0.063, 0.075, 0.138);
  browL.rotation.z = 0.12;
  head.add(browL);
  const browR = browL.clone();
  browR.position.x = 0.063;
  browR.rotation.z = -0.12;
  head.add(browR);
  // SARDAS
  const freckleMat = mat("#d98a5f", 0.9);
  const frecklePos = [[-0.095, -0.02], [-0.082, -0.035], [-0.105, -0.038], [0.095, -0.02], [0.082, -0.035], [0.105, -0.038]];
  for (const [fx, fy] of frecklePos) {
    const f = new THREE.Mesh(new THREE.SphereGeometry(0.006, 6, 6), freckleMat);
    f.position.set(fx, fy, 0.128);
    head.add(f);
  }
  // Bochechas + sorriso
  const cheekMat = new THREE.MeshStandardMaterial({ color: "#ff9d9d", roughness: 0.9 });
  const cheekL = new THREE.Mesh(new THREE.SphereGeometry(0.02, 8, 8), cheekMat);
  cheekL.position.set(-0.1, -0.05, 0.112);
  cheekL.scale.set(1, 0.7, 0.5);
  head.add(cheekL);
  const cheekR = cheekL.clone();
  cheekR.position.x = 0.1;
  head.add(cheekR);
  const smile = new THREE.Mesh(new THREE.TorusGeometry(0.034, 0.008, 8, 14, Math.PI * 0.9), mat("#a34a3a", 0.6));
  smile.position.set(0, -0.058, 0.133);
  smile.rotation.set(0.15, 0, Math.PI * 1.05);
  head.add(smile);

  // ---- Cabelo RUIVO LONGO ONDULADO ----
  const hair = new THREE.Group();
  head.add(hair);
  // Topo
  const cap = new THREE.Mesh(new THREE.SphereGeometry(0.166, 18, 14, 0, Math.PI * 2, 0, Math.PI * 0.6), hairMat);
  cap.position.set(0, 0.028, -0.015);
  cap.castShadow = true;
  hair.add(cap);
  // Franja dividida ao meio
  for (let i = -2; i <= 2; i++) {
    const bang = new THREE.Mesh(new THREE.SphereGeometry(0.034, 8, 8), hairMat);
    bang.position.set(i * 0.052, 0.115, 0.12);
    bang.scale.set(1, 1.25, 0.7);
    hair.add(bang);
  }
  // Cachos longos laterais e traseiros (descem até o quadril)
  const wave = (side: number, zOff: number, len: number, r0: number) => {
    const segs = 5;
    for (let i = 0; i < segs; i++) {
      const t = i / (segs - 1);
      const r = r0 * (1 - t * 0.45);
      const s = new THREE.Mesh(new THREE.SphereGeometry(r, 10, 8), i % 2 ? hairMat : hairDarkMat);
      const wiggle = Math.sin(t * Math.PI * 2.2 + side) * 0.03;
      s.position.set(side * (0.15 + wiggle), 0.02 - t * len, -0.02 + zOff + Math.cos(t * Math.PI * 1.5) * 0.02);
      s.scale.set(0.9, 1.35, 0.85);
      s.castShadow = true;
      hair.add(s);
    }
  };
  wave(-1, 0.02, 0.72, 0.07);
  wave(1, 0.02, 0.72, 0.07);
  wave(-1, -0.09, 0.62, 0.06);
  wave(1, -0.09, 0.62, 0.06);
  // Volume traseiro
  const back = new THREE.Mesh(new THREE.SphereGeometry(0.14, 14, 12), hairDarkMat);
  back.position.set(0, -0.1, -0.1);
  back.scale.set(1.15, 1.9, 0.8);
  back.castShadow = true;
  hair.add(back);

  // Âncoras
  const headAnchor = new THREE.Object3D();
  headAnchor.position.set(0, 0.18, 0);
  head.add(headAnchor);
  const mouth = new THREE.Object3D();
  mouth.position.set(0, -0.05, 0.15);
  head.add(mouth);

  group.traverse((o) => { if (o instanceof THREE.Mesh) o.castShadow = true; });

  return {
    group,
    parts: {
      head, eyeL, eyeR, pupilL, pupilR, hair, torso, dress,
      sleeveL: aL.sleeve, sleeveR: aR.sleeve,
      armL: aL.g, armR: aR.g, legL, legR, body,
    },
    mats: { top: topMat, bottom: bottomMat, boots: bootsMat },
    anchors: { handR: aR.anchor, handL: aL.anchor, head: headAnchor, mouth },
    height: 1.08,
  };
}

/** Troca de roupa provisória: só materiais + mangas. Sem nudez. */
export function setOutfitColors(rig: GabiRig, outfit: OutfitLook) {
  rig.mats.top.color.set(outfit.top);
  rig.mats.bottom.color.set(outfit.bottom);
  rig.mats.boots.color.set(outfit.boots);
  rig.parts.sleeveL.visible = !!outfit.sleeves;
  rig.parts.sleeveR.visible = !!outfit.sleeves;
}
