# CourseService

CourseService stores course structure and author-created course content for the interactive learning platform.

It owns courses, modules, course items, rich content blocks, hints, test cases and quiz options. It does not store user enrollment, course participants, progress, attempts or task completion state. Those belong to LearningService.

## Tech stack

- Java 21
- Spring Boot 4.0.6
- Maven
- PostgreSQL
- Spring Data JPA
- Flyway
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
GET  /api/v1/admin/courses?page=0&size=20&status=DRAFT&difficulty=BEGINNER&accessType=PUBLIC&createdByUserId=123
POST /api/v1/admin/courses
GET  /api/v1/admin/courses/{courseId}
PUT  /api/v1/admin/courses/{courseId}
POST /api/v1/admin/courses/{courseId}/publish
POST /api/v1/admin/courses/{courseId}/archive
```

`GET /api/v1/admin/courses` returns a paginated admin course list. `ADMIN` can list all courses. `TEACHER` can list only courses where `createdByUserId` equals the authenticated user id. Default sorting is `createdAt DESC`.

### Module management

```http
POST   /api/v1/admin/courses/{courseId}/modules
PUT    /api/v1/admin/courses/{courseId}/modules/reorder
PUT    /api/v1/admin/modules/{moduleId}
DELETE /api/v1/admin/modules/{moduleId}
```

Module reorder request:

```json
{
  "orderedModuleIds": [3, 1, 2]
}
```

The request must contain all existing module IDs from the course exactly once. Missing, duplicate and foreign IDs are rejected with `400 Bad Request`.

### Course item management

```http
POST   /api/v1/admin/modules/{moduleId}/items
PUT    /api/v1/admin/modules/{moduleId}/items/reorder
GET    /api/v1/admin/course-items/{itemId}
PUT    /api/v1/admin/course-items/{itemId}
DELETE /api/v1/admin/course-items/{itemId}
```

Course item reorder request:

```json
{
  "orderedItemIds": [8, 5, 6, 7]
}
```

The request must contain all existing item IDs from the module exactly once. Missing, duplicate and foreign IDs are rejected with `400 Bad Request`.

### Child content replacement

```http
PUT /api/v1/admin/course-items/{itemId}/content-blocks
PUT /api/v1/admin/course-items/{itemId}/hints
PUT /api/v1/admin/course-items/{itemId}/test-cases
PUT /api/v1/admin/course-items/{itemId}/options
```

These endpoints replace the whole child collection for a course item. This is simpler for an MVP course editor: the frontend/BFF sends the current list as the source of truth.



## Course editor validation rules

CourseService validates course structure and content consistency. It does not decide which programming languages can actually be executed. The `language` field remains a string because LearningService, CodeExecutorService and frontend creation UI own execution-language support.

Validation rules:

- `orderIndex` must be non-negative for modules, items, content blocks, hints, test cases and quiz options.
- Duplicate `orderIndex` values inside the same parent collection are rejected before database unique constraints are hit.
- `CODING` and `SQL` items must have a non-blank `language`. CourseService does not restrict the value to a hardcoded language allowlist.
- `CODING` and `SQL` items may have test cases and must not have quiz options.
- `QUIZ` items may have quiz options and must not have test cases. Before publishing, a quiz must contain at least one option and at least one correct option.
- `THEORY` and `FILE` items must not have test cases or quiz options. Before publishing, they must contain either `statement` or content blocks.
- `THEORY`, `QUIZ` and `FILE` items do not automatically receive `language = "python"`.

Publishing runs full structure validation before changing course status to `PUBLISHED`. Invalid courses remain unchanged and return `400 Bad Request` with a clear message.

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

Runtime docs through the development Nginx gateway:

```text
https://dev-api.studybytes.ru/course-service/swagger-ui.html
https://dev-api.studybytes.ru/course-service/v3/api-docs
https://dev-api.studybytes.ru/course-service/v3/api-docs.yaml
```

Local direct runtime docs:

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


## Database migrations

Flyway owns the CourseService database schema. Hibernate must validate the mapped schema; it must not create or mutate production tables silently.

Runtime defaults:

```properties
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.default-schema=public
spring.jpa.hibernate.ddl-auto=validate
```

The initial schema is stored in:

```text
src/main/resources/db/migration/V1__init_course_service_schema.sql
```

On application startup Flyway creates the `flyway_schema_history` table and applies every pending migration before Hibernate validates JPA mappings.

### Local database reset

For local development with Docker Compose, reset the CourseService database volume only when losing local data is acceptable:

```powershell
docker compose down -v
docker compose up --build
```

For the standalone PostgreSQL container shown below:

```powershell
docker rm -f course-service-postgres

docker run --name course-service-postgres `
  -e POSTGRES_DB=course_service `
  -e POSTGRES_USER=postgres `
  -e POSTGRES_PASSWORD=postgres `
  -p 5432:5432 `
  -d postgres:16
```

After reset, CourseService should start against the empty database and Flyway should recreate all schema objects from migrations.

### Adding future migrations

Migration files must be immutable after they are committed and applied to any shared environment. Do not edit an existing `V*__*.sql` file to change an already-applied schema. Add a new migration instead.

Naming rules:

```text
src/main/resources/db/migration/V<next_number>__short_description.sql
```

Examples:

```text
V2__add_course_tags.sql
V3__add_item_publish_settings.sql
V4__create_course_assets_table.sql
```

Use lowercase snake_case table and column names to match the existing JPA mappings. After adding a migration, run:

```powershell
.\mvnw.cmd clean test
docker compose down -v
docker compose up --build
```

Then verify startup logs contain successful Flyway migration output and that `/health`, public API, admin API and internal API still work.

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

Reset is useful when testing migrations from a completely empty database. After reset, Flyway should recreate the schema and Hibernate should validate it.

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

Public development gateway:

```text
https://dev-api.studybytes.ru/course-service
```

Health checks:

```powershell
irm http://localhost:8082/health
irm http://localhost:8082/ready
irm http://localhost:8082/api/v1/courses | ConvertTo-Json -Depth 20

irm https://dev-api.studybytes.ru/course-service/health
irm https://dev-api.studybytes.ru/course-service/ready
irm https://dev-api.studybytes.ru/course-service/api/v1/courses | ConvertTo-Json -Depth 20
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

Run the full CourseService test suite:

```powershell
.\mvnw.cmd clean test
```

Run tests plus verification phases and generate the JaCoCo coverage report:

```powershell
.\mvnw.cmd clean verify
```

Coverage report output:

```text
target/site/jacoco/index.html
```

The current test suite covers the critical CourseService contracts:

- public course catalog/details/item preview safety;
- admin course listing, filtering and ownership rules;
- course/module/item editor flows;
- module and item reorder validation;
- `orderIndex` and duplicate-order validation;
- `CourseItemType` business validation;
- publish-time validation;
- internal API key protection;
- execution package contract for LearningService;
- JWT/JWKS validation and admin ownership checks.

Run OpenAPI generation:

```powershell
.\mvnw.cmd verify -Popenapi
```

The Maven `openapi` profile starts the app with Spring profiles `dev,openapi`. The `openapi` Spring profile disables external JWT/JWKS lookup during contract generation, so this command does not require UserService to be running.

## Manual API checks

Public development gateway:

```powershell
$courseServiceUrl = "https://dev-api.studybytes.ru/course-service"
irm "$courseServiceUrl/api/v1/courses"
irm "$courseServiceUrl/api/v1/courses/1"
irm "$courseServiceUrl/api/v1/course-items/1"
```

Local direct access:

```powershell
irm http://localhost:8082/api/v1/courses
irm http://localhost:8082/api/v1/courses/1
irm http://localhost:8082/api/v1/course-items/1
```

Check admin API:

```powershell
irm http://localhost:8082/api/v1/admin/courses/1
irm http://localhost:8082/api/v1/admin/course-items/1

irm "$courseServiceUrl/api/v1/admin/courses/1"
irm "$courseServiceUrl/api/v1/admin/course-items/1"
```

PowerShell does not support raw `GET http://...` syntax. Use `irm` or Postman.


Check internal API:

```powershell
irm http://localhost:8082/api/v1/internal/course-items/2/execution-package -Headers @{"X-Internal-Api-Key"="dev-course-service-internal-key"} | ConvertTo-Json -Depth 20
irm http://localhost:8082/api/v1/internal/courses/1/availability -Headers @{"X-Internal-Api-Key"="dev-course-service-internal-key"} | ConvertTo-Json -Depth 20

irm "$courseServiceUrl/api/v1/internal/course-items/2/execution-package" -Headers @{"X-Internal-Api-Key"="dev-course-service-internal-key"} | ConvertTo-Json -Depth 20
irm "$courseServiceUrl/api/v1/internal/courses/1/availability" -Headers @{"X-Internal-Api-Key"="dev-course-service-internal-key"} | ConvertTo-Json -Depth 20
```

## Integration notes

Site should normally call BFF, not CourseService directly. CourseService public DTOs are still useful as contract references for course catalog, course page and item page UI models.

Detailed integration guides:

- [Endpoints quick reference](docs/integration/ENDPOINTS_QUICK_REFERENCE.md)
- [BFF integration](docs/integration/BFF_USAGE.md)
- [LearningService integration](docs/integration/LEARNING_SERVICE_USAGE.md)
- [Networks and secrets](docs/integration/NETWORK_AND_SECRETS.md)

## Next planned tasks

- Start LearningService implementation and cover CourseService/LearningService contract behavior during integration.


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
