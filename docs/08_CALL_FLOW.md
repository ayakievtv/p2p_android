# Call Flow Diagram

## Audio Call Flow

```
┌─────────────┐     ┌─────────────┐
│   Caller    │     │   Callee    │
└──────┬──────┘     └──────┬──────┘
       │                   │
       │ 1. Initiate Call  │
       │──────────────────>│
       │                   │
       │ 2. FCM Notification
       │<──────────────────│
       │                   │
       │ 3. Accept/Reject  │
       │<──────────────────│
       │                   │
       │ 4. Start WebRTC   │
       │──────────────────>│
       │                   │
       │ 5. Exchange SDP   │
       │──────────────────>│
       │                   │
       │ 6. Exchange ICE   │
       │◄═════════════════►│
       │                   │
       │ 7. Call Active    │
       │                 │
       │ (Audio Streaming)
       │                 │
       │                 │
       │ 8. End Call     │
       │──────────────────>│
       │                 │
```

## Sequence Diagram

### 1. Initiate Call
```json
// Caller → Server
POST /call_initiate
{
  "caller_id": "alice",
  "callee_id": "bob"
}

// Server → Caller
{
  "session_id": "uuid-123"
}
```

### 2. FCM Invitation
```kotlin
// Server → FCM
{
  "to": "bob_fcm_token",
  "data": {
    "type": "call_invitation",
    "session_id": "uuid-123",
    "caller_id": "alice"
  }
}
```

### 3. Callee Response
```kotlin
// Callee → Server
{
  "session_id": "uuid-123",
  "status": "active"  // or "rejected"
}
```

### 4. WebRTC Offer (Caller)
```kotlin
// Caller creates offer
val offer = peerConnection.createOffer()
peerConnection.setLocalDescription(offer)

// Send to server
apiClient.sendOffer("uuid-123", offer.sdp)
```

### 5. WebRTC Answer (Callee)
```kotlin
// Callee creates answer
val answer = peerConnection.createAnswer()
peerConnection.setLocalDescription(answer)

// Send to server  
apiClient.saveAnswer("uuid-123", answer.sdp)
```

### 6. ICE Candidates
```kotlin
// Both sides collect candidates
peerConnection.onIceCandidate = { candidate ->
    repository.addIceCandidate("uuid-123", candidate)
}
```

## Status Values

| Status | Description |
|--------|-------------|
| `pending` | Session created, waiting for response |
| `ringing` | Callee is being notified |
 | `active`    | Call is active (audio streaming) |
| `ended`   | Call finished normally |
| `rejected`| Call was rejected |

## Error Handling

- **Network failure**: Retry with exponential backoff
- **Session timeout**: Auto-end after 30 seconds
- **No FCM token**: Show error to user
- **WebRTC failure**: Fallback to audio notification
