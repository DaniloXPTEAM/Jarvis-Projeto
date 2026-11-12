import time, json
from pathlib import Path
import numpy as np
import soundfile as sf
from pocket_tts import TTSModel

REF = "ref20.wav"

TEXT_B = "Claro! Eu posso fazer isso para você. Só preciso que me diga por onde gostaria de começar."
TEXT_C = "Entendi. Então vamos fazer assim: primeiro eu verifico isso para você e depois seguimos para o próximo passo."

# Variantes de pontuacao com pausas naturais (reticencias/virgulas em pontos de
# respiracao), sem mudar nenhuma palavra do texto original.
TEXT_B_PAUSED = "Claro... Eu posso fazer isso para você. Só preciso que me diga, por onde gostaria de começar."
TEXT_C_PAUSED = "Entendi... Então, vamos fazer assim: primeiro, eu verifico isso para você, e depois, seguimos para o próximo passo."

CANDIDATES = [
    # (label, texto, eos_threshold, max_tokens)
    ("B2_base",        TEXT_B,        -4.0, 200),
    ("B2_eos6",        TEXT_B,        -6.0, 260),
    ("B2_eos8",        TEXT_B,        -8.0, 300),
    ("B2_pausas",      TEXT_B_PAUSED, -4.0, 220),
    ("B2_pausas_eos6", TEXT_B_PAUSED, -6.0, 260),
    ("C2_base",        TEXT_C,        -4.0, 220),
    ("C2_eos6",        TEXT_C,        -6.0, 280),
    ("C2_eos8",        TEXT_C,        -8.0, 320),
    ("C2_pausas",      TEXT_C_PAUSED, -4.0, 240),
    ("C2_pausas_eos6", TEXT_C_PAUSED, -6.0, 300),
]

results = []
loaded = {}
for label, text, eos, max_tok in CANDIDATES:
    key = eos
    if key not in loaded:
        t0 = time.time()
        loaded[key] = TTSModel.load_model(language="portuguese_24l", eos_threshold=eos)
        print(f"[modelo eos={eos} carregado em {time.time()-t0:.1f}s]", flush=True)
    model = loaded[key]
    if "voice_state" not in loaded.get((key, "voice"), {}):
        pass
    state = model.get_state_for_audio_prompt(Path(REF))
    t0 = time.time()
    audio = model.generate_audio(state, text, max_tokens=max_tok)
    gen_time = time.time() - t0
    a = audio.detach().cpu().numpy().reshape(-1)
    dur = len(a) / model.sample_rate
    words = len(text.split())
    wpm = words / dur * 60
    sf.write(f"out/{label}.wav", np.clip(a, -1, 1), model.sample_rate)
    r = {"label": label, "eos_threshold": eos, "max_tokens": max_tok, "texto": text,
         "duracao_s": round(dur, 2), "palavras": words, "wpm": round(wpm, 1),
         "tempo_geracao_s": round(gen_time, 1)}
    results.append(r)
    print(f"{label}: dur={dur:.2f}s palavras={words} wpm={wpm:.0f} (eos={eos}, max_tok={max_tok}, gerar={gen_time:.1f}s)", flush=True)

with open("out/prosody_results.json", "w", encoding="utf-8") as f:
    json.dump(results, f, ensure_ascii=False, indent=2)
print("FIM_PROSODY", flush=True)
