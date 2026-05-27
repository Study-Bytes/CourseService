package org.studyplatform.courseservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.studyplatform.courseservice.dto.internal.ExecutionPackageResponse;
import org.studyplatform.courseservice.dto.internal.InternalCourseItemContentResponse;
import org.studyplatform.courseservice.dto.internal.InternalCourseOwnershipResponse;
import org.studyplatform.courseservice.dto.publicapi.CourseItemOptionResponse;
import org.studyplatform.courseservice.dto.publicapi.OpenTestCaseResponse;
import org.studyplatform.courseservice.entity.Course;
import org.studyplatform.courseservice.entity.CourseItem;
import org.studyplatform.courseservice.entity.CourseItemContentBlock;
import org.studyplatform.courseservice.entity.CourseItemOption;
import org.studyplatform.courseservice.entity.CourseItemTestCase;
import org.studyplatform.courseservice.entity.CourseModule;
import org.studyplatform.courseservice.entity.enums.CourseAccessType;
import org.studyplatform.courseservice.entity.enums.CourseStatus;
import org.studyplatform.courseservice.entity.enums.TestVisibility;
import org.studyplatform.courseservice.exception.ResourceNotFoundException;
import org.studyplatform.courseservice.repository.CourseItemContentBlockRepository;
import org.studyplatform.courseservice.repository.CourseItemHintRepository;
import org.studyplatform.courseservice.repository.CourseItemOptionRepository;
import org.studyplatform.courseservice.repository.CourseItemRepository;
import org.studyplatform.courseservice.repository.CourseItemTestCaseRepository;
import org.studyplatform.courseservice.repository.CourseRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.studyplatform.courseservice.testsupport.CourseTestFixtures.codingItem;
import static org.studyplatform.courseservice.testsupport.CourseTestFixtures.course;
import static org.studyplatform.courseservice.testsupport.CourseTestFixtures.module;
import static org.studyplatform.courseservice.testsupport.CourseTestFixtures.option;
import static org.studyplatform.courseservice.testsupport.CourseTestFixtures.testCase;
import static org.studyplatform.courseservice.testsupport.CourseTestFixtures.textBlock;

@ExtendWith(MockitoExtension.class)
class CourseInternalServiceUnitTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseItemRepository itemRepository;

    @Mock
    private CourseItemContentBlockRepository contentBlockRepository;

    @Mock
    private CourseItemHintRepository hintRepository;

    @Mock
    private CourseItemOptionRepository optionRepository;

    @Mock
    private CourseItemTestCaseRepository testCaseRepository;

    @Test
    void executionPackageShouldIncludeHiddenTestsAndExpectedOutputs() {
        SeededItem seeded = seededCodingItem();
        CourseInternalService service = service();

        when(itemRepository.findById(seeded.item().getId())).thenReturn(Optional.of(seeded.item()));
        when(testCaseRepository.findByItemIdOrderByOrderIndexAsc(seeded.item().getId()))
                .thenReturn(List.of(seeded.openTest(), seeded.hiddenTest()));

        ExecutionPackageResponse response = service.getExecutionPackage(seeded.item().getId());

        assertEquals(seeded.item().getId(), response.itemId());
        assertEquals("python", response.language());
        assertEquals(2, response.tests().size());
        assertEquals(TestVisibility.OPEN, response.tests().getFirst().visibility());
        assertEquals("4\n", response.tests().getFirst().expectedOutput());
        assertEquals(TestVisibility.HIDDEN, response.tests().get(1).visibility());
        assertEquals("25\n", response.tests().get(1).expectedOutput());
    }

    @Test
    void internalContentShouldExposeStudentSafeContentOnly() {
        SeededItem seeded = seededCodingItem();
        CourseInternalService service = service();

        when(itemRepository.findById(seeded.item().getId())).thenReturn(Optional.of(seeded.item()));
        when(contentBlockRepository.findByItemIdOrderByOrderIndexAsc(seeded.item().getId()))
                .thenReturn(List.of(seeded.block()));
        when(testCaseRepository.findByItemIdOrderByOrderIndexAsc(seeded.item().getId()))
                .thenReturn(List.of(seeded.hiddenTest(), seeded.openTest()));
        when(hintRepository.findByItemIdOrderByOrderIndexAsc(seeded.item().getId()))
                .thenReturn(List.of());
        when(optionRepository.findByItemIdOrderByOrderIndexAsc(seeded.item().getId()))
                .thenReturn(List.of(seeded.correctOption()));

        InternalCourseItemContentResponse response = service.getCourseItemContent(seeded.item().getId());

        assertEquals(1, response.contentBlocks().size());
        assertEquals(1, response.openTests().size());
        assertEquals("open-1", response.openTests().getFirst().testKey());
        assertEquals(1, response.options().size());
        assertEquals("A", response.options().getFirst().label());

        assertRecordDoesNotExpose(OpenTestCaseResponse.class, "expectedOutput", "visibility");
        assertRecordDoesNotExpose(CourseItemOptionResponse.class, "correct", "explanation");
    }

    @Test
    void ownershipShouldCompareCourseAuthorWithUserId() {
        CourseInternalService service = service();

        when(courseRepository.findCreatedByUserIdById(10L)).thenReturn(Optional.of(5L));

        InternalCourseOwnershipResponse ownerResponse = service.getCourseOwnership(10L, 5L);
        InternalCourseOwnershipResponse nonOwnerResponse = service.getCourseOwnership(10L, 8L);

        assertEquals(10L, ownerResponse.courseId());
        assertEquals(5L, ownerResponse.userId());
        assertEquals(true, ownerResponse.owner());
        assertEquals(8L, nonOwnerResponse.userId());
        assertEquals(false, nonOwnerResponse.owner());
    }

    @Test
    void ownershipShouldReturnNotFoundWhenCourseDoesNotExist() {
        CourseInternalService service = service();

        when(courseRepository.findCreatedByUserIdById(10L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.getCourseOwnership(10L, 5L)
        );

        assertEquals("Course not found: 10", exception.getMessage());
    }

    private CourseInternalService service() {
        return new CourseInternalService(
                courseRepository,
                itemRepository,
                contentBlockRepository,
                hintRepository,
                optionRepository,
                testCaseRepository
        );
    }

    private SeededItem seededCodingItem() {
        Course course = course("internal-service-course", 1L, CourseStatus.PUBLISHED, CourseAccessType.PUBLIC);
        course.setId(1L);

        CourseModule module = module(course, "Internal module", 0);
        module.setId(2L);

        CourseItem item = codingItem(module, "Internal item", 0, "python");
        item.setId(3L);

        CourseItemContentBlock block = textBlock(item, 0, "Visible content");
        block.setId(4L);

        CourseItemTestCase openTest = testCase(item, "open-1", 0, TestVisibility.OPEN, "2\n", "4\n");
        openTest.setId(5L);

        CourseItemTestCase hiddenTest = testCase(item, "hidden-1", 1, TestVisibility.HIDDEN, "5\n", "25\n");
        hiddenTest.setId(6L);

        CourseItemOption correctOption = option(item, 0, "A", "n * n", true);
        correctOption.setId(7L);
        correctOption.setExplanation("Correct explanation");

        return new SeededItem(item, block, openTest, hiddenTest, correctOption);
    }

    private void assertRecordDoesNotExpose(Class<?> recordType, String... componentNames) {
        List<String> exposedComponents = Arrays.stream(recordType.getRecordComponents())
                .map(component -> component.getName())
                .toList();

        for (String componentName : componentNames) {
            assertFalse(exposedComponents.contains(componentName), recordType.getSimpleName() + " exposes " + componentName);
        }
    }

    private record SeededItem(
            CourseItem item,
            CourseItemContentBlock block,
            CourseItemTestCase openTest,
            CourseItemTestCase hiddenTest,
            CourseItemOption correctOption
    ) {
    }
}
