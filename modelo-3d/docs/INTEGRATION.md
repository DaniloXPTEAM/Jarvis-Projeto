# INTEGRAÇÃO — minigame da Gabi no aplicativo

## 1. Onde está a integração (local exato)

- Botão **“Cuidar da Gabi”**: `src/App.tsx` → `HomeScreen` → `onPlay`.
- Abertura dentro do app: `App` alterna `screen: "home" | "game"` (sem rota externa, sem iframe).
- Jogo: `src/App.tsx` → `GameView` (exportado; pode ser importado por outro app: `import { GameView } from "./game/GameView"` — hoje vive em `App.tsx` para manter o build single-file simples).
- Voltar: `HUD` (botão “Voltar”) → `GameView.handleBack()` → `engine.save()` + desmonta.
- Fechar **para tudo**: cleanup do `GameCanvas` cancela o RAF, faz `renderer.dispose()`, `audio.stopAll()` e `engine.save()`. Nenhum timer sobrevive (engine usa um único RAF; autosave é contador interno desse loop).

## 2. Como executar

```bash
npm install
npm run dev     # desenvolvimento
npm run build   # gera dist/index.html (single-file)
```

## 3. Configuração da simulação

Arquivo único: `src/config/simulation.ts`
- `clock.realMinutesPerGameDay` (padrão 24), fases do dia.
- `navigation` (raio, velocidade, célula A*).
- `needs.decayPerGameHour`, limiares, cap offline (`maxOfflineGameHours: 10`).
- `foods` (7 opções com efeitos), `activities` (durações + marcadores provisórios).
- `evolution` (XP, fases, intervalos de autonomia), `save` (chaves).

## 4. Catálogo de animações pendentes

- Fonte de verdade: `src/game/animationManifest.ts` (`ANIMATION_MANIFEST`, 39 clipes, todos `pending`).
- Grupos: repouso, locomoção, postura, alimentação, estudo, brincadeira, sono, guarda-roupa, banheiro, reações.
- Importação: ver `docs/CHARACTER-ANIMATION-BIBLE.md` §7. Resumo: soltar GLB em `public/animations/`, atualizar manifesto, trocar driver. **Sem reescrever a simulação.**

## 5. Persistência (limitação documentada)

- Hoje: **somente local** (`localStorage`, chaves `gabi-save-v1`, `gabi-settings-v1`).
- Formato `SaveData` (`src/game/types.ts`): gameMinutes, dayCount, needs, xp, outfit, daytimeOutfit, state, activity, activityPhase, position, facing, lastRealTime.
- Para backend: substituir `loadSave/saveGame` em `src/game/persistence.ts` por chamadas à API do app mantendo o formato. Reconciliação offline (`engine.reconcile`) já limita deterioração (cap 10h de jogo) e retoma madrugada na cama de pijama em SLEEP.

## 6. Arquitetura (arquivos)

| Arquivo | Papel |
|---|---|
| `src/game/engine.ts` | relógio, necessidades, máquina de estados, autonomia, saves |
| `src/game/navigation.ts` | 12 pontos oficiais, obstáculos, A* com raio, validação |
| `src/game/roomBuilder.ts` | quarto 3D fiel às referências (parede verde, tapete redondo, porta BANHEIRO real), props, iluminação por horário |
| `src/game/gabiProvisional.ts` | malha temporária (⚠️ não final) |
| `src/game/animationLayer.ts` | driver procedural + `clipForState()` + loader GLB futuro |
| `src/game/animationManifest.ts` | 39 clipes pendentes |
| `src/game/GameCanvas.tsx` | renderer, câmera touch, raycast, loop, cleanup |
| `src/components/*` | HUD, ActionBar, painéis, inspeção |
