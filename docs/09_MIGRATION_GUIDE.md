# Migration Guide

## Migrating from Version 1.x to 2.0

### Architecture Changes

**Old (Version 1):**
- Single Activity with fragment-based navigation
- Manual state management
- Direct API calls from UI

**New (Version 2):**
- Single Activity with Jetpack Compose
- StateFlow-based state management
- Repository pattern for data
- Background CallService

### Key Files Changed

| File | Change Type | Notes |
|------|------------|-------|
| `MainActivity.kt` | Rewrite | Now Compose-based |
| `ApiClient.kt` | Enhanced | Added new endpoints |
| `build.gradle` | Updated | WebRTC + Compose deps |
| `AndroidManifest.xml` | Updated | New services/receivers |

### Migration Steps

1. **Update dependencies**
   ```gradle
   // Add to app/build.gradle
   implementation 'org.webrtc:google-webrtc:1.123.0'
   implementation 'androidx.activity:compose'
   ```

2. **Create new theme**
   - Delete old `styles.xml`
   - Create `Theme.kt` with Material 3

3. **Migrate UI**
   - Replace XML layouts with Compose
   - Use `Scaffold`, `Card`, `IconButton`
   - Add state management with `mutableStateOf`

4. **Add services**
   ```xml
   <service android:name=".CallService" />
   <service android:name=".CallConnectionService" />
   ```

5. **Update permissions**
   ```xml
   <uses-permission android:name="android.permission.CAMERA" />
   ```

### Breaking Changes

- `ApiClient.initiateCall()` now returns session ID asynchronously
- Call status enum values changed: `IDLE`, `CONNECTING`, `RINGING`, `IN_PROGRESS`, `ENDED`
- FCM actions: `call_invitation`, `call_accepted`, `call_rejected`, `call_ended`

### Deprecated

- `VideoCallFragment` — replaced by `VideoCallActivity`
- `CallAdapter` — replaced by `CallRepository`

## Quick Reference

### Starting a Call (New)
```kotlin
// Old way (removed)
startCallFragment(callerId, calleeId)

// New way
val intent = Intent(this, CallService::class.java).apply {
    putExtra("caller_id", callerId)
    putExtra("callee_id", calleeId)
}
startService(intent)
```

### Handling Incoming Call
```kotlin
// FCM will trigger MainActivity with extras
intent.getStringExtra("session_id")
intent.getStringExtra("caller_id")
```
