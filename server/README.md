# P2P Calling Server - Oracle RDS

## Oracle Setup

1. Launch Oracle RDS instance (19c or 21c)
2. Create user `P2P` with password
3. Grant necessary privileges
4. Run the SQL script

## Deployment Steps

```sql
-- Connect as admin user
CONNECT admin/password@HOST:PORT/PDB

-- Run the script
@/path/to/oracle_plsql.sql
```

## ORDS Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /ords/p2p/calls/ | Initiate call |
| GET | /ords/p2p/users/{id} | Get user info |
| GET | /ords/p2p/health/ | Health check |
| GET | /ords/p2p/sdp/offers/{id}/ | Get offer |

## Parameters

### Initiate Call
```json
POST /ords/p2p/calls/
{
  "caller_id": "user1",
  "callee_id": "user2"
}
```

### Get User
```json
GET /ords/p2p/users/user1
Response: {"fcm_token": "...", "name": "User 1"}
```

## Required Oracle Privileges

```sql
GRANT CONNECT, RESOURCE TO P2P;
GRANT CREATE VIEW, CREATE PROCEDURE TO P2P;
GRANT ALTER, INSERT, UPDATE, DELETE ON call_sessions TO P2P;
```
