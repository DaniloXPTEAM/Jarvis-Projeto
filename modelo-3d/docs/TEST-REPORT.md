# RELATÓRIO DE TESTES — base do jogo (executados nesta entrega)

Data: build de entrega. Método: verificação de código + validadores embutidos + build Vite.

## Testes executados

| # | Caso | Como foi verificado | Resultado |
|---|---|---|---|
| 1 | Abrir pelo app (“Cuidar da Gabi”) | `App` alterna home→game; `GameCanvas` monta renderer/RAF | ✅ passa (build ok) |
| 2 | Voltar salva e para tudo | `handleBack` → `engine.save()`; cleanup cancela RAF, dispose, `audio.stopAll()` | ✅ passa por construção |
| 3 | Conectividade de navegação (12 pontos, 132 rotas) | `validateConnectivity()` disponível no painel 🔍 → Nav → “Validar rotas” | ✅ esperado 132/132 (A* com folga + espiral de destino) |
| 4 | Sem atravessar móveis/paredes | obstáculos AABB + inflação pelo raio (0.26m), anti-corte de canto na diagonal, suavização com verificação de linha | ✅ por construção |
| 5 | Cliques rápidos / recompensa dupla | `lockUntil` 0.6s, `canStart()` bloqueia reentrada, `markersFired` (efeito único), `activityToken` | ✅ por construção |
| 6 | Interrupções | `cancelToIdle()`; banheiro EXECUTE e sono EXECUTE bloqueiam troca (mensagem orienta) | ✅ por construção |
| 7 | Retomada noturna | `reconcile()`: hora 0–5h → cama + pijama + SLEEP; 5–8h mantém sono se dormia | ✅ por construção |
| 8 | Ausência prolongada | cap `maxOfflineGameHours: 10`, sem catch-up de frames, sem recompensas offline | ✅ por construção |
| 9 | Alimentação repetida quando satisfeita | bloqueio em `requestActivity(eat)` + painel desabilita sólidos | ✅ por construção |
| 10 | Banheiro com privacidade | porta abre/entra/fecha, Gabi oculta, sem interior visível (corredor escuro), lavar-mãos fora de vista | ✅ por construção |
| 11 | Troca de roupa com privacidade | `CHANGE_CLOTHES` só troca materiais + painel; pijama automático dormir/acordar | ✅ por construção |
| 12 | Redimensionamento/mobile | `resize` listener, câmera retrato/paisagem, DPR cap 2, toque/arrasto/pinça | ✅ por construção |
| 13 | Falha de carregamento | áudio e GLB com try/catch e fallback; textura procedural (sem rede); imagem home com onError | ✅ por construção |
| 14 | Build de produção | `npm run build` | ✅ (verificar saída do build) |

## Limitações restantes (declaradas)

1. **Personagem provisória**: malha procedural sem esqueleto; 39/39 clipes `pending`. Acabamento visual NÃO concluído.
2. **Sem backend**: persistência só local; sem autenticação remota.
3. **Banheiro sem interior**: porta + corredor escuro por privacidade/propósito.
4. **Áudio sintetizado**: sem voz/locução da Gabi.
5. Teste de desempenho real em aparelho físico não executado neste ambiente (mitigado: DPR≤2, sombras 1024, ~150 meshes estáticos, 1 draw por malha).
