# FCM (Firebase Cloud Messaging) Реализация

## Обновления

### 2026-03-31

#### Добавлен MyFirebaseMessagingService

Файл: `app/src/main/java/com/example/p2papp/MyFirebaseMessagingService.kt`

**Поддерживаемые действия:**
- `call_invitation` — входящий звонок
- `call_accepted` — звонок принят
 Событие `call_rejected` — звонок отклонен
- `call_ended` — звонок завершен

**Обработка сообщений:**
1. При получении `call_invitation` — отображается диалог с просьбой принять/отклонить
2. При получении `call_accepted` — статус обновляется
3. При получении `call_rejected` — звонок отклонен
4. При получении `call_ended` — звонок завершен

## Настройка Firebase

1. Создать проект в [Firebase Console](https://console.firebase.google.com/)
2. Добавить Android приложение
3. Скачать `google-services.json` и положить в `app/`
4. Включить FCM

## Требуемые разрешения

Уже добавлены в AndroidManifest.xml:
- `INTERNET`
- `ACCESS_NETWORK_STATE`
- `RECORD_AUDIO`
- `MODIFY_AUDIO_SETTINGS`
- `FOREGROUND_SERVICE`
- `WAKE_LOCK`
