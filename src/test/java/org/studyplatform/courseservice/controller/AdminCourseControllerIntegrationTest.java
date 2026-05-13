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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
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


    private Course seedCourse(Long ownerId, String slug) {
        return courseRepository.save(Course.builder()
                .slug(slug)
                .title("Seeded course")
                .shortDescription("Seeded course for ownership tests.")
                .description("Seeded course for ownership tests.")
                .difficulty(CourseDifficulty.BEGINNER)
                .status(CourseStatus.DRAFT)
                .accessType(CourseAccessType.PUBLIC)
                .enrollmentEnabled(true)
                .estimatedMinutes(30)
                .createdByUserId(ownerId)
                .publishedAt(Instant.now())
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
