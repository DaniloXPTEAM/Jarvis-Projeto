# Estrutura das classes de áudio do Alexa (campos e métodos)

Fonte: `inspect_android_class` do REA no APK `com.amazon.dee.app` 2.2.675097.0 (SHA-256 `574919d5a769b92b6d3f8c892c49cc952dcbc2481601dedef8a65c3c83ed045a`).
**É só a estrutura** (nomes, tipos e assinaturas). **Não há código nem corpo de método aqui.** Os valores já estão em `PARAMETROS_DE_VOZ.md`.
Omitidos de propósito: métodos sintéticos gerados pelo compilador (`m179xx$$Nest$...`), que são só acessos internos entre classes.
Os nomes das classes são os da Amazon, usados aqui como **rótulos de referência**; não os use no código da Gabi.

---

## 1. `com.amazon.alexa.wakeword.ShortRingBuffer` — buffer circular de áudio
**Campos:** `TAG: String` · `capacity: int` · `elements: short[]` · `writePosition: long` · `bufferStartPosition: long`
**Classe interna:** `RingBufferReader` (leitor independente)
**Métodos:** `ShortRingBuffer(int capacity)` · `available(): int` · `getReader(): RingBufferReader` · `reset()` · `write(short)` · `write(short[], int)` · `toString()`

## 2. `com.amazon.alexa.wakeword.WakeWordDetectingAudioCapturer` — captação + detecção + envio
**Campos:** `PREROLL_SAMPLE_SIZE: int` · `SAMPLES_IN_ONE_MS: int` · `TAG: String` · `audioSink: AlexaAudioSink` · `bufferingWakeWordDetector: BufferingWakeWordDetector` · `callbacks: WakeWordDetectionListener` · `detectorCompat: PryonWakeWordDetectorCompat` · `isTransferringData: boolean` · `pryonCallbacksWrapper: PryonCallbacksWrapper` · `transferringExecutor: ExecutorService` · `useAcousticEchoCanceler: boolean` · `useLegacySDKSettingForPryon: boolean` · `wakeWordDetectionMetricsListener`
**Construtores (4):** recebem o detector (ou um provedor dele), o listener de detecção, listener de captura, listener de métricas e **dois booleanos** (AEC e config "legada")
**Métodos:** `startCapturing(AlexaAudioSink): boolean` · `stopCapturing()` · `isCapturing(): boolean` · `isDetectingWakeWord(): boolean` · `pauseDetectingWakeWord()` · `resumeDetectingWakeWord()` · `stopDetectingWakeWord()` · `onWakeWordDetected(AlexaWakeWord, byte[])` · `onClassificationEvent(byte[], int, byte[])` · `onEnrollmentExampleEvent(AlexaWakeWord, short[], byte[])` · `startTransferring(RingBufferReader)` · `adjustWakeWordIndices(AlexaWakeWord, RingBufferReader): AlexaWakeWord` · `allocateByteBuffer(): ByteBuffer` · `getArray(ByteBuffer): byte[]` · `convertSamplesToMilliseconds(long): long` · `createPryonCallbacksWrapper()`

## 3. `com.amazon.alexa.wakeword.BufferingWakeWordDetector` — detector com buffer
**Campos:** `BUFFER_CAPACITY_IN_SECONDS: int` · `CAPACITY: int` · `SAMPLE_RATE: int` · `TAG` · `audioCaptureListener` · `executor: ExecutorService` · `metricsListener` · `pryon: PryonLite2000` · `recordingRunnable: PryonRecordingRunnable` · `ringBuffer: ShortRingBuffer`
**Métodos:** `startDetectingWakeWord(): boolean` · `startDetectingWakeWord(boolean, boolean): boolean` · `stopDetectingWakeWord()` · `pauseDetectingWakeWord()` · `resumeDetectingWakeWord()` · `stopCapturing()` · `isCapturing()` · `isDetectingWakeWord()` · `hasPryon()` · `getRingBuffer(): ShortRingBuffer` · `getSamplesPushedToPryon(): long` · `createRecordingRunnable(boolean, boolean)` · `release()`

## 4. `com.amazon.alexa.wakeword.PryonRecordingRunnable` — laço de captação
**Campos:** `SHUTDOWN_TIMEOUT_MS: long` · `TAG` · `audioCaptureListener` · `capturing: boolean` · `detectingWakeWord: boolean` · `wakeWordDetectionPaused: boolean` · `lazyTimeProvider` · `metricsListener` · `pryon: PryonLite2000` · `ringBuffer: ShortRingBuffer` · `samplesPushedToPryon: AtomicLong` · `shutdownCondition: ConditionVariable` · `useAcousticEchoCanceler: boolean` · `useLegacySDKSettingForPryon: boolean`
**Métodos:** `run()` · `createAudioCapturer(): AudioCapturer` · `transferAudio(AudioCapturer)` · `setWakeWordDetectionPaused(boolean)` · `isWakeWordDetectionPaused()` · `isCapturing()` · `isDetectingWakeWord()` · `stopCapturing()` · `stopDetectingWakeWord()` · `getSamplesPushedToPryon()` · `getCurrentTime()` · `isReportingMetric()`

## 5. `com.amazon.alexa.wakeword.pryon.PryonConfigProvider` — configuração do detector
**Campos:** `DEFAULT_DETECTION_SENSITIVITY: int` · `TAG`
**Métodos:** `createPryonConfig(): PryonLite2000.Config` · `createPryonConfigUsingLegacySetting(): PryonLite2000.Config`

## 6. `com.amazon.alexa.voice.pryon.asr.AudioCapturer` — o microfone de fato
**Campos:** `SIXTEEN_KHZ: int` · `TEST_SAMPLE_RATE: int` · `TAG` · `audioRecord: android.media.AudioRecord` · `audioBuffer: short[]` · `bufferSize: int` · `bytesBufferSize: int` · `acousticEchoCanceler: android.media.audiofx.AcousticEchoCanceler` · `noiseSuppressor: android.media.audiofx.NoiseSuppressor`
**Construtores:** `(int, boolean, boolean)` e `(int, boolean, boolean, boolean)`
**Métodos:** `read(): short[]` · `release()` · `buildAudioRecord(boolean)` · `requestAcousticEchoCanceler(int)` · `releaseAcousticEchoCanceler()` · `requestNoiseSuppressor(int)` · `releaseNoiseSuppressor()` · `checkAudioRecord(AudioRecord, int)` · `buildAudioRecordData(int,int,int,int,int)` · `logCurrentAudioRecord()` · `logTestAudioRecord(int)`

## 7. `com.amazon.alexa.client.alexaservice.base.interactions.AudioFocusManager` — foco de áudio da conversa
**Campos:** `OVERRIDE_FOCUS_DURATION_VALUE: int` · `OVERRIDE_FOCUS_POLICIES: boolean` · `TAG` · `audioManager: android.media.AudioManager` · `audioFocusRequestProvider` · `audioMetadataByType: TreeMap<AudioFocusDuration, TreeSet<AudioMetadata>>` · `channels: Channels` · `currentAudioFocus: int` · `currentAudioFocusRequest` · `currentAudioMetadata` · `eventBus` · `externalMediaPlayerPlaybackAuthority` · `hasFocus: AtomicBoolean` · `hasFullFocus: AtomicBoolean` · `isPersistentAudioPlaying: boolean` · `isTransientAudioPlaying: boolean` · `nextItemState`
**Classes internas:** `AudioFocusDurationComparator` · `AudioMetadataComparator` · `AudioFocusManagerChannelsListener` · `NextItemState`
**Métodos:** `requestAudioFocus(): boolean` · `abandonAudioFocus()` · `onAudioFocusChange(int)` · `notifyFocusGained(AudioFocusDuration, boolean, boolean)` · `notifyFocusLost()` · `notifyFocusRequestFailed()` · `getBestAudioMetadata()` · `createAudioMetadataCounts()` · `updateAudioMetadata()` · `updateChannelFocus()` · `sendAudioPlaybackChangedEvents()` · `transitionInteractionLifecycleToAppropriateState(AudioInteractionLifecycle, boolean)` · três `on(...)` para eventos de item de áudio e de playlist

---

### Como esta estrutura se encaixa (leitura minha, pela estrutura)
`AudioCapturer` (microfone) → `PryonRecordingRunnable` (laço) → alimenta o detector e o `ShortRingBuffer` → `WakeWordDetectingAudioCapturer` detecta e, em thread própria, lê o buffer a partir do início da palavra e envia ao destino. O `AudioFocusManager` fica à parte, no lado do serviço de conversa.
