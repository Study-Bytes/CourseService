package org.studyplatform.courseservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.studyplatform.courseservice.dto.admin.AdminCourseItemResponse;
import org.studyplatform.courseservice.dto.admin.AdminCourseResponse;
import org.studyplatform.courseservice.dto.admin.AdminModuleResponse;
import org.studyplatform.courseservice.dto.admin.ContentBlockRequest;
import org.studyplatform.courseservice.dto.admin.CreateCourseItemRequest;
import org.studyplatform.courseservice.dto.admin.CreateCourseRequest;
import org.studyplatform.courseservice.dto.admin.CreateModuleRequest;
import org.studyplatform.courseservice.dto.admin.HintRequest;
import org.studyplatform.courseservice.dto.admin.QuizOptionRequest;
import org.studyplatform.courseservice.dto.admin.ReplaceContentBlocksRequest;
import org.studyplatform.courseservice.dto.admin.ReplaceHintsRequest;
import org.studyplatform.courseservice.dto.admin.ReplaceQuizOptionsRequest;
import org.studyplatform.courseservice.dto.admin.ReplaceTestCasesRequest;
import org.studyplatform.courseservice.dto.admin.TestCaseRequest;
import org.studyplatform.courseservice.dto.admin.UpdateCourseItemRequest;
import org.studyplatform.courseservice.dto.admin.UpdateCourseRequest;
import org.studyplatform.courseservice.dto.admin.UpdateModuleRequest;
import org.studyplatform.courseservice.entity.Course;
import org.studyplatform.courseservice.entity.CourseItem;
import org.studyplatform.courseservice.entity.CourseItemContentBlock;
import org.studyplatform.courseservice.entity.CourseItemHint;
import org.studyplatform.courseservice.entity.CourseItemOption;
import org.studyplatform.courseservice.entity.CourseItemTestCase;
import org.studyplatform.courseservice.entity.CourseModule;
import org.studyplatform.courseservice.entity.enums.CourseAccessType;
import org.studyplatform.courseservice.entity.enums.CourseDifficulty;
import org.studyplatform.courseservice.entity.enums.CourseItemType;
import org.studyplatform.courseservice.entity.enums.CourseStatus;
import org.studyplatform.courseservice.entity.enums.TestVisibility;
import org.studyplatform.courseservice.exception.BadRequestException;
import org.studyplatform.courseservice.exception.ConflictException;
import org.studyplatform.courseservice.exception.ResourceNotFoundException;
import org.studyplatform.courseservice.mapper.AdminCourseMapper;
import org.studyplatform.courseservice.repository.CourseItemContentBlockRepository;
import org.studyplatform.courseservice.repository.CourseItemHintRepository;
import org.studyplatform.courseservice.repository.CourseItemOptionRepository;
import org.studyplatform.courseservice.repository.CourseItemRepository;
import org.studyplatform.courseservice.repository.CourseItemTestCaseRepository;
import org.studyplatform.courseservice.repository.CourseModuleRepository;
import org.studyplatform.courseservice.repository.CourseRepository;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CourseAdminService {

    private final CourseRepository courseRepository;
    private final CourseModuleRepository moduleRepository;
    private final CourseItemRepository itemRepository;
    private final CourseItemContentBlockRepository contentBlockRepository;
    private final CourseItemHintRepository hintRepository;
    private final CourseItemTestCaseRepository testCaseRepository;
    private final CourseItemOptionRepository optionRepository;
    private final AdminCourseMapper mapper;

    @Transactional
    public AdminCourseResponse createCourse(CreateCourseRequest request) {
        String slug = normalizeSlug(request.slug());

        if (courseRepository.existsBySlug(slug)) {
            throw new ConflictException("Course slug already exists: " + slug);
        }

        Course course = Course.builder()
                .slug(slug)
                .title(request.title().trim())
                .shortDescription(blankToNull(request.shortDescription()))
                .description(blankToNull(request.description()))
                .difficulty(valueOrDefault(request.difficulty(), CourseDifficulty.BEGINNER))
                .status(CourseStatus.DRAFT)
                .accessType(valueOrDefault(request.accessType(), CourseAccessType.PUBLIC))
                .enrollmentEnabled(valueOrDefault(request.enrollmentEnabled(), true))
                .coverImageUrl(blankToNull(request.coverImageUrl()))
                .estimatedMinutes(request.estimatedMinutes())
                .createdByUserId(request.createdByUserId())
                .build();

        Course saved = courseRepository.save(course);
        return mapper.toCourseResponse(saved, List.of(), List.of());
    }

    @Transactional(readOnly = true)
    public AdminCourseResponse getCourse(Long courseId) {
        Course course = getCourseOrThrow(courseId);
        List<CourseModule> modules = moduleRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
        List<CourseItem> items = modules.stream()
                .flatMap(module -> itemRepository.findByModuleIdOrderByOrderIndexAsc(module.getId()).stream())
                .toList();

        return mapper.toCourseResponse(course, modules, items);
    }

    @Transactional
    public AdminCourseResponse updateCourse(Long courseId, UpdateCourseRequest request) {
        Course course = getCourseOrThrow(courseId);

        if (hasText(request.slug())) {
            String slug = normalizeSlug(request.slug());
            if (courseRepository.existsBySlugAndIdNot(slug, courseId)) {
                throw new ConflictException("Course slug already exists: " + slug);
            }
            course.setSlug(slug);
        }

        if (hasText(request.title())) {
            course.setTitle(request.title().trim());
        }

        if (request.shortDescription() != null) {
            course.setShortDescription(blankToNull(request.shortDescription()));
        }

        if (request.description() != null) {
            course.setDescription(blankToNull(request.description()));
        }

        if (request.difficulty() != null) {
            course.setDifficulty(request.difficulty());
        }

        if (request.accessType() != null) {
            course.setAccessType(request.accessType());
        }

        if (request.enrollmentEnabled() != null) {
            course.setEnrollmentEnabled(request.enrollmentEnabled());
        }

        if (request.coverImageUrl() != null) {
            course.setCoverImageUrl(blankToNull(request.coverImageUrl()));
        }

        if (request.estimatedMinutes() != null) {
            course.setEstimatedMinutes(request.estimatedMinutes());
        }

        Course saved = courseRepository.save(course);
        return getCourse(saved.getId());
    }

    @Transactional
    public AdminCourseResponse publishCourse(Long courseId) {
        Course course = getCourseOrThrow(courseId);
        course.setStatus(CourseStatus.PUBLISHED);

        if (course.getPublishedAt() == null) {
            course.setPublishedAt(Instant.now());
        }

        courseRepository.save(course);
        return getCourse(courseId);
    }

    @Transactional
    public AdminCourseResponse archiveCourse(Long courseId) {
        Course course = getCourseOrThrow(courseId);
        course.setStatus(CourseStatus.ARCHIVED);
        courseRepository.save(course);
        return getCourse(courseId);
    }

    @Transactional
    public AdminModuleResponse createModule(Long courseId, CreateModuleRequest request) {
        Course course = getCourseOrThrow(courseId);

        CourseModule module = CourseModule.builder()
                .course(course)
                .title(request.title().trim())
                .description(blankToNull(request.description()))
                .orderIndex(request.orderIndex())
                .build();

        CourseModule saved = moduleRepository.save(module);
        return mapper.toModuleResponse(saved, List.of());
    }

    @Transactional
    public AdminModuleResponse updateModule(Long moduleId, UpdateModuleRequest request) {
        CourseModule module = getModuleOrThrow(moduleId);

        if (hasText(request.title())) {
            module.setTitle(request.title().trim());
        }

        if (request.description() != null) {
            module.setDescription(blankToNull(request.description()));
        }

        if (request.orderIndex() != null) {
            module.setOrderIndex(request.orderIndex());
        }

        CourseModule saved = moduleRepository.save(module);
        List<CourseItem> items = itemRepository.findByModuleIdOrderByOrderIndexAsc(saved.getId());
        return mapper.toModuleResponse(saved, items);
    }

    @Transactional
    public void deleteModule(Long moduleId) {
        CourseModule module = getModuleOrThrow(moduleId);
        List<CourseItem> items = itemRepository.findByModuleIdOrderByOrderIndexAsc(moduleId);

        for (CourseItem item : items) {
            deleteItemChildren(item.getId());
        }

        itemRepository.deleteAll(items);
        moduleRepository.delete(module);
    }

    @Transactional
    public AdminCourseItemResponse createItem(Long moduleId, CreateCourseItemRequest request) {
        CourseModule module = getModuleOrThrow(moduleId);

        CourseItem item = CourseItem.builder()
                .module(module)
                .title(request.title().trim())
                .itemType(valueOrDefault(request.itemType(), CourseItemType.CODING))
                .statement(blankToNull(request.statement()))
                .starterCode(blankToNull(request.starterCode()))
                .language(blankToNull(request.language()))
                .orderIndex(request.orderIndex())
                .timeLimitMs(request.timeLimitMs())
                .memoryLimitMb(request.memoryLimitMb())
                .outputLimitKb(request.outputLimitKb())
                .networkDisabled(request.networkDisabled())
                .readOnlyFs(request.readOnlyFs())
                .comparisonMode(request.comparisonMode())
                .normalizeLineEndings(request.normalizeLineEndings())
                .trimTrailingWhitespaces(request.trimTrailingWhitespaces())
                .build();

        CourseItem saved = itemRepository.save(item);
        return mapper.toItemResponse(saved);
    }

    @Transactional(readOnly = true)
    public AdminCourseItemResponse getItem(Long itemId) {
        CourseItem item = getItemOrThrow(itemId);
        return mapFullItem(item);
    }

    @Transactional
    public AdminCourseItemResponse updateItem(Long itemId, UpdateCourseItemRequest request) {
        CourseItem item = getItemOrThrow(itemId);

        if (hasText(request.title())) {
            item.setTitle(request.title().trim());
        }

        if (request.itemType() != null) {
            item.setItemType(request.itemType());
        }

        if (request.statement() != null) {
            item.setStatement(blankToNull(request.statement()));
        }

        if (request.starterCode() != null) {
            item.setStarterCode(blankToNull(request.starterCode()));
        }

        if (request.language() != null) {
            item.setLanguage(blankToNull(request.language()));
        }

        if (request.orderIndex() != null) {
            item.setOrderIndex(request.orderIndex());
        }

        if (request.timeLimitMs() != null) {
            item.setTimeLimitMs(request.timeLimitMs());
        }

        if (request.memoryLimitMb() != null) {
            item.setMemoryLimitMb(request.memoryLimitMb());
        }

        if (request.outputLimitKb() != null) {
            item.setOutputLimitKb(request.outputLimitKb());
        }

        if (request.networkDisabled() != null) {
            item.setNetworkDisabled(request.networkDisabled());
        }

        if (request.readOnlyFs() != null) {
            item.setReadOnlyFs(request.readOnlyFs());
        }

        if (request.comparisonMode() != null) {
            item.setComparisonMode(request.comparisonMode());
        }

        if (request.normalizeLineEndings() != null) {
            item.setNormalizeLineEndings(request.normalizeLineEndings());
        }

        if (request.trimTrailingWhitespaces() != null) {
            item.setTrimTrailingWhitespaces(request.trimTrailingWhitespaces());
        }

        CourseItem saved = itemRepository.save(item);
        return mapFullItem(saved);
    }

    @Transactional
    public void deleteItem(Long itemId) {
        CourseItem item = getItemOrThrow(itemId);
        deleteItemChildren(itemId);
        itemRepository.delete(item);
    }

    @Transactional
    public AdminCourseItemResponse replaceContentBlocks(Long itemId, ReplaceContentBlocksRequest request) {
        CourseItem item = getItemOrThrow(itemId);

        List<CourseItemContentBlock> existing = contentBlockRepository.findByItemIdOrderByOrderIndexAsc(itemId);
        contentBlockRepository.deleteAllInBatch(existing);
        contentBlockRepository.flush();

        List<CourseItemContentBlock> blocks = request.contentBlocks().stream()
                .map(block -> toContentBlock(item, block))
                .toList();

        contentBlockRepository.saveAll(blocks);
        return mapFullItem(item);
    }

    @Transactional
    public AdminCourseItemResponse replaceHints(Long itemId, ReplaceHintsRequest request) {
        CourseItem item = getItemOrThrow(itemId);

        List<CourseItemHint> existing = hintRepository.findByItemIdOrderByOrderIndexAsc(itemId);
        hintRepository.deleteAllInBatch(existing);
        hintRepository.flush();

        List<CourseItemHint> hints = request.hints().stream()
                .map(hint -> toHint(item, hint))
                .toList();

        hintRepository.saveAll(hints);
        return mapFullItem(item);
    }

    @Transactional
    public AdminCourseItemResponse replaceTestCases(Long itemId, ReplaceTestCasesRequest request) {
        CourseItem item = getItemOrThrow(itemId);

        List<CourseItemTestCase> existing = testCaseRepository.findByItemIdOrderByOrderIndexAsc(itemId);
        testCaseRepository.deleteAllInBatch(existing);
        testCaseRepository.flush();

        List<CourseItemTestCase> testCases = request.testCases().stream()
                .map(testCase -> toTestCase(item, testCase))
                .toList();

        testCaseRepository.saveAll(testCases);
        return mapFullItem(item);
    }

    @Transactional
    public AdminCourseItemResponse replaceOptions(Long itemId, ReplaceQuizOptionsRequest request) {
        CourseItem item = getItemOrThrow(itemId);

        List<CourseItemOption> existing = optionRepository.findByItemIdOrderByOrderIndexAsc(itemId);
        optionRepository.deleteAllInBatch(existing);
        optionRepository.flush();

        List<CourseItemOption> options = request.options().stream()
                .map(option -> toOption(item, option))
                .toList();

        optionRepository.saveAll(options);
        return mapFullItem(item);
    }

    private AdminCourseItemResponse mapFullItem(CourseItem item) {
        return mapper.toItemResponse(
                item,
                contentBlockRepository.findByItemIdOrderByOrderIndexAsc(item.getId()),
                hintRepository.findByItemIdOrderByOrderIndexAsc(item.getId()),
                testCaseRepository.findByItemIdOrderByOrderIndexAsc(item.getId()),
                optionRepository.findByItemIdOrderByOrderIndexAsc(item.getId())
        );
    }

    private Course getCourseOrThrow(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));
    }

    private CourseModule getModuleOrThrow(Long moduleId) {
        return moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Course module not found: " + moduleId));
    }

    private CourseItem getItemOrThrow(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Course item not found: " + itemId));
    }

    private void deleteItemChildren(Long itemId) {
        contentBlockRepository.deleteAllInBatch(contentBlockRepository.findByItemIdOrderByOrderIndexAsc(itemId));
        hintRepository.deleteAllInBatch(hintRepository.findByItemIdOrderByOrderIndexAsc(itemId));
        testCaseRepository.deleteAllInBatch(testCaseRepository.findByItemIdOrderByOrderIndexAsc(itemId));
        optionRepository.deleteAllInBatch(optionRepository.findByItemIdOrderByOrderIndexAsc(itemId));
    }

    private CourseItemContentBlock toContentBlock(CourseItem item, ContentBlockRequest request) {
        return CourseItemContentBlock.builder()
                .item(item)
                .blockType(request.blockType())
                .orderIndex(request.orderIndex())
                .title(blankToNull(request.title()))
                .textContent(blankToNull(request.textContent()))
                .url(blankToNull(request.url()))
                .language(blankToNull(request.language()))
                .metadataJson(blankToNull(request.metadataJson()))
                .build();
    }

    private CourseItemHint toHint(CourseItem item, HintRequest request) {
        return CourseItemHint.builder()
                .item(item)
                .orderIndex(request.orderIndex())
                .text(request.text())
                .build();
    }

    private CourseItemTestCase toTestCase(CourseItem item, TestCaseRequest request) {
        return CourseItemTestCase.builder()
                .item(item)
                .testKey(request.testKey())
                .orderIndex(request.orderIndex())
                .visibility(valueOrDefault(request.visibility(), TestVisibility.OPEN))
                .inputData(request.inputData())
                .expectedOutput(request.expectedOutput())
                .build();
    }

    private CourseItemOption toOption(CourseItem item, QuizOptionRequest request) {
        return CourseItemOption.builder()
                .item(item)
                .orderIndex(request.orderIndex())
                .label(blankToNull(request.label()))
                .text(request.text())
                .correct(request.correct())
                .explanation(blankToNull(request.explanation()))
                .build();
    }

    private String normalizeSlug(String slug) {
        if (!hasText(slug)) {
            throw new BadRequestException("Course slug must not be blank");
        }

        return slug.trim().toLowerCase(Locale.ROOT);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String blankToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private <T> T valueOrDefault(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }
}
