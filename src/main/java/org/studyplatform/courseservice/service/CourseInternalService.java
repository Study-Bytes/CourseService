package org.studyplatform.courseservice.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.studyplatform.courseservice.dto.internal.ExecutionPackageResponse;
import org.studyplatform.courseservice.dto.internal.InternalCourseAvailabilityResponse;
import org.studyplatform.courseservice.dto.internal.InternalTestCaseResponse;
import org.studyplatform.courseservice.entity.Course;
import org.studyplatform.courseservice.entity.CourseItem;
import org.studyplatform.courseservice.entity.CourseItemTestCase;
import org.studyplatform.courseservice.entity.enums.CourseAccessType;
import org.studyplatform.courseservice.entity.enums.CourseStatus;
import org.studyplatform.courseservice.exception.ResourceNotFoundException;
import org.studyplatform.courseservice.repository.CourseItemRepository;
import org.studyplatform.courseservice.repository.CourseItemTestCaseRepository;
import org.studyplatform.courseservice.repository.CourseRepository;

import java.util.List;

@Service
public class CourseInternalService {

    private final CourseRepository courseRepository;
    private final CourseItemRepository itemRepository;
    private final CourseItemTestCaseRepository testCaseRepository;

    public CourseInternalService(
            CourseRepository courseRepository,
            CourseItemRepository itemRepository,
            CourseItemTestCaseRepository testCaseRepository
    ) {
        this.courseRepository = courseRepository;
        this.itemRepository = itemRepository;
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
