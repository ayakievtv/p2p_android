# Архитектура P2P звонков между Android приложениями

## Обзор системы

Данное приложение реализует P2P видео/аудио звонки между двумя Android устройствами с использованием WebRTC.

## Компоненты архитектуры

### 1. Android Client (Kotlin)
- **MainActivity** — главный экран с UI для звонков
- **ApiClient** — работа с REST API (ORDS/APEX)
- **WebRTCManager** — управление WebRTC соединением
- **MyFirebaseMessagingService** — обработка push уведомлений

### 2. Signaling Server (ORDS/APEX)
REST API для обмена сигнальными сообщениями:
- `POST /register_token` — регистрация FCM токена пользователя
- `POST /call_initiate` — инициация звонка
- `POST /call_send_offer` — отправка SDP offer
- `POST /call_send_answer` — отправка SDP answer
- `POST /call_send_candidate` — отправка ICE кандидата
- `GET /call_get_offer` — получение offer от callee

### 3. Firebase Cloud Messaging
Push уведомления для:
- Нового звонка (`call_invitation`)
- Принятия звонка (`call_accepted`)
- Отказа в звонке (`call_rejected`)

## Поток звонка (Call Flow)

```
1. Caller (A)                    Callee (B)
       |                            |
       |--- initiateCall() -------->|  (API + FCM)
       |                            |
       |<--- onIncomingCall() ------|  (FCM)
       |                            |
       |--- createOffer() --------->|  (SDP offer)
       |                            |
       |<--- createAnswer() --------|  (SDP answer)
       |                            |
       |--- ICE candidates -------->|  (ICE)
       |                            |
       |<--- ICE candidates --------|  (ICE)
       |                            |
       |--- P2P Media ------------->|  (Video/Audio)
```

## Базы данных (ORDS/APEX)

### Таблица app_users
| Column | Type | Description |
|--------|------|-------------|
| user_id | VARCHAR2 | Уникальный ID пользователя |
| fcm_token | VARCHAR2 | FCM токен для push уведомлений |
| name | VARCHAR2 | Имя пользователя |
| created_at | TIMESTAMP | Время создания |

### Таблица call_sessions
| Column | Type | Description |
|--------|------|-------------|
| session_id | VARCHAR2 | Уникальный ID сессии |
| caller_id | VARCHAR2 | ID звонящего |
| callee_id | VARCHAR2 | ID получателя |
| status | VARCHAR2 | Статус (pending, active, ended) |
| created_at | TIMESTAMP | Время начала звонка |
| updated_at | TIMESTAMP | Время обновления |

### Таблица ice_candidates
| Column | Type | Description |
|--------|------|-------------|
| id | NUMBER | Первичный ключ |
 | session_id | VARCHAR2 | ID сессии |
| candidate | CLOB | ICE кандидат |
| sdp_mid | VARCHAR2 | SDP mid |
| sdp_manced | NUMBER | SDP manced |
| created_at | TIMESTAMP | Время создания |

## Текущий статус реализации

- [x] Базовая структура проекта
- [ ] WebRTCManager и зависимости
- [ ] FCM сервис
- [ ] API endpoints (ORDS/APEX)
- [ ] Полная логика звонка
