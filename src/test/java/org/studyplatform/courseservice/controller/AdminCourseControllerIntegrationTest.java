package org.studyplatform.courseservice.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.studyplatform.courseservice.repository.CourseItemContentBlockRepository;
import org.studyplatform.courseservice.repository.CourseItemHintRepository;
import org.studyplatform.courseservice.repository.CourseItemOptionRepository;
import org.studyplatform.courseservice.repository.CourseItemRepository;
import org.studyplatform.courseservice.repository.CourseItemTestCaseRepository;
import org.studyplatform.courseservice.repository.CourseModuleRepository;
import org.studyplatform.courseservice.repository.CourseRepository;

import static org.hamcrest.Matchers.hasSize;
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
    void shouldCreateAndPublishCourseWithEditableContent() throws Exception {
        Long courseId = createCourse();
        Long moduleId = createModule(courseId);
        Long itemId = createCodingItem(moduleId);

        replaceContentBlocks(itemId);
        replaceHints(itemId);
        replaceTestCases(itemId);
        replaceOptions(itemId);

        mockMvc.perform(post("/api/v1/admin/courses/{courseId}/publish", courseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.publishedAt").exists());

        mockMvc.perform(get("/api/v1/admin/course-items/{itemId}", itemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contentBlocks", hasSize(1)))
                .andExpect(jsonPath("$.hints", hasSize(1)))
                .andExpect(jsonPath("$.testCases", hasSize(2)))
                .andExpect(jsonPath("$.options", hasSize(2)))
                .andExpect(jsonPath("$.testCases[1].visibility").value("HIDDEN"))
                .andExpect(jsonPath("$.testCases[1].expectedOutput").value("25\n"))
                .andExpect(jsonPath("$.options[0].correct").value(true));
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
        String request = """
                {
                  "title": "Practice",
                  "description": "Practice module.",
                  "orderIndex": 1
                }
                """;

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
        String request = """
                {
                  "title": "Print square",
                  "itemType": "CODING",
                  "statement": "Read n and print n squared.",
                  "starterCode": "n = int(input())\\nprint(n * n)\\n",
                  "language": "python",
                  "orderIndex": 1,
                  "timeLimitMs": 1500,
                  "memoryLimitMb": 256,
                  "outputLimitKb": 256,
                  "networkDisabled": true,
                  "readOnlyFs": true,
                  "comparisonMode": "EXACT",
                  "normalizeLineEndings": true,
                  "trimTrailingWhitespaces": true
                }
                """;

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
