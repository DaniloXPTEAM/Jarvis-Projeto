import time, json
from pathlib import Path
import numpy as np
import soundfile as sf
from pocket_tts import TTSModel

REF = "ref20.wav"
EOS = -4.0  # fixo nesta rodada, isolando so o efeito da pontuacao/segmentacao
MAX_TOKENS = 260  # fixo pra todos, mesma margem pra nenhum ser cortado

CANDIDATES = [
    ("B3", "Claro. Eu posso fazer isso para você. Só preciso que me diga por onde gostaria de começar."),
    ("B4", "Claro, eu posso fazer isso para você. Só preciso que me diga por onde gostaria de começar."),
    ("C3", "Entendi. Então vamos fazer assim. Primeiro, eu verifico isso para você. Depois, seguimos para o próximo passo."),
    ("C4", "Entendi. Então vamos fazer assim: primeiro, eu verifico isso para você e depois seguimos para o próximo passo."),
]

t0 = time.time()
model = TTSModel.load_model(language="portuguese_24l", eos_threshold=EOS)
print(f"[modelo eos={EOS} carregado em {time.time()-t0:.1f}s]", flush=True)
state = model.get_state_for_audio_prompt(Path(REF))

results = []
for label, text in CANDIDATES:
    t0 = time.time()
    audio = model.generate_audio(state, text, max_tokens=MAX_TOKENS)
    gen_time = time.time() - t0
    a = audio.detach().cpu().numpy().reshape(-1)
    dur = len(a) / model.sample_rate
    words = len(text.split())
    wpm = words / dur * 60
    sf.write(f"out/{label}.wav", np.clip(a, -1, 1), model.sample_rate)
    r = {"label": label, "eos_threshold": EOS, "max_tokens": MAX_TOKENS, "texto": text,
         "duracao_s": round(dur, 2), "palavras": words, "wpm": round(wpm, 1),
         "tempo_geracao_s": round(gen_time, 1)}
    results.append(r)
    print(f"{label}: dur={dur:.2f}s palavras={words} wpm={wpm:.0f} (gerar={gen_time:.1f}s)", flush=True)

with open("out/prosody_results2.json", "w", encoding="utf-8") as f:
    json.dump(results, f, ensure_ascii=False, indent=2)
print("FIM_PROSODY2", flush=True)
