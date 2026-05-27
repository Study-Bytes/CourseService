# Endpoints Quick Reference

CourseService owns course structure and author-created content. LearningService owns enrollment, attempts, progress and task state.

## Base URLs

| Context | Base URL | Notes |
| --- | --- | --- |
| Public development gateway | `https://dev-api.studybytes.ru/course-service` | Browser, Swagger and external manual checks |
| Docker backend network | `http://course-service:8082` | BFF/LearningService service-to-service calls |
| Local direct access | `http://localhost:8082` | Local development only |

| API area | Main endpoints | Used by | Auth | Purpose | Frontend exposure |
| --- | --- | --- | --- | --- | --- |
| Public catalog | `GET /api/v1/courses`<br>`GET /api/v1/courses/{courseId}` | Site/BFF | Public | Course catalog and course preview data | Safe for frontend |
| Public item preview | `GET /api/v1/course-items/{itemId}` | Site/BFF | Public | Course item summary/preview | Safe for frontend |
| Admin course editor | `GET /api/v1/admin/courses`<br>`POST /api/v1/admin/courses`<br>`PUT /api/v1/admin/courses/{courseId}`<br>`POST /api/v1/admin/courses/{courseId}/submit-review`<br>`POST /api/v1/admin/courses/{courseId}/publish`<br>`POST /api/v1/admin/courses/{courseId}/archive` | BFF creator panel | Bearer JWT, `TEACHER` or `ADMIN` | Create, update, submit, publish and archive courses | Never called directly by Site |
| Admin moderation | `GET /api/v1/admin/courses/moderation`<br>`GET /api/v1/admin/courses/{courseId}/review`<br>`POST /api/v1/admin/courses/{courseId}/approve`<br>`POST /api/v1/admin/courses/{courseId}/reject` | BFF admin moderation panel | Bearer JWT, `ADMIN` only | Review, approve and reject submitted courses | Never called directly by Site |
| Admin structure editor | `POST /api/v1/admin/courses/{courseId}/modules`<br>`PUT /api/v1/admin/modules/{moduleId}`<br>`DELETE /api/v1/admin/modules/{moduleId}`<br>`POST /api/v1/admin/modules/{moduleId}/items`<br>`PUT /api/v1/admin/course-items/{itemId}`<br>`DELETE /api/v1/admin/course-items/{itemId}` | BFF creator panel | Bearer JWT, `TEACHER` or `ADMIN` | Edit modules and course items | Never called directly by Site |
| Admin item children | `PUT /api/v1/admin/course-items/{itemId}/content-blocks`<br>`PUT /api/v1/admin/course-items/{itemId}/hints`<br>`PUT /api/v1/admin/course-items/{itemId}/test-cases`<br>`PUT /api/v1/admin/course-items/{itemId}/options` | BFF creator panel | Bearer JWT, `TEACHER` or `ADMIN` | Replace editor-owned item content, hints, tests and options | Never called directly by Site |
| Internal availability | `GET /api/v1/internal/courses/{courseId}/availability` | LearningService | `X-Internal-Api-Key` | Check publication/access/enrollment settings before enrollment | Do not expose directly |
| Internal content | `GET /api/v1/internal/course-items/{itemId}/content` | BFF/LearningService after enrollment check | `X-Internal-Api-Key` | Return student-safe full item content after LearningService decides access | May be transformed by BFF for enrolled student views |
| Execution package | `GET /api/v1/internal/course-items/{itemId}/execution-package` | LearningService only | `X-Internal-Api-Key` | Return execution limits, policies, hidden tests and expected outputs | Never expose to frontend |

## Access Boundaries

- `Site` should call `BFF`, not CourseService directly in production.
- `BFF` may call public CourseService endpoints for catalog and preview screens.
- `BFF` should use LearningService state when rendering enrolled course pages.
- `LearningService` decides enrollment, attempt rules and progress.
- `LearningService` calls CourseService internal endpoints only after its own business checks.
- `execution-package` is only for backend evaluation flow and must not be returned to Site.

## Sensitive Data Rules

Public endpoints must not expose:

- hidden tests;
- expected output;
- correct quiz answers;
- quiz explanations;
- execution packages.

Internal content may expose student-safe full content, but not hidden tests or expected outputs.

Execution package may expose hidden tests and expected outputs only to LearningService.


## Admin editor additions

```http
GET /api/v1/admin/courses?page=0&size=20&status=DRAFT&difficulty=BEGINNER&accessType=PUBLIC&createdByUserId=123
PUT /api/v1/admin/courses/{courseId}/modules/reorder
PUT /api/v1/admin/modules/{moduleId}/items/reorder
```

Module reorder body:

```json
{
  "orderedModuleIds": [3, 1, 2]
}
```

Item reorder body:

```json
{
  "orderedItemIds": [8, 5, 6, 7]
}
```

Reorder requests must include every current child ID exactly once. Duplicate, missing and foreign IDs return `400 Bad Request`.

CourseService keeps `language` as a string and does not own the execution language allowlist. It only checks that `CODING` and `SQL` items have non-blank `language`; LearningService, CodeExecutorService and frontend decide which language values are executable.

## Module deadlines

CourseService stores optional module deadline settings and returns them in admin and public course structure responses.

```json
{
  "id": 10,
  "title": "SQL basics",
  "orderIndex": 1,
  "deadlineType": "ABSOLUTE",
  "deadlineAt": "2026-06-01T23:59:00",
  "timeLimitMinutes": null
}
```

For timer-from-start modules, CourseService returns:

```json
{
  "deadlineType": "RELATIVE_FROM_START",
  "deadlineAt": null,
  "timeLimitMinutes": 180
}
```

BFF/Site should call LearningService deadline-state with an effective `deadlineAt` when one is available:

```http
GET /api/v1/learn/courses/{courseId}/modules/{moduleId}/deadline-state?deadlineAt={deadlineAt}
```

## Course moderation

CourseService owns moderation status and stores review metadata on the course.

```http
POST /api/v1/admin/courses/{courseId}/submit-review
GET  /api/v1/admin/courses/moderation
GET  /api/v1/admin/courses/{courseId}/review
POST /api/v1/admin/courses/{courseId}/approve
POST /api/v1/admin/courses/{courseId}/reject
```

Supported statuses:

```text
DRAFT
PENDING_REVIEW
CHANGES_REQUESTED
PUBLISHED
ARCHIVED
```

BFF can keep frontend-facing teacher URLs such as `/api/v1/teacher/courses/{courseId}/submit-review`, but it should map them to the CourseService admin URL space. CourseService does not expose `/api/v1/teacher/**`.
