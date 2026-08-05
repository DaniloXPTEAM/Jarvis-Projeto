'use client';
import { useEffect, useRef } from 'react';

interface Props { listening: boolean; thinking: boolean; size?: number }

export default function Orb({ listening, thinking, size = 220 }: Props) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const frameRef  = useRef<number>(0);
  const timeRef   = useRef(0);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d')!;
    const W = size, H = size;
    canvas.width  = W * devicePixelRatio;
    canvas.height = H * devicePixelRatio;
    canvas.style.width  = `${W}px`;
    canvas.style.height = `${H}px`;
    ctx.scale(devicePixelRatio, devicePixelRatio);

    const cx = W / 2, cy = H / 2, R = W * 0.38;
    const N = 280;

    // Particles on sphere surface (Fibonacci spiral)
    const pts = Array.from({ length: N }, (_, i) => {
      const phi   = Math.acos(1 - (2 * (i + 0.5)) / N);
      const theta = Math.PI * (1 + Math.sqrt(5)) * i;
      return {
        ox: R * Math.sin(phi) * Math.cos(theta),
        oy: R * Math.sin(phi) * Math.sin(theta),
        oz: R * Math.cos(phi),
        phase: Math.random() * Math.PI * 2,
        speed: 0.4 + Math.random() * 0.8,
      };
    });

    // Connections (close pairs)
    const edges: [number, number][] = [];
    for (let i = 0; i < N; i++) {
      for (let j = i + 1; j < N; j++) {
        const dx = pts[i].ox - pts[j].ox;
        const dy = pts[i].oy - pts[j].oy;
        const dz = pts[i].oz - pts[j].oz;
        if (Math.sqrt(dx*dx+dy*dy+dz*dz) < R * 0.42) edges.push([i, j]);
      }
    }

    function draw(t: number) {
      timeRef.current = t;
      ctx.clearRect(0, 0, W, H);

      const base  = listening ? 0.006 : 0.0015;
      const speed = thinking  ? 0.004 : base;
      const rotY  = t * speed;
      const rotX  = Math.sin(t * 0.0004) * 0.3;

      const sinY = Math.sin(rotY), cosY = Math.cos(rotY);
      const sinX = Math.sin(rotX), cosX = Math.cos(rotX);

      // Project particles
      const proj = pts.map(p => {
        // rotY
        let x = p.ox * cosY - p.oz * sinY;
        let z = p.ox * sinY + p.oz * cosY;
        let y = p.oy;
        // rotX
        const y2 = y * cosX - z * sinX;
        const z2 = y * sinX + z * cosX;
        y = y2; z = z2;
        // pulse
        const pulse = listening
          ? 1 + 0.12 * Math.sin(t * 0.008 + p.phase)
          : 1 + 0.03 * Math.sin(t * 0.002 + p.phase);
        x *= pulse; y *= pulse; z *= pulse;

        const fov = 320;
        const scale = fov / (fov + z + R);
        const sx = cx + x * scale;
        const sy = cy + y * scale;
        const depth = (z + R) / (2 * R); // 0..1 (back..front)
        return { sx, sy, depth, scale };
      });

      // Draw edges
      for (const [a, b] of edges) {
        const pa = proj[a], pb = proj[b];
        const d = (pa.depth + pb.depth) * 0.5;
        if (d < 0.2) continue;
        const alpha = d * (listening ? 0.35 : 0.18);
        ctx.beginPath();
        ctx.moveTo(pa.sx, pa.sy);
        ctx.lineTo(pb.sx, pb.sy);
        if (listening) {
          ctx.strokeStyle = `rgba(0,255,136,${alpha})`;
        } else if (thinking) {
          ctx.strokeStyle = `rgba(167,139,250,${alpha})`;
        } else {
          ctx.strokeStyle = `rgba(56,189,248,${alpha * 0.7})`;
        }
        ctx.lineWidth = 0.6;
        ctx.stroke();
      }

      // Draw particles
      proj.forEach((p, i) => {
        if (p.depth < 0.1) return;
        const r   = p.scale * (2.2 + p.depth * 1.8);
        const val = pts[i].phase;
        const alpha = 0.4 + p.depth * 0.6;

        let color: string;
        if (listening) {
          color = `rgba(0,255,136,${alpha})`;
        } else if (thinking) {
          color = val < 2 ? `rgba(167,139,250,${alpha})` : `rgba(56,189,248,${alpha})`;
        } else {
          color = val < 2 ? `rgba(0,255,136,${alpha * 0.8})` : `rgba(56,189,248,${alpha * 0.6})`;
        }

        ctx.beginPath();
        ctx.arc(p.sx, p.sy, Math.max(r, 0.5), 0, Math.PI * 2);
        ctx.fillStyle = color;
        ctx.fill();
      });

      // Core glow
      const grd = ctx.createRadialGradient(cx, cy, 0, cx, cy, R * 0.55);
      if (listening) {
        grd.addColorStop(0, 'rgba(0,255,136,0.18)');
        grd.addColorStop(1, 'rgba(0,255,136,0)');
      } else if (thinking) {
        grd.addColorStop(0, 'rgba(167,139,250,0.14)');
        grd.addColorStop(1, 'rgba(167,139,250,0)');
      } else {
        grd.addColorStop(0, 'rgba(56,189,248,0.08)');
        grd.addColorStop(1, 'rgba(56,189,248,0)');
      }
      ctx.beginPath();
      ctx.arc(cx, cy, R * 0.55, 0, Math.PI * 2);
      ctx.fillStyle = grd;
      ctx.fill();

      frameRef.current = requestAnimationFrame(draw);
    }

    frameRef.current = requestAnimationFrame(draw);
    return () => cancelAnimationFrame(frameRef.current);
  }, [listening, thinking, size]);

  return (
    <canvas
      ref={canvasRef}
      style={{ display: 'block', filter: listening ? 'drop-shadow(0 0 30px rgba(0,255,136,.6))' : thinking ? 'drop-shadow(0 0 20px rgba(167,139,250,.5))' : 'drop-shadow(0 0 15px rgba(56,189,248,.3))' }}
    />
  );
}
