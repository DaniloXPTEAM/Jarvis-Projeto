# Gabi V1 — primeira malha 3D integrada

Esta entrega contém uma personagem modelada proceduralmente em 3D, baseada nas proporções e características das referências: cabelo ruivo longo, olhos castanhos, sardas, vestido vermelho e botas. É uma primeira interpretação visual, não uma reprodução exata/aprovada da imagem e não tem o acabamento da referência renderizada.

## Abrir

Abra `dist/index.html` em um navegador com WebGL. O modelo de ~4,4 MB está incorporado ao HTML; não depende de buscar um GLB externo para iniciar. Também há o GLB separado em `public/assets/gabi/age_01/gabi_red_dress.glb`.

## Modelo real

22 ossos e 15 AnimationClips: idle_breathe, walk, turn_left, turn_right, listen, present_talk, sit_down, stand_up, read, study, eat, yawn, lie_down, sleep_breathe, wake_up. São animações esqueléticas autoradas por código, não vídeos ou imagens deslizando. A malha tem aproximadamente 64 mil vértices e foi agrupada por material para reduzir draw calls.

O controlador existente carrega esse GLB e oculta a representação antiga quando encontra um clipe utilizável. Walk usa deslocamento da engine, os giros acompanham sua rotação e o sono usa clipes de deitar/respirar/acordar. Não há root motion global. Mãos possuem sockets; dedos têm geometria, mas não rig individual. Há apenas vestido vermelho nesta fase.

Engine, navegação, necessidades, relógio, save, HUD e quarto do ZIP recebido foram mantidos. Fases age_02/03 continuam sem modelo: devem receber designs próprios.

## Editar

`npm ci` → `npm run dev`. `npm test` executa os testes. `npm run build` produz o HTML.

A fonte da malha e clipes está em `scripts/build-gabi-model.mjs`. `node scripts/build-gabi-model.mjs` regenera o GLB e sua cópia incorporada em `src/game/gabi/age_01/modelUrl.ts`; depois execute o build. Para substituir por outro GLB, atualize também esse módulo incorporado ou remova o override GABI_MODEL_URL de GameCanvas para usar o caminho externo configurado.

GabiController, GabiLoader e config/gabiAnimations.ts fazem o mapeamento. `addClips(AnimationClip[])` aceita clipes compatíveis com o mesmo esqueleto. Não faz retargeting automático. Ausências continuam registradas como PENDING_ASSET, usando idle ou a personagem antiga conforme disponibilidade.

## Validação real e limites

TypeScript, build e sete testes automatizados passaram, incluindo leitura binária do GLB real pelo GLTFLoader, existência de pele/ossos/clipes e amostragem de vértices animados sem deformação explosiva. As prévias são renderizações geométricas por software e não capturas WebGL.

A aparência foi inspecionada em repouso e sono. Isso NÃO aprova fidelidade final nem contato correto de cada mão, alimento, cadeira ou cama durante toda a animação. Esses ajustes de acabamento ainda são necessários. Mobile/touch, desempenho e fluxo visual completo não foram validados em navegador nesta sessão, cujo acesso local havia sido bloqueado. Não é um APK. A primeira malha existe e está conectada; a experiência visual completa continua em desenvolvimento.
