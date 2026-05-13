# CourseService

CourseService stores course structure and author-created course content for the interactive learning platform.

It owns courses, modules, course items, rich content blocks, hints, test cases and quiz options. It does not store user enrollment, course participants, progress, attempts or task completion state. Those belong to LearningService.

## Tech stack

- Java 21
- Spring Boot 4.0.6
- Maven
- PostgreSQL
- Spring Data JPA
- Spring Security
- springdoc-openapi

## Responsibility

CourseService answers:

> What is this course and what is it made of?

It stores:

- course metadata;
- course publication/access settings;
- modules;
- course items;
- theory/video/image/code/embed/file content blocks;
- coding and SQL task configuration;
- open and hidden test cases;
- hints;
- quiz options.

It does not store:

- course enrollment;
- user progress;
- task completion status;
- code execution attempts;
- best user results;
- discussion comments.

## Domain model

```text
Course
 └── CourseModule
      └── CourseItem
           ├── CourseItemContentBlock
           ├── CourseItemHint
           ├── CourseItemTestCase
           └── CourseItemOption
```

`CourseItem` is a generic course element. It can represent:

- `THEORY`
- `CODING`
- `SQL`
- `QUIZ`
- `FILE`

## Public API

Public read endpoints expose only safe public data.

```http
GET /api/v1/courses
GET /api/v1/courses/{courseId}
GET /api/v1/course-items/{itemId}
```

Public responses must not expose:

- hidden tests;
- expected output;
- correct quiz answers.

## Admin API

Admin endpoints are used by course authors, teachers or BFF creator flows.

Admin endpoints require Bearer JWT authentication. The token must contain the `TEACHER` or `ADMIN` role in the configured roles claim.

```http
Authorization: Bearer <jwt>
```

The current development configuration expects roles in the `roles` claim, for example:

```json
{
  "sub": "teacher-1",
  "roles": ["TEACHER"]
}
```

### Course management

```http
POST /api/v1/admin/courses
GET  /api/v1/admin/courses/{courseId}
PUT  /api/v1/admin/courses/{courseId}
POST /api/v1/admin/courses/{courseId}/publish
POST /api/v1/admin/courses/{courseId}/archive
```

### Module management

```http
POST   /api/v1/admin/courses/{courseId}/modules
PUT    /api/v1/admin/modules/{moduleId}
DELETE /api/v1/admin/modules/{moduleId}
```

### Course item management

```http
POST   /api/v1/admin/modules/{moduleId}/items
GET    /api/v1/admin/course-items/{itemId}
PUT    /api/v1/admin/course-items/{itemId}
DELETE /api/v1/admin/course-items/{itemId}
```

### Child content replacement

```http
PUT /api/v1/admin/course-items/{itemId}/content-blocks
PUT /api/v1/admin/course-items/{itemId}/hints
PUT /api/v1/admin/course-items/{itemId}/test-cases
PUT /api/v1/admin/course-items/{itemId}/options
```

These endpoints replace the whole child collection for a course item. This is simpler for an MVP course editor: the frontend/BFF sends the current list as the source of truth.



## Internal API

Internal endpoints are intended for trusted backend services, primarily `LearningService`.
They expose data that must never be returned through public student-facing endpoints.

Current MVP protection uses a static header:

```http
X-Internal-Api-Key: <internal-api-key>
```

Configured in `application.properties`:

```properties
app.internal-api-key=dev-course-service-internal-key
```

### Execution package

```http
GET /api/v1/internal/course-items/{itemId}/execution-package
```

This endpoint returns the full execution package for a course item:

- course/item identifiers;
- item type and language;
- starter code;
- execution limits;
- execution policy;
- evaluation policy;
- all tests, including hidden tests;
- expected outputs.

This endpoint is for `LearningService` only. It is used when a student runs or submits a solution.
`CodeExecutorService` still does not compare answers; it only executes code and returns technical output.
`LearningService` should compare executor output with the expected outputs from this package.

### Course availability

```http
GET /api/v1/internal/courses/{courseId}/availability
```

This endpoint returns publication/access/enrollment state for a course. `LearningService` can use it before creating enrollment records.

Public endpoints must continue to hide:

- hidden tests;
- expected output;
- correct quiz answers;
- quiz explanations.


## Security model

CourseService separates public, admin and internal access:

```text
/health, /ready                  -> public
/swagger-ui.html, /swagger-ui/** -> public in the current dev configuration
/v3/api-docs, /v3/api-docs.yaml  -> public in the current dev configuration
/api/v1/courses/**               -> public read API
/api/v1/course-items/**          -> public read API
/api/v1/admin/**                 -> Bearer JWT with TEACHER or ADMIN role
/api/v1/internal/**              -> X-Internal-Api-Key
```

JWT verification configuration:

```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=${USER_SERVICE_JWT_ISSUER_URI:http://user-service:8081}
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=${USER_SERVICE_JWT_JWK_SET_URI:http://user-service:8081/api/v1/auth/.well-known/jwks.json}
spring.security.oauth2.resourceserver.jwt.audiences=${USER_SERVICE_JWT_AUDIENCE:study-platform}
app.security.jwt.roles-claim=${COURSE_SERVICE_JWT_ROLES_CLAIM:roles}
app.security.jwt.role-prefix=${COURSE_SERVICE_JWT_ROLE_PREFIX:ROLE_}
```

UserService owns the private signing key. CourseService verifies access tokens through UserService JWKS/public keys and must not store the private key or a shared JWT secret.

Internal API configuration:

```properties
app.internal-api-key=${COURSE_SERVICE_INTERNAL_API_KEY:dev-course-service-internal-key}
```

Public endpoints must never expose hidden tests, expected outputs or correct quiz answers. Internal endpoints may expose hidden tests and expected outputs only to trusted backend services.

## Swagger/OpenAPI

Runtime docs:

```text
http://localhost:8082/swagger-ui.html
http://localhost:8082/v3/api-docs
http://localhost:8082/v3/api-docs.yaml
```

Local version-controlled contract:

```text
docs/openapi/course-service-openapi.yaml
```

Regenerate local OpenAPI after changing controllers, DTOs or response models:

```powershell
.\mvnw.cmd verify -Popenapi
```

Then commit the updated file:

```powershell
git add docs/openapi/course-service-openapi.yaml
```

## Local PostgreSQL

Start PostgreSQL:

```powershell
docker run --name course-service-postgres `
  -e POSTGRES_DB=course_service `
  -e POSTGRES_USER=postgres `
  -e POSTGRES_PASSWORD=postgres `
  -p 5432:5432 `
  -d postgres:16
```

If the container already exists:

```powershell
docker start course-service-postgres
```

Reset local development DB:

```powershell
docker rm -f course-service-postgres

docker run --name course-service-postgres `
  -e POSTGRES_DB=course_service `
  -e POSTGRES_USER=postgres `
  -e POSTGRES_PASSWORD=postgres `
  -p 5432:5432 `
  -d postgres:16
```

Reset is useful after entity field renames because `spring.jpa.hibernate.ddl-auto=update` may keep old columns.

## Deployment

CourseService deployment runs the application container with a dedicated PostgreSQL container. Inside Docker Compose, CourseService connects to PostgreSQL by service name, not `localhost`:

```properties
COURSE_SERVICE_DB_URL=jdbc:postgresql://course-postgres:5432/course_service
```

This compose file uses two Docker networks:

- `COURSE_BACKEND_NETWORK` is the shared external backend network for BFF, UserService, CourseService and LearningService.
- `COURSE_DB_NETWORK` is the private CourseService database network. Do not create it manually; Docker Compose creates it as an internal network for `course-service` and `course-postgres`.

Create only the shared backend network once before startup:

```powershell
docker network create studybytes_backend_net
```

If `course_db_net` was created manually before, remove it once before the next startup so Compose can recreate it with the right internal settings:

```powershell
docker network rm course_db_net
```

Local compose startup:

```powershell
Copy-Item .env.example .env
docker compose up --build
```

Until BFF/reverse proxy is ready, `docker-compose.yml` publishes CourseService only on the VPS loopback interface for Nginx:

```text
127.0.0.1:8082 -> course-service:8082
```

This means Nginx on the VPS can proxy to `http://127.0.0.1:8082`, but `http://<vps-ip>:8082` should not be reachable from outside. Public team access should go through Nginx, for example:

```text
https://dev-api.studybytes.ru/course-service/swagger-ui.html
```

This is temporary. In the full platform deployment, public traffic should enter through BFF/reverse proxy and this CourseService-specific exposure should be removed or kept localhost-only.

The production profile enables forwarded header support so Spring and Springdoc can build correct URLs behind Nginx path prefixes:

```properties
server.forward-headers-strategy=framework
```

Nginx should pass `X-Forwarded-Proto`, `X-Forwarded-Host` and `X-Forwarded-Prefix` when serving CourseService under `/course-service`.

Health checks:

```powershell
irm http://localhost:8082/health
irm http://localhost:8082/ready
irm http://localhost:8082/api/v1/courses | ConvertTo-Json -Depth 20
```

The repository keeps only `.env.example`. Real `.env` files, private keys and secrets must stay outside Git.

Detailed deployment and integration guides:

- [Endpoints quick reference](docs/integration/ENDPOINTS_QUICK_REFERENCE.md)
- [BFF integration](docs/integration/BFF_USAGE.md)
- [LearningService integration](docs/integration/LEARNING_SERVICE_USAGE.md)
- [Networks and secrets](docs/integration/NETWORK_AND_SECRETS.md)
- [VPS deployment](docs/deployment/VPS_DEPLOYMENT.md)
- [CI/CD pipeline](docs/deployment/CI_CD.md)

## Run

Default profile:

```powershell
.\mvnw.cmd spring-boot:run
```

Dev profile:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

## Test

```powershell
.\mvnw.cmd clean test
```

Run OpenAPI generation:

```powershell
.\mvnw.cmd verify -Popenapi
```

The Maven `openapi` profile starts the app with Spring profiles `dev,openapi`. The `openapi` Spring profile disables external JWT/JWKS lookup during contract generation, so this command does not require UserService to be running.

## Manual API checks

```powershell
irm http://localhost:8082/api/v1/courses
irm http://localhost:8082/api/v1/courses/1
irm http://localhost:8082/api/v1/course-items/1
```

Check admin API:

```powershell
irm http://localhost:8082/api/v1/admin/courses/1
irm http://localhost:8082/api/v1/admin/course-items/1
```

PowerShell does not support raw `GET http://...` syntax. Use `irm` or Postman.


Check internal API:

```powershell
irm http://localhost:8082/api/v1/internal/course-items/2/execution-package -Headers @{"X-Internal-Api-Key"="dev-course-service-internal-key"} | ConvertTo-Json -Depth 20
irm http://localhost:8082/api/v1/internal/courses/1/availability -Headers @{"X-Internal-Api-Key"="dev-course-service-internal-key"} | ConvertTo-Json -Depth 20
```

## Integration notes

Site should normally call BFF, not CourseService directly. CourseService public DTOs are still useful as contract references for course catalog, course page and item page UI models.

Detailed integration guides:

- [Endpoints quick reference](docs/integration/ENDPOINTS_QUICK_REFERENCE.md)
- [BFF integration](docs/integration/BFF_USAGE.md)
- [LearningService integration](docs/integration/LEARNING_SERVICE_USAGE.md)
- [Networks and secrets](docs/integration/NETWORK_AND_SECRETS.md)

## Next planned tasks

- Add Flyway migrations and switch production `ddl-auto` to `validate`.
- Add contract tests for BFF and LearningService integration.


## JWT verification model

CourseService does not issue JWT tokens and must not store the UserService private key.

Target platform authentication flow:

```text
UserService signs access JWT with RS256 private key.
CourseService verifies access JWT using UserService public JWKS endpoint.
```

CourseService expects UserService JWT tokens to contain:

```json
{
  "iss": "study-platform-user-service",
  "sub": "123",
  "aud": ["study-platform"],
  "roles": ["TEACHER"]
}
```

CourseService configuration:

```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=${USER_SERVICE_JWT_ISSUER_URI:http://user-service:8081}
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=${USER_SERVICE_JWT_JWK_SET_URI:http://user-service:8081/api/v1/auth/.well-known/jwks.json}
spring.security.oauth2.resourceserver.jwt.audiences=${USER_SERVICE_JWT_AUDIENCE:study-platform}
app.security.jwt.roles-claim=${COURSE_SERVICE_JWT_ROLES_CLAIM:roles}
app.security.jwt.role-prefix=${COURSE_SERVICE_JWT_ROLE_PREFIX:ROLE_}
```

Rules:

- UserService owns the private signing key.
- CourseService uses only JWKS/public key verification.
- CourseService never receives or stores the UserService private key.
- `TEACHER` and `ADMIN` roles are read from the `roles` claim.
- For `TEACHER`, CourseService additionally checks ownership: `Course.createdByUserId == JWT.sub`.
- Internal endpoints still use `X-Internal-Api-Key` and do not rely on user JWT.

When UserService is not running locally, public endpoints and internal API key endpoints can still be checked. Admin endpoints that require real Bearer JWT need a token issued by UserService.
