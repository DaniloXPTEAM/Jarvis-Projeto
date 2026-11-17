// ============================================================
// QUARTO 3D — fiel às referências visuais entregues:
// parede esquerda verde-sálvia com prateleiras/ursinho/varal de luzes,
// cama de madeira clara com almofadas redondas rosa + manta,
// janela central com cortinas leves e pôr do sol, porta "BANHEIRO"
// em ABERTURA REAL na parede do fundo (à direita da janela),
// escrivaninha com laptop+coração e luminária, tapete redondo creme.
// A porta tem batente, maçaneta alavanca, dobradiças, placa e rodapé
// interrompido. Todos os móveis tocam o chão, escala em metros.
// ============================================================

import * as THREE from "three";

export interface RoomHandles {
  group: THREE.Group;
  hitboxes: THREE.Object3D[];
  highlightables: Map<string, THREE.MeshStandardMaterial[]>;
  doorPivot: THREE.Group;
  wardrobeDoorL: THREE.Mesh;
  wardrobeDoorR: THREE.Mesh;
  skyMat: THREE.MeshBasicMaterial;
  sunMesh: THREE.Mesh;
  moonMesh: THREE.Mesh;
  stars: THREE.Points;
  starsMat: THREE.PointsMaterial;
  sun: THREE.DirectionalLight;
  hemi: THREE.HemisphereLight;
  lampLight: THREE.PointLight;
  deskLampLight: THREE.PointLight;
  fairyMat: THREE.MeshStandardMaterial;
  laptopScreen: THREE.Group;
  foodAnchor: THREE.Object3D;
}

const C = {
  floorA: "#d7ad7e", floorB: "#c89c6c",
  wallCream: "#f3e9d8", wallGreen: "#a3b18a",
  baseboard: "#fbf6ec",
  woodLight: "#d8b184", woodMid: "#c39a6a", woodDark: "#9a7148",
  sheet: "#fdf6ec", blanket: "#e8a0a8", pillowRound: "#e7a6ad", pillow: "#fffaf2",
  rug: "#efe2cc", rugEdge: "#e6d5ba",
  doorLeaf: "#d3a874", doorFrame: "#fbf6ec",
  curtain: "#fdfaf4",
  laptop: "#b9bcc4",
};

function woodTexture(): THREE.CanvasTexture {
  const c = document.createElement("canvas");
  c.width = 256; c.height = 256;
  const g = c.getContext("2d")!;
  g.fillStyle = C.floorA; g.fillRect(0, 0, 256, 256);
  for (let row = 0; row < 8; row++) {
    const y = row * 32;
    g.fillStyle = row % 2 ? C.floorA : C.floorB;
    g.fillRect(0, y, 256, 32);
    g.fillStyle = "rgba(120,80,40,0.28)";
    g.fillRect(0, y, 256, 2);
    g.strokeStyle = "rgba(150,105,55,0.25)";
    g.lineWidth = 1;
    for (let i = 0; i < 3; i++) {
      g.beginPath();
      const yy = y + 8 + i * 8;
      g.moveTo(0, yy);
      g.bezierCurveTo(70, yy + 2, 150, yy - 2, 256, yy + 1);
      g.stroke();
    }
    const jx = (row * 97) % 256;
    g.fillStyle = "rgba(120,80,40,0.28)";
    g.fillRect(jx, y, 2, 32);
  }
  const t = new THREE.CanvasTexture(c);
  t.wrapS = t.wrapT = THREE.RepeatWrapping;
  t.repeat.set(3.2, 2.6);
  t.colorSpace = THREE.SRGBColorSpace;
  return t;
}

function textTexture(text: string, w = 256, h = 96): THREE.CanvasTexture {
  const c = document.createElement("canvas");
  c.width = w; c.height = h;
  const g = c.getContext("2d")!;
  g.fillStyle = "#f6efe2"; g.fillRect(0, 0, w, h);
  g.strokeStyle = "#b8a888"; g.lineWidth = 4; g.strokeRect(6, 6, w - 12, h - 12);
  g.fillStyle = "#6b5d45";
  g.font = "600 34px Georgia, serif";
  g.textAlign = "center"; g.textBaseline = "middle";
  g.fillText(text, w / 2, h / 2 + 2);
  const t = new THREE.CanvasTexture(c);
  t.colorSpace = THREE.SRGBColorSpace;
  return t;
}

function frameTexture(bg: string, motif: "sun" | "leaf" | "arc"): THREE.CanvasTexture {
  const c = document.createElement("canvas");
  c.width = 128; c.height = 160;
  const g = c.getContext("2d")!;
  g.fillStyle = "#fbf6ec"; g.fillRect(0, 0, 128, 160);
  g.fillStyle = bg; g.fillRect(10, 10, 108, 140);
  if (motif === "sun") {
    g.fillStyle = "#e9c46a"; g.beginPath(); g.arc(64, 70, 26, 0, Math.PI * 2); g.fill();
    g.fillStyle = "#7a8b6f"; g.fillRect(10, 108, 108, 42);
  } else if (motif === "leaf") {
    g.strokeStyle = "#5f7052"; g.lineWidth = 5;
    g.beginPath(); g.moveTo(64, 130); g.quadraticCurveTo(58, 70, 66, 34); g.stroke();
    g.fillStyle = "#7a8b6f";
    for (let i = 0; i < 5; i++) {
      g.beginPath();
      g.ellipse(64 + (i % 2 ? 16 : -16), 50 + i * 18, 13, 6, (i % 2 ? -0.5 : 0.5), 0, Math.PI * 2);
      g.fill();
    }
  } else {
    g.fillStyle = "#e8a0a8"; g.beginPath(); g.arc(64, 110, 34, Math.PI, 0); g.fill();
    g.fillStyle = "#e9c46a"; g.beginPath(); g.arc(64, 110, 20, Math.PI, 0); g.fill();
  }
  const t = new THREE.CanvasTexture(c);
  t.colorSpace = THREE.SRGBColorSpace;
  return t;
}

const std = (color: string, roughness = 0.85, metalness = 0.02) =>
  new THREE.MeshStandardMaterial({ color, roughness, metalness });

function box(w: number, h: number, d: number, material: THREE.Material, x = 0, y = 0, z = 0): THREE.Mesh {
  const m = new THREE.Mesh(new THREE.BoxGeometry(w, h, d), material);
  m.position.set(x, y, z);
  m.castShadow = true; m.receiveShadow = true;
  return m;
}
function cyl(rt: number, rb: number, h: number, material: THREE.Material, x = 0, y = 0, z = 0, seg = 18): THREE.Mesh {
  const m = new THREE.Mesh(new THREE.CylinderGeometry(rt, rb, h, seg), material);
  m.position.set(x, y, z);
  m.castShadow = true; m.receiveShadow = true;
  return m;
}

export function buildRoom(): RoomHandles {
  const group = new THREE.Group();
  const hitboxes: THREE.Object3D[] = [];
  const highlightables = new Map<string, THREE.MeshStandardMaterial[]>();

  const reg = (objectId: string, meshes: THREE.Mesh[]) => {
    const mats: THREE.MeshStandardMaterial[] = [];
    for (const m of meshes) {
      const mm = m.material as THREE.MeshStandardMaterial;
      if (mm && mm.emissive && !mats.includes(mm)) mats.push(mm);
    }
    const prev = highlightables.get(objectId) ?? [];
    highlightables.set(objectId, [...prev, ...mats]);
  };
  const hit = (objectId: string, w: number, h: number, d: number, x: number, y: number, z: number) => {
    const m = new THREE.Mesh(new THREE.BoxGeometry(w, h, d), new THREE.MeshBasicMaterial({ visible: false }));
    m.position.set(x, y, z);
    m.userData.objectId = objectId;
    group.add(m);
    hitboxes.push(m);
  };

  const RW = 6.4, RD = 5.2, WH = 2.8;
  const x0 = -RW / 2, x1 = RW / 2, zB = -RD / 2, zF = RD / 2;
  const T = 0.15;

  // ================= CHÃO (carvalho claro) =================
  const floor = new THREE.Mesh(
    new THREE.BoxGeometry(RW + 0.4, 0.1, RD + 0.4),
    new THREE.MeshStandardMaterial({ map: woodTexture(), roughness: 0.65 })
  );
  floor.position.y = -0.05;
  floor.receiveShadow = true;
  group.add(floor);

  // ================= PAREDES =================
  const creamMat = std(C.wallCream, 0.95);
  const greenMat = std(C.wallGreen, 0.95);

  // Parede esquerda VERDE (accent)
  group.add(box(T, WH, RD + T, greenMat, x0 - T / 2, WH / 2, 0));
  // Parede direita creme
  group.add(box(T, WH, RD + T, creamMat, x1 + T / 2, WH / 2, 0));
  // Parede frontal (câmera) creme
  group.add(box(RW + T, WH, T, creamMat, 0, WH / 2, zF + T / 2));

  // Parede do fundo: JANELA x[-0.75,0.10] y[1.0,2.15] + PORTA x[1.45,2.35] h2.05
  const winX0 = -0.75, winX1 = 0.1, winY0 = 1.0, winY1 = 2.15;
  const doorX0 = 1.45, doorX1 = 2.35, doorH = 2.05;
  const backMat = std(C.wallCream, 0.95);
  // segmentos
  group.add(box(winX0 - x0, WH, T, backMat, (x0 + winX0) / 2, WH / 2, zB - T / 2));
  group.add(box(doorX0 - winX1, WH, T, backMat, (winX1 + doorX0) / 2, WH / 2, zB - T / 2));
  group.add(box(x1 - doorX1, WH, T, backMat, (doorX1 + x1) / 2, WH / 2, zB - T / 2));
  group.add(box(winX1 - winX0, winY0, T, backMat, (winX0 + winX1) / 2, winY0 / 2, zB - T / 2));
  group.add(box(winX1 - winX0, WH - winY1, T, backMat, (winX0 + winX1) / 2, (WH + winY1) / 2, zB - T / 2));
  group.add(box(doorX1 - doorX0, WH - doorH, T, backMat, (doorX0 + doorX1) / 2, (WH + doorH) / 2, zB - T / 2));

  // ---- Janela: moldura branca, cruzeta, peitoril, vidro ----
  const frameMat = std("#ffffff", 0.55);
  const winCX = (winX0 + winX1) / 2;
  const frameParts = [
    box(0.07, winY1 - winY0 + 0.1, 0.2, frameMat, winX0 - 0.02, (winY0 + winY1) / 2, zB - T / 2),
    box(0.07, winY1 - winY0 + 0.1, 0.2, frameMat, winX1 + 0.02, (winY0 + winY1) / 2, zB - T / 2),
    box(winX1 - winX0 + 0.12, 0.07, 0.2, frameMat, winCX, winY1 + 0.02, zB - T / 2),
    box(winX1 - winX0 + 0.18, 0.06, 0.28, frameMat, winCX, winY0 - 0.02, zB - T / 2 + 0.04),
    box(0.04, winY1 - winY0, 0.05, frameMat, winCX, (winY0 + winY1) / 2, zB - T / 2),
    box(winX1 - winX0, 0.04, 0.05, frameMat, winCX, (winY0 + winY1) / 2, zB - T / 2),
  ];
  frameParts.forEach((m) => group.add(m));
  const glass = new THREE.Mesh(
    new THREE.BoxGeometry(winX1 - winX0, winY1 - winY0, 0.02),
    new THREE.MeshStandardMaterial({ color: "#dfeaf5", transparent: true, opacity: 0.18, roughness: 0.1 })
  );
  glass.position.set(winCX, (winY0 + winY1) / 2, zB - T / 2);
  group.add(glass);

  // Céu + sol + lua + estrelas + colinas
  const skyMat = new THREE.MeshBasicMaterial({ color: "#f2a65a" });
  const sky = new THREE.Mesh(new THREE.PlaneGeometry(7, 4.5), skyMat);
  sky.position.set(winCX, 1.7, zB - 1.6);
  group.add(sky);
  const sunMesh = new THREE.Mesh(new THREE.CircleGeometry(0.2, 24), new THREE.MeshBasicMaterial({ color: "#ffd98a" }));
  sunMesh.position.set(winCX - 0.15, 1.35, zB - 1.55);
  group.add(sunMesh);
  const moonMesh = new THREE.Mesh(new THREE.CircleGeometry(0.14, 24), new THREE.MeshBasicMaterial({ color: "#f4f6ff" }));
  moonMesh.position.set(winCX + 0.45, 1.95, zB - 1.55);
  moonMesh.visible = false;
  group.add(moonMesh);
  const starGeo = new THREE.BufferGeometry();
  const starPos = new Float32Array(60 * 3);
  for (let i = 0; i < 60; i++) {
    starPos[i * 3] = winCX - 2.5 + Math.random() * 5;
    starPos[i * 3 + 1] = 0.6 + Math.random() * 2.6;
    starPos[i * 3 + 2] = zB - 1.5;
  }
  starGeo.setAttribute("position", new THREE.BufferAttribute(starPos, 3));
  const starsMat = new THREE.PointsMaterial({ color: "#ffffff", size: 0.035, transparent: true, opacity: 0 });
  const stars = new THREE.Points(starGeo, starsMat);
  group.add(stars);
  const hill1 = new THREE.Mesh(new THREE.CircleGeometry(0.9, 3), new THREE.MeshBasicMaterial({ color: "#6f7f5e" }));
  hill1.position.set(winCX - 0.6, 0.55, zB - 1.58); hill1.rotation.z = Math.PI;
  group.add(hill1);
  const hill2 = new THREE.Mesh(new THREE.CircleGeometry(0.7, 3), new THREE.MeshBasicMaterial({ color: "#5c6f50" }));
  hill2.position.set(winCX + 0.7, 0.62, zB - 1.59); hill2.rotation.z = Math.PI;
  group.add(hill2);

  // Cortinas leves (sheer) + varão
  const curtainMat = new THREE.MeshStandardMaterial({ color: C.curtain, transparent: true, opacity: 0.45, roughness: 0.95, side: THREE.DoubleSide });
  const curtL = box(0.34, 1.85, 0.04, curtainMat, winX0 - 0.28, 1.55, zB + 0.1);
  const curtR = box(0.34, 1.85, 0.04, curtainMat, winX1 + 0.28, 1.55, zB + 0.1);
  curtL.castShadow = false; curtR.castShadow = false;
  group.add(curtL, curtR);
  const rod = cyl(0.02, 0.02, winX1 - winX0 + 1.3, std(C.woodMid, 0.5), winCX, 2.42, zB + 0.1);
  rod.rotation.z = Math.PI / 2;
  group.add(rod);
  // Planta pendente no peitoril
  const sillPot = cyl(0.06, 0.05, 0.1, std("#c96f4a", 0.8), winX1 - 0.05, winY0 + 0.07, zB + 0.05);
  group.add(sillPot);
  const vineMat = std("#5da65d", 0.9);
  for (let i = 0; i < 5; i++) {
    const leaf = new THREE.Mesh(new THREE.SphereGeometry(0.035, 8, 6), vineMat);
    leaf.position.set(winX1 - 0.02 + (i % 2) * 0.05, winY0 - 0.05 - i * 0.11, zB + 0.08);
    leaf.scale.set(1, 0.7, 0.6);
    group.add(leaf);
  }
  reg("window", [curtL, curtR, ...frameParts]);
  hit("window", 1.6, 1.7, 0.7, winCX, 1.55, zB + 0.3);

  // ---- Porta do BANHEIRO: batente, folha, alavanca, dobradiças, placa ----
  const jambMat = std(C.doorFrame, 0.6);
  const jambL = box(0.09, doorH + 0.06, 0.22, jambMat, doorX0 - 0.015, (doorH + 0.06) / 2, zB);
  const jambR = box(0.09, doorH + 0.06, 0.22, jambMat, doorX1 + 0.015, (doorH + 0.06) / 2, zB);
  const jambHead = box(doorX1 - doorX0 + 0.2, 0.09, 0.22, jambMat, (doorX0 + doorX1) / 2, doorH + 0.075, zB);
  group.add(jambL, jambR, jambHead);

  const doorPivot = new THREE.Group();
  doorPivot.position.set(doorX0 + 0.045, 0, zB);
  group.add(doorPivot);
  const leafW = doorX1 - doorX0 - 0.09;
  const leafMat = std(C.doorLeaf, 0.55);
  const leaf = box(leafW, doorH - 0.06, 0.055, leafMat, leafW / 2, (doorH - 0.06) / 2 + 0.02, 0);
  doorPivot.add(leaf);
  // Placa BANHEIRO
  const signTex = textTexture("BANHEIRO");
  const sign = new THREE.Mesh(
    new THREE.PlaneGeometry(0.42, 0.16),
    new THREE.MeshStandardMaterial({ map: signTex, roughness: 0.6 })
  );
  sign.position.set(leafW / 2, 1.62, 0.031);
  doorPivot.add(sign);
  // Alavanca prateada (lado direito da folha)
  const knobMat = new THREE.MeshStandardMaterial({ color: "#cfd2d8", roughness: 0.3, metalness: 0.75 });
  const lever = new THREE.Mesh(new THREE.BoxGeometry(0.13, 0.022, 0.028), knobMat);
  lever.position.set(leafW - 0.16, 0.98, 0.055);
  doorPivot.add(lever);
  const leverBase = new THREE.Mesh(new THREE.CylinderGeometry(0.028, 0.028, 0.03, 12), knobMat);
  leverBase.rotation.x = Math.PI / 2;
  leverBase.position.set(leafW - 0.11, 0.98, 0.04);
  doorPivot.add(leverBase);
  // Dobradiças
  const hingeMat = new THREE.MeshStandardMaterial({ color: "#9a9aa4", roughness: 0.4, metalness: 0.6 });
  for (const hy of [0.45, 1.6]) {
    const hinge = new THREE.Mesh(new THREE.CylinderGeometry(0.018, 0.018, 0.11, 10), hingeMat);
    hinge.position.set(0.012, hy, 0);
    doorPivot.add(hinge);
  }
  reg("bathroom-door", [leaf, jambL, jambR]);
  hit("bathroom-door", 1.3, 2.2, 0.9, (doorX0 + doorX1) / 2, 1.1, zB + 0.25);

  // Corredor do banheiro (fora de vista) + glow
  const hallFloor = box(doorX1 - doorX0 + 0.3, 0.06, 1.4, std("#e3d3bd", 0.95), (doorX0 + doorX1) / 2, -0.03, zB - 0.7);
  hallFloor.castShadow = false;
  group.add(hallFloor);
  const hallBack = new THREE.Mesh(
    new THREE.PlaneGeometry(doorX1 - doorX0 + 0.6, doorH + 0.3),
    new THREE.MeshBasicMaterial({ color: "#3a3348" })
  );
  hallBack.position.set((doorX0 + doorX1) / 2, doorH / 2, zB - 1.35);
  group.add(hallBack);
  const bathGlow = new THREE.Mesh(
    new THREE.PlaneGeometry(doorX1 - doorX0, doorH),
    new THREE.MeshBasicMaterial({ color: "#ffe9c4", transparent: true, opacity: 0 })
  );
  bathGlow.position.set((doorX0 + doorX1) / 2, doorH / 2, zB - 0.35);
  group.add(bathGlow);
  group.userData.bathGlow = bathGlow;

  // ---- Rodapés (interrompidos na porta) ----
  const bbMat = std(C.baseboard, 0.7);
  const bbH = 0.09, bbT = 0.03;
  group.add(box(winX0 - x0 - 0.02, bbH, bbT, bbMat, (x0 + winX0) / 2, bbH / 2, zB + bbT / 2));
  group.add(box(doorX0 - winX1 - 0.02, bbH, bbT, bbMat, (winX1 + doorX0) / 2, bbH / 2, zB + bbT / 2));
  group.add(box(x1 - doorX1 - 0.02, bbH, bbT, bbMat, (doorX1 + x1) / 2, bbH / 2, zB + bbT / 2));
  group.add(box(bbT, bbH, RD, bbMat, x0 + bbT / 2, bbH / 2, 0));
  group.add(box(bbT, bbH, RD, bbMat, x1 - bbT / 2, bbH / 2, 0));

  // ================= CAMA (esquerda, cabeceira na parede verde) =================
  const bedMeshes: THREE.Mesh[] = [];
  const bedCX = -2.32, bedCZ = -1.28;
  const bedFrameMat = std(C.woodLight, 0.65);
  // Estrado: cabeceira na parede esquerda (x = -3.05)
  bedMeshes.push(box(1.5, 0.24, 1.5, bedFrameMat, bedCX, 0.28, bedCZ));
  bedMeshes.push(box(0.1, 1.0, 1.56, bedFrameMat, x0 + 0.14, 0.55, bedCZ)); // cabeceira
  bedMeshes.push(box(0.12, 0.12, 1.56, std(C.woodMid, 0.65), x0 + 0.14, 1.06, bedCZ));
  for (const [px, pz] of [[-0.66, -0.66], [0.66, -0.66], [-0.66, 0.66], [0.66, 0.66]]) {
    bedMeshes.push(cyl(0.045, 0.04, 0.2, bedFrameMat, bedCX + px, 0.1, bedCZ + pz, 10));
  }
  bedMeshes.push(box(1.42, 0.22, 1.42, std(C.sheet, 0.9), bedCX + 0.02, 0.49, bedCZ)); // colchão
  // Manta rosa dobrada no lado dos pés (leste)
  bedMeshes.push(box(0.78, 0.09, 1.46, std(C.blanket, 0.92), bedCX + 0.35, 0.6, bedCZ));
  // Travesseiros: 2 retangulares + 2 redondos rosa
  const pil1 = box(0.28, 0.3, 0.62, std(C.pillow, 0.95), bedCX - 0.58, 0.66, bedCZ);
  pil1.rotation.z = 0.18;
  bedMeshes.push(pil1);
  const rnd1 = cyl(0.2, 0.2, 0.14, std(C.pillowRound, 0.95), bedCX - 0.42, 0.66, bedCZ - 0.28, 20);
  rnd1.rotation.z = Math.PI / 2;
  bedMeshes.push(rnd1);
  const rnd2 = cyl(0.2, 0.2, 0.14, std(C.pillowRound, 0.95), bedCX - 0.42, 0.66, bedCZ + 0.28, 20);
  rnd2.rotation.z = Math.PI / 2;
  bedMeshes.push(rnd2);
  bedMeshes.forEach((m) => group.add(m));
  reg("bed", bedMeshes);
  hit("bed", 1.8, 1.1, 1.8, bedCX, 0.55, bedCZ);

  // Banquinho lateral com plantinha
  const stoolMat = std(C.woodMid, 0.7);
  const stoolTop = cyl(0.22, 0.22, 0.05, stoolMat, -1.28, 0.42, -1.95, 16);
  group.add(stoolTop);
  for (let i = 0; i < 4; i++) {
    const a = (i / 4) * Math.PI * 2;
    const leg = cyl(0.022, 0.025, 0.42, stoolMat, -1.28 + Math.cos(a) * 0.15, 0.2, -1.95 + Math.sin(a) * 0.15, 8);
    leg.rotation.z = Math.cos(a) * 0.12;
    leg.rotation.x = -Math.sin(a) * 0.12;
    group.add(leg);
  }
  const spPot = cyl(0.07, 0.06, 0.11, std("#d8c2a0", 0.85), -1.28, 0.5, -1.95, 12);
  const spLeaf = new THREE.Mesh(new THREE.SphereGeometry(0.1, 10, 8), std("#5da65d", 0.9));
  spLeaf.position.set(-1.28, 0.62, -1.95);
  spLeaf.castShadow = true;
  group.add(spPot, spLeaf);
  reg("bed", [stoolTop, spPot]);

  // ================= PAREDE VERDE: prateleiras, ursinho, varal de luzes, quadros =================
  const shelfMat = std(C.woodMid, 0.6);
  const sh1 = box(0.24, 0.05, 1.1, shelfMat, x0 + 0.13, 1.72, -0.7);
  const sh2 = box(0.24, 0.05, 0.9, shelfMat, x0 + 0.13, 2.0, 0.75);
  group.add(sh1, sh2);
  // Livros na prateleira 1
  const bookCols = ["#d98a5f", "#7a8b6f", "#e8a0a8", "#c9b27e", "#8aa0b8"];
  bookCols.forEach((col, i) => {
    group.add(box(0.16, 0.26 - (i % 3) * 0.03, 0.06, std(col, 0.85), x0 + 0.13, 1.88 - (i % 3) * 0.015, -1.05 + i * 0.085));
  });
  // Ursinho de pelúcia
  const bearMat = std("#b08a5a", 0.95);
  const bearBody = new THREE.Mesh(new THREE.SphereGeometry(0.13, 12, 10), bearMat);
  bearBody.position.set(x0 + 0.13, 1.87, -0.5); bearBody.castShadow = true;
  const bearHead = new THREE.Mesh(new THREE.SphereGeometry(0.1, 12, 10), bearMat);
  bearHead.position.set(x0 + 0.13, 2.06, -0.5); bearHead.castShadow = true;
  const earL = new THREE.Mesh(new THREE.SphereGeometry(0.035, 8, 8), bearMat);
  earL.position.set(x0 + 0.06, 2.13, -0.5);
  const earR = earL.clone(); earR.position.x = x0 + 0.2;
  group.add(bearBody, bearHead, earL, earR);
  // Planta pendente na prateleira 2
  const hPot = cyl(0.07, 0.06, 0.1, std("#c96f4a", 0.8), x0 + 0.13, 2.07, 0.95, 12);
  group.add(hPot);
  for (let i = 0; i < 6; i++) {
    const lf = new THREE.Mesh(new THREE.SphereGeometry(0.03, 8, 6), vineMat);
    lf.position.set(x0 + 0.16 + (i % 2) * 0.04, 2.0 - i * 0.09, 1.0 + (i % 3) * 0.02);
    lf.scale.set(1, 0.7, 0.6);
    group.add(lf);
  }
  // Quadros
  const mkFrame = (tex: THREE.Texture, h: number, y: number, z: number) => {
    const fr = box(0.04, h + 0.08, h * 0.82 + 0.08, std(C.woodLight, 0.6), x0 + 0.03, y, z);
    const art = new THREE.Mesh(new THREE.PlaneGeometry(h * 0.82, h), new THREE.MeshStandardMaterial({ map: tex, roughness: 0.8 }));
    art.rotation.y = Math.PI / 2;
    art.position.set(x0 + 0.055, y, z);
    group.add(fr, art);
  };
  mkFrame(frameTexture("#f2d5b8", "sun"), 0.42, 1.55, 0.15);
  mkFrame(frameTexture("#e8efe2", "leaf"), 0.3, 1.15, -1.55);
  mkFrame(frameTexture("#fbe9e2", "arc"), 0.26, 1.28, -1.95);
  // Quadro na parede do fundo (esquerda da janela)
  const frB = box(0.44, 0.56, 0.04, std(C.woodLight, 0.6), -1.6, 1.75, zB + 0.03);
  const artB = new THREE.Mesh(new THREE.PlaneGeometry(0.38, 0.5), new THREE.MeshStandardMaterial({ map: frameTexture("#e8efe2", "leaf"), roughness: 0.8 }));
  artB.position.set(-1.6, 1.75, zB + 0.055);
  group.add(frB, artB);

  // Varal de luzes (fairy lights) na parede verde
  const fairyMat = new THREE.MeshStandardMaterial({ color: "#fff2c9", emissive: "#ffcf7a", emissiveIntensity: 0.6, roughness: 0.4 });
  const wire = new THREE.Mesh(new THREE.CylinderGeometry(0.006, 0.006, 3.4, 6), std("#6b5d45", 0.8));
  wire.rotation.x = Math.PI / 2;
  wire.position.set(x0 + 0.05, 2.32, -0.1);
  wire.castShadow = false;
  group.add(wire);
  for (let i = 0; i <= 12; i++) {
    const t = i / 12;
    const z = -1.75 + t * 3.2;
    const sag = Math.sin(t * Math.PI) * 0.22;
    const bulb = new THREE.Mesh(new THREE.SphereGeometry(0.028, 8, 8), fairyMat);
    bulb.position.set(x0 + 0.07, 2.3 - sag, z);
    group.add(bulb);
  }

  // ================= GUARDA-ROUPA (parede direita, sul) =================
  const wardX = 2.85, wardCZ = 1.8;
  const wardMat = std(C.woodMid, 0.6);
  const wardMeshes = [
    box(0.6, 2.0, 1.4, wardMat, wardX, 1.0, wardCZ),
    box(0.66, 0.08, 1.46, std(C.woodDark, 0.6), wardX, 2.03, wardCZ),
    box(0.66, 0.1, 1.46, std(C.woodDark, 0.6), wardX, 0.05, wardCZ),
  ];
  wardMeshes.forEach((m) => group.add(m));
  const wardrobeDoorL = box(0.04, 1.8, 0.68, std(C.woodLight, 0.6), wardX - 0.32, 1.02, wardCZ - 0.35);
  const wardrobeDoorR = box(0.04, 1.8, 0.68, std(C.woodLight, 0.6), wardX - 0.32, 1.02, wardCZ + 0.35);
  group.add(wardrobeDoorL, wardrobeDoorR);
  const knobWMat = new THREE.MeshStandardMaterial({ color: "#5b3a1e", roughness: 0.5 });
  const wk1 = new THREE.Mesh(new THREE.SphereGeometry(0.03, 10, 8), knobWMat);
  wk1.position.set(-0.04, 0, 0.28);
  wardrobeDoorL.add(wk1);
  const wk2 = new THREE.Mesh(new THREE.SphereGeometry(0.03, 10, 8), knobWMat);
  wk2.position.set(-0.04, 0, -0.28);
  wardrobeDoorR.add(wk2);
  // Interior com roupas penduradas
  const interior = new THREE.Mesh(
    new THREE.PlaneGeometry(1.3, 1.7),
    new THREE.MeshStandardMaterial({ color: "#4a3a28", roughness: 0.95 })
  );
  interior.rotation.y = -Math.PI / 2;
  interior.position.set(wardX - 0.26, 1.02, wardCZ);
  group.add(interior);
  const clothCols = ["#d92b2b", "#b07fe0", "#e0a11f", "#9cc3e8"];
  clothCols.forEach((col, i) => {
    const hanging = box(0.03, 0.5, 0.24, std(col, 0.9), wardX - 0.28, 1.25, wardCZ - 0.45 + i * 0.3);
    hanging.castShadow = false;
    group.add(hanging);
  });
  group.add(box(0.04, 0.04, 1.3, std(C.woodDark, 0.8), wardX - 0.28, 1.52, wardCZ));
  reg("wardrobe", [...wardMeshes, wardrobeDoorL, wardrobeDoorR]);
  hit("wardrobe", 0.9, 2.1, 1.6, wardX - 0.1, 1.05, wardCZ);

  // ================= ESCRIVANINHA (parede direita) + LAPTOP =================
  const deskX = 2.85, deskCZ = -0.3;
  const deskMat = std(C.woodLight, 0.6);
  const deskMeshes = [
    box(0.62, 0.06, 1.7, deskMat, deskX, 0.74, deskCZ),
    box(0.55, 0.7, 0.06, std(C.woodMid, 0.7), deskX, 0.37, deskCZ - 0.8),
    box(0.55, 0.7, 0.06, std(C.woodMid, 0.7), deskX, 0.37, deskCZ + 0.8),
    box(0.5, 0.5, 0.5, std(C.woodMid, 0.7), deskX, 0.45, deskCZ + 0.55), // gaveteiro
  ];
  deskMeshes.forEach((m) => group.add(m));
  // Laptop com adesivo de coração
  const laptop = new THREE.Group();
  laptop.position.set(2.78, 0.77, -0.25);
  laptop.rotation.y = -Math.PI / 2;
  const lapBase = box(0.3, 0.02, 0.22, std(C.laptop, 0.4), 0, 0, 0);
  const screenPivot = new THREE.Group();
  screenPivot.position.set(0, 0.01, -0.11);
  const screen = box(0.3, 0.2, 0.012, std(C.laptop, 0.4), 0, 0.1, 0);
  const heart = new THREE.Mesh(
    new THREE.CircleGeometry(0.035, 20),
    new THREE.MeshBasicMaterial({ color: "#f5f5f7" })
  );
  heart.position.set(0, 0.1, -0.008);
  heart.rotation.y = Math.PI;
  screenPivot.add(screen, heart);
  screenPivot.rotation.x = -1.85; // fechado por padrão
  laptop.add(lapBase, screenPivot);
  laptop.userData.screen = screenPivot;
  group.add(laptop);
  // Luminária amarela
  const dlBase = cyl(0.07, 0.09, 0.04, std("#e9c46a", 0.5), 2.8, 0.79, 0.35, 12);
  const dlArm = cyl(0.014, 0.014, 0.42, std("#e9c46a", 0.5), 2.8, 0.98, 0.35, 8);
  dlArm.rotation.z = 0.4;
  const dlHead = new THREE.Mesh(new THREE.ConeGeometry(0.1, 0.14, 14, 1, true), std("#e9c46a", 0.5));
  dlHead.position.set(2.7, 1.16, 0.35);
  dlHead.rotation.z = 2.4;
  group.add(dlBase, dlArm, dlHead);
  // Porta-lápis
  const cup = cyl(0.05, 0.045, 0.1, std("#7a8b6f", 0.7), 2.85, 0.82, -0.85, 12);
  group.add(cup);
  for (let i = 0; i < 3; i++) {
    const pencil = cyl(0.008, 0.008, 0.15, std(["#e9c46a", "#d92b2b", "#3a86ff"][i], 0.7), 2.84 + i * 0.022, 0.9, -0.85, 6);
    group.add(pencil);
  }
  reg("desk", deskMeshes);
  hit("desk", 0.9, 1.0, 1.8, deskX - 0.1, 0.6, deskCZ);

  // Cadeira com rodinhas e almofada rosa
  const chairX = 2.15, chairZ = -0.3;
  const chairMat = std(C.woodLight, 0.65);
  const chairMeshes = [
    box(0.46, 0.06, 0.44, chairMat, chairX, 0.48, chairZ),
    box(0.44, 0.42, 0.06, chairMat, chairX + 0.21, 0.72, chairZ), // encosto (lado leste)
    new THREE.Mesh(new THREE.CylinderGeometry(0.2, 0.2, 0.07, 16), std(C.pillowRound, 0.95)),
  ];
  (chairMeshes[2] as THREE.Mesh).position.set(chairX, 0.545, chairZ);
  const postMesh = cyl(0.03, 0.03, 0.34, std("#4a4a55", 0.5), chairX, 0.28, chairZ, 10);
  group.add(postMesh);
  for (let i = 0; i < 5; i++) {
    const a = (i / 5) * Math.PI * 2;
    const legc = box(0.22, 0.03, 0.04, std("#4a4a55", 0.5), chairX + Math.cos(a) * 0.14, 0.06, chairZ + Math.sin(a) * 0.14);
    legc.rotation.y = -a;
    group.add(legc);
    const wheel = new THREE.Mesh(new THREE.SphereGeometry(0.032, 8, 8), std("#2b2b2b", 0.6));
    wheel.position.set(chairX + Math.cos(a) * 0.24, 0.032, chairZ + Math.sin(a) * 0.24);
    group.add(wheel);
  }
  chairMeshes.forEach((m) => group.add(m));
  reg("chair", chairMeshes);
  hit("chair", 0.7, 0.95, 0.7, chairX, 0.45, chairZ);

  // Prateleira alta com planta (parede direita, norte)
  const shR = box(0.24, 0.05, 0.9, shelfMat, x1 - 0.13, 2.05, -1.6);
  group.add(shR);
  const rpPot = cyl(0.08, 0.07, 0.12, std("#c96f4a", 0.8), x1 - 0.13, 2.13, -1.75, 12);
  group.add(rpPot);
  for (let i = 0; i < 5; i++) {
    const lf = new THREE.Mesh(new THREE.SphereGeometry(0.035, 8, 6), vineMat);
    lf.position.set(x1 - 0.1 + (i % 2) * 0.04, 2.05 - i * 0.1, -1.78);
    lf.scale.set(1, 0.7, 0.6);
    group.add(lf);
  }

  // ================= TAPETE REDONDO CREME (centro) =================
  const rug = cyl(1.32, 1.32, 0.035, std(C.rug, 0.98), 0.05, 0.018, 0.5, 40);
  rug.castShadow = false;
  const rugEdge = cyl(1.36, 1.36, 0.02, std(C.rugEdge, 0.98), 0.05, 0.011, 0.5, 40);
  rugEdge.castShadow = false;
  group.add(rug, rugEdge);
  reg("rug", [rug]);
  hit("rug", 2.4, 0.4, 2.4, 0.05, 0.2, 0.5);
  // Blocos e ursinho na beirada do tapete
  const blockCols = ["#e25555", "#3a86ff", "#ffbe0b", "#5da65d"];
  blockCols.forEach((col, i) => {
    const b = box(0.13, 0.13, 0.13, std(col, 0.8), -0.75 + (i % 2) * 0.18, 0.1 + (i > 1 ? 0.13 : 0), 1.05 + Math.floor(i / 2) * 0.16);
    b.rotation.y = i * 0.4;
    group.add(b);
  });
  const tbMat = std("#b08a5a", 0.95);
  const tb = new THREE.Group();
  const tbB = new THREE.Mesh(new THREE.SphereGeometry(0.1, 12, 10), tbMat);
  tbB.position.y = 0.12; tbB.castShadow = true;
  const tbH = new THREE.Mesh(new THREE.SphereGeometry(0.08, 12, 10), tbMat);
  tbH.position.y = 0.27; tbH.castShadow = true;
  tb.add(tbB, tbH);
  tb.position.set(0.75, 0.03, 0.95);
  group.add(tb);

  // ================= MESINHA DE ALIMENTAÇÃO (frente, centro-esquerda) =================
  const foodX = -0.55, foodZ = 1.85;
  const foodMeshes = [
    cyl(0.42, 0.42, 0.05, std("#fff3df", 0.7), foodX, 0.5, foodZ, 24),
    cyl(0.05, 0.07, 0.48, std(C.woodMid, 0.7), foodX, 0.25, foodZ, 12),
    cyl(0.2, 0.24, 0.04, std(C.woodMid, 0.7), foodX, 0.02, foodZ, 16),
  ];
  foodMeshes.forEach((m) => group.add(m));
  const stool2 = cyl(0.19, 0.22, 0.3, std("#e8a0a8", 0.85), foodX, 0.15, foodZ + 0.55, 16);
  group.add(stool2);
  const cloth = cyl(0.3, 0.32, 0.02, std("#f2d5b8", 0.95), foodX, 0.53, foodZ, 20);
  cloth.castShadow = false;
  group.add(cloth);
  const plate = cyl(0.14, 0.11, 0.025, std("#ffffff", 0.4), foodX, 0.55, foodZ, 20);
  group.add(plate);
  reg("food-table", [...foodMeshes, stool2]);
  hit("food-table", 1.0, 0.9, 1.2, foodX, 0.45, foodZ + 0.25);
  const foodAnchor = new THREE.Object3D();
  foodAnchor.position.set(foodX, 0.58, foodZ);
  group.add(foodAnchor);

  // ================= BAÚ + PLANTA GRANDE (frente-esquerda) =================
  const chestX = -2.62, chestZ = 1.85;
  const chestMeshes = [
    box(0.8, 0.42, 0.55, std(C.woodMid, 0.7), chestX, 0.21, chestZ),
    box(0.84, 0.1, 0.59, std(C.woodDark, 0.7), chestX, 0.47, chestZ),
  ];
  chestMeshes.forEach((m) => group.add(m));
  reg("toy-chest", chestMeshes);
  hit("toy-chest", 0.9, 0.7, 0.65, chestX, 0.3, chestZ);
  // Monstera em cesto de palha
  const basket = cyl(0.22, 0.18, 0.32, std("#c9b27e", 0.95), -1.7, 0.16, 2.25, 14);
  group.add(basket);
  const monsteraMat = std("#3f7d4e", 0.85);
  for (let i = 0; i < 6; i++) {
    const a = (i / 6) * Math.PI * 2;
    const lf = new THREE.Mesh(new THREE.SphereGeometry(0.14, 10, 8), monsteraMat);
    lf.position.set(-1.7 + Math.cos(a) * 0.14, 0.5 + (i % 3) * 0.12, 2.25 + Math.sin(a) * 0.14);
    lf.scale.set(1, 0.55, 0.8);
    lf.castShadow = true;
    group.add(lf);
  }

  // ================= LUMINÁRIA PENDENTE =================
  const cord = cyl(0.012, 0.012, 0.9, std("#4a4a55", 0.6), 0.1, 3.1, 0.4, 6);
  cord.castShadow = false;
  const shadeMat = std("#f6e7cf", 0.9);
  const shade = cyl(0.12, 0.3, 0.22, shadeMat, 0.1, 2.55, 0.4, 18);
  const bulb = new THREE.Mesh(new THREE.SphereGeometry(0.06, 12, 10), new THREE.MeshBasicMaterial({ color: "#fff6d8" }));
  bulb.position.set(0.1, 2.48, 0.4);
  group.add(cord, shade, bulb);

  // ================= LUZES =================
  const hemi = new THREE.HemisphereLight("#fff4e0", "#8a6a4a", 0.75);
  group.add(hemi);
  const sun = new THREE.DirectionalLight("#ffedd0", 1.5);
  sun.position.set(-1.0, 4.0, -4.8);
  sun.target.position.set(0.3, 0, 0.6);
  sun.castShadow = true;
  sun.shadow.mapSize.set(1024, 1024);
  sun.shadow.camera.left = -4; sun.shadow.camera.right = 4;
  sun.shadow.camera.top = 4; sun.shadow.camera.bottom = -4;
  sun.shadow.camera.far = 15;
  sun.shadow.bias = -0.002;
  group.add(sun, sun.target);
  const lampLight = new THREE.PointLight("#ffd9a0", 0.4, 9, 1.6);
  lampLight.position.set(0.1, 2.4, 0.4);
  group.add(lampLight);
  const deskLampLight = new THREE.PointLight("#ffe9b0", 0.0, 3.2, 1.8);
  deskLampLight.position.set(2.65, 1.1, 0.35);
  group.add(deskLampLight);

  return {
    group, hitboxes, highlightables,
    doorPivot, wardrobeDoorL, wardrobeDoorR,
    skyMat, sunMesh, moonMesh, stars, starsMat,
    sun, hemi, lampLight, deskLampLight, fairyMat,
    laptopScreen: screenPivot, foodAnchor,
  };
}

// ---- Props de comida (sobre a mesa) e de mão ----
export function createFoodProp(foodId: string): THREE.Group {
  const g = new THREE.Group();
  const add = (m: THREE.Mesh) => { m.castShadow = true; g.add(m); return m; };
  switch (foodId) {
    case "pizza": {
      add(new THREE.Mesh(new THREE.CylinderGeometry(0.11, 0.11, 0.025, 14), std("#f2c14e", 0.8))).position.y = 0.02;
      for (let i = 0; i < 4; i++) {
        const pep = new THREE.Mesh(new THREE.CylinderGeometry(0.018, 0.018, 0.01, 8), std("#c9401c", 0.8));
        pep.position.set(Math.cos(i * 1.7) * 0.05, 0.038, Math.sin(i * 1.7) * 0.05);
        g.add(pep);
      }
      break;
    }
    case "fruits": {
      add(new THREE.Mesh(new THREE.SphereGeometry(0.05, 12, 10), std("#e25555", 0.6))).position.set(-0.04, 0.05, 0);
      const banana = add(new THREE.Mesh(new THREE.SphereGeometry(0.045, 12, 10), std("#ffbe0b", 0.7)));
      banana.scale.set(1.3, 0.8, 0.8);
      banana.position.set(0.05, 0.04, 0.02);
      break;
    }
    case "sandwich": {
      add(new THREE.Mesh(new THREE.BoxGeometry(0.11, 0.03, 0.08), std("#f2c14e", 0.85))).position.y = 0.03;
      const fill = new THREE.Mesh(new THREE.BoxGeometry(0.115, 0.02, 0.085), std("#5da65d", 0.85));
      fill.position.y = 0.05; g.add(fill);
      add(new THREE.Mesh(new THREE.BoxGeometry(0.11, 0.03, 0.08), std("#e8a94e", 0.85))).position.y = 0.07;
      break;
    }
    case "cake": {
      const slice = add(new THREE.Mesh(new THREE.CylinderGeometry(0.08, 0.08, 0.07, 3), std("#f7a8c4", 0.85)));
      slice.position.y = 0.05; slice.rotation.y = 0.5;
      const cherry = new THREE.Mesh(new THREE.SphereGeometry(0.018, 8, 8), std("#c40f3c", 0.5));
      cherry.position.y = 0.095; g.add(cherry);
      break;
    }
    case "water":
    case "juice": {
      add(new THREE.Mesh(new THREE.CylinderGeometry(0.045, 0.035, 0.11, 12), std(foodId === "water" ? "#bfe3ff" : "#ffb35c", 0.5))).position.y = 0.06;
      break;
    }
    default: {
      add(new THREE.Mesh(new THREE.CylinderGeometry(0.09, 0.06, 0.06, 14), std("#9cc3e8", 0.6))).position.y = 0.04;
      const cereal = new THREE.Mesh(new THREE.SphereGeometry(0.065, 12, 8, 0, Math.PI * 2, 0, Math.PI / 2), std("#f2c14e", 0.9));
      cereal.position.y = 0.065; g.add(cereal);
      break;
    }
  }
  return g;
}

export function createHandProp(kind: "food" | "cup" | "pencil" | "toy", foodId?: string): THREE.Group {
  const g = new THREE.Group();
  if (kind === "pencil") {
    const p = new THREE.Mesh(new THREE.CylinderGeometry(0.012, 0.012, 0.14, 6), std("#ffbe0b", 0.7));
    p.rotation.x = 0.6;
    g.add(p);
  } else if (kind === "cup") {
    const c = new THREE.Mesh(new THREE.CylinderGeometry(0.035, 0.028, 0.08, 10), std(foodId === "juice" ? "#ffb35c" : "#bfe3ff", 0.5));
    g.add(c);
  } else if (kind === "toy") {
    const t = new THREE.Mesh(new THREE.BoxGeometry(0.09, 0.09, 0.09), std("#e25555", 0.8));
    t.rotation.set(0.4, 0.5, 0.1);
    g.add(t);
  } else {
    const inner = createFoodProp(foodId ?? "meal");
    inner.scale.setScalar(0.8);
    g.add(inner);
  }
  return g;
}

// ================= ILUMINAÇÃO POR HORÁRIO =================
interface LightKey {
  hour: number;
  sky: string; sun: string; sunI: number; hemiI: number;
  lampI: number; stars: number; sunPos: [number, number]; moon: boolean;
}

const KEYS: LightKey[] = [
  { hour: 0, sky: "#1b2440", sun: "#8fa8ff", sunI: 0.12, hemiI: 0.28, lampI: 0.9, stars: 1, sunPos: [0, -1], moon: true },
  { hour: 5, sky: "#3a4a6e", sun: "#b0a8d8", sunI: 0.25, hemiI: 0.4, lampI: 0.8, stars: 0.6, sunPos: [-1.4, 0.4], moon: true },
  { hour: 6.5, sky: "#f7b267", sun: "#ffb37a", sunI: 0.9, hemiI: 0.6, lampI: 0.25, stars: 0, sunPos: [-1.1, 0.9], moon: false },
  { hour: 9, sky: "#a7d3ee", sun: "#ffedd0", sunI: 1.5, hemiI: 0.8, lampI: 0.0, stars: 0, sunPos: [-0.4, 1.9], moon: false },
  { hour: 13, sky: "#9ccbe8", sun: "#fff6e0", sunI: 1.7, hemiI: 0.85, lampI: 0.0, stars: 0, sunPos: [0.3, 2.2], moon: false },
  { hour: 17, sky: "#f2a65a", sun: "#ff9d5c", sunI: 1.2, hemiI: 0.68, lampI: 0.15, stars: 0, sunPos: [0.9, 1.1], moon: false },
  { hour: 18.5, sky: "#b06a8e", sun: "#e88a9a", sunI: 0.5, hemiI: 0.5, lampI: 0.55, stars: 0.2, sunPos: [1.3, 0.4], moon: false },
  { hour: 20, sky: "#232a4d", sun: "#8fa8ff", sunI: 0.15, hemiI: 0.32, lampI: 0.95, stars: 0.9, sunPos: [0, -1], moon: true },
  { hour: 24, sky: "#1b2440", sun: "#8fa8ff", sunI: 0.12, hemiI: 0.28, lampI: 0.9, stars: 1, sunPos: [0, -1], moon: true },
];

const tmpA = new THREE.Color();
const tmpB = new THREE.Color();

export function updateRoomLighting(h: RoomHandles, gameMinutes: number, studying: boolean) {
  const hour = (gameMinutes / 60) % 24;
  let a = KEYS[0], b = KEYS[KEYS.length - 1];
  for (let i = 0; i < KEYS.length - 1; i++) {
    if (hour >= KEYS[i].hour && hour <= KEYS[i + 1].hour) { a = KEYS[i]; b = KEYS[i + 1]; break; }
  }
  const t = b.hour === a.hour ? 0 : (hour - a.hour) / (b.hour - a.hour);
  const li = (x: number, y: number) => x + (y - x) * t;

  h.skyMat.color.copy(tmpA.set(a.sky)).lerp(tmpB.set(b.sky), t);
  h.sun.color.copy(tmpA.set(a.sun)).lerp(tmpB.set(b.sun), t);
  h.sun.intensity = li(a.sunI, b.sunI);
  h.hemi.intensity = li(a.hemiI, b.hemiI);
  h.lampLight.intensity = li(a.lampI, b.lampI);
  h.starsMat.opacity = li(a.stars, b.stars);
  h.fairyMat.emissiveIntensity = 0.3 + li(a.lampI, b.lampI) * 1.4;
  const moonOn = t < 0.5 ? a.moon : b.moon;
  h.moonMesh.visible = moonOn;
  h.sunMesh.visible = !moonOn && li(a.sunPos[1], b.sunPos[1]) > 0.2;
  if (h.sunMesh.visible) {
    h.sunMesh.position.x = -0.32 + li(a.sunPos[0], b.sunPos[0]) * 0.5;
    h.sunMesh.position.y = 0.95 + li(a.sunPos[1], b.sunPos[1]) * 0.5;
  }
  const arc = t < 0.5 ? a.sunPos : b.sunPos;
  h.sun.position.set(-0.8 + arc[0] * 1.5, 2.2 + Math.max(0.3, arc[1]) * 1.4, -4.8);
  h.sun.target.position.set(0.3, 0, 0.6);
  h.deskLampLight.intensity += ((studying ? 0.95 : 0.25) - h.deskLampLight.intensity) * 0.05;
}
