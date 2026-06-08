# API Endpoints для Signaling Server (ORDS/APEX)

## Обновления

### 2026-03-31

#### Добавлены новые эндпоинты в ApiClient

Файл: `app/src/main/java/com/example/p2papp/ApiClient.kt`

## Эндпоинты

### 1. Регистрация пользователя
```
PUT /users/{user_id}
Body: {
  "user_id": "string",
  "fcm_token": "string",
  "name": "string"
}
Response: {"success": true}
```

### 2. Инициация звонка
```
POST /calls/
Body: {
  "caller_id": "string",
  "callee_id": "string"
}
Response: {"session_id": "uuid"}
```

### 3. Получение сессии
```
GET /session/{session_id}/
Response: {
  "session_id": "uuid",
  "caller_id": "string",
  "callee_id": "string",
  "status": "string"
}
```

### 4. Обновление статуса сессии
```
PUT /session/{session_id}/
Body: {
  "status": "pending|active|ended|rejected"
}
Response: {"success": true}
```

### 5. Отправка SDP offer
```
POST /sdp/offers/
Body: {
  "session_id": "uuid",
  "sdp": "sdp string"
}
Response: {"success": true}
```

### 6. Получение SDP offer
```
GET /sdp/offers/{session_id}/
Response: {"offer_sdp": "sdp string"}
```

### 7. Отправка SDP answer
```
POST /sdp/answers/
Body: {
  "session_id": "uuid",
  "sdp": "sdp string"
}
Response: {"success": true}
```

### 8. Получение SDP answer
```
GET /sdp/answers/{session_id}/
Response: {"answer_sdp": "sdp string"}
```

### 9. Отправка ICE кандидата
```
POST /candidates/
Body: {
  "session_id": "uuid",
  "candidate": "ice candidate string",
  "sdp_mid": "string",
  "sdp_manced": 0
}
Response: {"success": true}
```

### 10. Получение ICE кандидатов
```
GET /candidates/{session_id}/
Response: [
  {"candidate": "...", "sdp_mid": "...", "sdp_manced": 0}
]
```

### 11. Проверка здоровья сервиса
```
GET /health/
Response: {"status": "ok"}
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
