# Gabi — Pacote de Prompts (vídeo + poses)

## Especificação técnica dos vídeos
- Proporção: 9:16 vertical (tela cheia)
- Resolução: 1080×1920 (ou 720×1280)
- Duração: 4–6s em loop perfeito
- Formato: MP4 (H.264)
- Áudio: nenhum — o app toca sempre mudo
- FPS: 24–30fps
- Salvar em (Android): android/app/src/main/res/raw/
- Salvar em (preview web): public/videos/

> O app usa VÍDEO quando o arquivo existe e cai automaticamente
> para a POSE ESTÁTICA quando não existe. Nada trava.

## Âncora de estilo (colar no início de TODO prompt)
```
3D animated character render, Pixar/DreamWorks-style children's illustration,
stylized chibi proportions but NOT toddler-like — head-to-body ratio matches
the reference image closely, do not oversize the head or shrink the body.
Long, voluminous, wavy copper-ginger hair reaching past the waist, soft side
part, gentle bangs framing the face — hair must read as LONG, never trimmed
shorter than the reference image. Large warm brown eyes with soft lashes,
light freckles scattered across nose and cheeks, rosy cheeks, fair skin,
small button nose, gentle closed-mouth smile. Smooth porcelain-like stylized
3D shading, soft ambient occlusion, warm cinematic rim light. Full body
visible from head to feet. Character must match the attached reference image
exactly in face and proportions — same girl, different outfit/pose only.
```

## Cenas

### Café — scene_comer.mp4  (5s loop)
Fallback estático: gabi-comer.png
```
9:16 vertical video, 5 second seamless loop, 1080x1920.
[COLE A ÂNCORA DE ESTILO AQUI] wearing the mustard-yellow sweater and black leggings,
sitting at a small wooden breakfast table with a bowl of cereal, in a cozy
bedroom, soft warm morning light through a window. Subtle continuous
breathing motion, natural occasional blinking, periodically lifts a spoon
to her mouth and takes a small bite, glances down at the bowl then back up
with a content smile. Gentle looping motion only — no camera movement,
static camera.
```

### Estudar — scene_computador.mp4  (5s loop)
Fallback estático: gabi-computador.png
```
9:16 vertical video, 5 second seamless loop, 1080x1920.
[COLE A ÂNCORA DE ESTILO AQUI] wearing the mustard-yellow sweater and black leggings,
sitting cross-legged on a chair with an open laptop on her lap, in a cozy
bedroom, soft daylight. Subtle continuous breathing motion, natural
occasional blinking, fingers gently tapping the keyboard in a steady
typing rhythm, occasionally tilts her head slightly and brushes a strand
of hair behind her ear. Static camera, no camera movement.
```

### Ler — scene_ler.mp4  (5s loop)
Fallback estático: gabi-ler.png
```
9:16 vertical video, 5 second seamless loop, 1080x1920.
[COLE A ÂNCORA DE ESTILO AQUI] wearing a cozy cardigan over a simple dress, sitting on the
edge of a bed with an open book on her lap, warm evening lamp light.
Subtle continuous breathing motion, natural occasional blinking, eyes
gently moving as if reading, occasionally turns a page with a small smile.
Static camera, no camera movement.
```

### Dormir — scene_dormir.mp4  (5s loop)
Fallback estático: gabi-dormir.png
```
9:16 vertical video, 5 second seamless loop, 1080x1920.
[COLE A ÂNCORA DE ESTILO AQUI] wearing soft pajamas, sitting on the bed hugging a pillow,
dim warm nightlight glow. Slow, sleepy continuous breathing motion (chest
gently rising and falling), eyes slowly blinking and drifting half-closed,
small sleepy head nod. Static camera, no camera movement.
```

### Nham! — scene_comendo_reacao.mp4  (3s única)
Fallback estático: gabi-comer.png
```
9:16 vertical video, 3 seconds, 1080x1920.
[COLE A ÂNCORA DE ESTILO AQUI] wearing the mustard-yellow sweater, sitting at the
breakfast table, big delighted open-mouth smile mid-bite building up
across the clip, eyes closing happily, one hand near her mouth, cheeks
rosy with joy. Static camera, no camera movement.
```

### Vestido — scene_outfit_vestido_vermelho.mp4  (5s loop)
Fallback estático: gabi-idle.png
```
9:16 vertical video, 5 second seamless loop, 1080x1920.
[COLE A ÂNCORA DE ESTILO AQUI] wearing the exact same red sleeveless fit-and-flare dress
and black ankle boots as the reference image, standing in a cozy bedroom
near a sunset-lit window, weight on one leg, arms relaxed. Subtle
continuous breathing motion, natural occasional blinking, gentle swaying,
occasionally tilts her head and looks around the room with a soft smile.
Static camera, no camera movement.
```

### Moletom — scene_outfit_moletom_preto.mp4  (5s loop)
Fallback estático: gabi-computador.png
```
9:16 vertical video, 5 second seamless loop, 1080x1920.
[COLE A ÂNCORA DE ESTILO AQUI] wearing a mustard-yellow chunky knit sweater, black skinny
leggings and black ankle boots, standing relaxed in a cozy bedroom. Subtle
continuous breathing motion, natural occasional blinking, occasionally
touches and plays with a strand of her hair, soft warm smile. Static
camera, no camera movement.
```