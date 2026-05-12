# VPS Deployment

This guide describes a simple Docker Compose deployment for CourseService and its PostgreSQL database.

## 1. Install Docker

Install Docker Engine and Docker Compose on the VPS.

Check:

```bash
docker --version
docker compose version
```

## 2. Create Directories

Recommended layout:

```bash
sudo mkdir -p /opt/studybytes/course-service
sudo mkdir -p /opt/studybytes/secrets
```

Place repository files under:

```text
/opt/studybytes/course-service
```

## 3. Create Backend Network

CourseService uses two Docker networks:

- `studybytes_backend_net` is the shared external backend network. BFF, UserService, CourseService and LearningService must all join this network.
- `course_db_net` is the private CourseService database network. It is created by `docker compose` as an internal network and should contain only `course-service` and `course-postgres`.

Create only the shared backend network manually:

```bash
docker network create studybytes_backend_net
```

If the full platform compose already creates the backend network, reuse the same name.

If `course_db_net` was created manually while testing an older compose file, remove it before starting CourseService:

```bash
docker network rm course_db_net
```

## 4. Create Environment File

Copy the example file:

```bash
cp .env.example .env
```

Edit `.env`:

```properties
COURSE_SERVICE_PORT=8082
COURSE_BACKEND_NETWORK=studybytes_backend_net
COURSE_DB_NETWORK=course_db_net

COURSE_SERVICE_DB_NAME=course_service
COURSE_SERVICE_DB_USERNAME=postgres
COURSE_SERVICE_DB_PASSWORD=<real-password>
COURSE_SERVICE_DB_URL=jdbc:postgresql://course-postgres:5432/course_service

COURSE_SERVICE_INTERNAL_API_KEY=<real-internal-api-key>

USER_SERVICE_JWT_ISSUER_URI=http://user-service:8081
USER_SERVICE_JWT_JWK_SET_URI=http://user-service:8081/api/v1/auth/.well-known/jwks.json
USER_SERVICE_JWT_AUDIENCE=study-platform
COURSE_SERVICE_JWT_ROLES_CLAIM=roles
COURSE_SERVICE_JWT_ROLE_PREFIX=ROLE_

SPRING_PROFILES_ACTIVE=prod
```

Do not put UserService private keys into CourseService.

## 5. Start Service

```bash
docker compose up -d --build
```

Check containers:

```bash
docker compose ps
```

## 6. Verify Health

If the host port is exposed:

```bash
curl http://localhost:8082/health
curl http://localhost:8082/ready
```

Expected:

```json
{"status":"UP"}
```

## 7. Check Logs

```bash
docker compose logs -f course-service
docker compose logs -f course-postgres
```

Useful checks:

- CourseService starts on port `8082`.
- PostgreSQL hostname is `course-postgres`.
- JWT verifier points to UserService JWKS.
- No real secrets are printed in logs.

## 8. Update Version

Pull or copy the new version, then rebuild:

```bash
docker compose pull
docker compose up -d --build
```

If using Git on the VPS:

```bash
git pull
docker compose up -d --build
```

## 9. Rollback

If the new version fails:

```bash
git checkout <previous-commit-or-tag>
docker compose up -d --build
```

Check logs and health endpoints again.

## 10. Shutdown

Stop containers without deleting data:

```bash
docker compose down
```

Do not delete the `course_postgres_data` volume unless a full database reset is intended.
