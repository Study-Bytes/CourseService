# BFF Integration

CourseService is not a public production entry point. Site should call BFF, and BFF should call CourseService and LearningService from the backend network.

For a compact endpoint ownership table, see [Endpoints Quick Reference](ENDPOINTS_QUICK_REFERENCE.md).

## Base URL

Inside Docker:

```text
http://course-service:8082
```

For local host checks:

```text
http://localhost:8082
```

Temporary public development gateway:

```text
https://dev-api.studybytes.ru/course-service
```

Use the Docker backend URL for service-to-service traffic. The public gateway URL is for browser, Swagger and external manual checks.

## Public Course Data

BFF can use public CourseService endpoints without authentication for catalog and preview pages:

```http
GET /api/v1/courses
GET /api/v1/courses/{courseId}
GET /api/v1/course-items/{itemId}
```

Public item data is preview-only. It must not include full content blocks, hidden tests, expected output, correct quiz answers, or quiz explanations.

## Admin Flows

Admin endpoints require the user JWT from UserService:

```http
Authorization: Bearer <accessToken>
```

Allowed roles:

```text
TEACHER -> own courses only
ADMIN   -> all courses
STUDENT -> no admin access
```

For `TEACHER`, CourseService checks ownership with:

```text
Course.createdByUserId == JWT.sub
```

BFF should forward the original user access token to CourseService for creator/admin pages.

## Course Page Flow

Recommended course page flow:

```text
Site
  -> BFF
  -> CourseService GET /api/v1/courses/{courseId}
  -> LearningService GET user enrollment/progress
  -> BFF merges course structure + user state
  -> Site
```

BFF should build a page model from:

- CourseService course metadata, modules and item summaries;
- LearningService enrollment, item status, completion and progress;
- UserService current user/profile data when needed.

## Enrolled Content Flow

CourseService does not decide whether a student is enrolled. LearningService owns that decision.

Recommended flow:

```text
Site
  -> BFF
  -> LearningService checks enrollment
  -> CourseService internal content API
  -> BFF
  -> Site
```

BFF may call internal CourseService endpoints only after LearningService has approved access.

## Execution Package Rule

BFF must not return execution package data to the frontend.

```http
GET /api/v1/internal/course-items/{itemId}/execution-package
```

This endpoint can include hidden tests and expected output. It is for LearningService execution/evaluation flows only.
