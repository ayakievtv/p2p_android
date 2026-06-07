# API Endpoints для Signaling Server (ORDS/APEX)

## Обновления

### 2026-03-31

#### Добавлены новые эндпоинты в ApiClient

Файл: `app/src/main/java/com/example/p2papp/ApiClient.kt`

## Эндпоинты

### 1. Регистрация токена
```
POST /register_token
Body: {
  "user_id": "string",
  "fcm_token": "string",
  "name": "string"
}
Response: {"success": true}
```

### 2. Инициация звонка
```
POST /call_initiate
Body: {
  "caller_id": "string",
  "callee_id": "string"
}
Response: {"session_id": "uuid"}
```

### 3. Получение сессии
```
GET /call_get_session?session_id=uuid
Response: {
  "session_id": "uuid",
  "caller_id": "string",
  "callee_id": "string",
  "status": "string"
}
```

### 4. Отправка SDP offer
```
POST /call_send_offer
Body: {
  "session_id": "uuid",
  "offer_sdp": "sdp string"
}
```

### 5. Получение SDP offer
```
GET /call_get_offer?session_id=uuid
Response: {"offer_sdp": "sdp string"}
```

### 6. Отправка SDP answer
```
POST /call_send_answer
Body: {
  "session_id": "uuid",
  "answer_sdp": "sdp string"
}
```

### 7. Получение SDP answer
```
GET /call_get_answer?session_id=uuid
Response: {"answer_sdp": "sdp string"}
```

### 8. Отправка ICE кандидата
```
POST /call_send_candidate
Body: {
  "session_id": "uuid",
  "candidate": "ice candidate string",
  "sdp_mid": "string",
  "sdp_manced": 0
}
```

### 9. Получение ICE кандидатов
```
GET /call_get_candidates?session_id=uuid
Response: [
  {"candidate": "...", "sdp_mid": "...", "sdp_manced": 0}
]
```

### 10. Обновление статуса звонка
```
POST /call_update_status
Body: {
  "session_id": "uuid",
  "status": "pending|active|ended|rejected"
}
```

## Требуемые таблицы базы данных

### Таблица app_users
```sql
CREATE TABLE app_users (
  user_id VARCHAR2(50) PRIMARY KEY,
  fcm_token VARCHAR2(500),
  name VARCHAR2(100),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Таблица call_sessions
```sql
CREATE TABLE call_sessions (
  session_id VARCHAR2(50) PRIMARY KEY,
  caller_id VARCHAR2(50),
  callee_id VARCHAR2(50),
  status VARCHAR2(20),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Таблица ice_candidates
```sql
CREATE TABLE ice_candidates (
  id NUMBER PRIMARY KEY,
  session_id VARCHAR2(50),
  candidate CLOB,
  sdp_mid VARCHAR2(50),
  sdp_manced NUMBER,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```
