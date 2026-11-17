// ============================================================
// GAME CANVAS — renderização Three.js + loop principal.
// Responsável por: renderer, câmera touch, raycast de toques,
// sincronizar Gabi/props/porta/luzes com o engine, e PARAR TUDO
// ao desmontar (salva, cancela RAF, dispose, silencia áudio).
// ============================================================

import { useEffect, useRef } from "react";
import * as THREE from "three";
import { buildRoom, createFoodProp, createHandProp, updateRoomLighting } from "./roomBuilder";
import { buildProvisionalGabi, type GabiRig } from "./gabiProvisional";
import { GABI_MODEL_URL } from './gabi/age_01/modelUrl';
import { GabiController } from "./gabi/GabiController";
import { NAV_POINTS } from "./navigation";
import type { GameEngine } from "./engine";
import { OUTFITS } from "./engine";
import { audio } from "./audio";

interface Props {
  engine: GameEngine;
  inspect: { showNav: boolean; showLabels: boolean };
  onFps?: (fps: number) => void;
}

export default function GameCanvas({ engine, inspect, onFps }: Props) {
  const mountRef = useRef<HTMLDivElement>(null);
  const inspectRef = useRef(inspect);
  inspectRef.current = inspect;
  const fpsRef = useRef(onFps);
  fpsRef.current = onFps;

  useEffect(() => {
    const mount = mountRef.current;
    if (!mount) return;

    // ---------- renderer ----------
    const renderer = new THREE.WebGLRenderer({ antialias: true, powerPreference: "high-performance" });
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    renderer.shadowMap.enabled = true;
    renderer.shadowMap.type = THREE.PCFSoftShadowMap;
    renderer.outputColorSpace = THREE.SRGBColorSpace;
    mount.appendChild(renderer.domElement);
    renderer.domElement.style.touchAction = "none";

    const scene = new THREE.Scene();
    scene.background = new THREE.Color("#241d28");

    const camera = new THREE.PerspectiveCamera(50, 1, 0.1, 60);

    // ---------- quarto + Gabi ----------
    const room = buildRoom();
    scene.add(room.group);
    const outfit = OUTFITS.find((o) => o.id === "red-dress") ?? OUTFITS[0];
    const rig: GabiRig = buildProvisionalGabi(outfit.colors);
    scene.add(rig.group);

    const driver = new GabiController(rig);
    void driver.load("age_01", "red_dress", import.meta.env.BASE_URL, GABI_MODEL_URL);
    rig.group.userData.gabiV1 = driver.status;

    // Marcadores de navegação (modo inspeção)
    const navGroup = new THREE.Group();
    navGroup.visible = false;
    scene.add(navGroup);
    const navDotGeo = new THREE.SphereGeometry(0.045, 10, 8);
    const navMatA = new THREE.MeshBasicMaterial({ color: "#22d3ee" });
    const navMatP = new THREE.MeshBasicMaterial({ color: "#f472b6" });
    for (const p of Object.values(NAV_POINTS)) {
      const a = new THREE.Mesh(navDotGeo, navMatA);
      a.position.set(p.approach.x, 0.06, p.approach.z);
      navGroup.add(a);
      const pl = new THREE.Mesh(navDotGeo, navMatP);
      pl.position.set(p.placement.x, 0.06, p.placement.z);
      navGroup.add(pl);
    }

    // Anel de destaque (toque discreto)
    const ringGeo = new THREE.RingGeometry(0.3, 0.38, 32);
    const ringMat = new THREE.MeshBasicMaterial({ color: "#ffd166", transparent: true, opacity: 0.85, side: THREE.DoubleSide });
    const ring = new THREE.Mesh(ringGeo, ringMat);
    ring.rotation.x = -Math.PI / 2;
    ring.position.y = 0.03;
    ring.visible = false;
    scene.add(ring);
    let ringTimer = 0;
    let hovered: string | null = null;

    // Props dinâmicos
    let tableProp: THREE.Group | null = null;
    let handProp: THREE.Group | null = null;


    const setTableProp = (foodId: string | null) => {
      if (tableProp) { room.foodAnchor.remove(tableProp); tableProp = null; }
      if (foodId) {
        tableProp = createFoodProp(foodId);
        room.foodAnchor.add(tableProp);
      }
    };
    const setHandProp = (kind: "food" | "cup" | "pencil" | "toy" | null, foodId?: string) => {
      if (handProp) { handProp.removeFromParent(); handProp = null; }
      if (kind) {
        handProp = createHandProp(kind, foodId);
        driver.handR?.add(handProp);
      }
    };

    // ---------- câmera touch (orbit limitado + zoom) ----------
    const cam = { yaw: 0, pitch: 0.98, dist: 7.4, tyaw: 0, tpitch: 0.98, tdist: 7.4 };
    const fitCamera = () => {
      const w = mount.clientWidth, h = mount.clientHeight;
      renderer.setSize(w, h);
      camera.aspect = w / h;
      camera.updateProjectionMatrix();
      // celular retrato: mais longe para enquadrar o quarto
      const portrait = h > w;
      cam.tdist = portrait ? 8.2 : 7.0;
      cam.tpitch = portrait ? 1.02 : 0.92;
    };
    fitCamera();
    window.addEventListener("resize", fitCamera);

    const pointers = new Map<number, { x: number; y: number }>();
    let pinchD = 0;
    let downAt = 0;
    let downPos = { x: 0, y: 0 };
    let dragged = false;

    const ray = new THREE.Raycaster();
    const ndc = new THREE.Vector2();
    const pick = (cx: number, cy: number): string | null => {
      const r = renderer.domElement.getBoundingClientRect();
      ndc.set(((cx - r.left) / r.width) * 2 - 1, -((cy - r.top) / r.height) * 2 + 1);
      ray.setFromCamera(ndc, camera);
      const hits = ray.intersectObjects(room.hitboxes, false);
      if (hits.length > 0) return (hits[0].object.userData.objectId as string) ?? null;
      // Gabi tocável?
      const gHits = ray.intersectObject(rig.group, true);
      if (gHits.length > 0) return "gabi";
      return null;
    };

    const el = renderer.domElement;
    const onDown = (e: PointerEvent) => {
      el.setPointerCapture?.(e.pointerId);
      pointers.set(e.pointerId, { x: e.clientX, y: e.clientY });
      if (pointers.size === 2) {
        const [a, b] = [...pointers.values()];
        pinchD = Math.hypot(a.x - b.x, a.y - b.y);
      }
      downAt = performance.now();
      downPos = { x: e.clientX, y: e.clientY };
      dragged = false;
    };
    const onMove = (e: PointerEvent) => {
      const prev = pointers.get(e.pointerId);
      if (prev) {
        const dx = e.clientX - prev.x, dy = e.clientY - prev.y;
        pointers.set(e.pointerId, { x: e.clientX, y: e.clientY });
        if (pointers.size === 1 && (Math.abs(e.clientX - downPos.x) > 8 || Math.abs(e.clientY - downPos.y) > 8)) {
          dragged = true;
          cam.tyaw = THREE.MathUtils.clamp(cam.tyaw - dx * 0.004, -0.55, 0.55);
          cam.tpitch = THREE.MathUtils.clamp(cam.tpitch + dy * 0.003, 0.65, 1.25);
        } else if (pointers.size === 2) {
          dragged = true;
          const [a, b] = [...pointers.values()];
          const d = Math.hypot(a.x - b.x, a.y - b.y);
          if (pinchD > 0) cam.tdist = THREE.MathUtils.clamp(cam.tdist - (d - pinchD) * 0.015, 4.5, 10.5);
          pinchD = d;
        }
      } else if (e.pointerType === "mouse") {
        // hover discreto (desktop)
        const id = pick(e.clientX, e.clientY);
        if (id !== hovered) {
          setHighlight(hovered, false);
          hovered = id;
          setHighlight(hovered, true);
          el.style.cursor = id ? "pointer" : "default";
        }
      }
    };
    const onUp = (e: PointerEvent) => {
      pointers.delete(e.pointerId);
      if (pointers.size < 2) pinchD = 0;
      const quick = performance.now() - downAt < 500;
      if (!dragged && quick && e.isPrimary) {
        const id = pick(e.clientX, e.clientY);
        if (id) {
          showRing(id);
          audio.play("tap");
          engine.tapObject(id);
          if(id==="gabi")driver.playNamed("listen",2.5);
        }
      }
    };
    el.addEventListener("pointerdown", onDown);
    el.addEventListener("pointermove", onMove);
    el.addEventListener("pointerup", onUp);
    el.addEventListener("pointercancel", onUp);

    function setHighlight(id: string | null, on: boolean) {
      if (!id) return;
      const mats = room.highlightables.get(id);
      if (!mats) return;
      for (const m of mats) {
        m.emissive.set(on ? "#664411" : "#000000");
        m.emissiveIntensity = on ? 0.55 : 0;
      }
    }
    function showRing(objectId: string) {
      const map: Record<string, { x: number; z: number }> = {
        bed: NAV_POINTS.BED_POINT.approach,
        desk: NAV_POINTS.DESK_POINT.approach,
        chair: NAV_POINTS.CHAIR_POINT.approach,
        wardrobe: NAV_POINTS.WARDROBE_POINT.approach,
        "food-table": NAV_POINTS.FOOD_POINT.approach,
        rug: NAV_POINTS.PLAY_POINT.approach,
        "toy-chest": NAV_POINTS.PLAY_POINT.approach,
        "bathroom-door": NAV_POINTS.BATHROOM_DOOR_POINT.approach,
        window: NAV_POINTS.WINDOW_POINT.approach,
      };
      const p = map[objectId];
      if (p) {
        ring.position.set(p.x, 0.03, p.z);
        ring.visible = true;
        ringTimer = 1.4;
      }
    }

    // ---------- loop ----------
    let raf = 0;
    let last = performance.now();
    let fpsAcc = 0, fpsN = 0, fpsT = 0;
    let time = 0;
    let disposed = false;

    const loop = (now: number) => {
      if (disposed) return;
      raf = requestAnimationFrame(loop);
      let dt = (now - last) / 1000;
      last = now;
      if (dt > 0.1) dt = 0.1; // aba segura
      time += dt;

      // simulação
      engine.update(dt);
      const s = engine.snapshot;

      // Gabi: posição/orientação/visibilidade
      rig.group.visible = !s.gabiInsideBathroom;
      rig.group.position.x = s.position.x;
      rig.group.position.z = s.position.z;
      // giro suave da malha
      let d = s.facing - rig.group.rotation.y;
      while (d > Math.PI) d -= Math.PI * 2;
      while (d < -Math.PI) d += Math.PI * 2;
      rig.group.rotation.y += d * Math.min(1, dt * (engine.reducedMotion ? 14 : 9));

      const sittingStates = s.state === "SIT" || s.state === "EAT" || s.state === "DRINK" ||
        s.state === "STUDY" || s.state === "READ" || s.state === "WRITE" || s.state === "PLAY" ||
        (s.activity === "memory" && s.activityPhase === "EXECUTE");
      driver.setTurn(d);
      driver.setSitting(sittingStates);
      driver.setLying(s.state === "SLEEP" || s.state === "WAKE_UP");
      driver.update(dt, time, s.state, s.isMoving, engine.reducedMotion);

      // V1 renders only the red dress; saved outfit preferences are preserved for future assets.
      if(handProp){const hand=driver.handR;handProp.visible=!!hand;if(hand&&handProp.parent!==hand)hand.add(handProp);}

      // Porta do banheiro (0 fechada .. 1 aberta, gira para o corredor)
      room.doorPivot.rotation.y = 1.92 * easeInOut(s.doorOpen);
      const glow = room.group.userData.bathGlow as THREE.Mesh | undefined;
      if (glow) {
        (glow.material as THREE.MeshBasicMaterial).opacity = 0.35 * s.doorOpen;
      }

      // Guarda-roupa entreaberto enquanto escolhe (portas deslizantes ao longo de Z)
      const wardOpen = s.activity === "wardrobe" ? 1 : 0;
      const wLz = 1.45 - 0.32 * wardOpen;
      const wRz = 2.15 + 0.32 * wardOpen;
      room.wardrobeDoorL.position.z += (wLz - room.wardrobeDoorL.position.z) * Math.min(1, dt * 4);
      room.wardrobeDoorR.position.z += (wRz - room.wardrobeDoorR.position.z) * Math.min(1, dt * 4);

      // Laptop abre/fecha a tela
      const lapOpen = s.activity === "study" && s.activityPhase === "EXECUTE" ? 1 : 0;
      const targetRot = -1.85 + 1.55 * lapOpen;
      room.laptopScreen.rotation.x += (targetRot - room.laptopScreen.rotation.x) * Math.min(1, dt * 3);

      // Props de comida/mão conforme estado
      const wantTable = (s.activity === "eat" || s.activity === "drink") && s.activityPhase !== "NONE";
      const foodId = engine.currentFoodId();
      if (wantTable && !tableProp) setTableProp(foodId);
      if (!wantTable && tableProp) setTableProp(null);

      const wantHand =
        s.state === "EAT" ? "food" as const :
        s.state === "DRINK" ? "cup" as const :
        (s.state === "WRITE" || s.state === "STUDY") ? "pencil" as const :
        s.state === "PLAY" ? "toy" as const : null;
      const handKey = wantHand ? wantHand + foodId : "";
      if (handKey !== (setHandProp as unknown as { _k?: string })._k) {
        (setHandProp as unknown as { _k?: string })._k = handKey;
        setHandProp(wantHand, foodId);
      }

      // Luzes do dia
      updateRoomLighting(room, s.gameMinutes, s.activity === "study");

      // Anel de destaque
      if (ring.visible) {
        ringTimer -= dt;
        const pulse = 1 + Math.sin(time * 6) * 0.08;
        ring.scale.setScalar(pulse);
        ringMat.opacity = Math.max(0, Math.min(0.85, ringTimer));
        if (ringTimer <= 0) ring.visible = false;
      }

      // Inspeção
      navGroup.visible = inspectRef.current.showNav;

      // Câmera
      const ck = engine.reducedMotion ? 1 : Math.min(1, dt * 5);
      cam.yaw += (cam.tyaw - cam.yaw) * ck;
      cam.pitch += (cam.tpitch - cam.pitch) * ck;
      cam.dist += (cam.tdist - cam.dist) * ck;
      const target = new THREE.Vector3(0, 0.35, -0.35);
      camera.position.set(
        target.x + Math.sin(cam.yaw) * Math.cos(cam.pitch) * cam.dist,
        target.y + Math.sin(cam.pitch) * cam.dist,
        target.z + Math.cos(cam.yaw) * Math.cos(cam.pitch) * cam.dist
      );
      camera.lookAt(target);

      renderer.render(scene, camera);

      // fps
      fpsAcc += dt; fpsN++;
      if (fpsAcc >= 0.5) {
        fpsRef.current?.(Math.round(fpsN / fpsAcc));
        fpsAcc = 0; fpsN = 0;
      }
      void fpsT;
    };
    raf = requestAnimationFrame(loop);

    // ---------- limpeza total ao fechar ----------
    return () => {
      disposed = true;
      cancelAnimationFrame(raf);
      window.removeEventListener("resize", fitCamera);
      el.removeEventListener("pointerdown", onDown);
      el.removeEventListener("pointermove", onMove);
      el.removeEventListener("pointerup", onUp);
      el.removeEventListener("pointercancel", onUp);
      driver.dispose();
      engine.save();
      audio.stopAll();
      scene.traverse((o) => {
        if (o instanceof THREE.Mesh || o instanceof THREE.Points) {
          o.geometry.dispose();
          const m = o.material as THREE.Material | THREE.Material[];
          if (Array.isArray(m)) m.forEach((x) => disposeMat(x));
          else disposeMat(m);
        }
      });
      renderer.dispose();
      mount.removeChild(renderer.domElement);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return <div ref={mountRef} className="absolute inset-0" />;
}

function easeInOut(t: number): number {
  return t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2;
}
function disposeMat(m: THREE.Material) {
  const mm = m as unknown as { map?: THREE.Texture };
  if (mm.map) mm.map.dispose();
  m.dispose();
}
