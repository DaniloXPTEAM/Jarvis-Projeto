# Gabi — Avatar da Assistente Virtual

Personagem: menina de corpo inteiro, cabelo ruivo ondulado longo até a cintura
repartido ao meio, olhos castanhos, sardinhas leves. Estilo 3D fofinho e minimalista.

Todas as poses têm **fundo transparente real (PNG com alfa)**, canvas 768x1024,
mesma escala e alinhadas pela base — dá pra fazer crossfade sem "pulo".

## Poses de assistente (vestido vermelho + botinhas pretas)
- gabi-idle.png       → PARADA (repouso, sorriso fechado)
- gabi-listening.png  → OUVINDO (atenta, mão perto da orelha)
- gabi-talking.png    → FALANDO (boca aberta, gesticulando)

## Poses de rotina (dia a dia no quarto)
- gabi-dormir.png      → 21:30 · pijama azul, espreguiçando com travesseiro
- gabi-comer.png       → 08:00 · sentada comendo cereal
- gabi-computador.png  → 14:00 · suéter mostarda, no laptop
- gabi-ler.png         → 18:00 · vestido lavanda, lendo um livro

## Cenário
- room-bg.png → quarto de adolescente, minimalista e aconchegante (sem personagem)
- gabi-spritesheet.png → todas as poses lado a lado

## Uso no app (React)
```tsx
const SRC = { idle, listening, talking, dormir, comer, computador, ler };

function Gabi({ pose }: { pose: keyof typeof SRC }) {
  return (
    <div className="relative w-56 aspect-[3/4]">
      {Object.keys(SRC).map((k) => (
        <img key={k} src={SRC[k]} alt="Gabi"
          className={"absolute inset-0 w-full h-full object-contain transition-opacity duration-500 " +
            (k === pose ? "opacity-100" : "opacity-0")} />
      ))}
    </div>
  );
}
```

Ciclo de conversa: toque -> listening (1.8s) -> talking (3.4s) -> idle.
Rotina por horário: 6-11 comer · 11-17 computador · 17-21 ler · senão dormir.
