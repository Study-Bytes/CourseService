package org.studyplatform.courseservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.studyplatform.courseservice.dto.admin.AdminCourseItemResponse;
import org.studyplatform.courseservice.dto.admin.AdminCoursePageResponse;
import org.studyplatform.courseservice.dto.admin.AdminCourseResponse;
import org.studyplatform.courseservice.dto.admin.AdminCourseSummaryResponse;
import org.studyplatform.courseservice.dto.admin.AdminModuleResponse;
import org.studyplatform.courseservice.dto.admin.ContentBlockRequest;
import org.studyplatform.courseservice.dto.admin.CourseModerationReviewRequest;
import org.studyplatform.courseservice.dto.admin.CreateCourseItemRequest;
import org.studyplatform.courseservice.dto.admin.CreateCourseRequest;
import org.studyplatform.courseservice.dto.admin.CreateModuleRequest;
import org.studyplatform.courseservice.dto.admin.HintRequest;
import org.studyplatform.courseservice.dto.admin.QuizOptionRequest;
import org.studyplatform.courseservice.dto.admin.ReorderItemsRequest;
import org.studyplatform.courseservice.dto.admin.ReorderModulesRequest;
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
import org.studyplatform.courseservice.security.CurrentUserService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseAdminService {

    private static final int MAX_ADMIN_PAGE_SIZE = 100;

    private final CourseRepository courseRepository;
    private final CourseModuleRepository moduleRepository;
    private final CourseItemRepository itemRepository;
    private final CourseItemContentBlockRepository contentBlockRepository;
    private final CourseItemHintRepository hintRepository;
    private final CourseItemTestCaseRepository testCaseRepository;
    private final CourseItemOptionRepository optionRepository;
    private final AdminCourseMapper mapper;
    private final CurrentUserService currentUserService;

    @Transactional
    @PreAuthorize("@courseAdminAuthorizationService.canCreateCourseFor(#request.createdByUserId())")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public AdminCoursePageResponse listCourses(
            CourseStatus status,
            CourseDifficulty difficulty,
            CourseAccessType accessType,
            Long createdByUserId,
            int page,
            int size
    ) {
        validateAdminPagination(page, size);

        Specification<Course> specification = buildCourseListSpecification(status, difficulty, accessType, createdByUserId);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        return toCoursePageResponse(specification, pageRequest);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public AdminCoursePageResponse listModerationQueue(
            CourseDifficulty difficulty,
            CourseAccessType accessType,
            Long createdByUserId,
            int page,
            int size
    ) {
        validateAdminPagination(page, size);

        Specification<Course> specification = buildCourseListSpecification(
                CourseStatus.PENDING_REVIEW,
                difficulty,
                accessType,
                createdByUserId
        );
        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.asc("submittedForReviewAt").nullsLast(),
                        Sort.Order.asc("createdAt"),
                        Sort.Order.asc("id")
                )
        );

        return toCoursePageResponse(specification, pageRequest);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@courseAdminAuthorizationService.canManageCourse(#courseId)")
    public AdminCourseResponse getCourse(Long courseId) {
        Course course = getCourseOrThrow(courseId);
        List<CourseModule> modules = moduleRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
        List<CourseItem> items = modules.stream()
                .flatMap(module -> itemRepository.findByModuleIdOrderByOrderIndexAsc(module.getId()).stream())
                .toList();

        return mapper.toCourseResponse(course, modules, items);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public AdminCourseResponse getCourseReview(Long courseId) {
        return getCourse(courseId);
    }

    @Transactional
    @PreAuthorize("@courseAdminAuthorizationService.canManageCourse(#courseId)")
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
    @PreAuthorize("@courseAdminAuthorizationService.canManageCourse(#courseId)")
    public AdminCourseResponse submitCourseForReview(Long courseId) {
        Course course = getCourseOrThrow(courseId);
        assertCourseStatus(
                course,
                Set.of(CourseStatus.DRAFT, CourseStatus.CHANGES_REQUESTED),
                CourseStatus.PENDING_REVIEW,
                "submit course for review"
        );
        validateCourseForPublish(course);

        course.setStatus(CourseStatus.PENDING_REVIEW);
        course.setSubmittedForReviewAt(Instant.now());
        course.setReviewedAt(null);
        course.setReviewedByUserId(null);
        course.setReviewComment(null);

        courseRepository.save(course);
        return getCourse(courseId);
    }

    @Transactional
    @PreAuthorize("@courseAdminAuthorizationService.canManageCourse(#courseId)")
    public AdminCourseResponse publishCourse(Long courseId) {
        Course course = getCourseOrThrow(courseId);
        validateCourseForPublish(course);

        course.setStatus(CourseStatus.PUBLISHED);

        if (course.getPublishedAt() == null) {
            course.setPublishedAt(Instant.now());
        }

        courseRepository.save(course);
        return getCourse(courseId);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public AdminCourseResponse approveCourse(Long courseId) {
        Course course = getCourseOrThrow(courseId);
        assertCourseStatus(
                course,
                Set.of(CourseStatus.PENDING_REVIEW),
                CourseStatus.PUBLISHED,
                "approve course"
        );
        validateCourseForPublish(course);

        Instant now = Instant.now();
        course.setStatus(CourseStatus.PUBLISHED);
        course.setPublishedAt(now);
        course.setReviewedAt(now);
        course.setReviewedByUserId(getCurrentAdminUserId());
        course.setReviewComment(null);

        courseRepository.save(course);
        return getCourse(courseId);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public AdminCourseResponse rejectCourse(Long courseId, CourseModerationReviewRequest request) {
        Course course = getCourseOrThrow(courseId);
        assertCourseStatus(
                course,
                Set.of(CourseStatus.PENDING_REVIEW),
                CourseStatus.CHANGES_REQUESTED,
                "reject course"
        );

        Instant now = Instant.now();
        course.setStatus(CourseStatus.CHANGES_REQUESTED);
        course.setReviewedAt(now);
        course.setReviewedByUserId(getCurrentAdminUserId());
        course.setReviewComment(request.reviewComment().trim());

        courseRepository.save(course);
        return getCourse(courseId);
    }

    @Transactional
    @PreAuthorize("@courseAdminAuthorizationService.canManageCourse(#courseId)")
    public AdminCourseResponse archiveCourse(Long courseId) {
        Course course = getCourseOrThrow(courseId);
        course.setStatus(CourseStatus.ARCHIVED);
        courseRepository.save(course);
        return getCourse(courseId);
    }

    @Transactional
    @PreAuthorize("@courseAdminAuthorizationService.canManageCourse(#courseId)")
    public AdminModuleResponse createModule(Long courseId, CreateModuleRequest request) {
        Course course = getCourseOrThrow(courseId);
        assertOrderIndexAvailableForModule(courseId, request.orderIndex(), null);

        CourseModule module = CourseModule.builder()
                .course(course)
                .title(request.title().trim())
                .description(blankToNull(request.description()))
                .orderIndex(request.orderIndex())
                .deadlineAt(request.deadlineAt())
                .build();

        CourseModule saved = moduleRepository.save(module);
        return mapper.toModuleResponse(saved, List.of());
    }

    @Transactional
    @PreAuthorize("@courseAdminAuthorizationService.canManageCourse(#courseId)")
    public AdminCourseResponse reorderModules(Long courseId, ReorderModulesRequest request) {
        getCourseOrThrow(courseId);
        List<CourseModule> modules = moduleRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
        validateReorderIds("orderedModuleIds", request.orderedModuleIds(), modules.stream().map(CourseModule::getId).toList());

        Map<Long, CourseModule> modulesById = modules.stream()
                .collect(Collectors.toMap(CourseModule::getId, Function.identity()));

        for (int i = 0; i < modules.size(); i++) {
            modules.get(i).setOrderIndex(-i - 1);
        }
        moduleRepository.saveAllAndFlush(modules);

        for (int i = 0; i < request.orderedModuleIds().size(); i++) {
            modulesById.get(request.orderedModuleIds().get(i)).setOrderIndex(i);
        }
        moduleRepository.saveAllAndFlush(modules);

        return getCourse(courseId);
    }

    @Transactional
    @PreAuthorize("@courseAdminAuthorizationService.canManageModule(#moduleId)")
    public AdminModuleResponse updateModule(Long moduleId, UpdateModuleRequest request) {
        CourseModule module = getModuleOrThrow(moduleId);

        if (hasText(request.title())) {
            module.setTitle(request.title().trim());
        }

        if (request.description() != null) {
            module.setDescription(blankToNull(request.description()));
        }

        if (request.orderIndex() != null) {
            assertOrderIndexAvailableForModule(module.getCourse().getId(), request.orderIndex(), moduleId);
            module.setOrderIndex(request.orderIndex());
        }

        if (request.deadlineAtProvided()) {
            module.setDeadlineAt(request.deadlineAt());
        }

        CourseModule saved = moduleRepository.save(module);
        List<CourseItem> items = itemRepository.findByModuleIdOrderByOrderIndexAsc(saved.getId());
        return mapper.toModuleResponse(saved, items);
    }

    @Transactional
    @PreAuthorize("@courseAdminAuthorizationService.canManageModule(#moduleId)")
    public AdminModuleResponse reorderItems(Long moduleId, ReorderItemsRequest request) {
        CourseModule module = getModuleOrThrow(moduleId);
        List<CourseItem> items = itemRepository.findByModuleIdOrderByOrderIndexAsc(moduleId);
        validateReorderIds("orderedItemIds", request.orderedItemIds(), items.stream().map(CourseItem::getId).toList());

        Map<Long, CourseItem> itemsById = items.stream()
                .collect(Collectors.toMap(CourseItem::getId, Function.identity()));

        for (int i = 0; i < items.size(); i++) {
            items.get(i).setOrderIndex(-i - 1);
        }
        itemRepository.saveAllAndFlush(items);

        for (int i = 0; i < request.orderedItemIds().size(); i++) {
            itemsById.get(request.orderedItemIds().get(i)).setOrderIndex(i);
        }
        itemRepository.saveAllAndFlush(items);

        List<CourseItem> reorderedItems = itemRepository.findByModuleIdOrderByOrderIndexAsc(moduleId);
        return mapper.toModuleResponse(module, reorderedItems);
    }

    @Transactional
    @PreAuthorize("@courseAdminAuthorizationService.canManageModule(#moduleId)")
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
    @PreAuthorize("@courseAdminAuthorizationService.canManageModule(#moduleId)")
    public AdminCourseItemResponse createItem(Long moduleId, CreateCourseItemRequest request) {
        CourseModule module = getModuleOrThrow(moduleId);
        CourseItemType itemType = valueOrDefault(request.itemType(), CourseItemType.CODING);
        assertOrderIndexAvailableForItem(moduleId, request.orderIndex(), null);

        CourseItem item = CourseItem.builder()
                .module(module)
                .title(request.title().trim())
                .itemType(itemType)
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

        validateItemForSave(item, List.of(), List.of(), List.of());

        CourseItem saved = itemRepository.save(item);
        return mapper.toItemResponse(saved);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@courseAdminAuthorizationService.canManageItem(#itemId)")
    public AdminCourseItemResponse getItem(Long itemId) {
        CourseItem item = getItemOrThrow(itemId);
        return mapFullItem(item);
    }

    @Transactional
    @PreAuthorize("@courseAdminAuthorizationService.canManageItem(#itemId)")
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
            assertOrderIndexAvailableForItem(item.getModule().getId(), request.orderIndex(), itemId);
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

        validateItemForSave(
                item,
                contentBlockRepository.findByItemIdOrderByOrderIndexAsc(itemId),
                testCaseRepository.findByItemIdOrderByOrderIndexAsc(itemId),
                optionRepository.findByItemIdOrderByOrderIndexAsc(itemId)
        );

        CourseItem saved = itemRepository.save(item);
        return mapFullItem(saved);
    }

    @Transactional
    @PreAuthorize("@courseAdminAuthorizationService.canManageItem(#itemId)")
    public void deleteItem(Long itemId) {
        CourseItem item = getItemOrThrow(itemId);
        deleteItemChildren(itemId);
        itemRepository.delete(item);
    }

    @Transactional
    @PreAuthorize("@courseAdminAuthorizationService.canManageItem(#itemId)")
    public AdminCourseItemResponse replaceContentBlocks(Long itemId, ReplaceContentBlocksRequest request) {
        CourseItem item = getItemOrThrow(itemId);
        validateUniqueOrderIndexes("contentBlocks", request.contentBlocks().stream().map(ContentBlockRequest::orderIndex).toList());

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
    @PreAuthorize("@courseAdminAuthorizationService.canManageItem(#itemId)")
    public AdminCourseItemResponse replaceHints(Long itemId, ReplaceHintsRequest request) {
        CourseItem item = getItemOrThrow(itemId);
        validateUniqueOrderIndexes("hints", request.hints().stream().map(HintRequest::orderIndex).toList());

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
    @PreAuthorize("@courseAdminAuthorizationService.canManageItem(#itemId)")
    public AdminCourseItemResponse replaceTestCases(Long itemId, ReplaceTestCasesRequest request) {
        CourseItem item = getItemOrThrow(itemId);
        validateTestCasesAllowed(item);
        validateUniqueOrderIndexes("testCases", request.testCases().stream().map(TestCaseRequest::orderIndex).toList());
        validateUniqueValues("testCases.testKey", request.testCases().stream().map(TestCaseRequest::testKey).toList());

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
    @PreAuthorize("@courseAdminAuthorizationService.canManageItem(#itemId)")
    public AdminCourseItemResponse replaceOptions(Long itemId, ReplaceQuizOptionsRequest request) {
        CourseItem item = getItemOrThrow(itemId);
        validateOptionsAllowed(item);
        validateUniqueOrderIndexes("options", request.options().stream().map(QuizOptionRequest::orderIndex).toList());

        List<CourseItemOption> existing = optionRepository.findByItemIdOrderByOrderIndexAsc(itemId);
        optionRepository.deleteAllInBatch(existing);
        optionRepository.flush();

        List<CourseItemOption> options = request.options().stream()
                .map(option -> toOption(item, option))
                .toList();

        optionRepository.saveAll(options);
        return mapFullItem(item);
    }

    private AdminCourseSummaryResponse toCourseSummaryResponse(Course course) {
        return new AdminCourseSummaryResponse(
                course.getId(),
                course.getSlug(),
                course.getTitle(),
                course.getShortDescription(),
                course.getDifficulty(),
                course.getStatus(),
                course.getAccessType(),
                course.getEnrollmentEnabled(),
                course.getCoverImageUrl(),
                course.getEstimatedMinutes(),
                course.getCreatedByUserId(),
                course.getCreatedAt(),
                course.getUpdatedAt(),
                course.getPublishedAt(),
                course.getSubmittedForReviewAt(),
                course.getReviewedAt(),
                course.getReviewedByUserId(),
                course.getReviewComment()
        );
    }

    private void validateAdminPagination(int page, int size) {
        if (page < 0) {
            throw new BadRequestException("page must be greater than or equal to 0");
        }

        if (size <= 0 || size > MAX_ADMIN_PAGE_SIZE) {
            throw new BadRequestException("size must be between 1 and " + MAX_ADMIN_PAGE_SIZE);
        }
    }

    private Specification<Course> buildCourseListSpecification(
            CourseStatus status,
            CourseDifficulty difficulty,
            CourseAccessType accessType,
            Long createdByUserId
    ) {
        Specification<Course> specification = (root, query, builder) -> builder.conjunction();

        if (status != null) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("status"), status));
        }

        if (difficulty != null) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("difficulty"), difficulty));
        }

        if (accessType != null) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("accessType"), accessType));
        }

        Long effectiveCreatedByUserId = createdByUserId;
        if (!currentUserService.isAdmin()) {
            effectiveCreatedByUserId = currentUserService.getCurrentUserId()
                    .orElseThrow(() -> new BadRequestException("Current teacher user id is missing"));
        }

        if (effectiveCreatedByUserId != null) {
            Long ownerId = effectiveCreatedByUserId;
            specification = specification.and((root, query, builder) -> builder.equal(root.get("createdByUserId"), ownerId));
        }

        return specification;
    }

    private AdminCoursePageResponse toCoursePageResponse(Specification<Course> specification, PageRequest pageRequest) {
        Page<Course> coursePage = courseRepository.findAll(specification, pageRequest);

        return new AdminCoursePageResponse(
                coursePage.getContent().stream()
                        .map(this::toCourseSummaryResponse)
                        .toList(),
                coursePage.getNumber(),
                coursePage.getSize(),
                coursePage.getTotalElements(),
                coursePage.getTotalPages()
        );
    }

    private void assertCourseStatus(
            Course course,
            Set<CourseStatus> allowedStatuses,
            CourseStatus targetStatus,
            String action
    ) {
        if (!allowedStatuses.contains(course.getStatus())) {
            throw new ConflictException(
                    "Cannot " + action + " from status " + course.getStatus() + " to " + targetStatus
            );
        }
    }

    private Long getCurrentAdminUserId() {
        return currentUserService.getCurrentUserId()
                .orElseThrow(() -> new BadRequestException("Current admin user id is missing"));
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

    private void validateCourseForPublish(Course course) {
        List<String> errors = new ArrayList<>();

        if (!hasText(course.getTitle())) {
            errors.add("Course title must not be blank");
        }

        if (!hasText(course.getSlug())) {
            errors.add("Course slug must not be blank");
        }

        List<CourseModule> modules = moduleRepository.findByCourseIdOrderByOrderIndexAsc(course.getId());
        if (modules.isEmpty()) {
            errors.add("Course must contain at least one module");
        }

        errors.addAll(validateExistingOrderIndexes("modules", modules.stream().map(CourseModule::getOrderIndex).toList()));

        for (CourseModule module : modules) {
            if (!hasText(module.getTitle())) {
                errors.add("Module id=" + module.getId() + " must have a title");
            }

            List<CourseItem> items = itemRepository.findByModuleIdOrderByOrderIndexAsc(module.getId());
            if (items.isEmpty()) {
                errors.add("Module '" + displayName(module.getTitle(), module.getId()) + "' must contain at least one course item");
            }

            errors.addAll(validateExistingOrderIndexes(
                    "items in module '" + displayName(module.getTitle(), module.getId()) + "'",
                    items.stream().map(CourseItem::getOrderIndex).toList()
            ));

            for (CourseItem item : items) {
                List<CourseItemContentBlock> blocks = contentBlockRepository.findByItemIdOrderByOrderIndexAsc(item.getId());
                List<CourseItemTestCase> testCases = testCaseRepository.findByItemIdOrderByOrderIndexAsc(item.getId());
                List<CourseItemOption> options = optionRepository.findByItemIdOrderByOrderIndexAsc(item.getId());

                errors.addAll(validateExistingOrderIndexes(
                        "content blocks in item '" + displayName(item.getTitle(), item.getId()) + "'",
                        blocks.stream().map(CourseItemContentBlock::getOrderIndex).toList()
                ));
                errors.addAll(validateExistingOrderIndexes(
                        "test cases in item '" + displayName(item.getTitle(), item.getId()) + "'",
                        testCases.stream().map(CourseItemTestCase::getOrderIndex).toList()
                ));
                errors.addAll(validateExistingOrderIndexes(
                        "quiz options in item '" + displayName(item.getTitle(), item.getId()) + "'",
                        options.stream().map(CourseItemOption::getOrderIndex).toList()
                ));
                errors.addAll(validateExistingUniqueValues(
                        "test case keys in item '" + displayName(item.getTitle(), item.getId()) + "'",
                        testCases.stream().map(CourseItemTestCase::getTestKey).toList()
                ));

                errors.addAll(validateItemForPublish(item, blocks, testCases, options));
            }
        }

        if (!errors.isEmpty()) {
            throw new BadRequestException("Course cannot be published: " + String.join("; ", errors));
        }
    }

    private void validateItemForSave(
            CourseItem item,
            List<CourseItemContentBlock> contentBlocks,
            List<CourseItemTestCase> testCases,
            List<CourseItemOption> options
    ) {
        List<String> errors = validateItemBaseStructure(item, contentBlocks, testCases, options, false);
        if (!errors.isEmpty()) {
            throw new BadRequestException(String.join("; ", errors));
        }
    }

    private List<String> validateItemForPublish(
            CourseItem item,
            List<CourseItemContentBlock> contentBlocks,
            List<CourseItemTestCase> testCases,
            List<CourseItemOption> options
    ) {
        return validateItemBaseStructure(item, contentBlocks, testCases, options, true);
    }

    private List<String> validateItemBaseStructure(
            CourseItem item,
            List<CourseItemContentBlock> contentBlocks,
            List<CourseItemTestCase> testCases,
            List<CourseItemOption> options,
            boolean publishValidation
    ) {
        List<String> errors = new ArrayList<>();
        String itemName = displayName(item.getTitle(), item.getId());
        CourseItemType itemType = item.getItemType();

        if (itemType == null) {
            errors.add("Course item '" + itemName + "' must have itemType");
            return errors;
        }

        if (!hasText(item.getTitle())) {
            errors.add("Course item id=" + item.getId() + " must have a title");
        }

        if (item.getOrderIndex() == null || item.getOrderIndex() < 0) {
            errors.add("Course item '" + itemName + "' must have non-negative orderIndex");
        }

        if (isExecutable(itemType)) {
            if (!hasText(item.getLanguage())) {
                errors.add(itemType + " item '" + itemName + "' must have non-blank language");
            }

            if (publishValidation) {
                if (!isPositive(item.getTimeLimitMs())) {
                    errors.add(itemType + " item '" + itemName + "' must have positive timeLimitMs");
                }
                if (!isPositive(item.getMemoryLimitMb())) {
                    errors.add(itemType + " item '" + itemName + "' must have positive memoryLimitMb");
                }
                if (!isPositive(item.getOutputLimitKb())) {
                    errors.add(itemType + " item '" + itemName + "' must have positive outputLimitKb");
                }
            }

            if (!options.isEmpty()) {
                errors.add(itemType + " item '" + itemName + "' must not have quiz options");
            }
        }

        if (itemType == CourseItemType.QUIZ) {
            if (!testCases.isEmpty()) {
                errors.add("QUIZ item '" + itemName + "' must not have test cases");
            }

            if (publishValidation) {
                if (options.isEmpty()) {
                    errors.add("QUIZ item '" + itemName + "' must contain at least one option");
                }

                boolean hasCorrectOption = options.stream().anyMatch(option -> Boolean.TRUE.equals(option.getCorrect()));
                if (!hasCorrectOption) {
                    errors.add("QUIZ item '" + itemName + "' must contain at least one correct option");
                }
            }
        }

        if (itemType == CourseItemType.THEORY || itemType == CourseItemType.FILE) {
            if (!testCases.isEmpty()) {
                errors.add(itemType + " item '" + itemName + "' must not have test cases");
            }

            if (!options.isEmpty()) {
                errors.add(itemType + " item '" + itemName + "' must not have quiz options");
            }

            if (publishValidation && !hasVisibleContent(item, contentBlocks)) {
                errors.add(itemType + " item '" + itemName + "' must have statement or content blocks before publishing");
            }
        }

        return errors;
    }

    private void validateTestCasesAllowed(CourseItem item) {
        if (!isExecutable(item.getItemType())) {
            throw new BadRequestException(item.getItemType() + " item cannot have test cases");
        }
    }

    private void validateOptionsAllowed(CourseItem item) {
        if (item.getItemType() != CourseItemType.QUIZ) {
            throw new BadRequestException(item.getItemType() + " item cannot have quiz options");
        }
    }

    private void assertOrderIndexAvailableForModule(Long courseId, Integer orderIndex, Long currentModuleId) {
        List<CourseModule> modules = moduleRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
        boolean duplicate = modules.stream()
                .filter(module -> !Objects.equals(module.getId(), currentModuleId))
                .anyMatch(module -> Objects.equals(module.getOrderIndex(), orderIndex));

        if (duplicate) {
            throw new BadRequestException("Duplicate module orderIndex inside course: " + orderIndex);
        }
    }

    private void assertOrderIndexAvailableForItem(Long moduleId, Integer orderIndex, Long currentItemId) {
        List<CourseItem> items = itemRepository.findByModuleIdOrderByOrderIndexAsc(moduleId);
        boolean duplicate = items.stream()
                .filter(item -> !Objects.equals(item.getId(), currentItemId))
                .anyMatch(item -> Objects.equals(item.getOrderIndex(), orderIndex));

        if (duplicate) {
            throw new BadRequestException("Duplicate course item orderIndex inside module: " + orderIndex);
        }
    }

    private void validateReorderIds(String fieldName, List<Long> requestedIds, List<Long> existingIds) {
        if (requestedIds == null || requestedIds.isEmpty()) {
            throw new BadRequestException(fieldName + " must not be empty");
        }

        if (requestedIds.stream().anyMatch(Objects::isNull)) {
            throw new BadRequestException(fieldName + " must not contain null values");
        }

        Set<Long> requestedUniqueIds = new LinkedHashSet<>(requestedIds);
        if (requestedUniqueIds.size() != requestedIds.size()) {
            throw new BadRequestException(fieldName + " must not contain duplicate IDs");
        }

        Set<Long> existingIdSet = new LinkedHashSet<>(existingIds);
        Set<Long> missingIds = new LinkedHashSet<>(existingIdSet);
        missingIds.removeAll(requestedUniqueIds);

        Set<Long> foreignIds = new LinkedHashSet<>(requestedUniqueIds);
        foreignIds.removeAll(existingIdSet);

        if (!missingIds.isEmpty()) {
            throw new BadRequestException(fieldName + " is missing existing IDs: " + missingIds);
        }

        if (!foreignIds.isEmpty()) {
            throw new BadRequestException(fieldName + " contains foreign IDs: " + foreignIds);
        }
    }

    private void validateUniqueOrderIndexes(String fieldName, List<Integer> orderIndexes) {
        List<String> errors = validateExistingOrderIndexes(fieldName, orderIndexes);
        if (!errors.isEmpty()) {
            throw new BadRequestException(String.join("; ", errors));
        }
    }

    private List<String> validateExistingOrderIndexes(String fieldName, List<Integer> orderIndexes) {
        List<String> errors = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        Set<Integer> duplicates = new LinkedHashSet<>();

        for (Integer orderIndex : orderIndexes) {
            if (orderIndex == null) {
                errors.add(fieldName + " must not contain null orderIndex");
                continue;
            }

            if (orderIndex < 0) {
                errors.add(fieldName + " must not contain negative orderIndex: " + orderIndex);
            }

            if (!seen.add(orderIndex)) {
                duplicates.add(orderIndex);
            }
        }

        if (!duplicates.isEmpty()) {
            errors.add(fieldName + " contains duplicate orderIndex values: " + duplicates);
        }

        return errors;
    }

    private void validateUniqueValues(String fieldName, List<String> values) {
        List<String> errors = validateExistingUniqueValues(fieldName, values);
        if (!errors.isEmpty()) {
            throw new BadRequestException(String.join("; ", errors));
        }
    }

    private List<String> validateExistingUniqueValues(String fieldName, List<String> values) {
        Set<String> seen = new HashSet<>();
        Set<String> duplicates = new LinkedHashSet<>();

        for (String value : values) {
            if (value == null) {
                continue;
            }

            String normalized = value.trim().toLowerCase(Locale.ROOT);
            if (!seen.add(normalized)) {
                duplicates.add(value);
            }
        }

        if (duplicates.isEmpty()) {
            return List.of();
        }

        return List.of(fieldName + " contains duplicate values: " + duplicates);
    }

    private boolean isExecutable(CourseItemType itemType) {
        return itemType == CourseItemType.CODING || itemType == CourseItemType.SQL;
    }

    private boolean hasVisibleContent(CourseItem item, List<CourseItemContentBlock> contentBlocks) {
        return hasText(item.getStatement()) || !contentBlocks.isEmpty();
    }

    private boolean isPositive(Integer value) {
        return value != null && value > 0;
    }

    private String displayName(String title, Long id) {
        return hasText(title) ? title.trim() : "id=" + id;
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
