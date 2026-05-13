package org.studyplatform.courseservice.mapper;

import org.junit.jupiter.api.Test;
import org.studyplatform.courseservice.dto.admin.AdminCourseItemResponse;
import org.studyplatform.courseservice.dto.publicapi.CourseItemDetailsResponse;
import org.studyplatform.courseservice.dto.publicapi.CourseItemOptionResponse;
import org.studyplatform.courseservice.dto.publicapi.CourseItemPreviewResponse;
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

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.studyplatform.courseservice.testsupport.CourseTestFixtures.codingItem;
import static org.studyplatform.courseservice.testsupport.CourseTestFixtures.course;
import static org.studyplatform.courseservice.testsupport.CourseTestFixtures.module;
import static org.studyplatform.courseservice.testsupport.CourseTestFixtures.option;
import static org.studyplatform.courseservice.testsupport.CourseTestFixtures.testCase;
import static org.studyplatform.courseservice.testsupport.CourseTestFixtures.textBlock;

class CourseMapperUnitTest {

    private final PublicCourseMapper publicMapper = new PublicCourseMapper();
    private final AdminCourseMapper adminMapper = new AdminCourseMapper();

    @Test
    void publicItemDetailsShouldExposeOnlyOpenTestsAndSafeQuizOptionFields() {
        SeededItem seeded = seededCodingItem();

        CourseItemDetailsResponse response = publicMapper.toItemDetails(
                seeded.item(),
                List.of(seeded.block()),
                List.of(seeded.hiddenTest(), seeded.openTest()),
                List.of(),
                List.of(seeded.correctOption())
        );

        assertEquals(1, response.openTests().size());
        assertEquals("open-1", response.openTests().getFirst().testKey());
        assertEquals("2\n", response.openTests().getFirst().inputData());
        assertEquals(1, response.options().size());
        assertEquals("A", response.options().getFirst().label());

        assertRecordDoesNotExpose(OpenTestCaseResponse.class, "expectedOutput", "visibility");
        assertRecordDoesNotExpose(CourseItemOptionResponse.class, "correct", "explanation");
    }

    @Test
    void publicItemPreviewShouldNotExposeFullContentOrExecutionData() {
        CourseItemPreviewResponse response = publicMapper.toItemPreview(seededCodingItem().item());

        assertEquals("Mapper item", response.title());
        assertRecordDoesNotExpose(
                CourseItemPreviewResponse.class,
                "statement",
                "starterCode",
                "limits",
                "contentBlocks",
                "openTests",
                "hints",
                "options"
        );
    }

    @Test
    void adminItemResponseShouldExposeEditorOnlyFields() {
        SeededItem seeded = seededCodingItem();

        AdminCourseItemResponse response = adminMapper.toItemResponse(
                seeded.item(),
                List.of(seeded.block()),
                List.of(),
                List.of(seeded.hiddenTest(), seeded.openTest()),
                List.of(seeded.correctOption())
        );

        assertEquals(2, response.testCases().size());
        assertEquals("open-1", response.testCases().getFirst().testKey());
        assertEquals("hidden-1", response.testCases().get(1).testKey());
        assertEquals("25\n", response.testCases().get(1).expectedOutput());
        assertEquals(TestVisibility.HIDDEN, response.testCases().get(1).visibility());
        assertEquals(true, response.options().getFirst().correct());
        assertEquals("Correct explanation", response.options().getFirst().explanation());
    }

    private SeededItem seededCodingItem() {
        Course course = course("mapper-course", 1L, CourseStatus.PUBLISHED, CourseAccessType.PUBLIC);
        course.setId(1L);

        CourseModule module = module(course, "Mapper module", 0);
        module.setId(2L);

        CourseItem item = codingItem(module, "Mapper item", 0, "python");
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
