package org.studyplatform.courseservice.testsupport;

import org.studyplatform.courseservice.entity.Course;
import org.studyplatform.courseservice.entity.CourseItem;
import org.studyplatform.courseservice.entity.CourseItemContentBlock;
import org.studyplatform.courseservice.entity.CourseItemHint;
import org.studyplatform.courseservice.entity.CourseItemOption;
import org.studyplatform.courseservice.entity.CourseItemTestCase;
import org.studyplatform.courseservice.entity.CourseModule;
import org.studyplatform.courseservice.entity.enums.ComparisonMode;
import org.studyplatform.courseservice.entity.enums.ContentBlockType;
import org.studyplatform.courseservice.entity.enums.CourseAccessType;
import org.studyplatform.courseservice.entity.enums.CourseDifficulty;
import org.studyplatform.courseservice.entity.enums.CourseItemType;
import org.studyplatform.courseservice.entity.enums.CourseStatus;
import org.studyplatform.courseservice.entity.enums.TestVisibility;

import java.time.Instant;

public final class CourseTestFixtures {

    private CourseTestFixtures() {
    }

    public static Course course(String slug, Long ownerId, CourseStatus status, CourseAccessType accessType) {
        return Course.builder()
                .slug(slug)
                .title("Course " + slug)
                .shortDescription("Short description for " + slug)
                .description("Full description for " + slug)
                .difficulty(CourseDifficulty.BEGINNER)
                .status(status)
                .accessType(accessType)
                .enrollmentEnabled(true)
                .estimatedMinutes(30)
                .createdByUserId(ownerId)
                .publishedAt(status == CourseStatus.PUBLISHED ? Instant.now() : null)
                .build();
    }

    public static CourseModule module(Course course, String title, int orderIndex) {
        return CourseModule.builder()
                .course(course)
                .title(title)
                .description(title + " description")
                .orderIndex(orderIndex)
                .build();
    }

    public static CourseItem codingItem(CourseModule module, String title, int orderIndex, String language) {
        return executableItem(module, title, CourseItemType.CODING, orderIndex, language);
    }

    public static CourseItem sqlItem(CourseModule module, String title, int orderIndex, String language) {
        return executableItem(module, title, CourseItemType.SQL, orderIndex, language);
    }

    public static CourseItem theoryItem(CourseModule module, String title, int orderIndex, String statement) {
        return CourseItem.builder()
                .module(module)
                .title(title)
                .itemType(CourseItemType.THEORY)
                .statement(statement)
                .orderIndex(orderIndex)
                .build();
    }

    public static CourseItem fileItem(CourseModule module, String title, int orderIndex, String statement) {
        return CourseItem.builder()
                .module(module)
                .title(title)
                .itemType(CourseItemType.FILE)
                .statement(statement)
                .orderIndex(orderIndex)
                .build();
    }

    public static CourseItem quizItem(CourseModule module, String title, int orderIndex) {
        return CourseItem.builder()
                .module(module)
                .title(title)
                .itemType(CourseItemType.QUIZ)
                .statement("Choose the correct answer.")
                .orderIndex(orderIndex)
                .build();
    }

    public static CourseItemContentBlock textBlock(CourseItem item, int orderIndex, String text) {
        return CourseItemContentBlock.builder()
                .item(item)
                .blockType(ContentBlockType.TEXT)
                .orderIndex(orderIndex)
                .title("Text block")
                .textContent(text)
                .build();
    }

    public static CourseItemHint hint(CourseItem item, int orderIndex, String text) {
        return CourseItemHint.builder()
                .item(item)
                .orderIndex(orderIndex)
                .text(text)
                .build();
    }

    public static CourseItemTestCase testCase(
            CourseItem item,
            String testKey,
            int orderIndex,
            TestVisibility visibility,
            String inputData,
            String expectedOutput
    ) {
        return CourseItemTestCase.builder()
                .item(item)
                .testKey(testKey)
                .orderIndex(orderIndex)
                .visibility(visibility)
                .inputData(inputData)
                .expectedOutput(expectedOutput)
                .build();
    }

    public static CourseItemOption option(CourseItem item, int orderIndex, String label, String text, boolean correct) {
        return CourseItemOption.builder()
                .item(item)
                .orderIndex(orderIndex)
                .label(label)
                .text(text)
                .correct(correct)
                .explanation(correct ? "Correct explanation" : "Wrong explanation")
                .build();
    }

    private static CourseItem executableItem(
            CourseModule module,
            String title,
            CourseItemType itemType,
            int orderIndex,
            String language
    ) {
        return CourseItem.builder()
                .module(module)
                .title(title)
                .itemType(itemType)
                .statement("Read input and solve the task.")
                .starterCode("# write solution here\n")
                .language(language)
                .orderIndex(orderIndex)
                .timeLimitMs(1500)
                .memoryLimitMb(256)
                .outputLimitKb(256)
                .networkDisabled(true)
                .readOnlyFs(true)
                .comparisonMode(ComparisonMode.EXACT)
                .normalizeLineEndings(true)
                .trimTrailingWhitespaces(true)
                .build();
    }
}
