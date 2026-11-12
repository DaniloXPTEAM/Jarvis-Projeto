import time, json, sys, os
sys.path.insert(0, "pocket-onnx")
import psutil
import numpy as np
from pocket_tts_onnx import PocketTTSOnnx

proc = psutil.Process(os.getpid())
def ram_mb():
    return proc.memory_info().rss / 1024 / 1024

REF = "ref20.wav"
TEXTS = {
    "B3_onnx": "Claro. Eu posso fazer isso para você. Só preciso que me diga por onde gostaria de começar.",
    "C3_onnx": "Entendi. Então vamos fazer assim. Primeiro, eu verifico isso para você. Depois, seguimos para o próximo passo.",
}

ram_before = ram_mb()
t0 = time.time()
tts = PocketTTSOnnx(models_dir="pocket-onnx/onnx", language="portuguese_24l", precision="int8")
t_load = time.time() - t0
ram_after_load = ram_mb()
print(f"carga do modelo (ONNX int8): {t_load:.2f}s | RAM: {ram_before:.0f}MB -> {ram_after_load:.0f}MB (+{ram_after_load-ram_before:.0f}MB)", flush=True)

t0 = time.time()
tts.prepare_voice_state(REF)  # aquece o _voice_cache interno (embeddings da voz)
t_voice = time.time() - t0
ram_after_voice = ram_mb()
print(f"preparo da referencia de voz (encode + 1a condicao): {t_voice:.2f}s | RAM: {ram_after_voice:.0f}MB", flush=True)
voice_state = REF  # generate()/stream() aceitam o caminho do arquivo direto; reusam o cache acima

results = {"load_s": round(t_load, 2), "voice_prep_s": round(t_voice, 2),
           "ram_after_load_mb": round(ram_after_load, 0), "cases": []}

for label, text in TEXTS.items():
    # --- first-PCM via stream() ---
    t0 = time.time()
    first_chunk_time = None
    chunks = []
    for chunk in tts.stream(text, voice=voice_state):
        if first_chunk_time is None:
            first_chunk_time = time.time() - t0
        chunks.append(chunk)
    t_stream_total = time.time() - t0
    audio_stream = np.concatenate(chunks) if chunks else np.zeros(0, dtype=np.float32)
    dur_stream = len(audio_stream) / tts.sample_rate

    # --- geracao normal (generate(), nao-streaming) para comparar RTF puro ---
    t0 = time.time()
    audio = tts.generate(text, voice=voice_state)
    t_gen = time.time() - t0
    dur = len(audio) / tts.sample_rate
    rtf = t_gen / dur if dur > 0 else float("inf")
    words = len(text.split())
    wpm = words / dur * 60 if dur > 0 else 0
    ram_now = ram_mb()

    tts.save_audio(audio, f"out/{label}.wav")

    r = {"label": label, "texto": text, "duracao_s": round(dur, 2), "palavras": words,
         "wpm": round(wpm, 1), "tempo_geracao_s": round(t_gen, 2), "rtf": round(rtf, 3),
         "first_pcm_ms": round(first_chunk_time * 1000, 0) if first_chunk_time else None,
         "tempo_stream_total_s": round(t_stream_total, 2), "ram_mb": round(ram_now, 0)}
    results["cases"].append(r)
    print(f"{label}: dur={dur:.2f}s RTF={rtf:.3f} first_pcm={r['first_pcm_ms']}ms "
          f"wpm={wpm:.0f} ram={ram_now:.0f}MB", flush=True)

with open("out/onnx_bench_results.json", "w", encoding="utf-8") as f:
    json.dump(results, f, ensure_ascii=False, indent=2)
print("FIM_ONNX_BENCH", flush=True)
