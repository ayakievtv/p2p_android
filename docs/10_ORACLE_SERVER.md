# Oracle RDS Server Setup

## Database Schema (DDL)

```sql
-- 1. Users table
CREATE TABLE app_users (
    user_id VARCHAR2(50) PRIMARY KEY,
    fcm_token VARCHAR2(1000),
    name VARCHAR2(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_app_users_fcm ON app_users(fcm_token);

-- 2. Call sessions table
CREATE TABLE call_sessions (
    session_id VARCHAR2(50) PRIMARY KEY,
    caller_id VARCHAR2(50) NOT NULL,
    callee_id VARCHAR2(50) NOT NULL,
    status VARCHAR2(20) DEFAULT 'pending' CHECK (status IN ('pending', 'ringing', 'active', 'ended', 'rejected')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE call_sessions ADD CONSTRAINT fk_caller FOREIGN KEY (caller_id) REFERENCES app_users(user_id);
ALTER TABLE call_sessions ADD CONSTRAINT fk_callee FOREIGN KEY (callee_id) REFERENCES app_users(user_id);

CREATE INDEX idx_call_sessions_status ON call_sessions(status);
CREATE INDEX idx_call_sessions_caller ON call_sessions(caller_id);
CREATE INDEX idx_call_sessions_callee ON call_sessions(callee_id);

-- 3. ICE candidates table
CREATE TABLE ice_candidates (
    id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    session_id VARCHAR2(50) NOT NULL,
    candidate CLOB,
    sdp_mid VARCHAR2(50),
    sdp_manced NUMBER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE ice_candidates ADD CONSTRAINT fk_ice_session FOREIGN KEY (session_id) REFERENCES call_sessions(session_id) ON DELETE CASCADE;
CREATE INDEX idx_ice_session ON ice_candidates(session_id);

-- 4. SDP offers table
CREATE TABLE sdp_offers (
    session_id VARCHAR2(50) PRIMARY KEY,
    offer_sdp CLOB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 5. SDP answers table
CREATE TABLE sdp_answers (
    session_id VARCHAR2(50) PRIMARY KEY,
    answer_sdp CLOB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## ORDS REST Services

### 1. Register User
```
POST /ords/p2p/i/users/
Content-Type: application/json

{
  "user_id": "user123",
  "fcm_token": "token_xyz",
  "name": "John Doe"
}
```

### 2. Get User FCM Token
```
GET /ords/p2p/i/users/user123
```

### 3. Initiate Call
```
POST /ords/p2p/i/calls/
Content-Type: application/json

{
  "caller_id": "user1",
  "callee_id": "user2"
}
```

Response:
```json
{
  "session_id": "uuid-123"
}
```

### 4. Get Session
```
GET /ords/p2p/i/calls/uuid-123
```

### 5. Update Session Status
```
PUT /ords/p2p/i/calls/uuid-123
Content-Type: application/json

{
  "status": "ringing"
}
```

### 6. Save Offer
```
POST /ords/p2p/i/offers/
Content-Type: application/json

{
  "session_id": "uuid-123",
  "offer_sdp": "v=0..."
}
```

### 7. Get Offer
```
GET /ords/p2p/i/offers/uuid-123
```

### 8. Save Answer
```
POST /ords/p2p/i/answers/
Content-Type: application/json

{
  "session_id": "uuid-123",
  "answer_sdp": "v=0..."
}
```

### 9. Get Answer
```
GET /ords/p2p/i/answers/uuid-123
```

### 10. Send ICE Candidate
```
POST /ords/p2p/i/candidates/
Content-Type: application/json

{
  "session_id": "uuid-123",
  "candidate": "candidate:...",
  "sdp_mid": "audio0",
  "sdp_manced": 0
}
```

### 11. Get ICE Candidates
```
GET /ords/p2p/i/candidates/uuid-123
```

## ORDS Module Setup

```sql
-- Enable REST services
BEGIN
  ORDS.ENABLE_SCHEMA(
    p_enabled => TRUE,
    p_schema => 'P2P',
    p_url_mapping_type => 'BASE_PATH',
    p_url_mapping_pattern => 'p2p',
    p_validate_module => TRUE
  );

  ORDS.DEFINE_MODULE(
    p_module_name => 'p2p_api',
    p_base_path => '/i/',
    p_items_per_page => 25,
    p_status => 'PUBLISHED',
    p_comments => 'P2P Calling API'
  );

  -- Users resource
  ORDS.DEFINE_TEMPLATE(
    p_module_name => 'p2p_api',
    p_pattern => 'users/',
    p_priority => 1,
    p_etag_type => 'HASH',
    p_etag_query => NULL,
    p_comments => ''
  );

  ORDS.DEFINE_HANDLER(
    p_module_name => 'p2p_api',
    p_pattern => 'users/',
    p_method => 'POST',
    p_source_type => 'plsql/block',
    p_items_per_page => 0,
    p_mimes_allowed => '',
    p_comments => '',
    p_source => '
BEGIN
  INSERT INTO app_users (user_id, fcm_token, name)
  VALUES (:user_id, :fcm_token, :name);
  :out := ''{"success":true}'';
END;'
  );

  COMMIT;
END;
/
```

## Python FastAPI Server (Alternative)

If Oracle RDS is not available, use this FastAPI server:

```python
# main.py
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import Optional
import uvicorn

app = FastAPI()

# In-memory storage (replace with Oracle connection)
users = {}
sessions = {}
offers = {}
answers = {}
candidates = {}

class User(BaseModel):
    user_id: str
    fcm_token: str
    name: str

class CallRequest(BaseModel):
    caller_id: str
    callee_id: str

class SDPRequest(BaseModel):
    session_id: str
    sdp: str

class CandidateRequest(BaseModel):
    session_id: str
    candidate: str
    sdp_mid: str
    sdp_manced: int

@app.post("/register")
async def register_user(user: User):
    users[user.user_id] = user
    return {"success": True}

@app.post("/call_initiate")
async def initiate_call(req: CallRequest):
    import uuid
    session_id = str(uuid.uuid4())
    sessions[session_id] = {
        "caller_id": req.caller_id,
        "callee_id": req.callee_id,
        "status": "pending"
    }
    return {"session_id": session_id}

@app.get("/call/{session_id}")
async def get_session(session_id: str):
    if session_id not in sessions:
        raise HTTPException(status_code=404)
    return sessions[session_id]

@app.post("/call/{session_id}/status")
async def update_status(session_id: str, status: str):
    if session_id not in sessions:
        raise HTTPException(status_code=404)
    sessions[session_id]["status"] = status
    return {"success": True}

@app.post("/offer")
async def save_offer(req: SDPRequest):
    offers[req.session_id] = req.sdp
    return {"success": True}

@app.get("/offer/{session_id}")
async def get_offer(session_id: str):
    if session_id not in offers:
        raise HTTPException(status_code=404)
    return {"offer_sdp": offers[session_id]}

@app.post("/answer")
async def save_answer(req: SDPRequest):
    answers[req.session_id] = req.sdp
    return {"success": True}

@app.get("/answer/{session_id}")
async def get_answer(session_id: str):
    if session_id not in answers:
        raise HTTPException(status_code=404)
    return {"answer_sdp": answers[session_id]}

@app.post("/candidate")
async def add_candidate(req: CandidateRequest):
    if req.session_id not in candidates:
        candidates[req.session_id] = []
    candidates[req.session_id].append({
        "candidate": req.candidate,
        "sdp_mid": req.sdp_mid,
        "sdp_manced": req.sdp_manced
    })
    return {"success": True}

@app.get("/candidates/{session_id}")
async def get_candidates(session_id: str):
    return candidates.get(session_id, [])

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
```

Run with:
```bash
pip install fastapi uvicorn
python main.py
```
