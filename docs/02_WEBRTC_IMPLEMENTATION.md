# WebRTC Реализация

## Обновления

### 2026-03-31

#### Добавлен WebRTCManager

Файл: `app/src/main/java/com/example/p2papp/webrtc/WebRTCManager.kt`

**Возможности:**
- Инициализация PeerConnectionFactory
- Создание PeerConnection с ICE серверами
- Создание и обработка SDP offer/answer
- Добавление ICE кандидатов
- Завершение звонка

**Основные методы:**
- `initialize()` — инициализация фабрики
- `createPeerConnection(servers)` — создание соединения
- `createOffer(callback)` — создание SDP предложения
- `setRemoteDescription(sdp, type)` — установка удаленной описи
- `addIceCandidate(candidate)` — добавление ICE кандидата
- `hangUp()` — завершение звонка

#### Добавлен PeerConnectionObserver

Файл: `app/src/main/java/com/example/p2papp/webrtc/PeerConnectionObserver.kt`

Интерфейс для обработки событий WebRTC:
- `onIceCandidate(candidate)` — новый ICE кандидат
- `onAddStream(track)` — добавление медиа потока
- `onConnectionChange(state)` — изменение состояния соединения
- `onIceConnectionChange(state)` — изменение ICE состояния

## Следующие шаги

1. Добавить SurfaceView для отображения видео
2. Реализовать capture of audio/video
3. Добавить обработку ICE кандидатов через API
4. Тестирование соединения
