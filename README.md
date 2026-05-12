# CourseService

CourseService is the domain service responsible for storing and exposing course structure and author-created course content for the interactive learning platform.

It owns the course model itself: courses, modules, course items, content blocks, hints, quiz options and tests. It does **not** own user enrollment, user progress, attempts, submissions or task completion state. Those belong to `LearningService`.

## Current status

Implemented:

- Spring Boot service skeleton
- PostgreSQL + Spring Data JPA persistence
- Course domain model
- Public read API
- Swagger/OpenAPI runtime documentation
- Local OpenAPI contract file in the repository
- Development demo course seed for manual QA

Not implemented yet:

- Admin API for course authors
- Internal execution package API for `LearningService`
- Final JWT/role-based security
- Full integration tests

## Technology stack

- Java 21
- Spring Boot 4.0.6
- Maven
- Spring Web MVC
- Spring Data JPA
- Spring Security
- PostgreSQL
- Lombok
- springdoc-openapi

## Service responsibility

CourseService answers the question:

> What is this course and what is it made of?

CourseService stores:

- course metadata;
- publication/access settings;
- modules;
- course items;
- theory blocks;
- video/image/embed/file/code content blocks;
- coding/SQL/theory/quiz/file item types;
- hints;
- open and hidden tests;
- quiz options;
- execution limits and evaluation settings for executable items.

CourseService does **not** store:

- enrolled users;
- course participants;
- user progress;
- task statuses for a user;
- attempts history;
- submitted user code;
- best result by user;
- course completion state;
- comments/discussions.

Those responsibilities belong to:

| Area | Owner service |
|---|---|
| Users, roles, JWT, profiles | `UserService` |
| Course structure and content | `CourseService` |
| Enrollment, attempts, progress | `LearningService` |
| Code execution | `CodeExecutorService` |
| Comments and discussions | `DiscussionService` |
| Frontend aggregation | `BFF` |

## Domain model

Current CourseService content model:

```text
Course
 └── CourseModule
      └── CourseItem
           ├── CourseItemContentBlock
           ├── CourseItemHint
           ├── CourseItemTestCase
           └── CourseItemOption
```

### Course

Represents the course as a top-level object.

Important fields:

- `id`
- `slug`
- `title`
- `shortDescription`
- `description`
- `difficulty`
- `status`
- `accessType`
- `enrollmentEnabled`
- `coverImageUrl`
- `estimatedMinutes`
- `createdByUserId`
- `createdAt`
- `updatedAt`
- `publishedAt`

Course status:

```text
DRAFT
PUBLISHED
ARCHIVED
```

Course access type:

```text
PUBLIC
UNLISTED
PRIVATE
```

Public read API exposes only courses that are:

```text
status = PUBLISHED
accessType = PUBLIC or UNLISTED
```

The catalog endpoint returns only:

```text
status = PUBLISHED
accessType = PUBLIC
```

### CourseModule

Represents an ordered course section.

Important fields:

- `id`
- `course`
- `title`
- `description`
- `orderIndex`
- `createdAt`
- `updatedAt`

### CourseItem

Represents an ordered item inside a module.

A course item can be a theory lesson, coding task, SQL task, quiz, file assignment or another supported learning object.

Supported item types:

```text
CODING
SQL
QUIZ
THEORY
FILE
```

Important fields:

- `id`
- `module`
- `title`
- `itemType`
- `statement`
- `starterCode`
- `language`
- `orderIndex`
- `timeLimitMs`
- `memoryLimitMb`
- `outputLimitKb`
- `networkDisabled`
- `readOnlyFs`
- `comparisonMode`
- `normalizeLineEndings`
- `trimTrailingWhitespaces`
- `createdAt`
- `updatedAt`

For executable items, CourseService stores the task configuration and tests, but it does not execute code and does not compare actual output with expected output.

### CourseItemContentBlock

Represents rich content inside a course item.

Supported block types:

```text
TEXT
VIDEO
IMAGE
CODE
EMBED
FILE
```

Important fields:

- `id`
- `item`
- `blockType`
- `orderIndex`
- `title`
- `textContent`
- `url`
- `language`
- `metadataJson`

Examples:

```text
TEXT  -> textContent
VIDEO -> url + metadataJson
IMAGE -> url + metadataJson
CODE  -> textContent + language
EMBED -> url or textContent
FILE  -> url + metadataJson
```

### CourseItemTestCase

Represents a test case for executable course items.

Important fields:

- `id`
- `item`
- `testKey`
- `orderIndex`
- `visibility`
- `inputData`
- `expectedOutput`

Visibility:

```text
OPEN
HIDDEN
```

Public API exposes only `OPEN` tests and does not expose `expectedOutput`.

### CourseItemHint

Represents an ordered hint for a course item.

Important fields:

- `id`
- `item`
- `orderIndex`
- `text`

### CourseItemOption

Represents an answer option for quiz items.

Important fields:

- `id`
- `item`
- `orderIndex`
- `label`
- `text`
- `correct`
- `explanation`

Public API exposes only:

- `id`
- `orderIndex`
- `label`
- `text`

Public API does **not** expose:

- `correct`
- `explanation`

## Public API

Base URL for local development:

```text
http://localhost:8082
```

### Get published public course catalog

```http
GET /api/v1/courses
```

Returns only courses that are:

```text
status = PUBLISHED
accessType = PUBLIC
```

Response:

```json
{
  "courses": [
    {
      "id": 1,
      "slug": "python-basics",
      "title": "Python Basics",
      "shortDescription": "Learn Python from scratch",
      "difficulty": "BEGINNER",
      "status": "PUBLISHED",
      "accessType": "PUBLIC",
      "enrollmentEnabled": true,
      "coverImageUrl": "https://example.com/cover.png",
      "estimatedMinutes": 120
    }
  ]
}
```

### Get published course details

```http
GET /api/v1/courses/{courseId}
```

Returns course details, modules and item summaries.

Readable courses:

```text
status = PUBLISHED
accessType = PUBLIC or UNLISTED
```

Not readable courses return `404`.

### Get published course item details

```http
GET /api/v1/course-items/{itemId}
```

Returns item details:

- statement;
- starter code;
- limits;
- execution policy;
- evaluation policy;
- content blocks;
- open tests;
- hints;
- public quiz options.

This endpoint does **not** expose:

- hidden tests;
- expected output;
- correct quiz answers;
- quiz explanations.

## Error response

Example:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Course not found",
  "path": "/api/v1/courses/999",
  "timestamp": "2026-05-11T12:00:00Z"
}
```

## OpenAPI / Swagger

Runtime documentation:

```text
Swagger UI:       http://localhost:8082/swagger-ui.html
OpenAPI JSON:     http://localhost:8082/v3/api-docs
OpenAPI YAML:     http://localhost:8082/v3/api-docs.yaml
```

Local contract file committed to the repository:

```text
docs/openapi/course-service-openapi.yaml
```

Other services should use the local OpenAPI file when CourseService is not running.

Regenerate the local OpenAPI YAML:

```bash
./mvnw verify -Popenapi
```

Windows PowerShell:

```powershell
.\mvnw.cmd verify -Popenapi
```

The generated file should be written to:

```text
docs/openapi/course-service-openapi.yaml
```

## Local setup

### Requirements

- Java 21
- Docker
- Maven wrapper from this repository
- PostgreSQL, usually via Docker

### Start PostgreSQL with Docker

PowerShell:

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

If port `5432` is already busy, run PostgreSQL on `5433` and update `spring.datasource.url`:

```powershell
docker run --name course-service-postgres `
  -e POSTGRES_DB=course_service `
  -e POSTGRES_USER=postgres `
  -e POSTGRES_PASSWORD=postgres `
  -p 5433:5432 `
  -d postgres:16
```

Then change:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5433/course_service
```

### Application properties

Default local configuration:

```properties
spring.application.name=CourseService

server.port=8082

spring.datasource.url=jdbc:postgresql://localhost:5432/course_service
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.open-in-view=false
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

app.internal-api-key=temp
app.jwt.secret=temp
app.demo-data.enabled=false
```

`ddl-auto=update` is acceptable for local MVP development. It should be replaced by real database migrations before production usage.

## Build and run

Run tests:

```bash
./mvnw clean test
```

Windows PowerShell:

```powershell
.\mvnw.cmd clean test
```

Start the service:

```bash
./mvnw spring-boot:run
```

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

Health checks:

```http
GET /health
GET /ready
```

Expected response:

```json
{
  "status": "UP"
}
```

## Demo data for manual QA

CourseService has a development-only seed that creates a demo published course with:

- two modules;
- a theory item with text, video, image and code content blocks;
- a coding item with starter code, hints, open tests and a hidden test;
- a quiz item with answer options.

The seed is guarded by the `dev` Spring profile and by the `app.demo-data.enabled` property. It is idempotent: if a course with slug `python-basics-demo` already exists, it does not create duplicates.

Start the service with demo data enabled:

```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

The dev profile uses:

```properties
app.demo-data.enabled=true
```

Default profile keeps demo data disabled:

```properties
app.demo-data.enabled=false
```

Manual QA flow after starting with `dev` profile:

```http
GET http://localhost:8082/api/v1/courses
GET http://localhost:8082/api/v1/courses/1
GET http://localhost:8082/api/v1/course-items/1
GET http://localhost:8082/swagger-ui.html
```

Expected public API behavior:

- catalog returns the demo course;
- course details return modules and item summaries;
- item details return content blocks, hints, open tests and public quiz options;
- hidden tests are not returned;
- `expectedOutput` is not returned;
- quiz option `correct` and `explanation` are not returned.

If old local data conflicts with the seed during MVP development, reset the local PostgreSQL container:

```powershell
docker rm -f course-service-postgres

docker run --name course-service-postgres `
  -e POSTGRES_DB=course_service `
  -e POSTGRES_USER=postgres `
  -e POSTGRES_PASSWORD=postgres `
  -p 5432:5432 `
  -d postgres:16
```

## Database checks

Enter PostgreSQL container:

```powershell
docker exec -it course-service-postgres psql -U postgres -d course_service
```

List tables:

```sql
\dt
```

Describe a table:

```sql
\d courses
```

One-command check:

```powershell
docker exec -it course-service-postgres psql -U postgres -d course_service -c "\dt"
```

Expected core tables:

```text
courses
course_modules
course_items
course_item_content_blocks
course_item_hints
course_item_test_cases
course_item_options
```

## Integration guide

### For Site

Site should normally call BFF, not CourseService directly.

Public CourseService DTOs are still useful for UI shape:

- course catalog page;
- course details page;
- module tree;
- course item page;
- theory/video/image/code content rendering;
- open tests and hints display;
- quiz options display.

Site must not expect public CourseService API to return hidden tests, expected output or correct answers.

### For BFF

BFF should use CourseService public endpoints to build UI-friendly models:

```http
GET /api/v1/courses
GET /api/v1/courses/{courseId}
GET /api/v1/course-items/{itemId}
```

For a course page, BFF should combine:

```text
CourseService -> course structure and content
LearningService -> user enrollment/progress/statuses
UserService -> authenticated user profile/roles
```

CourseService should not be asked for user progress.

### For LearningService

LearningService owns:

- enrollment;
- attempts;
- task status;
- progress;
- best result;
- last activity;
- course completion state.

LearningService should reference CourseService entities by ids:

```text
courseId
moduleId
itemId
```

The future internal CourseService endpoint should return execution package data for executable items:

```http
GET /api/v1/internal/course-items/{itemId}/execution-package
```

That endpoint is not implemented yet.

It should eventually return:

- item id;
- item type;
- language;
- starter code;
- execution limits;
- execution policy;
- evaluation policy;
- all tests, including hidden tests;
- expected output.

LearningService will compare actual execution result with expected output. CodeExecutorService must not perform business evaluation.

### For CodeExecutorService

CodeExecutorService receives code, language, tests and limits from LearningService.

It returns technical execution results only:

- stdout;
- stderr;
- exit code;
- runtime error;
- timeout;
- memory usage;
- duration.

It does not know about course content, task correctness or user progress.

## Security model

Current security is development-only.

At the moment public endpoints and Swagger endpoints are permitted.

Future target:

```text
/public endpoints   -> public or through BFF
/admin endpoints    -> TEACHER / ADMIN
/internal endpoints -> service-level internal API key or stronger service auth
/swagger endpoints  -> allowed in dev, restricted in production if needed
```

Important future rule:

```text
Hidden tests and expected outputs must never be exposed through public endpoints.
```

## Development order

Recommended next tasks:

1. Create admin CourseService DTOs.
2. Implement admin course management API.
3. Implement internal execution package API for LearningService.
4. Implement CourseService security checks.
5. Add integration tests for public/admin/internal APIs.
6. Replace `ddl-auto=update` with database migrations.

## Current limitations

- No admin create/edit API yet.
- No internal execution package API yet.
- No final JWT role validation yet.
- No migration tool yet.
- No production-ready storage/media handling yet.
- `metadataJson` is stored as text for MVP simplicity.

## Repository structure

```text
src/main/java/org/studyplatform/courseservice
├── config
│   └── DevDemoDataSeeder.java
├── controller
├── dto
│   └── publicapi
├── entity
│   └── enums
├── exception
├── mapper
├── repository
├── security
└── service

docs/openapi
└── course-service-openapi.yaml
```

## PR verification checklist

Before opening a PR:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

Check manually:

```text
http://localhost:8082/health
http://localhost:8082/ready
http://localhost:8082/swagger-ui.html
http://localhost:8082/v3/api-docs.yaml
http://localhost:8082/api/v1/courses

# optional demo data run
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

If OpenAPI changed, regenerate and commit:

```powershell
.\mvnw.cmd verify -Popenapi
```
