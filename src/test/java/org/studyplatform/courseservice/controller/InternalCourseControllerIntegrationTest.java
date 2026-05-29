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
import org.studyplatform.courseservice.entity.CourseItemOption;
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
    void shouldRejectQuizEvaluationPackageWithoutApiKey() throws Exception {
        Long itemId = seedQuizItem().itemId();

        mockMvc.perform(get("/api/v1/internal/course-items/{itemId}/quiz-evaluation-package", itemId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or missing internal API key"));
    }

    @Test
    void shouldRejectCourseOwnershipWithoutApiKey() throws Exception {
        Long courseId = seedCodingItem().courseId();

        mockMvc.perform(get("/api/v1/internal/courses/{courseId}/ownership", courseId)
                        .param("userId", "1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or missing internal API key"));
    }

    @Test
    void shouldRejectInternalEndpointWithInvalidApiKey() throws Exception {
        Long itemId = seedCodingItem().itemId();

        mockMvc.perform(get("/api/v1/internal/course-items/{itemId}/execution-package", itemId)
                        .header("X-Internal-Api-Key", "wrong-key"))
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
    void shouldReturnQuizEvaluationPackageWithCorrectOptionsForValidInternalApiKey() throws Exception {
        SeededQuizItem seededItem = seedQuizItem();

        mockMvc.perform(get("/api/v1/internal/course-items/{itemId}/quiz-evaluation-package", seededItem.itemId())
                        .header("X-Internal-Api-Key", INTERNAL_API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemId").value(seededItem.itemId()))
                .andExpect(jsonPath("$.moduleId").value(seededItem.moduleId()))
                .andExpect(jsonPath("$.courseId").value(seededItem.courseId()))
                .andExpect(jsonPath("$.itemType").value("QUIZ"))
                .andExpect(jsonPath("$.title").value("Syntax quiz"))
                .andExpect(jsonPath("$.options", hasSize(2)))
                .andExpect(jsonPath("$.options[0].id").value(seededItem.correctOptionId()))
                .andExpect(jsonPath("$.options[0].orderIndex").value(0))
                .andExpect(jsonPath("$.options[0].label").value("A"))
                .andExpect(jsonPath("$.options[0].text").value("x = 1"))
                .andExpect(jsonPath("$.options[0].correct").value(true))
                .andExpect(jsonPath("$.options[0].explanation").value("Assignment uses one equals sign."))
                .andExpect(jsonPath("$.options[1].id").value(seededItem.incorrectOptionId()))
                .andExpect(jsonPath("$.options[1].correct").value(false));
    }

    @Test
    void shouldRejectQuizEvaluationPackageForNonQuizItem() throws Exception {
        Long itemId = seedCodingItem().itemId();

        mockMvc.perform(get("/api/v1/internal/course-items/{itemId}/quiz-evaluation-package", itemId)
                        .header("X-Internal-Api-Key", INTERNAL_API_KEY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("not a QUIZ")));
    }

    @Test
    void shouldReturnNotFoundForMissingQuizEvaluationPackageItem() throws Exception {
        mockMvc.perform(get("/api/v1/internal/course-items/{itemId}/quiz-evaluation-package", 999999L)
                        .header("X-Internal-Api-Key", INTERNAL_API_KEY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Course item not found: 999999"));
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
    void shouldReturnCourseOwnershipForOwnerAndNonOwner() throws Exception {
        SeededCodingItem seededItem = seedCodingItem();
        Long courseId = seededItem.courseId();

        mockMvc.perform(get("/api/v1/internal/courses/{courseId}/ownership", courseId)
                        .param("userId", "1")
                        .header("X-Internal-Api-Key", INTERNAL_API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId").value(courseId))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.owner").value(true));

        mockMvc.perform(get("/api/v1/internal/courses/{courseId}/ownership", courseId)
                        .param("userId", "8")
                        .header("X-Internal-Api-Key", INTERNAL_API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId").value(courseId))
                .andExpect(jsonPath("$.userId").value(8))
                .andExpect(jsonPath("$.owner").value(false));
    }

    @Test
    void shouldReturnNotFoundForMissingCourseOwnership() throws Exception {
        mockMvc.perform(get("/api/v1/internal/courses/{courseId}/ownership", 999999L)
                        .param("userId", "1")
                        .header("X-Internal-Api-Key", INTERNAL_API_KEY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Course not found: 999999"));
    }

    @Test
    void shouldRejectNonPositiveCourseOwnershipUserId() throws Exception {
        Long courseId = seedCodingItem().courseId();

        mockMvc.perform(get("/api/v1/internal/courses/{courseId}/ownership", courseId)
                        .param("userId", "0")
                        .header("X-Internal-Api-Key", INTERNAL_API_KEY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("userId")));
    }

    @Test
    void shouldReturnStudentSafeContentForValidInternalApiKey() throws Exception {
        Long itemId = seedCodingItem().itemId();

        mockMvc.perform(get("/api/v1/internal/course-items/{itemId}/content", itemId)
                        .header("X-Internal-Api-Key", INTERNAL_API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemId").value(itemId))
                .andExpect(jsonPath("$.openTests", hasSize(1)))
                .andExpect(content().string(not(containsString("expectedOutput"))))
                .andExpect(content().string(not(containsString("HIDDEN"))))
                .andExpect(content().string(not(containsString("correct"))));
    }

    @Test
    void shouldKeepInternalQuizContentLearnerSafe() throws Exception {
        Long itemId = seedQuizItem().itemId();

        mockMvc.perform(get("/api/v1/internal/course-items/{itemId}/content", itemId)
                        .header("X-Internal-Api-Key", INTERNAL_API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemId").value(itemId))
                .andExpect(jsonPath("$.itemType").value("QUIZ"))
                .andExpect(jsonPath("$.options", hasSize(2)))
                .andExpect(jsonPath("$.options[0].text").value("x = 1"))
                .andExpect(content().string(not(containsString("correct"))))
                .andExpect(content().string(not(containsString("explanation"))))
                .andExpect(content().string(not(containsString("Assignment uses one equals sign."))));
    }

    @Test
    void shouldKeepPublicEndpointAsPreviewOnlyWhenInternalDataContainsHiddenTests() throws Exception {
        Long itemId = seedCodingItem().itemId();

        mockMvc.perform(get("/api/v1/course-items/{itemId}", itemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(itemId))
                .andExpect(jsonPath("$.title").value("Print square"))
                .andExpect(jsonPath("$.itemType").value("CODING"))
                .andExpect(content().string(not(containsString("createdByUserId"))))
                .andExpect(content().string(not(containsString("contentBlocks"))))
                .andExpect(content().string(not(containsString("starterCode"))))
                .andExpect(content().string(not(containsString("openTests"))))
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

    private SeededQuizItem seedQuizItem() {
        Course course = courseRepository.save(Course.builder()
                .slug("internal-quiz-test-" + System.nanoTime())
                .title("Internal Quiz Test Course")
                .shortDescription("Course for internal quiz API tests.")
                .description("Checks quiz evaluation package contract.")
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
                .title("Quiz")
                .description("Quiz module.")
                .orderIndex(1)
                .build());

        CourseItem item = itemRepository.save(CourseItem.builder()
                .module(module)
                .title("Syntax quiz")
                .itemType(CourseItemType.QUIZ)
                .statement("Choose valid assignment syntax.")
                .orderIndex(1)
                .networkDisabled(true)
                .readOnlyFs(true)
                .comparisonMode(ComparisonMode.EXACT)
                .normalizeLineEndings(true)
                .trimTrailingWhitespaces(true)
                .build());

        CourseItemOption correctOption = optionRepository.save(CourseItemOption.builder()
                .item(item)
                .orderIndex(0)
                .label("A")
                .text("x = 1")
                .correct(true)
                .explanation("Assignment uses one equals sign.")
                .build());

        CourseItemOption incorrectOption = optionRepository.save(CourseItemOption.builder()
                .item(item)
                .orderIndex(1)
                .label("B")
                .text("int x = 1")
                .correct(false)
                .explanation("Python does not require a type here.")
                .build());

        return new SeededQuizItem(
                course.getId(),
                module.getId(),
                item.getId(),
                correctOption.getId(),
                incorrectOption.getId()
        );
    }

    private record SeededCodingItem(Long courseId, Long itemId) {
    }

    private record SeededQuizItem(
            Long courseId,
            Long moduleId,
            Long itemId,
            Long correctOptionId,
            Long incorrectOptionId
    ) {
    }
}
