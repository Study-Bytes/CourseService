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

COURSE_SERVICE_FLYWAY_ENABLED=true
COURSE_SERVICE_FLYWAY_SCHEMA=public
COURSE_SERVICE_FLYWAY_BASELINE_ON_MIGRATE=false

COURSE_SERVICE_INTERNAL_API_KEY=<real-internal-api-key>

USER_SERVICE_JWT_ISSUER_URI=http://user-service:8081
USER_SERVICE_JWT_JWK_SET_URI=http://user-service:8081/api/v1/auth/.well-known/jwks.json
USER_SERVICE_JWT_AUDIENCE=study-platform
COURSE_SERVICE_JWT_ROLES_CLAIM=roles
COURSE_SERVICE_JWT_ROLE_PREFIX=ROLE_

SPRING_PROFILES_ACTIVE=prod
```

`COURSE_SERVICE_PORT` is currently a temporary localhost-only host port for Nginx while BFF/reverse proxy is not ready:

```text
127.0.0.1:8082 -> course-service:8082
```

The application container still listens on `8082`. Docker binds the host port to `127.0.0.1`, so Nginx on the VPS can proxy to `http://127.0.0.1:8082`, but direct external access to `http://<vps-ip>:8082` should not work.

Public development access goes through Nginx:

```text
https://dev-api.studybytes.ru/course-service/swagger-ui.html
https://dev-api.studybytes.ru/course-service/v3/api-docs
https://dev-api.studybytes.ru/course-service/api/v1/courses
```

In the full platform deployment, public traffic should enter through BFF/reverse proxy and this CourseService-specific exposure should be removed or kept localhost-only.

Production profile enables Spring forwarded headers support:

```properties
server.forward-headers-strategy=framework
```

When CourseService is served behind Nginx under `/course-service`, Nginx must pass forwarded headers:

```nginx
proxy_set_header Host $host;
proxy_set_header X-Forwarded-Host $host;
proxy_set_header X-Forwarded-Proto $scheme;
proxy_set_header X-Forwarded-Port $server_port;
proxy_set_header X-Forwarded-Prefix /course-service;
```

These headers let Spring and Springdoc build Swagger/OpenAPI URLs with the correct public scheme, host and path prefix.

Nginx strips the external `/course-service` prefix before proxying to the container. External requests like:

```text
https://dev-api.studybytes.ru/course-service/api/v1/courses
```

reach the application as:

```text
http://127.0.0.1:8082/api/v1/courses
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
curl https://dev-api.studybytes.ru/course-service/health
curl https://dev-api.studybytes.ru/course-service/ready
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


## 8. Database Migrations

CourseService uses Flyway for database schema changes. On startup, Flyway runs before Hibernate validation. In the production profile, Hibernate uses `ddl-auto=validate`, so it verifies that the database matches the entities but does not create or alter tables.

Expected production behavior:

```text
Flyway applies pending SQL migrations -> flyway_schema_history is updated -> Hibernate validates schema -> app starts
```

Check migration status in logs:

```bash
docker compose logs course-service | grep -i flyway
```

A clean PostgreSQL volume should be initialized automatically by `V1__init_course_service_schema.sql`. The database should contain the application tables and `flyway_schema_history` after the first successful startup.

To inspect tables on the VPS:

```bash
docker compose exec course-postgres psql -U "$COURSE_SERVICE_DB_USERNAME" -d "$COURSE_SERVICE_DB_NAME" -c "\dt"
docker compose exec course-postgres psql -U "$COURSE_SERVICE_DB_USERNAME" -d "$COURSE_SERVICE_DB_NAME" -c "select installed_rank, version, description, success from flyway_schema_history order by installed_rank;"
```

Do not set `COURSE_SERVICE_JPA_DDL_AUTO=update` in production. That would bypass the migration discipline and allow Hibernate to mutate the schema silently.

For a full local/VPS reset of only CourseService data, stop containers and remove the database volume:

```bash
docker compose down -v
docker compose up -d --build
```

Use this only for disposable environments. On a real VPS with useful data, create a new `V<next>__*.sql` migration instead of deleting the volume.

Future migration names must follow this pattern:

```text
src/main/resources/db/migration/V2__short_description.sql
src/main/resources/db/migration/V3__another_schema_change.sql
```

Never edit an already-applied migration on a shared or production database.

## 9. Update Version

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

## 10. Rollback

If the new version fails:

```bash
git checkout <previous-commit-or-tag>
docker compose up -d --build
```

Check logs and health endpoints again.

## 11. Shutdown

Stop containers without deleting data:

```bash
docker compose down
```

Do not delete the `course_postgres_data` volume unless a full database reset is intended.
