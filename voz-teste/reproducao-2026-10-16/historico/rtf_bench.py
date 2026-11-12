import time
from pathlib import Path
import numpy as np
from pocket_tts import TTSModel

REF = "ref20.wav"
TEXTS = {
    "A": "Oi! Eu sou a Gabi, sua assistente. Como posso te ajudar hoje?",
    "B": "Claro! Eu posso fazer isso para você. Só preciso que me diga por onde gostaria de começar.",
    "C": "Entendi. Então vamos fazer assim: primeiro eu verifico isso para você e depois seguimos para o próximo passo.",
}

t0 = time.time()
model = TTSModel.load_model(language="portuguese_24l")
t_load = time.time() - t0
print(f"carga do modelo (1x, nao conta pro RTF): {t_load:.1f}s", flush=True)

t0 = time.time()
state = model.get_state_for_audio_prompt(Path(REF))
t_voice = time.time() - t0
print(f"preparo da referencia de voz (1x): {t_voice:.1f}s", flush=True)

rtfs = []
for name, text in TEXTS.items():
    t0 = time.time()
    audio = model.generate_audio(state, text, max_tokens=200)
    t_gen = time.time() - t0
    dur = audio.shape[-1] / model.sample_rate
    rtf = t_gen / dur
    rtfs.append(rtf)
    primeiro_pcm_estimado = t_gen  # geracao nao e incremental aqui: só sai no final
    print(f"Teste {name}: audio={dur:.2f}s | geracao(sem carga)={t_gen:.2f}s | RTF={rtf:.2f}", flush=True)

print(f"\nRTF medio (so geracao, modelo ja carregado): {sum(rtfs)/len(rtfs):.2f}", flush=True)
print("Meta do dossie: RTF <= 0.8 (excelente <=0.5)", flush=True)
print("FIM_RTF", flush=True)
