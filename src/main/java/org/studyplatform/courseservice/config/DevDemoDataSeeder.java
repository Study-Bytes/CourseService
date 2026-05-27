package org.studyplatform.courseservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
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
import org.studyplatform.courseservice.repository.CourseItemContentBlockRepository;
import org.studyplatform.courseservice.repository.CourseItemHintRepository;
import org.studyplatform.courseservice.repository.CourseItemOptionRepository;
import org.studyplatform.courseservice.repository.CourseItemRepository;
import org.studyplatform.courseservice.repository.CourseItemTestCaseRepository;
import org.studyplatform.courseservice.repository.CourseModuleRepository;
import org.studyplatform.courseservice.repository.CourseRepository;

import java.time.Instant;

@Component
@Profile("dev")
public class DevDemoDataSeeder implements CommandLineRunner {

    private static final String DEMO_COURSE_SLUG = "python-basics-demo";

    private final boolean demoDataEnabled;
    private final CourseRepository courseRepository;
    private final CourseModuleRepository courseModuleRepository;
    private final CourseItemRepository courseItemRepository;
    private final CourseItemContentBlockRepository contentBlockRepository;
    private final CourseItemHintRepository hintRepository;
    private final CourseItemTestCaseRepository testCaseRepository;
    private final CourseItemOptionRepository optionRepository;

    public DevDemoDataSeeder(
            @Value("${app.demo-data.enabled:false}") boolean demoDataEnabled,
            CourseRepository courseRepository,
            CourseModuleRepository courseModuleRepository,
            CourseItemRepository courseItemRepository,
            CourseItemContentBlockRepository contentBlockRepository,
            CourseItemHintRepository hintRepository,
            CourseItemTestCaseRepository testCaseRepository,
            CourseItemOptionRepository optionRepository
    ) {
        this.demoDataEnabled = demoDataEnabled;
        this.courseRepository = courseRepository;
        this.courseModuleRepository = courseModuleRepository;
        this.courseItemRepository = courseItemRepository;
        this.contentBlockRepository = contentBlockRepository;
        this.hintRepository = hintRepository;
        this.testCaseRepository = testCaseRepository;
        this.optionRepository = optionRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!demoDataEnabled || courseRepository.existsBySlug(DEMO_COURSE_SLUG)) {
            return;
        }

        Course course = createDemoCourse();
        CourseModule introModule = createIntroModule(course);
        CourseModule practiceModule = createPracticeModule(course);

        createTheoryItem(introModule);
        createCodingItem(practiceModule);
        createQuizItem(practiceModule);
    }

    private Course createDemoCourse() {
        return courseRepository.save(Course.builder()
                .slug(DEMO_COURSE_SLUG)
                .title("Python Basics Demo")
                .shortDescription("Demo course for manual QA of CourseService public API.")
                .description("A small demo course with theory, video, image, code blocks, a coding task, hints, open and hidden tests, and a quiz.")
                .difficulty(CourseDifficulty.BEGINNER)
                .status(CourseStatus.PUBLISHED)
                .accessType(CourseAccessType.PUBLIC)
                .enrollmentEnabled(true)
                .coverImageUrl("https://images.unsplash.com/photo-1526379095098-d400fd0bf935?auto=format&fit=crop&w=1200&q=80")
                .estimatedMinutes(60)
                .createdByUserId(1L)
                .publishedAt(Instant.now())
                .build());
    }

    private CourseModule createIntroModule(Course course) {
        return courseModuleRepository.save(CourseModule.builder()
                .course(course)
                .title("Introduction")
                .description("Basic course materials and theory blocks.")
                .orderIndex(1)
                .build());
    }

    private CourseModule createPracticeModule(Course course) {
        return courseModuleRepository.save(CourseModule.builder()
                .course(course)
                .title("Practice")
                .description("Coding and quiz items for checking the public API shape.")
                .orderIndex(2)
                .build());
    }

    private void createTheoryItem(CourseModule module) {
        CourseItem item = courseItemRepository.save(CourseItem.builder()
                .module(module)
                .title("What is a variable?")
                .itemType(CourseItemType.THEORY)
                .statement("A variable is a named value used by a program. This item demonstrates rich content blocks.")
                .language(null)
                .orderIndex(1)
                .build());

        contentBlockRepository.save(CourseItemContentBlock.builder()
                .item(item)
                .blockType(ContentBlockType.TEXT)
                .orderIndex(1)
                .title("Theory")
                .textContent("Variables allow a program to store intermediate values and reuse them later. In Python, you create a variable by assigning a value to a name.")
                .build());

        contentBlockRepository.save(CourseItemContentBlock.builder()
                .item(item)
                .blockType(ContentBlockType.VIDEO)
                .orderIndex(2)
                .title("Short video explanation")
                .url("https://example.com/videos/python-variables-intro.mp4")
                .metadataJson("{\"durationSeconds\":180,\"provider\":\"demo\",\"thumbnailUrl\":\"https://example.com/assets/video-thumbnail.png\"}")
                .build());

        contentBlockRepository.save(CourseItemContentBlock.builder()
                .item(item)
                .blockType(ContentBlockType.IMAGE)
                .orderIndex(3)
                .title("Variable diagram")
                .url("https://example.com/assets/courses/python-variable-diagram.png")
                .metadataJson("{\"alt\":\"Variable name points to a stored value\"}")
                .build());

        contentBlockRepository.save(CourseItemContentBlock.builder()
                .item(item)
                .blockType(ContentBlockType.CODE)
                .orderIndex(4)
                .title("Python example")
                .language("python")
                .textContent("name = \"Alice\"\nprint(name)\n")
                .build());
    }

    private void createCodingItem(CourseModule module) {
        CourseItem item = courseItemRepository.save(CourseItem.builder()
                .module(module)
                .title("Print the square")
                .itemType(CourseItemType.CODING)
                .statement("Read one integer n and print n squared.")
                .starterCode("n = int(input())\n# write your code below\n")
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

        contentBlockRepository.save(CourseItemContentBlock.builder()
                .item(item)
                .blockType(ContentBlockType.TEXT)
                .orderIndex(1)
                .title("Task description")
                .textContent("Use multiplication to calculate the square of the input number. For example, if n = 5, the answer is 25.")
                .build());

        hintRepository.save(CourseItemHint.builder()
                .item(item)
                .orderIndex(1)
                .text("The square of n is n * n.")
                .build());

        hintRepository.save(CourseItemHint.builder()
                .item(item)
                .orderIndex(2)
                .text("Use print(...) to output the result.")
                .build());

        testCaseRepository.save(CourseItemTestCase.builder()
                .item(item)
                .testKey("open-positive")
                .orderIndex(1)
                .visibility(TestVisibility.OPEN)
                .inputData("5\n")
                .expectedOutput("25\n")
                .build());

        testCaseRepository.save(CourseItemTestCase.builder()
                .item(item)
                .testKey("open-zero")
                .orderIndex(2)
                .visibility(TestVisibility.OPEN)
                .inputData("0\n")
                .expectedOutput("0\n")
                .build());

        testCaseRepository.save(CourseItemTestCase.builder()
                .item(item)
                .testKey("hidden-negative")
                .orderIndex(3)
                .visibility(TestVisibility.HIDDEN)
                .inputData("-3\n")
                .expectedOutput("9\n")
                .build());
    }

    private void createQuizItem(CourseModule module) {
        CourseItem item = courseItemRepository.save(CourseItem.builder()
                .module(module)
                .title("Python variable quiz")
                .itemType(CourseItemType.QUIZ)
                .statement("Choose the valid Python assignment.")
                .language(null)
                .orderIndex(2)
                .build());

        contentBlockRepository.save(CourseItemContentBlock.builder()
                .item(item)
                .blockType(ContentBlockType.TEXT)
                .orderIndex(1)
                .title("Question")
                .textContent("Which expression creates a variable named age with value 20 in Python?")
                .build());

        optionRepository.save(CourseItemOption.builder()
                .item(item)
                .orderIndex(1)
                .label("A")
                .text("age = 20")
                .correct(true)
                .explanation("Python assignment uses a single equals sign.")
                .build());

        optionRepository.save(CourseItemOption.builder()
                .item(item)
                .orderIndex(2)
                .label("B")
                .text("20 -> age")
                .correct(false)
                .explanation("This is not valid Python assignment syntax.")
                .build());

        optionRepository.save(CourseItemOption.builder()
                .item(item)
                .orderIndex(3)
                .label("C")
                .text("int age = 20")
                .correct(false)
                .explanation("Python does not use Java-style type declarations for simple assignment.")
                .build());
    }
}
