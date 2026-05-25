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
import org.studyplatform.courseservice.entity.CourseModule;
import org.studyplatform.courseservice.entity.enums.CourseAccessType;
import org.studyplatform.courseservice.entity.enums.CourseStatus;
import org.studyplatform.courseservice.entity.enums.TestVisibility;
import org.studyplatform.courseservice.repository.CourseItemContentBlockRepository;
import org.studyplatform.courseservice.repository.CourseItemHintRepository;
import org.studyplatform.courseservice.repository.CourseItemOptionRepository;
import org.studyplatform.courseservice.repository.CourseItemRepository;
import org.studyplatform.courseservice.repository.CourseItemTestCaseRepository;
import org.studyplatform.courseservice.repository.CourseModuleRepository;
import org.studyplatform.courseservice.repository.CourseRepository;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.studyplatform.courseservice.testsupport.CourseTestFixtures.codingItem;
import static org.studyplatform.courseservice.testsupport.CourseTestFixtures.course;
import static org.studyplatform.courseservice.testsupport.CourseTestFixtures.module;
import static org.studyplatform.courseservice.testsupport.CourseTestFixtures.option;
import static org.studyplatform.courseservice.testsupport.CourseTestFixtures.quizItem;
import static org.studyplatform.courseservice.testsupport.CourseTestFixtures.testCase;
import static org.studyplatform.courseservice.testsupport.CourseTestFixtures.textBlock;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicCourseControllerIntegrationTest {

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
    void shouldReturnOnlyPublishedPublicCoursesInCatalog() throws Exception {
        Course publishedPublic = courseRepository.save(course(
                "public-published-" + System.nanoTime(),
                1L,
                CourseStatus.PUBLISHED,
                CourseAccessType.PUBLIC
        ));
        courseRepository.save(course("public-draft-" + System.nanoTime(), 1L, CourseStatus.DRAFT, CourseAccessType.PUBLIC));
        courseRepository.save(course("public-pending-review-" + System.nanoTime(), 1L, CourseStatus.PENDING_REVIEW, CourseAccessType.PUBLIC));
        courseRepository.save(course("public-changes-requested-" + System.nanoTime(), 1L, CourseStatus.CHANGES_REQUESTED, CourseAccessType.PUBLIC));
        courseRepository.save(course("public-archived-" + System.nanoTime(), 1L, CourseStatus.ARCHIVED, CourseAccessType.PUBLIC));
        courseRepository.save(course("unlisted-published-" + System.nanoTime(), 1L, CourseStatus.PUBLISHED, CourseAccessType.UNLISTED));
        courseRepository.save(course("private-published-" + System.nanoTime(), 1L, CourseStatus.PUBLISHED, CourseAccessType.PRIVATE));

        mockMvc.perform(get("/api/v1/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courses", hasSize(1)))
                .andExpect(jsonPath("$.courses[0].id").value(publishedPublic.getId()))
                .andExpect(jsonPath("$.courses[0].status").value("PUBLISHED"))
                .andExpect(jsonPath("$.courses[0].accessType").value("PUBLIC"));
    }

    @Test
    void shouldReturnPublishedPublicCourseDetailsWithModuleAndItemSummaries() throws Exception {
        SeededPublishedCourse seeded = seedPublishedCodingCourse(CourseAccessType.PUBLIC);

        mockMvc.perform(get("/api/v1/courses/{courseId}", seeded.courseId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(seeded.courseId()))
                .andExpect(jsonPath("$.modules", hasSize(1)))
                .andExpect(jsonPath("$.modules[0].items", hasSize(2)))
                .andExpect(jsonPath("$.modules[0].items[0].id").value(seeded.codingItemId()))
                .andExpect(content().string(not(containsString("expectedOutput"))))
                .andExpect(content().string(not(containsString("hidden-1"))))
                .andExpect(content().string(not(containsString("Correct explanation"))));
    }

    @Test
    void shouldAllowReadingPublishedUnlistedCourseDetailsButHideItFromCatalog() throws Exception {
        SeededPublishedCourse seeded = seedPublishedCodingCourse(CourseAccessType.UNLISTED);

        mockMvc.perform(get("/api/v1/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courses", hasSize(0)));

        mockMvc.perform(get("/api/v1/courses/{courseId}", seeded.courseId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(seeded.courseId()))
                .andExpect(jsonPath("$.accessType").value("UNLISTED"));
    }

    @Test
    void shouldRejectNonPublishedAndPrivateCourseDetailsAsNotFound() throws Exception {
        Course draft = courseRepository.save(course("draft-not-public-" + System.nanoTime(), 1L, CourseStatus.DRAFT, CourseAccessType.PUBLIC));
        Course pendingReview = courseRepository.save(course("pending-not-public-" + System.nanoTime(), 1L, CourseStatus.PENDING_REVIEW, CourseAccessType.PUBLIC));
        Course changesRequested = courseRepository.save(course("changes-not-public-" + System.nanoTime(), 1L, CourseStatus.CHANGES_REQUESTED, CourseAccessType.PUBLIC));
        Course privateCourse = courseRepository.save(course("private-not-public-" + System.nanoTime(), 1L, CourseStatus.PUBLISHED, CourseAccessType.PRIVATE));
        Course archived = courseRepository.save(course("archived-not-public-" + System.nanoTime(), 1L, CourseStatus.ARCHIVED, CourseAccessType.PUBLIC));

        mockMvc.perform(get("/api/v1/courses/{courseId}", draft.getId()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/courses/{courseId}", pendingReview.getId()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/courses/{courseId}", changesRequested.getId()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/courses/{courseId}", privateCourse.getId()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/courses/{courseId}", archived.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnOnlySafePublicItemPreview() throws Exception {
        SeededPublishedCourse seeded = seedPublishedCodingCourse(CourseAccessType.PUBLIC);

        mockMvc.perform(get("/api/v1/course-items/{itemId}", seeded.codingItemId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(seeded.codingItemId()))
                .andExpect(jsonPath("$.courseId").value(seeded.courseId()))
                .andExpect(jsonPath("$.itemType").value("CODING"))
                .andExpect(content().string(not(containsString("starterCode"))))
                .andExpect(content().string(not(containsString("openTests"))))
                .andExpect(content().string(not(containsString("expectedOutput"))))
                .andExpect(content().string(not(containsString("hidden-1"))))
                .andExpect(content().string(not(containsString("correct"))));
    }

    @Test
    void shouldRejectPublicItemPreviewWhenCourseIsNotPubliclyReadable() throws Exception {
        Course privateCourse = courseRepository.save(course(
                "private-preview-" + System.nanoTime(),
                1L,
                CourseStatus.PUBLISHED,
                CourseAccessType.PRIVATE
        ));
        CourseModule module = moduleRepository.save(module(privateCourse, "Private module", 0));
        CourseItem item = itemRepository.save(codingItem(module, "Private item", 0, "python"));

        mockMvc.perform(get("/api/v1/course-items/{itemId}", item.getId()))
                .andExpect(status().isNotFound());

        Course pendingCourse = courseRepository.save(course(
                "pending-preview-" + System.nanoTime(),
                1L,
                CourseStatus.PENDING_REVIEW,
                CourseAccessType.PUBLIC
        ));
        CourseModule pendingModule = moduleRepository.save(module(pendingCourse, "Pending module", 0));
        CourseItem pendingItem = itemRepository.save(codingItem(pendingModule, "Pending item", 0, "python"));

        mockMvc.perform(get("/api/v1/course-items/{itemId}", pendingItem.getId()))
                .andExpect(status().isNotFound());
    }

    private SeededPublishedCourse seedPublishedCodingCourse(CourseAccessType accessType) {
        Course course = courseRepository.save(course(
                "public-api-course-" + System.nanoTime(),
                1L,
                CourseStatus.PUBLISHED,
                accessType
        ));
        CourseModule module = moduleRepository.save(module(course, "Basics", 0));
        CourseItem codingItem = itemRepository.save(codingItem(module, "Print square", 0, "python"));
        CourseItem quizItem = itemRepository.save(quizItem(module, "Quiz item", 1));

        contentBlockRepository.save(textBlock(codingItem, 0, "Use multiplication."));
        testCaseRepository.save(testCase(codingItem, "open-1", 0, TestVisibility.OPEN, "3\n", "9\n"));
        testCaseRepository.save(testCase(codingItem, "hidden-1", 1, TestVisibility.HIDDEN, "5\n", "25\n"));
        optionRepository.save(option(quizItem, 0, "A", "n * n", true));
        optionRepository.save(option(quizItem, 1, "B", "n + n", false));

        return new SeededPublishedCourse(course.getId(), codingItem.getId());
    }

    private record SeededPublishedCourse(Long courseId, Long codingItemId) {
    }
}
