package org.studyplatform.courseservice.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.studyplatform.courseservice.entity.Course;
import org.studyplatform.courseservice.entity.CourseItem;
import org.studyplatform.courseservice.entity.CourseItemTestCase;
import org.studyplatform.courseservice.entity.CourseModule;
import org.studyplatform.courseservice.entity.enums.ComparisonMode;
import org.studyplatform.courseservice.entity.enums.CourseAccessType;
import org.studyplatform.courseservice.entity.enums.CourseDifficulty;
import org.studyplatform.courseservice.entity.enums.CourseItemType;
import org.studyplatform.courseservice.entity.enums.CourseStatus;
import org.studyplatform.courseservice.entity.enums.TestVisibility;
import org.studyplatform.courseservice.repository.CourseItemContentBlockRepository;
import org.studyplatform.courseservice.repository.CourseItemHintRepository;
import org.studyplatform.courseservice.repository.CourseItemOptionRepository;
import org.studyplatform.courseservice.repository.CourseItemRepository;
import org.studyplatform.courseservice.repository.CourseItemTestCaseRepository;
import org.studyplatform.courseservice.repository.CourseModuleRepository;
import org.studyplatform.courseservice.repository.CourseRepository;

import java.time.Instant;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InternalCourseControllerIntegrationTest {

    private static final String INTERNAL_API_KEY = "test-course-service-internal-key";

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
    void shouldRejectInternalEndpointWithoutApiKey() throws Exception {
        Long itemId = seedCodingItem().itemId();

        mockMvc.perform(get("/api/v1/internal/course-items/{itemId}/execution-package", itemId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or missing internal API key"));
    }

    @Test
    void shouldReturnExecutionPackageWithHiddenTestsForValidInternalApiKey() throws Exception {
        Long itemId = seedCodingItem().itemId();

        mockMvc.perform(get("/api/v1/internal/course-items/{itemId}/execution-package", itemId)
                        .header("X-Internal-Api-Key", INTERNAL_API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemId").value(itemId))
                .andExpect(jsonPath("$.itemType").value("CODING"))
                .andExpect(jsonPath("$.language").value("python"))
                .andExpect(jsonPath("$.limits.timeLimitMs").value(1500))
                .andExpect(jsonPath("$.executionPolicy.networkDisabled").value(true))
                .andExpect(jsonPath("$.evaluationPolicy.comparisonMode").value("EXACT"))
                .andExpect(jsonPath("$.tests", hasSize(2)))
                .andExpect(jsonPath("$.tests[0].visibility").value("OPEN"))
                .andExpect(jsonPath("$.tests[0].expectedOutput").value("9\n"))
                .andExpect(jsonPath("$.tests[1].visibility").value("HIDDEN"))
                .andExpect(jsonPath("$.tests[1].expectedOutput").value("25\n"));
    }

    @Test
    void shouldReturnCourseAvailabilityForValidInternalApiKey() throws Exception {
        SeededCodingItem seededItem = seedCodingItem();
        Long courseId = seededItem.courseId();

        mockMvc.perform(get("/api/v1/internal/courses/{courseId}/availability", courseId)
                        .header("X-Internal-Api-Key", INTERNAL_API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId").value(courseId))
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.accessType").value("PUBLIC"))
                .andExpect(jsonPath("$.enrollmentEnabled").value(true))
                .andExpect(jsonPath("$.availableForEnrollment").value(true));
    }

    @Test
    void shouldKeepPublicEndpointSafeWhenInternalDataContainsHiddenTests() throws Exception {
        Long itemId = seedCodingItem().itemId();

        mockMvc.perform(get("/api/v1/course-items/{itemId}", itemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openTests", hasSize(1)))
                .andExpect(content().string(not(containsString("expectedOutput"))))
                .andExpect(content().string(not(containsString("HIDDEN"))));
    }

    private SeededCodingItem seedCodingItem() {
        Course course = courseRepository.save(Course.builder()
                .slug("internal-api-test-" + System.nanoTime())
                .title("Internal API Test Course")
                .shortDescription("Course for internal API tests.")
                .description("Checks execution package contract.")
                .difficulty(CourseDifficulty.BEGINNER)
                .status(CourseStatus.PUBLISHED)
                .accessType(CourseAccessType.PUBLIC)
                .enrollmentEnabled(true)
                .estimatedMinutes(30)
                .createdByUserId(1L)
                .publishedAt(Instant.now())
                .build());

        CourseModule module = moduleRepository.save(CourseModule.builder()
                .course(course)
                .title("Practice")
                .description("Practice module.")
                .orderIndex(1)
                .build());

        CourseItem item = itemRepository.save(CourseItem.builder()
                .module(module)
                .title("Print square")
                .itemType(CourseItemType.CODING)
                .statement("Read n and print n squared.")
                .starterCode("n = int(input())\nprint(n * n)\n")
                .language("python")
                .orderIndex(1)
                .timeLimitMs(1500)
                .memoryLimitMb(256)
                .outputLimitKb(256)
                .networkDisabled(true)
                .readOnlyFs(true)
                .comparisonMode(ComparisonMode.EXACT)
                .normalizeLineEndings(true)
                .trimTrailingWhitespaces(true)
                .build());

        testCaseRepository.save(CourseItemTestCase.builder()
                .item(item)
                .testKey("open-1")
                .orderIndex(1)
                .visibility(TestVisibility.OPEN)
                .inputData("3\n")
                .expectedOutput("9\n")
                .build());

        testCaseRepository.save(CourseItemTestCase.builder()
                .item(item)
                .testKey("hidden-1")
                .orderIndex(2)
                .visibility(TestVisibility.HIDDEN)
                .inputData("5\n")
                .expectedOutput("25\n")
                .build());

        return new SeededCodingItem(course.getId(), item.getId());
    }

    private record SeededCodingItem(Long courseId, Long itemId) {
    }
}
