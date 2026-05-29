package org.studyplatform.courseservice.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.studyplatform.courseservice.dto.internal.ExecutionPackageResponse;
import org.studyplatform.courseservice.dto.internal.InternalCourseAvailabilityResponse;
import org.studyplatform.courseservice.dto.internal.InternalCourseItemContentResponse;
import org.studyplatform.courseservice.dto.internal.InternalCourseOwnershipResponse;
import org.studyplatform.courseservice.dto.internal.InternalTestCaseResponse;
import org.studyplatform.courseservice.dto.internal.QuizEvaluationPackageResponse;
import org.studyplatform.courseservice.dto.publicapi.ContentBlockResponse;
import org.studyplatform.courseservice.dto.publicapi.CourseItemHintResponse;
import org.studyplatform.courseservice.dto.publicapi.CourseItemLimitsResponse;
import org.studyplatform.courseservice.dto.publicapi.CourseItemOptionResponse;
import org.studyplatform.courseservice.dto.publicapi.EvaluationPolicyResponse;
import org.studyplatform.courseservice.dto.publicapi.ExecutionPolicyResponse;
import org.studyplatform.courseservice.dto.publicapi.OpenTestCaseResponse;
import org.studyplatform.courseservice.entity.Course;
import org.studyplatform.courseservice.entity.CourseItem;
import org.studyplatform.courseservice.entity.CourseItemContentBlock;
import org.studyplatform.courseservice.entity.CourseItemHint;
import org.studyplatform.courseservice.entity.CourseItemOption;
import org.studyplatform.courseservice.entity.CourseItemTestCase;
import org.studyplatform.courseservice.entity.enums.CourseAccessType;
import org.studyplatform.courseservice.entity.enums.CourseItemType;
import org.studyplatform.courseservice.entity.enums.TestVisibility;
import org.studyplatform.courseservice.entity.enums.CourseStatus;
import org.studyplatform.courseservice.exception.BadRequestException;
import org.studyplatform.courseservice.exception.ResourceNotFoundException;
import org.studyplatform.courseservice.repository.CourseItemContentBlockRepository;
import org.studyplatform.courseservice.repository.CourseItemHintRepository;
import org.studyplatform.courseservice.repository.CourseItemOptionRepository;
import org.studyplatform.courseservice.repository.CourseItemRepository;
import org.studyplatform.courseservice.repository.CourseItemTestCaseRepository;
import org.studyplatform.courseservice.repository.CourseRepository;

import java.util.Comparator;
import java.util.List;

@Service
public class CourseInternalService {

    private final CourseRepository courseRepository;
    private final CourseItemRepository itemRepository;
    private final CourseItemContentBlockRepository contentBlockRepository;
    private final CourseItemHintRepository hintRepository;
    private final CourseItemOptionRepository optionRepository;
    private final CourseItemTestCaseRepository testCaseRepository;

    public CourseInternalService(
            CourseRepository courseRepository,
            CourseItemRepository itemRepository,
            CourseItemContentBlockRepository contentBlockRepository,
            CourseItemHintRepository hintRepository,
            CourseItemOptionRepository optionRepository,
            CourseItemTestCaseRepository testCaseRepository
    ) {
        this.courseRepository = courseRepository;
        this.itemRepository = itemRepository;
        this.contentBlockRepository = contentBlockRepository;
        this.hintRepository = hintRepository;
        this.optionRepository = optionRepository;
        this.testCaseRepository = testCaseRepository;
    }

    @Transactional(readOnly = true)
    public ExecutionPackageResponse getExecutionPackage(Long itemId) {
        CourseItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Course item not found: " + itemId));

        Course course = item.getModule().getCourse();
        List<InternalTestCaseResponse> tests = testCaseRepository.findByItemIdOrderByOrderIndexAsc(itemId)
                .stream()
                .map(this::toInternalTestCase)
                .toList();

        return new ExecutionPackageResponse(
                item.getId(),
                item.getModule().getId(),
                course.getId(),
                item.getItemType(),
                item.getTitle(),
                item.getLanguage(),
                item.getStarterCode(),
                new ExecutionPackageResponse.ExecutionLimits(
                        item.getTimeLimitMs(),
                        item.getMemoryLimitMb(),
                        item.getOutputLimitKb()
                ),
                new ExecutionPackageResponse.ExecutionPolicy(
                        item.getNetworkDisabled(),
                        item.getReadOnlyFs()
                ),
                new ExecutionPackageResponse.EvaluationPolicy(
                        item.getComparisonMode(),
                        item.getNormalizeLineEndings(),
                        item.getTrimTrailingWhitespaces()
                ),
                tests
        );
    }

    @Transactional(readOnly = true)
    public QuizEvaluationPackageResponse getQuizEvaluationPackage(Long itemId) {
        CourseItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Course item not found: " + itemId));

        if (item.getItemType() != CourseItemType.QUIZ) {
            throw new BadRequestException("Course item is not a QUIZ: " + itemId);
        }

        Course course = item.getModule().getCourse();
        List<QuizEvaluationPackageResponse.QuizEvaluationOptionResponse> options = optionRepository.findByItemIdOrderByOrderIndexAsc(itemId)
                .stream()
                .map(this::toQuizEvaluationOption)
                .toList();

        return new QuizEvaluationPackageResponse(
                item.getId(),
                item.getModule().getId(),
                course.getId(),
                item.getItemType(),
                item.getTitle(),
                options
        );
    }

    @Transactional(readOnly = true)
    public InternalCourseItemContentResponse getCourseItemContent(Long itemId) {
        CourseItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Course item not found: " + itemId));

        Course course = item.getModule().getCourse();

        List<ContentBlockResponse> contentBlocks = contentBlockRepository.findByItemIdOrderByOrderIndexAsc(itemId)
                .stream()
                .sorted(Comparator.comparing(CourseItemContentBlock::getOrderIndex))
                .map(this::toContentBlock)
                .toList();

        List<OpenTestCaseResponse> openTests = testCaseRepository.findByItemIdOrderByOrderIndexAsc(itemId)
                .stream()
                .filter(testCase -> testCase.getVisibility() == TestVisibility.OPEN)
                .sorted(Comparator.comparing(CourseItemTestCase::getOrderIndex))
                .map(this::toOpenTestCase)
                .toList();

        List<CourseItemHintResponse> hints = hintRepository.findByItemIdOrderByOrderIndexAsc(itemId)
                .stream()
                .sorted(Comparator.comparing(CourseItemHint::getOrderIndex))
                .map(this::toHint)
                .toList();

        List<CourseItemOptionResponse> options = optionRepository.findByItemIdOrderByOrderIndexAsc(itemId)
                .stream()
                .sorted(Comparator.comparing(CourseItemOption::getOrderIndex))
                .map(this::toOption)
                .toList();

        return new InternalCourseItemContentResponse(
                item.getId(),
                item.getModule().getId(),
                course.getId(),
                item.getTitle(),
                item.getItemType(),
                item.getStatement(),
                item.getStarterCode(),
                item.getLanguage(),
                item.getOrderIndex(),
                new CourseItemLimitsResponse(item.getTimeLimitMs(), item.getMemoryLimitMb(), item.getOutputLimitKb()),
                new ExecutionPolicyResponse(item.getNetworkDisabled(), item.getReadOnlyFs()),
                new EvaluationPolicyResponse(item.getComparisonMode(), item.getNormalizeLineEndings(), item.getTrimTrailingWhitespaces()),
                contentBlocks,
                openTests,
                hints,
                options
        );
    }

    @Transactional(readOnly = true)
    public InternalCourseAvailabilityResponse getCourseAvailability(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));

        boolean availableForEnrollment = course.getStatus() == CourseStatus.PUBLISHED
                && Boolean.TRUE.equals(course.getEnrollmentEnabled())
                && course.getAccessType() != CourseAccessType.PRIVATE;

        return new InternalCourseAvailabilityResponse(
                course.getId(),
                course.getStatus(),
                course.getAccessType(),
                course.getEnrollmentEnabled(),
                availableForEnrollment
        );
    }

    @Transactional(readOnly = true)
    public InternalCourseOwnershipResponse getCourseOwnership(Long courseId, Long userId) {
        Long createdByUserId = courseRepository.findCreatedByUserIdById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));

        return new InternalCourseOwnershipResponse(
                courseId,
                userId,
                createdByUserId.equals(userId)
        );
    }

    private ContentBlockResponse toContentBlock(CourseItemContentBlock block) {
        return new ContentBlockResponse(
                block.getId(),
                block.getBlockType(),
                block.getOrderIndex(),
                block.getTitle(),
                block.getTextContent(),
                block.getUrl(),
                block.getLanguage(),
                block.getMetadataJson()
        );
    }

    private OpenTestCaseResponse toOpenTestCase(CourseItemTestCase testCase) {
        return new OpenTestCaseResponse(
                testCase.getTestKey(),
                testCase.getOrderIndex(),
                testCase.getInputData()
        );
    }

    private CourseItemHintResponse toHint(CourseItemHint hint) {
        return new CourseItemHintResponse(
                hint.getOrderIndex(),
                hint.getText()
        );
    }

    private CourseItemOptionResponse toOption(CourseItemOption option) {
        return new CourseItemOptionResponse(
                option.getId(),
                option.getOrderIndex(),
                option.getLabel(),
                option.getText()
        );
    }

    private QuizEvaluationPackageResponse.QuizEvaluationOptionResponse toQuizEvaluationOption(CourseItemOption option) {
        return new QuizEvaluationPackageResponse.QuizEvaluationOptionResponse(
                option.getId(),
                option.getOrderIndex(),
                option.getLabel(),
                option.getText(),
                option.getCorrect(),
                option.getExplanation()
        );
    }

    private InternalTestCaseResponse toInternalTestCase(CourseItemTestCase testCase) {
        return new InternalTestCaseResponse(
                testCase.getTestKey(),
                testCase.getVisibility(),
                testCase.getInputData(),
                testCase.getExpectedOutput(),
                testCase.getOrderIndex()
        );
    }
}
