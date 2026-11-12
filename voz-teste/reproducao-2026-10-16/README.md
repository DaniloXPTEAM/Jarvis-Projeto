# Evidências preservadas em 16/10/2026

Os arquivos em `historico/` foram copiados da pasta temporária do Claude. A procedência e os hashes estão em `proveniencia.json`. Os WAVs B3/C3 e ref20 originais não foram regenerados nem alterados; a conferência está em `baseline-verificado.json`.

Os scripts históricos dependem do diretório corrente, do ambiente Python e de pesos externos. Não os execute sobre a pasta do baseline: use uma pasta nova para cada rodada. Os pesos ONNX permanecem no caminho de origem informado no manifesto; não foram duplicados. O cache Hugging Face também permanece no local original.

## Resultado conhecido

PyTorch CPU no Windows: B3 RTF 2,75; C3 RTF 2,50. ONNX/int8 portuguese_24l: B3 first PCM 7843 ms / RTF 4,494; C3 first PCM 5706 ms / RTF 4,833. No script ONNX, streaming e geração completa são execuções distintas; RAM é amostra após cada caso, não pico. Não há medição de som audível, CPU, bateria ou underruns.

Esses dados são históricos, não uma nova execução de benchmark. Não demonstram desempenho em Android ARM. Nenhum runtime ARM/mixed-precision foi validado ou integrado. Pocket, AudioTrack/PCM progressivo e preservação auditiva da identidade no runtime otimizado continuam pendentes. A referência 24L e os baselines são preservados para essa validação.
