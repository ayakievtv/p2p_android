# Server Deployment Guide

## Oracle RDS Setup

### 1. Launch Oracle RDS Instance
- Engine: Oracle Database
- Version: 19c or 21c
- Instance class: db.t3.small (dev) or db.t3.medium (prod)
- Storage: 20GB (can auto-scale)

### 2. Configure Security
```sql
-- Create schema and user
CREATE USER p2p IDENTIFIED BY "your_password";
GRANT CONNECT, RESOURCE, CREATE VIEW TO p2p;
GRANT CREATE TABLE, CREATE SEQUENCE, CREATE TRIGGER TO p2p;
GRANT ALTER, INSERT, UPDATE, DELETE ON call_sessions TO p2p;
```

### 3. Enable ORDS
```sql
-- Connect as admin
BEGIN
  ORDS.ENABLE_SCHEMA(
    p_enabled => TRUE,
    p_schema => 'P2P',
    p_url_mapping_type => 'BASE_PATH',
    p_url_mapping_pattern => 'p2p'
  );
  COMMIT;
END;
/
```

## FastAPI Deployment

### Local Development
```bash
# Terminal 1
pip install -r requirements.txt

# Terminal 2 (optional)
npm install -g http-server
cd ../docs && http-server -p 8080
```

### Production (Docker)
```bash
# Build and run
docker-compose up -d

# Check logs
docker logs p2p-server

# Health check
curl http://localhost:8000/health
```

### Cloud Run (Google Cloud)
```bash
# Build
gcloud builds submit --tag gcr.io/PROJECT_ID/p2p-server

# Deploy
gcloud run deploy p2p-server \
  --image gcr.io/PROJECT_ID/p2p-server \
  --platform managed \
  --allow-unauthenticated
```

### AWS ECS
```yaml
# docker-compose.yml for ECS
version: '3.8'
services:
  server:
    image: YOUR_ECR_URI
    port: 8000
    environment:
      - PORT=8000
```

## Database Connection Pool

```python
# Oracle connection with connection pooling
import cx_Oracle

pool = cx_Oracle.create_pool(
    user="p2p",
    password="password",
    dsn="host:1521/PDB1",
    min=2, max=10, increment=1
)
```

## Monitoring

- Health endpoint: `GET /health`
- Logs: Use structured logging
- Metrics: Add Prometheus endpoint
