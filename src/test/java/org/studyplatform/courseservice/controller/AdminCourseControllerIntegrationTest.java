package org.studyplatform.courseservice.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.MockMvc;
import org.studyplatform.courseservice.repository.CourseItemContentBlockRepository;
import org.studyplatform.courseservice.repository.CourseItemHintRepository;
import org.studyplatform.courseservice.repository.CourseItemOptionRepository;
import org.studyplatform.courseservice.repository.CourseItemRepository;
import org.studyplatform.courseservice.repository.CourseItemTestCaseRepository;
import org.studyplatform.courseservice.repository.CourseModuleRepository;
import org.studyplatform.courseservice.repository.CourseRepository;
import org.studyplatform.courseservice.entity.Course;
import org.studyplatform.courseservice.entity.CourseItem;
import org.studyplatform.courseservice.entity.CourseModule;
import org.studyplatform.courseservice.entity.enums.CourseAccessType;
import org.studyplatform.courseservice.entity.enums.CourseDifficulty;
import org.studyplatform.courseservice.entity.enums.CourseStatus;
import org.studyplatform.courseservice.entity.enums.CourseItemType;
import org.studyplatform.courseservice.entity.enums.ComparisonMode;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "1", roles = "TEACHER")
class AdminCourseControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CourseItemContentBlockRepository contentBlockRepository;

    @Autowired
    private CourseItemHintRepository hintRepository;

    @Autowired
    private CourseItemOptionRepository optionRepository;

    @Autowired
    private CourseItemTestCaseRepository testCaseRepository;

    @Autowired
    private CourseItemRepository itemRepository;

    @Autowired
    private CourseModuleRepository moduleRepository;

    @Autowired
    private CourseRepository courseRepository;

    @BeforeEach
    void cleanDatabase() {
        contentBlockRepository.deleteAll();
        hintRepository.deleteAll();
        optionRepository.deleteAll();
        testCaseRepository.deleteAll();
        itemRepository.deleteAll();
        moduleRepository.deleteAll();
        courseRepository.deleteAll();
    }


    @Test
    @WithAnonymousUser
    void shouldRejectAdminRequestWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/admin/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(minimalCreateCourseRequest("security-no-auth")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void shouldRejectAdminRequestWithoutTeacherOrAdminRole() throws Exception {
        mockMvc.perform(post("/api/v1/admin/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(minimalCreateCourseRequest("security-student")))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "2", roles = "TEACHER")
    void shouldRejectTeacherCreatingCourseForAnotherUser() throws Exception {
        String request = """
                {
                  "slug": "foreign-owner-course-%s",
                  "title": "Foreign Owner Course",
                  "createdByUserId": 1
                }
                """.formatted(System.nanoTime());

        mockMvc.perform(post("/api/v1/admin/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "2", roles = "TEACHER")
    void shouldRejectTeacherUpdatingAnotherTeacherCourse() throws Exception {
        Course course = seedCourse(1L, "ownership-denied-" + System.nanoTime());

        String request = """
                {
                  "title": "Illegal update"
                }
                """;

        mockMvc.perform(put("/api/v1/admin/courses/{courseId}", course.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "2", roles = "ADMIN")
    void shouldAllowAdminUpdatingAnyCourse() throws Exception {
        Course course = seedCourse(1L, "admin-override-" + System.nanoTime());

        String request = """
                {
                  "title": "Admin update"
                }
                """;

        mockMvc.perform(put("/api/v1/admin/courses/{courseId}", course.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Admin update"));
    }

    @Test
    void shouldCreateAndPublishCourseWithEditableContent() throws Exception {
        Long courseId = createCourse();
        Long moduleId = createModule(courseId);
        Long itemId = createCodingItem(moduleId);

        replaceContentBlocks(itemId);
        replaceHints(itemId);
        replaceTestCases(itemId);

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/publish", courseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.publishedAt").exists());

        mockMvc.perform(get("/api/v1/admin/course-items/{itemId}", itemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contentBlocks", hasSize(1)))
                .andExpect(jsonPath("$.hints", hasSize(1)))
                .andExpect(jsonPath("$.testCases", hasSize(2)))
                .andExpect(jsonPath("$.options", hasSize(0)))
                .andExpect(jsonPath("$.testCases[1].visibility").value("HIDDEN"))
                .andExpect(jsonPath("$.testCases[1].expectedOutput").value("25\n"));
    }

    @Test
    void shouldListOnlyOwnCoursesForTeacher() throws Exception {
        seedCourse(1L, "teacher-owned-" + System.nanoTime());
        seedCourse(2L, "another-teacher-" + System.nanoTime());

        mockMvc.perform(get("/api/v1/admin/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].createdByUserId").value(1));
    }

    @Test
    @WithMockUser(username = "2", roles = "ADMIN")
    void shouldListAllCoursesForAdmin() throws Exception {
        seedCourse(1L, "admin-list-a-" + System.nanoTime());
        seedCourse(2L, "admin-list-b-" + System.nanoTime());

        mockMvc.perform(get("/api/v1/admin/courses")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void shouldSubmitCourseForReviewAndExposeModerationFields() throws Exception {
        Long courseId = createPublishableCourse("submit-review-" + System.nanoTime());

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/submit-review", courseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.submittedForReviewAt").exists())
                .andExpect(jsonPath("$.reviewedAt").value(nullValue()))
                .andExpect(jsonPath("$.reviewedByUserId").value(nullValue()))
                .andExpect(jsonPath("$.reviewComment").value(nullValue()));

        mockMvc.perform(get("/api/v1/admin/courses")
                        .param("status", "PENDING_REVIEW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(courseId))
                .andExpect(jsonPath("$.content[0].status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.content[0].submittedForReviewAt").exists())
                .andExpect(jsonPath("$.content[0].reviewedAt").value(nullValue()))
                .andExpect(jsonPath("$.content[0].reviewedByUserId").value(nullValue()))
                .andExpect(jsonPath("$.content[0].reviewComment").value(nullValue()));
    }

    @Test
    void shouldRejectTeacherSubmittingForeignCourseForReview() throws Exception {
        Course course = seedCourse(1L, "submit-review-foreign-" + System.nanoTime());

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/submit-review", course.getId())
                        .with(user("2").roles("TEACHER")))
                .andExpect(status().isForbidden());

        assertEquals(
                CourseStatus.DRAFT,
                courseRepository.findById(course.getId()).orElseThrow().getStatus()
        );
    }

    @Test
    void shouldAllowAdminSubmittingAnyCourseForReview() throws Exception {
        Course course = seedPublishableCourse(10L, "submit-review-admin-any-" + System.nanoTime());

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/submit-review", course.getId())
                        .with(user("99").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.submittedForReviewAt").exists());
    }

    @Test
    void shouldRejectSubmitReviewForInvalidCourseStructureAndKeepDraftStatus() throws Exception {
        Long courseId = createCourse("submit-review-invalid-structure-" + System.nanoTime());
        Long moduleId = createModule(courseId, "Theory", 0);
        createTheoryItem(moduleId, "Empty theory", 0, null);

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/submit-review", courseId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("must have statement or content blocks before publishing")));

        Course course = courseRepository.findById(courseId).orElseThrow();
        assertEquals(CourseStatus.DRAFT, course.getStatus());
        assertNull(course.getSubmittedForReviewAt());
    }

    @Test
    @WithMockUser(username = "99", roles = "ADMIN")
    void shouldFilterAdminCourseListByModerationStatusesAndExposeModerationFields() throws Exception {
        Instant submittedAt = Instant.parse("2026-05-17T12:00:00Z");
        Instant reviewedAt = Instant.parse("2026-05-17T12:05:00Z");
        Course pending = seedCourseWithModeration(
                10L,
                "admin-list-pending-" + System.nanoTime(),
                CourseStatus.PENDING_REVIEW,
                submittedAt,
                null,
                null,
                null
        );
        Course changesRequested = seedCourseWithModeration(
                10L,
                "admin-list-changes-" + System.nanoTime(),
                CourseStatus.CHANGES_REQUESTED,
                submittedAt,
                reviewedAt,
                99L,
                "Please improve the quiz."
        );

        mockMvc.perform(get("/api/v1/admin/courses")
                        .param("status", "PENDING_REVIEW")
                        .param("createdByUserId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(pending.getId()))
                .andExpect(jsonPath("$.content[0].submittedForReviewAt").value("2026-05-17T12:00:00Z"))
                .andExpect(jsonPath("$.content[0].reviewComment").value(nullValue()));

        mockMvc.perform(get("/api/v1/admin/courses")
                        .param("status", "CHANGES_REQUESTED")
                        .param("createdByUserId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(changesRequested.getId()))
                .andExpect(jsonPath("$.content[0].reviewedAt").value("2026-05-17T12:05:00Z"))
                .andExpect(jsonPath("$.content[0].reviewedByUserId").value(99))
                .andExpect(jsonPath("$.content[0].reviewComment").value("Please improve the quiz."));
    }

    @Test
    @WithMockUser(username = "99", roles = "ADMIN")
    void shouldListModerationQueueOldestSubmissionFirst() throws Exception {
        Course older = seedCourseWithModeration(
                10L,
                "moderation-older-" + System.nanoTime(),
                CourseStatus.PENDING_REVIEW,
                Instant.parse("2026-05-17T10:00:00Z"),
                null,
                null,
                null
        );
        Course newer = seedCourseWithModeration(
                11L,
                "moderation-newer-" + System.nanoTime(),
                CourseStatus.PENDING_REVIEW,
                Instant.parse("2026-05-17T12:00:00Z"),
                null,
                null,
                null
        );
        seedCourse(12L, "moderation-draft-" + System.nanoTime());

        mockMvc.perform(get("/api/v1/admin/courses/moderation")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(older.getId()))
                .andExpect(jsonPath("$.content[1].id").value(newer.getId()))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void shouldReturnReviewDetailsForAdminOnly() throws Exception {
        Long courseId = createPublishableCourse("review-details-" + System.nanoTime());

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/submit-review", courseId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/courses/{courseId}/review", courseId)
                        .with(user("99").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(courseId))
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.submittedForReviewAt").exists())
                .andExpect(jsonPath("$.modules", hasSize(1)))
                .andExpect(jsonPath("$.modules[0].items", hasSize(1)));

        mockMvc.perform(get("/api/v1/admin/courses/{courseId}/review", courseId))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldApproveSubmittedCourseAndSetReviewFields() throws Exception {
        Long courseId = createPublishableCourse("approve-course-" + System.nanoTime());

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/submit-review", courseId))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/approve", courseId)
                        .with(user("99").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.publishedAt").exists())
                .andExpect(jsonPath("$.submittedForReviewAt").exists())
                .andExpect(jsonPath("$.reviewedAt").exists())
                .andExpect(jsonPath("$.reviewedByUserId").value(99))
                .andExpect(jsonPath("$.reviewComment").value(nullValue()));
    }

    @Test
    void shouldRejectSubmittedCourseAndClearReviewFieldsOnResubmission() throws Exception {
        Long courseId = createPublishableCourse("reject-course-" + System.nanoTime());

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/submit-review", courseId))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/reject", courseId)
                        .with(user("99").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reviewComment": "Please add one more practice item."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHANGES_REQUESTED"))
                .andExpect(jsonPath("$.reviewedAt").exists())
                .andExpect(jsonPath("$.reviewedByUserId").value(99))
                .andExpect(jsonPath("$.reviewComment").value("Please add one more practice item."));

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/submit-review", courseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.reviewedAt").value(nullValue()))
                .andExpect(jsonPath("$.reviewedByUserId").value(nullValue()))
                .andExpect(jsonPath("$.reviewComment").value(nullValue()));
    }

    @Test
    void shouldRejectBlankModerationCommentAndKeepPendingStatus() throws Exception {
        Long courseId = createPublishableCourse("reject-blank-comment-" + System.nanoTime());

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/submit-review", courseId))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/reject", courseId)
                        .with(user("99").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reviewComment": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("reviewComment")));

        Course course = courseRepository.findById(courseId).orElseThrow();
        assertEquals(CourseStatus.PENDING_REVIEW, course.getStatus());
        assertNull(course.getReviewComment());
    }

    @Test
    void shouldRejectTeachersCallingAdminOnlyModerationEndpoints() throws Exception {
        Long courseId = createPublishableCourse("teacher-moderation-denied-" + System.nanoTime());

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/submit-review", courseId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/courses/moderation"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/approve", courseId))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/reject", courseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reviewComment": "Needs changes."
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectInvalidModerationTransitionsWithoutMutation() throws Exception {
        Course pending = seedCourseWithModeration(
                1L,
                "invalid-submit-pending-" + System.nanoTime(),
                CourseStatus.PENDING_REVIEW,
                Instant.parse("2026-05-17T10:00:00Z"),
                null,
                null,
                null
        );
        assertConflictAndUnchanged(
                post("/api/v1/admin/courses/{courseId}/submit-review", pending.getId()),
                pending.getId(),
                CourseStatus.PENDING_REVIEW
        );

        Course published = seedCourse(1L, "invalid-submit-published-" + System.nanoTime(), CourseStatus.PUBLISHED, CourseDifficulty.BEGINNER, CourseAccessType.PUBLIC);
        assertConflictAndUnchanged(
                post("/api/v1/admin/courses/{courseId}/submit-review", published.getId()),
                published.getId(),
                CourseStatus.PUBLISHED
        );

        Course archived = seedCourse(1L, "invalid-submit-archived-" + System.nanoTime(), CourseStatus.ARCHIVED, CourseDifficulty.BEGINNER, CourseAccessType.PUBLIC);
        assertConflictAndUnchanged(
                post("/api/v1/admin/courses/{courseId}/submit-review", archived.getId()),
                archived.getId(),
                CourseStatus.ARCHIVED
        );

        for (CourseStatus status : new CourseStatus[] {
                CourseStatus.DRAFT,
                CourseStatus.CHANGES_REQUESTED,
                CourseStatus.PUBLISHED,
                CourseStatus.ARCHIVED
        }) {
            Course course = seedCourse(1L, "invalid-approve-" + status + "-" + System.nanoTime(), status, CourseDifficulty.BEGINNER, CourseAccessType.PUBLIC);
            assertConflictAndUnchanged(
                    post("/api/v1/admin/courses/{courseId}/approve", course.getId())
                            .with(user("99").roles("ADMIN")),
                    course.getId(),
                    status
            );
        }

        for (CourseStatus status : new CourseStatus[] {
                CourseStatus.DRAFT,
                CourseStatus.CHANGES_REQUESTED,
                CourseStatus.PUBLISHED,
                CourseStatus.ARCHIVED
        }) {
            Course course = seedCourse(1L, "invalid-reject-" + status + "-" + System.nanoTime(), status, CourseDifficulty.BEGINNER, CourseAccessType.PUBLIC);
            assertConflictAndUnchanged(
                    post("/api/v1/admin/courses/{courseId}/reject", course.getId())
                            .with(user("99").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "reviewComment": "Needs changes."
                                    }
                                    """),
                    course.getId(),
                    status
            );
        }
    }

    @Test
    void shouldReorderModules() throws Exception {
        Long courseId = createCourse("module-reorder-" + System.nanoTime());
        Long firstModuleId = createModule(courseId, "First", 0);
        Long secondModuleId = createModule(courseId, "Second", 1);

        String request = """
                {
                  "orderedModuleIds": [%d, %d]
                }
                """.formatted(secondModuleId, firstModuleId);

        mockMvc.perform(put("/api/v1/admin/courses/{courseId}/modules/reorder", courseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modules[0].id").value(secondModuleId))
                .andExpect(jsonPath("$.modules[0].orderIndex").value(0))
                .andExpect(jsonPath("$.modules[1].id").value(firstModuleId))
                .andExpect(jsonPath("$.modules[1].orderIndex").value(1));
    }

    @Test
    void shouldRejectReorderModulesWithMissingId() throws Exception {
        Long courseId = createCourse("module-reorder-invalid-" + System.nanoTime());
        Long firstModuleId = createModule(courseId, "First", 0);
        createModule(courseId, "Second", 1);

        String request = """
                {
                  "orderedModuleIds": [%d]
                }
                """.formatted(firstModuleId);

        mockMvc.perform(put("/api/v1/admin/courses/{courseId}/modules/reorder", courseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("missing existing IDs")));
    }

    @Test
    void shouldReorderItems() throws Exception {
        Long courseId = createCourse("item-reorder-" + System.nanoTime());
        Long moduleId = createModule(courseId, "Practice", 0);
        Long firstItemId = createCodingItem(moduleId, "First", 0);
        Long secondItemId = createCodingItem(moduleId, "Second", 1);

        String request = """
                {
                  "orderedItemIds": [%d, %d]
                }
                """.formatted(secondItemId, firstItemId);

        mockMvc.perform(put("/api/v1/admin/modules/{moduleId}/items/reorder", moduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(secondItemId))
                .andExpect(jsonPath("$.items[0].orderIndex").value(0))
                .andExpect(jsonPath("$.items[1].id").value(firstItemId))
                .andExpect(jsonPath("$.items[1].orderIndex").value(1));
    }

    @Test
    void shouldRejectPublishingCourseWithoutModules() throws Exception {
        Long courseId = createCourse("publish-no-modules-" + System.nanoTime());

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/publish", courseId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Course must contain at least one module")));
    }

    @Test
    void shouldRejectPublishingQuizWithoutCorrectOption() throws Exception {
        Long courseId = createCourse("quiz-no-correct-" + System.nanoTime());
        Long moduleId = createModule(courseId, "Quiz", 0);
        Long quizItemId = createQuizItem(moduleId, "Quiz item", 0);

        String optionsRequest = """
                {
                  "options": [
                    {
                      "orderIndex": 0,
                      "label": "A",
                      "text": "Wrong answer",
                      "correct": false
                    }
                  ]
                }
                """;

        mockMvc.perform(put("/api/v1/admin/course-items/{itemId}/options", quizItemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(optionsRequest))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/publish", courseId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("must contain at least one correct option")));
    }

    @Test
    void shouldNotDefaultLanguageForTheoryItem() throws Exception {
        Long courseId = createCourse("theory-language-" + System.nanoTime());
        Long moduleId = createModule(courseId, "Theory", 0);

        String request = """
                {
                  "title": "Theory item",
                  "itemType": "THEORY",
                  "statement": "Read this text.",
                  "orderIndex": 0
                }
                """;

        mockMvc.perform(post("/api/v1/admin/modules/{moduleId}/items", moduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.language").value(nullValue()));
    }

    @Test
    void shouldRejectQuizOptionsForCodingItem() throws Exception {
        Long courseId = createCourse("coding-options-rejected-" + System.nanoTime());
        Long moduleId = createModule(courseId, "Practice", 0);
        Long itemId = createCodingItem(moduleId, "Print square", 0);

        String request = """
                {
                  "options": [
                    {
                      "orderIndex": 0,
                      "label": "A",
                      "text": "n * n",
                      "correct": true
                    }
                  ]
                }
                """;

        mockMvc.perform(put("/api/v1/admin/course-items/{itemId}/options", itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("cannot have quiz options")));
    }

    @Test
    void shouldRejectDuplicateCourseSlug() throws Exception {
        createCourse("admin-api-test");

        String duplicateRequest = """
                {
                  "slug": "admin-api-test",
                  "title": "Duplicate",
                  "createdByUserId": 1
                }
                """;

        mockMvc.perform(post("/api/v1/admin/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicateRequest))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Course slug already exists: admin-api-test"));
    }

    @Test
    void shouldReturnNotFoundForMissingCourse() throws Exception {
        mockMvc.perform(get("/api/v1/admin/courses/{courseId}", 999999L))
                .andExpect(status().isNotFound());
    }



    @Test
    @WithMockUser(username = "99", roles = "ADMIN")
    void shouldFilterAdminCourseListByStatusDifficultyAccessTypeAndCreator() throws Exception {
        Course matching = seedCourse(
                10L,
                "filter-match-" + System.nanoTime(),
                CourseStatus.PUBLISHED,
                CourseDifficulty.ADVANCED,
                CourseAccessType.UNLISTED
        );
        seedCourse(10L, "filter-wrong-status-" + System.nanoTime(), CourseStatus.DRAFT, CourseDifficulty.ADVANCED, CourseAccessType.UNLISTED);
        seedCourse(10L, "filter-wrong-difficulty-" + System.nanoTime(), CourseStatus.PUBLISHED, CourseDifficulty.BEGINNER, CourseAccessType.UNLISTED);
        seedCourse(11L, "filter-wrong-owner-" + System.nanoTime(), CourseStatus.PUBLISHED, CourseDifficulty.ADVANCED, CourseAccessType.UNLISTED);

        mockMvc.perform(get("/api/v1/admin/courses")
                        .param("status", "PUBLISHED")
                        .param("difficulty", "ADVANCED")
                        .param("accessType", "UNLISTED")
                        .param("createdByUserId", "10")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(matching.getId()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldIgnoreForeignCreatedByFilterForTeacherAndReturnOnlyOwnCourses() throws Exception {
        Course own = seedCourse(1L, "teacher-filter-own-" + System.nanoTime());
        seedCourse(2L, "teacher-filter-foreign-" + System.nanoTime());

        mockMvc.perform(get("/api/v1/admin/courses")
                        .param("createdByUserId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(own.getId()))
                .andExpect(jsonPath("$.content[0].createdByUserId").value(1));
    }

    @Test
    void shouldRejectInvalidAdminCourseListPagination() throws Exception {
        mockMvc.perform(get("/api/v1/admin/courses").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("page must be greater than or equal to 0")));

        mockMvc.perform(get("/api/v1/admin/courses").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("size must be between")));
    }

    @Test
    void shouldRejectReorderModulesWithDuplicateIdsEmptyListAndForeignIds() throws Exception {
        Long courseId = createCourse("module-reorder-extra-invalid-" + System.nanoTime());
        Long firstModuleId = createModule(courseId, "First", 0);
        Long secondModuleId = createModule(courseId, "Second", 1);
        Long foreignCourseId = createCourse("module-reorder-foreign-course-" + System.nanoTime());
        Long foreignModuleId = createModule(foreignCourseId, "Foreign", 0);

        mockMvc.perform(put("/api/v1/admin/courses/{courseId}/modules/reorder", courseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderedModuleIds": [%d, %d]}
                                """.formatted(firstModuleId, firstModuleId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("duplicate IDs")));

        mockMvc.perform(put("/api/v1/admin/courses/{courseId}/modules/reorder", courseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderedModuleIds": []}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/v1/admin/courses/{courseId}/modules/reorder", courseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderedModuleIds": [%d, %d, %d]}
                                """.formatted(firstModuleId, secondModuleId, foreignModuleId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("foreign IDs")));
    }

    @Test
    void shouldRejectReorderItemsWithDuplicateIdsMissingIdsEmptyListAndForeignIds() throws Exception {
        Long courseId = createCourse("item-reorder-extra-invalid-" + System.nanoTime());
        Long moduleId = createModule(courseId, "Practice", 0);
        Long firstItemId = createCodingItem(moduleId, "First", 0);
        Long secondItemId = createCodingItem(moduleId, "Second", 1);
        Long foreignModuleId = createModule(courseId, "Foreign module", 1);
        Long foreignItemId = createCodingItem(foreignModuleId, "Foreign", 0);

        mockMvc.perform(put("/api/v1/admin/modules/{moduleId}/items/reorder", moduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderedItemIds": [%d, %d]}
                                """.formatted(firstItemId, firstItemId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("duplicate IDs")));

        mockMvc.perform(put("/api/v1/admin/modules/{moduleId}/items/reorder", moduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderedItemIds": [%d]}
                                """.formatted(firstItemId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("missing existing IDs")));

        mockMvc.perform(put("/api/v1/admin/modules/{moduleId}/items/reorder", moduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderedItemIds": []}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/v1/admin/modules/{moduleId}/items/reorder", moduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderedItemIds": [%d, %d, %d]}
                                """.formatted(firstItemId, secondItemId, foreignItemId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("foreign IDs")));
    }

    @Test
    void shouldRejectNegativeOrderIndexesInAdminRequests() throws Exception {
        Long courseId = createCourse("negative-order-course-" + System.nanoTime());
        Long moduleId = createModule(courseId, "Practice", 0);
        Long codingItemId = createCodingItem(moduleId, "Coding", 0);
        Long quizItemId = createQuizItem(moduleId, "Quiz", 1);

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/modules", courseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Invalid module", "orderIndex": -1}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/admin/modules/{moduleId}/items", moduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Invalid item",
                                  "itemType": "CODING",
                                  "language": "python",
                                  "orderIndex": -1
                                }
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/v1/admin/course-items/{itemId}/content-blocks", codingItemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentBlocks": [{"blockType": "TEXT", "orderIndex": -1, "textContent": "Invalid"}]}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/v1/admin/course-items/{itemId}/hints", codingItemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"hints": [{"orderIndex": -1, "text": "Invalid"}]}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/v1/admin/course-items/{itemId}/test-cases", codingItemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"testCases": [{"testKey": "bad", "orderIndex": -1, "visibility": "OPEN"}]}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/v1/admin/course-items/{itemId}/options", quizItemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"options": [{"orderIndex": -1, "text": "Invalid", "correct": true}]}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectDuplicateOrderIndexesAndDuplicateTestKeysInChildCollections() throws Exception {
        Long courseId = createCourse("duplicate-child-order-" + System.nanoTime());
        Long moduleId = createModule(courseId, "Practice", 0);
        Long codingItemId = createCodingItem(moduleId, "Coding", 0);
        Long quizItemId = createQuizItem(moduleId, "Quiz", 1);

        mockMvc.perform(put("/api/v1/admin/course-items/{itemId}/content-blocks", codingItemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "contentBlocks": [
                                    {"blockType": "TEXT", "orderIndex": 0, "textContent": "A"},
                                    {"blockType": "TEXT", "orderIndex": 0, "textContent": "B"}
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("duplicate orderIndex")));

        mockMvc.perform(put("/api/v1/admin/course-items/{itemId}/hints", codingItemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"hints": [{"orderIndex": 0, "text": "A"}, {"orderIndex": 0, "text": "B"}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("duplicate orderIndex")));

        mockMvc.perform(put("/api/v1/admin/course-items/{itemId}/test-cases", codingItemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "testCases": [
                                    {"testKey": "same", "orderIndex": 0, "visibility": "OPEN"},
                                    {"testKey": "same", "orderIndex": 1, "visibility": "HIDDEN"}
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("duplicate values")));

        mockMvc.perform(put("/api/v1/admin/course-items/{itemId}/options", quizItemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "options": [
                                    {"orderIndex": 0, "text": "A", "correct": true},
                                    {"orderIndex": 0, "text": "B", "correct": false}
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("duplicate orderIndex")));
    }

    @Test
    void shouldRejectInvalidItemTypeChildCollections() throws Exception {
        Long courseId = createCourse("invalid-child-collections-" + System.nanoTime());
        Long moduleId = createModule(courseId, "Mixed", 0);
        Long theoryItemId = createTheoryItem(moduleId, "Theory", 0, "Read this.");
        Long quizItemId = createQuizItem(moduleId, "Quiz", 1);

        mockMvc.perform(put("/api/v1/admin/course-items/{itemId}/test-cases", theoryItemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"testCases": [{"testKey": "open", "orderIndex": 0, "visibility": "OPEN"}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("THEORY item cannot have test cases")));

        mockMvc.perform(put("/api/v1/admin/course-items/{itemId}/test-cases", quizItemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"testCases": [{"testKey": "open", "orderIndex": 0, "visibility": "OPEN"}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("QUIZ item cannot have test cases")));

        mockMvc.perform(put("/api/v1/admin/course-items/{itemId}/options", theoryItemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"options": [{"orderIndex": 0, "text": "A", "correct": true}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("THEORY item cannot have quiz options")));
    }

    @Test
    void shouldRejectExecutableItemsWithoutLanguageButAllowArbitraryNonBlankLanguage() throws Exception {
        Long courseId = createCourse("language-structural-validation-" + System.nanoTime());
        Long moduleId = createModule(courseId, "Practice", 0);

        mockMvc.perform(post("/api/v1/admin/modules/{moduleId}/items", moduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "No language coding",
                                  "itemType": "CODING",
                                  "orderIndex": 0,
                                  "timeLimitMs": 1500,
                                  "memoryLimitMb": 256,
                                  "outputLimitKb": 256
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("must have non-blank language")));

        mockMvc.perform(post("/api/v1/admin/modules/{moduleId}/items", moduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Java coding",
                                  "itemType": "CODING",
                                  "language": "java",
                                  "orderIndex": 0,
                                  "timeLimitMs": 1500,
                                  "memoryLimitMb": 256,
                                  "outputLimitKb": 256
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.language").value("java"));

        mockMvc.perform(post("/api/v1/admin/modules/{moduleId}/items", moduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "SQL task",
                                  "itemType": "SQL",
                                  "language": "postgresql",
                                  "orderIndex": 1,
                                  "timeLimitMs": 1500,
                                  "memoryLimitMb": 256,
                                  "outputLimitKb": 256
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.language").value("postgresql"));
    }

    @Test
    void shouldRejectPublishingInvalidCourseStructuresAndKeepDraftStatus() throws Exception {
        Long courseId = createCourse("publish-invalid-structures-" + System.nanoTime());
        Long moduleId = createModule(courseId, "Theory", 0);
        createTheoryItem(moduleId, "Empty theory", 0, null);

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/publish", courseId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("must have statement or content blocks before publishing")));

        mockMvc.perform(get("/api/v1/admin/courses/{courseId}", courseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void shouldRejectPublishingModuleWithoutItemsAndQuizWithoutOptions() throws Exception {
        Long emptyModuleCourseId = createCourse("publish-empty-module-" + System.nanoTime());
        createModule(emptyModuleCourseId, "Empty", 0);

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/publish", emptyModuleCourseId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("must contain at least one course item")));

        Long quizCourseId = createCourse("publish-quiz-no-options-" + System.nanoTime());
        Long quizModuleId = createModule(quizCourseId, "Quiz", 0);
        createQuizItem(quizModuleId, "Quiz item", 0);

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/publish", quizCourseId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("must contain at least one option")));
    }

    @Test
    void shouldPublishTheoryAndFileItemsWhenTheyHaveVisibleContent() throws Exception {
        Long courseId = createCourse("publish-theory-file-valid-" + System.nanoTime());
        Long moduleId = createModule(courseId, "Content", 0);
        createTheoryItem(moduleId, "Theory", 0, "Visible theory statement.");
        createFileItem(moduleId, "File", 1, "Download attached material.");

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/publish", courseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    private Long createPublishableCourse(String slug) throws Exception {
        Long courseId = createCourse(slug);
        Long moduleId = createModule(courseId, "Moderation", 0);
        createCodingItem(moduleId, "Review task", 0);
        return courseId;
    }

    private void assertConflictAndUnchanged(
            RequestBuilder requestBuilder,
            Long courseId,
            CourseStatus expectedStatus
    ) throws Exception {
        mockMvc.perform(requestBuilder)
                .andExpect(status().isConflict());

        assertEquals(
                expectedStatus,
                courseRepository.findById(courseId).orElseThrow().getStatus()
        );
    }

    private Course seedCourse(Long ownerId, String slug) {
        return seedCourse(ownerId, slug, CourseStatus.DRAFT, CourseDifficulty.BEGINNER, CourseAccessType.PUBLIC);
    }

    private Course seedPublishableCourse(Long ownerId, String slug) {
        Course course = seedCourse(ownerId, slug);
        CourseModule module = moduleRepository.save(CourseModule.builder()
                .course(course)
                .title("Seeded module")
                .description("Seeded module for moderation tests.")
                .orderIndex(0)
                .build());

        itemRepository.save(CourseItem.builder()
                .module(module)
                .title("Seeded coding item")
                .itemType(CourseItemType.CODING)
                .statement("Read n and print n squared.")
                .starterCode("n = int(input())\nprint(n * n)\n")
                .language("python")
                .orderIndex(0)
                .timeLimitMs(1500)
                .memoryLimitMb(256)
                .outputLimitKb(256)
                .networkDisabled(true)
                .readOnlyFs(true)
                .comparisonMode(ComparisonMode.EXACT)
                .normalizeLineEndings(true)
                .trimTrailingWhitespaces(true)
                .build());

        return course;
    }

    private Course seedCourseWithModeration(
            Long ownerId,
            String slug,
            CourseStatus status,
            Instant submittedForReviewAt,
            Instant reviewedAt,
            Long reviewedByUserId,
            String reviewComment
    ) {
        Course course = seedCourse(ownerId, slug, status, CourseDifficulty.BEGINNER, CourseAccessType.PUBLIC);
        course.setSubmittedForReviewAt(submittedForReviewAt);
        course.setReviewedAt(reviewedAt);
        course.setReviewedByUserId(reviewedByUserId);
        course.setReviewComment(reviewComment);
        return courseRepository.save(course);
    }

    private Course seedCourse(
            Long ownerId,
            String slug,
            CourseStatus status,
            CourseDifficulty difficulty,
            CourseAccessType accessType
    ) {
        return courseRepository.save(Course.builder()
                .slug(slug)
                .title("Seeded course")
                .shortDescription("Seeded course for ownership tests.")
                .description("Seeded course for ownership tests.")
                .difficulty(difficulty)
                .status(status)
                .accessType(accessType)
                .enrollmentEnabled(true)
                .estimatedMinutes(30)
                .createdByUserId(ownerId)
                .publishedAt(status == CourseStatus.PUBLISHED ? Instant.now() : null)
                .build());
    }

    private String minimalCreateCourseRequest(String slug) {
        return """
                {
                  "slug": "%s",
                  "title": "Security Test Course",
                  "createdByUserId": 1
                }
                """.formatted(slug + "-" + System.nanoTime());
    }

    private Long createCourse() throws Exception {
        return createCourse("admin-api-test-" + System.nanoTime());
    }

    private Long createCourse(String slug) throws Exception {
        String request = """
                {
                  "slug": "%s",
                  "title": "Admin API Test Course",
                  "shortDescription": "Course for admin API integration tests.",
                  "description": "Checks admin course management operations.",
                  "difficulty": "BEGINNER",
                  "accessType": "PUBLIC",
                  "enrollmentEnabled": true,
                  "estimatedMinutes": 30,
                  "createdByUserId": 1
                }
                """.formatted(slug);

        String response = mockMvc.perform(post("/api/v1/admin/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return extractLong(response, "id");
    }

    private Long createModule(Long courseId) throws Exception {
        return createModule(courseId, "Practice", 1);
    }

    private Long createModule(Long courseId, String title, int orderIndex) throws Exception {
        String request = """
                {
                  "title": "%s",
                  "description": "Practice module.",
                  "orderIndex": %d
                }
                """.formatted(title, orderIndex);

        String response = mockMvc.perform(post("/api/v1/admin/courses/{courseId}/modules", courseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.courseId").value(courseId))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return extractLong(response, "id");
    }

    private Long createCodingItem(Long moduleId) throws Exception {
        return createCodingItem(moduleId, "Print square", 1);
    }

    private Long createCodingItem(Long moduleId, String title, int orderIndex) throws Exception {
        String request = """
                {
                  "title": "%s",
                  "itemType": "CODING",
                  "statement": "Read n and print n squared.",
                  "starterCode": "n = int(input())\\nprint(n * n)\\n",
                  "language": "python",
                  "orderIndex": %d,
                  "timeLimitMs": 1500,
                  "memoryLimitMb": 256,
                  "outputLimitKb": 256,
                  "networkDisabled": true,
                  "readOnlyFs": true,
                  "comparisonMode": "EXACT",
                  "normalizeLineEndings": true,
                  "trimTrailingWhitespaces": true
                }
                """.formatted(title, orderIndex);

        String response = mockMvc.perform(post("/api/v1/admin/modules/{moduleId}/items", moduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.moduleId").value(moduleId))
                .andExpect(jsonPath("$.itemType").value("CODING"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return extractLong(response, "id");
    }

    private Long createQuizItem(Long moduleId, String title, int orderIndex) throws Exception {
        String request = """
                {
                  "title": "%s",
                  "itemType": "QUIZ",
                  "statement": "Choose the correct answer.",
                  "orderIndex": %d
                }
                """.formatted(title, orderIndex);

        String response = mockMvc.perform(post("/api/v1/admin/modules/{moduleId}/items", moduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.moduleId").value(moduleId))
                .andExpect(jsonPath("$.itemType").value("QUIZ"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return extractLong(response, "id");
    }

    private Long createTheoryItem(Long moduleId, String title, int orderIndex, String statement) throws Exception {
        String statementJson = statement == null ? "null" : "\"" + statement.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        String request = """
                {
                  "title": "%s",
                  "itemType": "THEORY",
                  "statement": %s,
                  "orderIndex": %d
                }
                """.formatted(title, statementJson, orderIndex);

        String response = mockMvc.perform(post("/api/v1/admin/modules/{moduleId}/items", moduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.moduleId").value(moduleId))
                .andExpect(jsonPath("$.itemType").value("THEORY"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return extractLong(response, "id");
    }

    private Long createFileItem(Long moduleId, String title, int orderIndex, String statement) throws Exception {
        String statementJson = statement == null ? "null" : "\"" + statement.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        String request = """
                {
                  "title": "%s",
                  "itemType": "FILE",
                  "statement": %s,
                  "orderIndex": %d
                }
                """.formatted(title, statementJson, orderIndex);

        String response = mockMvc.perform(post("/api/v1/admin/modules/{moduleId}/items", moduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.moduleId").value(moduleId))
                .andExpect(jsonPath("$.itemType").value("FILE"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return extractLong(response, "id");
    }

    private void replaceContentBlocks(Long itemId) throws Exception {
        String request = """
                {
                  "contentBlocks": [
                    {
                      "blockType": "TEXT",
                      "orderIndex": 1,
                      "title": "Explanation",
                      "textContent": "Use multiplication to calculate square."
                    }
                  ]
                }
                """;

        mockMvc.perform(put("/api/v1/admin/course-items/{itemId}/content-blocks", itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contentBlocks", hasSize(1)));
    }

    private void replaceHints(Long itemId) throws Exception {
        String request = """
                {
                  "hints": [
                    {
                      "orderIndex": 1,
                      "text": "Use n * n."
                    }
                  ]
                }
                """;

        mockMvc.perform(put("/api/v1/admin/course-items/{itemId}/hints", itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hints", hasSize(1)));
    }

    private void replaceTestCases(Long itemId) throws Exception {
        String request = """
                {
                  "testCases": [
                    {
                      "testKey": "open-1",
                      "orderIndex": 1,
                      "visibility": "OPEN",
                      "inputData": "3\\n",
                      "expectedOutput": "9\\n"
                    },
                    {
                      "testKey": "hidden-1",
                      "orderIndex": 2,
                      "visibility": "HIDDEN",
                      "inputData": "5\\n",
                      "expectedOutput": "25\\n"
                    }
                  ]
                }
                """;

        mockMvc.perform(put("/api/v1/admin/course-items/{itemId}/test-cases", itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.testCases", hasSize(2)));
    }

    private void replaceOptions(Long itemId) throws Exception {
        String request = """
                {
                  "options": [
                    {
                      "orderIndex": 1,
                      "label": "A",
                      "text": "n * n",
                      "correct": true,
                      "explanation": "This calculates the square."
                    },
                    {
                      "orderIndex": 2,
                      "label": "B",
                      "text": "n + n",
                      "correct": false,
                      "explanation": "This doubles the number."
                    }
                  ]
                }
                """;

        mockMvc.perform(put("/api/v1/admin/course-items/{itemId}/options", itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.options", hasSize(2)));
    }

    private Long extractLong(String json, String field) {
        String pattern = "\"" + field + "\":";
        int start = json.indexOf(pattern) + pattern.length();
        int end = start;

        while (end < json.length() && Character.isDigit(json.charAt(end))) {
            end++;
        }

        return Long.parseLong(json.substring(start, end));
    }
}
