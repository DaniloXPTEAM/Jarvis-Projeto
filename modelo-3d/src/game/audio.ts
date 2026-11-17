// ============================================================
// Áudio sintetizado (WebAudio) — sem arquivos externos.
// Sons suaves para UI, passos, comer, dormir, banheiro, sucesso.
// Respeita muted e é interrompido ao fechar o minigame.
// ============================================================

type SoundName =
  | "tap" | "open" | "close" | "step" | "eat" | "drink" | "page"
  | "success" | "sleep" | "wake" | "door" | "flush" | "pop" | "error";

class AudioEngine {
  private ctx: AudioContext | null = null;
  private master: GainNode | null = null;
  muted = false;

  private ensure(): AudioContext | null {
    if (this.muted) return null;
    try {
      if (!this.ctx) {
        const AC = window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
        this.ctx = new AC();
        this.master = this.ctx.createGain();
        this.master.gain.value = 0.35;
        this.master.connect(this.ctx.destination);
      }
      if (this.ctx.state === "suspended") void this.ctx.resume();
      return this.ctx;
    } catch {
      return null;
    }
  }

  setMuted(m: boolean) {
    this.muted = m;
    if (m && this.ctx) void this.ctx.suspend();
    else if (!m && this.ctx) void this.ctx.resume();
  }

  stopAll() {
    try {
      if (this.ctx) void this.ctx.suspend();
    } catch { /* noop */ }
  }

  resume() {
    if (!this.muted && this.ctx) void this.ctx.resume();
  }

  play(name: SoundName) {
    const ctx = this.ensure();
    if (!ctx || !this.master) return;
    const t = ctx.currentTime;
    const mk = (freq: number, dur: number, type: OscillatorType, vol: number, when = 0, slideTo?: number) => {
      const o = ctx.createOscillator();
      const g = ctx.createGain();
      o.type = type;
      o.frequency.setValueAtTime(freq, t + when);
      if (slideTo) o.frequency.exponentialRampToValueAtTime(slideTo, t + when + dur);
      g.gain.setValueAtTime(0.0001, t + when);
      g.gain.exponentialRampToValueAtTime(vol, t + when + 0.02);
      g.gain.exponentialRampToValueAtTime(0.0001, t + when + dur);
      o.connect(g); g.connect(this.master!);
      o.start(t + when); o.stop(t + when + dur + 0.05);
    };
    switch (name) {
      case "tap": mk(660, 0.08, "sine", 0.5); break;
      case "open": mk(440, 0.12, "sine", 0.5); mk(660, 0.14, "sine", 0.5, 0.08); break;
      case "close": mk(660, 0.1, "sine", 0.4); mk(440, 0.12, "sine", 0.4, 0.07); break;
      case "step": mk(180, 0.06, "triangle", 0.25); break;
      case "eat": mk(320, 0.09, "triangle", 0.5); mk(240, 0.1, "triangle", 0.4, 0.12); break;
      case "drink": mk(500, 0.12, "sine", 0.4, 0, 300); break;
      case "page": mk(900, 0.06, "sine", 0.2); mk(1200, 0.05, "sine", 0.15, 0.05); break;
      case "success": mk(523, 0.12, "sine", 0.5); mk(659, 0.12, "sine", 0.5, 0.1); mk(784, 0.2, "sine", 0.5, 0.2); break;
      case "sleep": mk(392, 0.3, "sine", 0.35); mk(330, 0.35, "sine", 0.3, 0.25); break;
      case "wake": mk(330, 0.15, "sine", 0.4); mk(440, 0.2, "sine", 0.4, 0.12); break;
      case "door": mk(220, 0.15, "triangle", 0.4, 0, 140); break;
      case "flush": mk(300, 0.4, "sawtooth", 0.12, 0, 120); break;
      case "pop": mk(700, 0.07, "square", 0.2); break;
      case "error": mk(200, 0.18, "sawtooth", 0.25); break;
    }
  }
}

export const audio = new AudioEngine();
