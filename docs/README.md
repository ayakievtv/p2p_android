# P2P Android Documentation

## Overview

P2P Android is a peer-to-peer calling application using WebRTC for audio/video communication and Firebase Cloud Messaging for signaling.

## Documentation Index

| # | Document | Description |
|--|-|-----------||
| 01 | [PROJECT_ARCHITECTURE](01_PROJECT_ARCHITECTURE.md) | System architecture overview |
| 02 | [WEBRTC_IMPLEMENTATION](02_WEBRTC_IMPLEMENTATION.md) | WebRTC client implementation |
| 03 | [FCM_IMPLEMENTATION](03_FCM_IMPLEMENTATION.md) | Firebase Cloud Messaging setup |
| 04 | [SERVER_API](04_SERVER_API.md) | REST API endpoints and database schema |
| 05 | [CHANGES_SUMMARY](05_CHANGES_SUMMARY.md) | Recent changes and completed tasks |
| 06 | [SERVER_DATABASE](06_SERVER_DATABASE.sql) | SQL schema and procedures |
| 07 | [ANDROID_INTEGRATION](07_ANDROID_INTEGRATION.md) | Android components integration |
| 08 | [CALL_FLOW](08_CALL_FLOW.md) | Call sequence diagrams |
| 09 | [MIGRATION_GUIDE](09_MIGRATION_GUIDE.md) | Migrating from v1.x to v2.0 |

## Quick Start

1. **Server Setup**
   - Deploy ORDS/APEX endpoints (see 04_SERVER_API.md)
   - Run SQL schema (see 06_SERVER_DATABASE.sql)
   - Configure Firebase project

2. **Android Setup**
   - Add `google-services.json` to `app/`
   - Update server URL in BuildConfig
   - Build and run

3. **Testing**
   - Use two devices with different user IDs
   - Ensure both devices have internet access
   - Check FCM token registration

## Current Status

- [x] WebRTC client (audio only)
- [x] FCM signaling
- [x] Compose UI
- [x] Call service
- [ ] Video capture
- [ ] Server deployment
- [ ] Production testing
