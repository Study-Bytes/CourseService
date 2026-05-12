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

Public read endpoints expose only safe course preview data.

```http
GET /api/v1/courses
GET /api/v1/courses/{courseId}
GET /api/v1/course-items/{itemId}
```

`GET /api/v1/courses/{courseId}` returns course metadata, module summaries and item summaries.

`GET /api/v1/course-items/{itemId}` returns item preview only: identifiers, title, type, language and order. It does not return full lesson content.

Public responses must not expose:

- full content blocks;
- starter code;
- hints;
- tests;
- hidden tests;
- expected output;
- correct quiz answers;
- quiz explanations.

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

### Admin ownership model

Admin endpoints use role-level and object-level checks.

```text
ADMIN   -> can manage every course
TEACHER -> can manage only courses where Course.createdByUserId == JWT.sub
STUDENT -> no admin access
```

Ownership is checked for:

- course operations;
- module operations;
- course item operations;
- content block replacement;
- hint replacement;
- test case replacement;
- quiz option replacement.

A `TEACHER` cannot create a course for another user id. `createdByUserId` must match the JWT `sub` value, unless the caller has `ADMIN` role.


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

### Student-safe item content

```http
GET /api/v1/internal/course-items/{itemId}/content
```

This endpoint is intended for BFF/LearningService after enrollment checks.

It returns:

- statement;
- starter code;
- content blocks;
- open tests only;
- hints;
- quiz options without correct flags;
- execution limits;
- execution policy;
- evaluation policy.

It does not return:

- hidden tests;
- expected outputs;
- correct quiz answers;
- quiz explanations.

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
/api/v1/courses/**               -> public preview API
/api/v1/course-items/**          -> public item preview API only
/api/v1/admin/**                 -> Bearer JWT with TEACHER or ADMIN role + ownership checks
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

Public endpoints must never expose hidden tests, expected outputs, full item content or correct quiz answers.

Internal content endpoints may expose enrolled-student-safe item content to trusted backend services.

Internal execution endpoints may expose hidden tests and expected outputs only to LearningService or trusted execution flows.

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
irm http://localhost:8082/api/v1/internal/course-items/2/content -Headers @{"X-Internal-Api-Key"="dev-course-service-internal-key"} | ConvertTo-Json -Depth 20
```

Check admin API:

```powershell
irm http://localhost:8082/api/v1/admin/courses/1
irm http://localhost:8082/api/v1/admin/course-items/1
```

PowerShell does not support raw `GET http://...` syntax. Use `irm` or Postman.


Check internal API:

```powershell
irm http://localhost:8082/api/v1/internal/course-items/2/content -Headers @{"X-Internal-Api-Key"="dev-course-service-internal-key"} | ConvertTo-Json -Depth 20
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

LearningService checks enrollment before requesting full course content.

Expected enrolled-content flow:

```text
Site -> BFF -> LearningService checks enrollment -> CourseService internal content API -> BFF -> Site
```

Expected execution flow:

```text
Site -> BFF -> LearningService checks enrollment/attempt rules -> CourseService execution package API -> CodeExecutorService -> LearningService saves attempt/progress
```

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

- Prepare deployment/secrets/network guide.
- Add Docker/deployment configuration.
- Add CI/CD pipeline.
- Add Flyway database migrations.
- Expand integration and contract tests.
