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

JWT configuration:

```properties
app.security.jwt.secret=dev-course-service-jwt-secret-key-which-is-at-least-32-bytes-long
app.security.jwt.roles-claim=roles
app.security.jwt.role-prefix=ROLE_
```

Internal API configuration:

```properties
app.internal-api-key=dev-course-service-internal-key
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

### Site

Site should normally call BFF, not CourseService directly. CourseService public DTOs are still useful as contract references for course catalog, course page and item page UI models.

### BFF

BFF should aggregate:

- CourseService course structure;
- LearningService user enrollment/progress;
- UserService current user/profile data.

### LearningService

LearningService should reference CourseService entities by ids:

- `courseId`
- `moduleId`
- `itemId`

LearningService owns:

- enrollment;
- progress;
- attempts;
- user item status;
- best result;
- aggregated course progress.

CourseService does not duplicate that state.

### CodeExecutorService

CodeExecutorService should not call CourseService directly in the normal flow. LearningService will later request execution package data from CourseService internal API and send technical execution requests to CodeExecutorService.

## Next planned tasks

- Implement CourseService security checks.
- Add broader integration tests for public/admin/internal APIs.
- Prepare deployment configuration.
