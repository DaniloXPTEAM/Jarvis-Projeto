# CHARACTER-ANIMATION-BIBLE — Gabi (personagem definitiva)

> Status: **ESPECIFICAÇÃO PARA PRODUÇÃO EXTERNA**. Nenhum asset final foi produzido nesta etapa.
> A malha atual do jogo (`src/game/gabiProvisional.ts`) é **provisória** e existe só para validar
> escala, navegação, câmera e estados. Não apresentá-la como Gabi final.

## 1. Referências oficiais (identidade — não mudar)

Baseadas nas imagens de referência entregues pelo cliente (poses: em pé, apresentando,
ouvindo, bocejando com travesseiro, comendo cereal sentada, no laptop, lendo):

- Rosto infantil, bochechas rosadas com **sardas**, sorriso suave, sobrancelhas ruivas.
- **Cabelo ruivo** (#c94a1e) **LONGO e ONDULADO até a cintura**, franja dividida ao meio.
- **Olhos grandes** castanhos (#5a3016) com brilho duplo.
- Proporções infantis: cabeça ≈ 1/3 da altura total.
- **Vestido vermelho liso** (#d92b2b) sem mangas + **botinhas pretas** de cano curto.
- Guarda-roupa das referências:
  - `red-dress` vermelho liso (padrão) · `blue-set` vestido lilás (leitura)
  - `yellow-set` suéter amarelo mostarda + legging preta (laptop/estudo, com mangas)
  - `pajama` azul-claro com estrelas brancas + pantufas de estrela (com mangas)
- Arte conceitual: `public/images/gabi-concept.jpg`.

## 2. Escala e orientação (obrigatório no GLB)

| Item | Valor |
|---|---|
| Unidades | **metros**, Y-up, frente **+Z** |
| Altura total | **≈ 1.08 m** (cabeça grande incluída) |
| Origem | centro entre os pés, y=0 no chão |
| Pose de referência | A-pose suave (braços ~15° do corpo), pés paralelos |
| Raio de colisão | 0.26 m (navegação infla obstáculos) |
| Velocidade de marcha | 1.35 m/s — ciclo de 2 passos ≈ 0.9 s |

## 3. Esqueleto esperado (nomes exatos)

```
Hips
├─ Spine → Chest → Neck → Head
├─ Shoulder_L → UpperArm_L → Forearm_L → Hand_L
├─ Shoulder_R → UpperArm_R → Forearm_R → Hand_R
├─ Thigh_L → Shin_L → Foot_L
└─ Thigh_R → Shin_R → Foot_R
```

Sockets (Object3D nomeados): `Hand_R_Attach` (palma, +Z para fora), `Hand_L_Attach`, `Head_Attach` (topo), `Mouth_Attach` (frente da boca, para sincronizar "levar à boca").

## 4. Roupas (variações do mesmo rig)

| id | peças | notas |
|---|---|---|
| `red-dress` | vestido vermelho liso sem mangas + botinhas pretas | roupa padrão |
| `blue-set` | vestido lilás + botinhas pretas | usada ao ler |
| `yellow-set` | suéter mostarda (mangas) + legging preta | usada no laptop/estudo |
| `pajama` | pijama azul de estrelas (mangas) + pantufas | vestida automaticamente ao dormir |

Regras: nenhuma roupa altera altura/colisão; saias precisam de peso para pose sentada (clip `sit_down` tem adaptação por roupa); troca sempre com fade/privacidade — **sem nudez** em nenhum frame.

## 5. Política de locomoção (única)

- **Todos os clipes de locomoção são IN-PLACE** (sem deslocamento da raiz).
- Deslocamento é 100% controlado pela navegação (`src/game/navigation.ts`).
- Velocidade do ciclo sincronizada: footstep L/R nos 25% e 75% do ciclo `walk`.
- **Root motion é PROIBIDO** junto com navegação. Se um take vier com root motion, assar como in-place na importação (documentar no manifesto).

## 6. Regras de consistência

1. Rosto, cabelo e vestido legíveis a 7 m de distância (câmera do jogo).
2. Mãos sempre com palma visível ao segurar objetos (sockets alinhados).
3. Piscar em camada aditiva (nunca embutido nos takes base).
4. Takes com `loop: true` devem fechar o ciclo no último frame (ver manifesto).
5. Cada take dispara seus marcadores (`src/game/animationManifest.ts`) — efeitos do jogo
   (ex.: aplicar fome, fechar porta) acontecem **uma única vez** por marcador.

## 7. Importação (checklist)

1. Exportar `gabi.glb` (+ opcional `gabi_<roupa>.glb` só com materiais) para `public/animations/`.
2. Nomear takes com os `clipId` do manifesto (`idle_breathe`, `walk`, …).
3. Atualizar `ANIMATION_MANIFEST`: `file`, `take`, `duration` real, `status: "ready"`.
4. Ativar `loadGabiGLB()` em `animationLayer.ts` (trocar driver procedural por GLBDriver).
5. Testar no **modo de inspeção** (🔍): escala, sockets, transições e os 132 caminhos.
6. Nenhuma mudança na simulação (`engine.ts`) deve ser necessária.

## 8. O que vídeos isolados NÃO substituem

Clipes de vídeo (ex.: gerados pelo Higgsfield/Veo) **não** riggam nem substituem animação
esquelética da personagem 3D. Podem servir como **referência de movimento** para o animador,
mas a integração final exige takes esqueléticos no GLB com os nomes de ossos desta bíblia.
