# Endpoints Quick Reference

CourseService owns course structure and author-created content. LearningService owns enrollment, attempts, progress and task state.

| API area | Main endpoints | Used by | Auth | Purpose | Frontend exposure |
| --- | --- | --- | --- | --- | --- |
| Public catalog | `GET /api/v1/courses`<br>`GET /api/v1/courses/{courseId}` | Site/BFF | Public | Course catalog and course preview data | Safe for frontend |
| Public item preview | `GET /api/v1/course-items/{itemId}` | Site/BFF | Public | Course item summary/preview | Safe for frontend |
| Admin course editor | `POST /api/v1/admin/courses`<br>`PUT /api/v1/admin/courses/{courseId}`<br>`POST /api/v1/admin/courses/{courseId}/publish`<br>`POST /api/v1/admin/courses/{courseId}/archive` | BFF creator panel | Bearer JWT, `TEACHER` or `ADMIN` | Create, update, publish and archive courses | Never called directly by Site |
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
