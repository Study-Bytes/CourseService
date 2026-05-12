# Network And Secrets

CourseService deployment uses separate networks for backend traffic and database traffic.

For endpoint ownership and caller rules, see [Endpoints Quick Reference](ENDPOINTS_QUICK_REFERENCE.md).

## Networks

Recommended platform networks:

```text
public_net:
  reverse-proxy
  site
  bff

backend_net:
  bff
  user-service
  course-service
  learning-service
  code-executor-service
  discussion-service

course_db_net:
  course-service
  course-postgres
```

In this repository, `docker-compose.yml` uses these network names:

```text
studybytes_backend_net
course_db_net
```

Only `studybytes_backend_net` is external. Create it once before running compose:

```powershell
docker network create studybytes_backend_net
```

`course_db_net` is not shared across services. It is created by `docker compose` as an internal private network for `course-service` and `course-postgres`.

The names can be overridden in `.env`:

```properties
COURSE_BACKEND_NETWORK=studybytes_backend_net
COURSE_DB_NETWORK=course_db_net
```

If `course_db_net` was created manually before, remove it once so Compose can recreate it with the correct internal settings:

```powershell
docker network rm course_db_net
```

## Exposure Rules

Production rules:

- PostgreSQL must not be exposed publicly.
- CourseService host exposure is temporary and must be bound to `127.0.0.1` for VPS Nginx.
- Public traffic should enter through reverse proxy, Site and BFF.
- Internal CourseService endpoints should be reachable only from backend services.

For local development and temporary VPS Nginx proxying, `docker-compose.yml` maps CourseService to the host loopback interface:

```text
127.0.0.1:8082 -> course-service:8082
```

Temporary public Swagger URL through Nginx:

```text
https://dev-api.studybytes.ru/course-service/swagger-ui.html
```

Direct external access to `http://<vps-ip>:8082` should not work. Remove this CourseService-specific mapping or keep it localhost-only when BFF/reverse proxy becomes the platform entry point.

## Environment Files

Only `.env.example` belongs in Git.

Local/VPS runtime values live in:

```text
.env
```

Recommended VPS path:

```text
/opt/studybytes/course-service/.env
```

Do not commit:

- `.env`;
- `.env.*`;
- `secrets/`;
- `*.pem`;
- `*.key`;
- real database passwords;
- real internal API keys.

## JWT Secrets

CourseService must not store UserService private keys or JWT signing secrets.

Correct model:

```text
UserService signs JWT with private key.
CourseService verifies JWT through UserService JWKS/public keys.
```

CourseService configuration:

```properties
USER_SERVICE_JWT_ISSUER_URI=http://user-service:8081
USER_SERVICE_JWT_JWK_SET_URI=http://user-service:8081/api/v1/auth/.well-known/jwks.json
USER_SERVICE_JWT_AUDIENCE=study-platform
```

Do not configure `COURSE_SERVICE_JWT_SECRET`.

## Internal API Key

`X-Internal-Api-Key` is for backend-to-backend calls, not user authentication.

Used by:

- LearningService -> CourseService;
- BFF -> CourseService internal API only after LearningService access checks.

The same value must be configured in CourseService and trusted callers:

```properties
COURSE_SERVICE_INTERNAL_API_KEY=change-me-internal-api-key
```
