# ORDS API Reference

## Base URL
`https://your-rds-host.com/ords/p2p/`

## Endpoints

### Health Check
```
GET /health/
```
Response:
```json
{"status":"ok","timestamp":"2024-01-01 12:00:00"}
```

---

### Users

#### Get User
```
GET /users/{user_id}
```
Response:
```json
{"fcm_token":"xxx","name":"John"}
```

#### Register/Update User
```
PUT /users/{user_id}
Content-Type: application/json

{"user_id":"xxx","fcm_token":"xxx","name":"John"}
```
Response:
```json
{"success":true}
```

---

### Calls

#### Initiate Call
```
POST /calls/
Content-Type: application/json

{"caller_id":"user1","callee_id":"user2"}
```
Response:
```json
{"session_id":"UUID"}
```

#### Get Session
```
GET /calls/{session_id}/
```
Response:
```json
{"session_id":"UUID","caller_id":"user1","callee_id":"user2","status":"pending","created_at":"2024-01-01 12:00:00"}
```

#### Update Session Status
```
PUT /calls/
Content-Type: application/json

{"status":"ringing"}  // or "active", "ended"
```

---

### SDP Offer

#### Save Offer
```
POST /sdp/offers/
Content-Type: application/json

{"session_id":"UUID","sdp":"v=0..."}
```

#### Get Offer
```
GET /sdp/offers/{session_id}/
```
Response:
```json
{"session_id":"UUID","offer_sdp":"v=0..."}
```

---

### SDP Answer

#### Save Answer
```
POST /sdp/answers/
Content-Type: application/json

{"session_id":"UUID","sdp":"v=0..."}
```

#### Get Answer
```
GET /sdp/answers/{session_id}/
```
Response:
```json
{"session_id":"UUID","answer_sdp":"v=0..."}
```

---

### ICE Candidates

#### Add Candidate
```
POST /candidates/
Content-Type: application/json

{"session_id":"UUID","candidate":"candidate:...","sdp_mid":"audio0","sdp_manced":0}
```

#### Get Candidates
```
GET /candidates/{session_id}/
```
Response:
```json
{"session_id":"UUID","candidates":[
  {"candidate":"candidate:...","sdp_mid":"audio0","sdp_manced":0}
]}
```
