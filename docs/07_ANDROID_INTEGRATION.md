# Android Integration Guide

## Обновления 2026-03-31

### Добавлены новые компоненты

1. **CallRepository** — централизованное хранилище состояния звонка
2. **CallService** — фоновый сервис для управления звонками
3. **CallConnectionService** — интеграция с системной телефонной подсистемой
4. **CallBroadcastReceiver** — обработка системных broadcast
5. **SignalingClient** — клиент сигналинга с WebSocket/polling

## Архитектура компонентов

```
┌─────────────────┐
│   MainActivity  │  (UI, Compose)
└────────┬────────┘
         │
       ▼
┌─────────────────┐
│  CallService    │  (фон. сервис)
└────────┬────────┘
         │
       ▼
┌─────────────────┐
│ CallRepository  │  ←→ API
└────────┬────────┘
         │
       ▼
┌─────────────────┐
│  ApiClient      │  HTTP API
│               │  FCM
└─────────────────┘
```

## Использование

### CallService

```kotlin
// В MainActivity
private fun startCall(callerId: String, calleeId: String) {
    val intent = Intent(this, CallService::class.java).apply {
        putExtra("caller_id", callerId)
        putExtra("callee_id", calleeId)
    }
    startService(intent)
}

private fun acceptCall(sessionId: String) {
    val intent = Intent(this, CallService::class.java).apply {
        putExtra("session_id", sessionId)
        putExtra("action", "accept_call")
    }
    startService(intent)
}
```

### CallRepository

```kotlin
// Создание сессии
val repository = CallRepository.getInstance()
val session = repository.createSession(
    sessionId = "uuid",
    callerId = "user1",
    calleeId = "user2"
)

// Обновление статуса
repository.updateSessionStatus("uuid", CallStatus.IN_PROGRESS)

// Добавление ICE кандидата
repository.addIceCandidate("uuid", iceCandidate)
```

### Системные разрешения

Добавлены в AndroidManifest.xml:
- `CAMERA` — для видео (в будущем)
- `BIND_TELECOM_SERVICE` — для ConnectionService

## Требуется google-services.json

1. Скачайте из Firebase Console
2. Положите в `app/google-services.json`
3. Приложение само подключится к FCM

## Настройка ProGuard

Добавить в `proguard-rules.pro`:

```proguard
# Keep WebRTC classes
-keep class org.webrtc.** { *; }
-keep class org.webrtc.** { *; }

# Keep OkHttp
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Keep Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.components.Component$Instantiation
```
