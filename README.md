# e-Adalat — Online Dispute Resolution Platform

e-Adalat is a full-stack **Online Dispute Resolution (ODR)** platform: citizens file
disputes, upload evidence, get AI-generated case summaries, schedule video hearings,
and receive notifications — all through a set of Spring Boot microservices behind a
single API gateway, with a plain-JS web client.

## Architecture

```
                        +------------------+
                        |   web-client     |  (nginx, static SPA)
                        |  localhost:3000  |
                        +--------+---------+
                                 |
                        +--------v---------+
                        |     gateway      |  Spring Cloud Gateway
                        |  localhost:8080  |  JWT auth · routing
                        +---+---+---+---+--+
                            |   |   |   |
        +-------------------+   |   |   +-------------------+
        |                       |   |                       |
+-------v-------+     +---------v-+ +-v-----------+ +-------v-------+
| auth-service  |     |case-service| |scheduling-  | |  hearing-     |
|    :8081      |     |   :8082    | | service     | |  service      |
| users · JWT   |     |cases · AI  | |   :8083     | |    :8086      |
|               |     | summaries  | | hearings    | | WebRTC rooms  |
+---------------+     +------------+ +-------------+ +---------------+
+-------v-------+     +---------v-----------------+ +-------v-------+
|document-      |     | notification-service      |
|service :8084  |     |        :8085              |
|uploads · Redis|     | Kafka consumer · bell     |
+-------+-------+     +---------------------------+
        |
+-------v------------------------------------------------------+
|  infra: mysql:8 (:3306) · redis:7 (:6379) · kafka:3.8 (:9092) |
+--------------------------------------------------------------+

Kafka topics: case events, hearing events, notification fan-out.
Redis: document metadata cache. MySQL: one schema per data-owning service.
```

## Services & Ports

| Service              | Port | Database              | Notes                                  |
|----------------------|------|-----------------------|----------------------------------------|
| gateway              | 8080 | —                     | JWT validation, routes `/api/*`        |
| auth-service         | 8081 | `eadalat_auth`        | register/login/me, BCrypt + JJWT       |
| case-service         | 8082 | `eadalat_cases`       | cases CRUD, AI summary                 |
| scheduling-service   | 8083 | `eadalat_scheduling`  | hearing scheduling                     |
| document-service     | 8084 | `eadalat_documents`   | uploads (Redis cache + `uploads-data`) |
| notification-service | 8085 | `eadalat_notifications` | Kafka consumer, unread bell          |
| hearing-service      | 8086 | — (in-memory/Redis)   | WebRTC rooms, STOMP signaling          |
| web (nginx)          | 3000 | —                     | serves `web-client/`                   |
| mysql                | 3306 | —                     | shared instance, per-service schemas   |
| redis                | 6379 | —                     | document cache                         |
| kafka                | 9092 | —                     | KRaft single broker (dev)              |

## Quickstart

Prerequisites: Docker + Docker Compose, Java 17 + Maven (for local builds).

```bash
# 1. Clone and build everything
git clone <repo-url> eadalat && cd eadalat

# 2. (optional) set secrets — dev defaults work out of the box
#    MYSQL_ROOT_PASSWORD, JWT_SECRET (>=32 chars in prod), KAFKA_CLUSTER_ID

# 3. Build & start the whole stack
docker compose up --build

# 4. Open the app
#    Web UI : http://localhost:3000
#    API    : http://localhost:8080/api/...
```

First run takes a few minutes (Maven builds inside each service image). MySQL
auto-creates the five schemas via `createDatabaseIfNotExist=true`.

### Demo accounts

| Email               | Password | Role  |
|---------------------|----------|-------|
| admin@eadalat.local | admin123 | ADMIN |

(Register via the UI for CITIZEN/LAWYER/JUDGE accounts, or hit
`POST /api/auth/register`.)

## API Overview (via gateway `http://localhost:8080`)

| Service | Method & Path | Description |
|---|---|---|
| auth | `POST /api/auth/register` | Register `{name, email, password, role}` → `{token, user}` |
| auth | `POST /api/auth/login` | Login `{email, password}` → `{token, user}` |
| auth | `GET /api/auth/me` | Current user (Bearer token) |
| cases | `GET /api/cases` | List my cases |
| cases | `POST /api/cases` | File a case `{title, category, description}` |
| cases | `GET /api/cases/{id}` | Case detail |
| cases | `GET /api/cases/{id}/summary` | AI-generated case summary |
| scheduling | `GET /api/hearings?caseId=` | Hearings for a case |
| scheduling | `POST /api/hearings` | Schedule `{caseId, scheduledAt, title}` (JUDGE/ADMIN) |
| documents | `GET /api/documents?caseId=` | List documents |
| documents | `POST /api/documents` | Upload (multipart `file` + `caseId`) |
| documents | `GET /api/documents/{id}/download` | Download file |
| notifications | `GET /api/notifications?unreadOnly=true` | Unread notifications |
| notifications | `POST /api/notifications/{id}/read` | Mark as read |
| hearing | `GET /api/hearings?caseId=` | Hearings incl. `roomId` |
| hearing | WS ` /ws-hearing` | STOMP/SockJS signaling; topic `/topic/room/{roomId}`, send to `/app/signal/{roomId}` with `{from, type: JOIN\|OFFER\|ANSWER\|ICE\|LEAVE, sdp, candidate}` |

All endpoints except `/api/auth/*` require `Authorization: Bearer <JWT>`.

## Environment Variables

| Variable | Default | Used by |
|---|---|---|
| `MYSQL_ROOT_PASSWORD` | `root` | mysql + all data services |
| `JWT_SECRET` | `eadalat-dev-secret-change-in-prod-min-32-chars!!` | gateway + all services (must match; ≥32 chars) |
| `KAFKA_CLUSTER_ID` | `I0PjR2sWTa6mX9dB8cF1eQ` | kafka (22-char base64) |
| `SPRING_DATASOURCE_URL` | per-service JDBC URL | auth/case/scheduling/document/notification |
| `KAFKA_BOOTSTRAP_SERVERS` | `kafka:9092` | all services |
| `REDIS_HOST` | `redis` | document-service |
| `UPLOAD_DIR` | `/data/uploads` | document-service |
| `*_SERVICE_URL` | `http://<service>:<port>` | gateway downstream routing |

## Kubernetes

Manifests live in `k8s/` (namespace `eadalat`):

```bash
kubectl apply -f k8s/

# add to /etc/hosts:
# <ingress-ip> eadalat.local
```

- `namespace.yaml` — the `eadalat` namespace.
- `mysql.yaml` — Secret, 5Gi PVC, Deployment, headless Service.
- `redis.yaml`, `kafka.yaml` — single-node KRaft broker (dev only).
- `apps.yaml` — Deployment + ClusterIP Service for each of the 7 app services.
  **Images are placeholders** (`eadalat/<name>:latest`): build each module
  (`mvn package` → module `Dockerfile`) and push to your registry, then update
  the image names.
- `web.yaml` — nginx placeholder serving the web client (serve `web-client/`
  from your own image or CDN in production).
- `ingress.yaml` — `ingressClassName: nginx`, host `eadalat.local`:
  `/api` → gateway:8080, `/` → web:80.

For production: replace the single-node Kafka with Strimzi/MSK, use real
Secrets management for `JWT_SECRET`/`MYSQL_ROOT_PASSWORD`, and add TLS on the
Ingress.

## CI

`.github/workflows/ci.yml` runs `mvn -B -DskipTests package` from the repo root
on every push/PR to `main` (Temurin JDK 17, Maven cache).

## Repo layout

```
eadalat/
├── pom.xml                 # parent: spring-boot 3.2.5, spring-cloud 2023.0.1, jjwt-bom 0.11.5
├── docker-compose.yml      # full local stack
├── k8s/                    # kubernetes manifests
├── .github/workflows/ci.yml
├── web-client/             # index.html, app.js, styles.css (static SPA)
├── auth-service/           # sibling teams — do not edit from this tree
├── case-service/
├── scheduling-service/
├── document-service/
├── notification-service/
├── hearing-service/
└── gateway/
```
