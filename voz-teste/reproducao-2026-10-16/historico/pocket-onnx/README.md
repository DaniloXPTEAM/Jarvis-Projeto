---
license: cc-by-4.0
language:
- en
- de
- fr
- it
- pt
- es
library_name: pocket-tts-onnx
base_model:
- kyutai/pocket-tts
pipeline_tag: text-to-speech
tags:
- tts
- voice-cloning
- onnx
- onnxruntime
---

# Pocket TTS ONNX

ONNX export of [Pocket TTS](https://huggingface.co/kyutai/pocket-tts) for lightweight multilingual text-to-speech with zero-shot voice cloning.

## Model Description

Pocket TTS is a compact text-to-speech model from Kyutai that supports voice cloning from short audio samples. This ONNX export provides:

- **Zero-shot voice cloning** from any audio reference
- **Multilingual bundles** for `english_2026-04`, `french_24l`, `german`, `german_24l`, `italian`, `italian_24l`, `portuguese`, `portuguese_24l`, `spanish`, and `spanish_24l`
- **INT8 quantized models** for fast CPU inference
- **Streaming support** with adaptive chunking for real-time playback
- **Temperature control** for generation diversity
- **Dual model architecture** for flexible flow matching

## Architecture

This export uses a **dual model split** for the Flow LM:

```
flow_lm_main   - Transformer/conditioner (produces conditioning vectors)
flow_lm_flow   - Flow network only (Euler integration for latent sampling)
```

This architecture enables:
- **Temperature control**: Adjust generation diversity via noise scaling
- **Variable LSD steps**: Trade off speed vs quality
- **External flow loop**: Full control over the sampling process

## Performance

Performance depends on the selected language bundle, precision, CPU, and `lsd_steps`.

- 6-layer bundles such as `english_2026-04`, `german`, `italian`, `portuguese`, and `spanish` are much smaller and faster than the `*_24l` bundles.
- INT8 bundles substantially reduce `flow_lm_main`, `flow_lm_flow`, and `mimi_decoder` size for CPU inference.
- `mimi_encoder.onnx` and `text_conditioner.onnx` remain FP32.

### Optimized Inference

Thread count is automatically tuned to avoid over-subscription on the small sequential matmuls in the autoregressive loop (`intra_op_num_threads=min(cpu_count, 4)`, `inter_op_num_threads=1`). This alone provides a **~2x speedup** over default ORT settings on multi-core machines.

### Decoding Strategy

- **Offline (`generate`)**: Uses **threaded parallel decoding** — the mimi decoder runs in a background thread, decoding 12-frame chunks while the flow loop generates the next frames. This overlaps generation and decoding for maximum throughput.
- **Streaming (`stream`)**: Uses **adaptive chunking** (starts at 2 frames). This ensures instant start (low TTFB) while scaling up chunk sizes for throughput.

## Usage

```python
from pocket_tts_onnx import PocketTTSOnnx

# Load the English bundle (INT8 by default)
tts = PocketTTSOnnx(
    models_dir="onnx",
    language="english_2026-04",
)

# Generate speech with built-in voice state
audio = tts.generate(
    text="Hello, this is a test.",
    voice="alba",
)

# Generate speech with voice cloning from audio
audio = tts.generate(
    text="Hello, this is a test of voice cloning.",
    voice="reference_sample.wav"
)

# Save output
tts.save_audio(audio, "output.wav")
```

### Temperature Control

Adjust generation diversity with the `temperature` parameter:

```python
# More deterministic (lower temperature)
tts = PocketTTSOnnx(temperature=0.3)

# Default balance
tts = PocketTTSOnnx(temperature=0.7)

# More diverse/expressive (higher temperature)
tts = PocketTTSOnnx(temperature=1.0)
```

### LSD Steps

Trade off speed vs quality with `lsd_steps`:

```python
# Default
tts = PocketTTSOnnx(lsd_steps=1)

# Slower, potentially smoother
tts = PocketTTSOnnx(lsd_steps=4)
```

### Streaming Mode

For real-time applications with low time-to-first-audio:

```python
for chunk in tts.stream("Hello world!", voice="reference_sample.wav"):
    play_audio(chunk)  # Process each chunk as it arrives
```

### Language Bundles

```python
# German bundle with built-in voice
tts = PocketTTSOnnx(language="german", precision="fp32")
audio = tts.generate("Hallo zusammen. Dies ist ein deutscher Test.", voice="alba")
```

Built-in voice names are loaded from each bundle's metadata. Voice cloning also accepts:

- a reference audio path
- a `.safetensors` prompt-state file
- a precomputed embedding array

### Legacy Layout

Root-level files under `onnx/` and the root `tokenizer.model` are still present for backward compatibility with older integrations that expect the original single-model layout.

New integrations should use the per-language bundle directories under `onnx/<language>/`.

### Command Line

```bash
python generate.py "Hello, this is a test." alba output.wav --language english_2026-04
python generate.py "Hallo zusammen." samples/reference.wav output.wav --language german --precision fp32
```

## Files

```
pocket-tts-onnx/
├── onnx/
│   ├── flow_lm_main.onnx          # Legacy root-level file kept for backward compatibility
│   ├── flow_lm_main_int8.onnx     # Legacy root-level file kept for backward compatibility
│   ├── flow_lm_flow.onnx          # Legacy root-level file kept for backward compatibility
│   ├── flow_lm_flow_int8.onnx     # Legacy root-level file kept for backward compatibility
│   ├── mimi_decoder.onnx          # Legacy root-level file kept for backward compatibility
│   ├── mimi_decoder_int8.onnx     # Legacy root-level file kept for backward compatibility
│   ├── mimi_encoder.onnx          # Legacy root-level file kept for backward compatibility
│   ├── text_conditioner.onnx      # Legacy root-level file kept for backward compatibility
│   ├── english_2026-04/
│   │   ├── bundle.json
│   │   ├── tokenizer.model
│   │   ├── bos_before_voice.npy
│   │   ├── flow_lm_main.onnx
│   │   ├── flow_lm_main_int8.onnx
│   │   ├── flow_lm_flow.onnx
│   │   ├── flow_lm_flow_int8.onnx
│   │   ├── mimi_decoder.onnx
│   │   ├── mimi_decoder_int8.onnx
│   │   ├── mimi_encoder.onnx
│   │   └── text_conditioner.onnx
│   ├── german/
│   ├── german_24l/
│   ├── french_24l/
│   ├── italian/
│   ├── italian_24l/
│   ├── portuguese/
│   ├── portuguese_24l/
│   ├── spanish/
│   └── spanish_24l/
├── reference_sample.wav           # Example voice reference
├── tokenizer.model                # Legacy root tokenizer kept for backward compatibility
├── pocket_tts_onnx.py             # Inference wrapper
├── generate.py                    # CLI script
├── requirements.txt               # Python dependencies
└── README.md
```

## Requirements

```
onnxruntime>=1.16.0
numpy
soundfile
sentencepiece
scipy  # Only needed if resampling from non-24kHz audio
huggingface_hub
safetensors
```

Install with:
```bash
pip install -r requirements.txt
```

## Notes

- Each bundle is self-contained and includes its own tokenizer and metadata.
- Built-in voice states are fetched from `kyutai/pocket-tts` via Hugging Face.
- Voice cloning from audio uses the exported `mimi_encoder.onnx`.

## License

- **Models**: CC BY 4.0 (inherited from [kyutai/pocket-tts](https://huggingface.co/kyutai/pocket-tts))
- **Code**: Apache 2.0

## Prohibited Use

Use of our model must comply with all applicable laws and regulations and must not result in, involve, or facilitate any illegal, harmful, deceptive, fraudulent, or unauthorized activity. Prohibited uses include, without limitation, voice impersonation or cloning without explicit and lawful consent; misinformation, disinformation, or deception (including fake news, fraudulent calls, or presenting generated content as genuine recordings of real people or events); and the generation of unlawful, harmful, libelous, abusive, harassing, discriminatory, hateful, or privacy-invasive content. We disclaim all liability for any non-compliant use.

## Acknowledgments

- [Kyutai](https://kyutai.org/) for the original Pocket TTS model
