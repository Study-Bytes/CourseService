# LearningService Integration

LearningService owns user-course state. CourseService owns course structure and author-created content.

## Base URL

Inside Docker:

```text
http://course-service:8082
```

## Authentication

Internal CourseService endpoints require:

```http
X-Internal-Api-Key: <COURSE_SERVICE_INTERNAL_API_KEY>
```

User JWT alone must not grant access to internal CourseService endpoints.

## Course Availability

Before creating enrollment records, LearningService should check CourseService availability:

```http
GET /api/v1/internal/courses/{courseId}/availability
X-Internal-Api-Key: <secret>
```

Response includes:

- `status`;
- `accessType`;
- `enrollmentEnabled`;
- `availableForEnrollment`.

LearningService should apply its own enrollment rules on top of this response.

## Course Item Content

After LearningService confirms that a student can access an item, it can request student-safe content:

```http
GET /api/v1/internal/course-items/{itemId}/content
X-Internal-Api-Key: <secret>
```

This endpoint may return:

- statement;
- starter code;
- content blocks;
- open tests without expected output;
- hints;
- quiz options without correct flags;
- execution limits;
- execution policy;
- evaluation policy.

It must not return:

- hidden tests;
- expected outputs;
- correct quiz answers;
- quiz explanations.

## Execution Package

When a student runs or submits a solution, LearningService can request:

```http
GET /api/v1/internal/course-items/{itemId}/execution-package
X-Internal-Api-Key: <secret>
```

This endpoint is trusted backend-only and can include:

- open tests;
- hidden tests;
- expected output;
- limits;
- execution policy;
- evaluation policy.

LearningService should send the execution input to CodeExecutorService and then compare the executor output with expected outputs from this package.

## LearningService Owns

LearningService stores:

- enrollment;
- course participants;
- progress;
- attempts;
- task status;
- best result;
- completion state;
- aggregated course progress.

## LearningService Does Not Store

LearningService should not duplicate CourseService-owned author content:

- course modules;
- course item statement;
- content blocks;
- hints;
- tests;
- expected output;
- quiz options;
- correct quiz answers.

Store CourseService identifiers instead:

- `courseId`;
- `moduleId`;
- `itemId`.

