param([string]$Package = "", [int]$DurationSeconds = 90)
$ErrorActionPreference = 'Stop'
if (-not (Get-Command adb -ErrorAction SilentlyContinue)) { throw 'ADB nao encontrado. Instale Android platform-tools e adicione adb ao PATH.' }
$devices = @(adb devices | Select-String '\sdevice$')
if ($devices.Count -ne 1) { throw "Conecte exatamente um aparelho autorizado por ADB. Encontrados: $($devices.Count)" }
if (-not $Package) {
  Write-Host 'Pacotes com gabi no nome:'
  adb shell pm list packages | Select-String -Pattern 'gabi' | ForEach-Object { Write-Host $_ }
  $Package = Read-Host 'Informe o identificador do pacote (ex: com.exemplo.gabi)'
}
if ($Package -notmatch '^[a-zA-Z][a-zA-Z0-9_]*(\.[a-zA-Z][a-zA-Z0-9_]*)+$') { throw 'Identificador de pacote invalido.' }
$stamp = Get-Date -Format 'yyyyMMdd_HHmmss'
$out = Join-Path (Get-Location) "gabi_diagnostico_$stamp"
New-Item -ItemType Directory -Path $out | Out-Null
"Pacote: $Package`nInicio: $(Get-Date -Format o)`nDuracao: $DurationSeconds segundos" | Set-Content -Encoding UTF8 (Join-Path $out 'info.txt')
& adb shell getprop ro.product.model > (Join-Path $out 'modelo.txt')
& adb shell dumpsys meminfo $Package > (Join-Path $out 'memoria_inicio.txt')
& adb shell dumpsys cpuinfo > (Join-Path $out 'cpu_inicio.txt')
& adb shell dumpsys activity processes > (Join-Path $out 'processos_inicio.txt')
& adb shell dumpsys audio > (Join-Path $out 'audio_inicio.txt')
& adb logcat -c
Write-Host "Abra a Gabi e reproduza o problema agora. Coletando por $DurationSeconds segundos..."
Start-Sleep -Seconds $DurationSeconds
& adb logcat -d -v threadtime > (Join-Path $out 'logcat.txt')
& adb shell dumpsys meminfo $Package > (Join-Path $out 'memoria_fim.txt')
& adb shell dumpsys cpuinfo > (Join-Path $out 'cpu_fim.txt')
& adb shell dumpsys audio > (Join-Path $out 'audio_fim.txt')
& adb shell dumpsys gfxinfo $Package > (Join-Path $out 'interface_gfxinfo.txt')
& adb shell dumpsys activity processes > (Join-Path $out 'processos_fim.txt')
Get-Content (Join-Path $out 'logcat.txt') | Select-String -Pattern 'ANR in|FATAL EXCEPTION|OutOfMemoryError|AudioRecord|AudioTrack|SpeechRecognizer|TextToSpeech|Underrun|BufferQueue|Skipped [0-9]+ frames' | Set-Content -Encoding UTF8 (Join-Path $out 'eventos_relevantes.txt')
"Fim: $(Get-Date -Format o)" | Add-Content -Encoding UTF8 (Join-Path $out 'info.txt')
Write-Host "Diagnostico salvo em: $out"
Write-Warning 'Os logs podem conter falas transcritas, dados pessoais e tokens. Revise e remova informacoes sensiveis antes de compartilhar.'
