package org.studyplatform.courseservice.mapper;

import org.springframework.stereotype.Component;
import org.studyplatform.courseservice.dto.publicapi.CourseCatalogItemResponse;
import org.studyplatform.courseservice.dto.publicapi.CourseDetailsResponse;
import org.studyplatform.courseservice.dto.publicapi.CourseItemDetailsResponse;
import org.studyplatform.courseservice.dto.publicapi.CourseItemHintResponse;
import org.studyplatform.courseservice.dto.publicapi.CourseItemLimitsResponse;
import org.studyplatform.courseservice.dto.publicapi.CourseItemSummaryResponse;
import org.studyplatform.courseservice.dto.publicapi.EvaluationPolicyResponse;
import org.studyplatform.courseservice.dto.publicapi.ExecutionPolicyResponse;
import org.studyplatform.courseservice.dto.publicapi.ModuleSummaryResponse;
import org.studyplatform.courseservice.dto.publicapi.OpenTestCaseResponse;
import org.studyplatform.courseservice.entity.Course;
import org.studyplatform.courseservice.entity.CourseItem;
import org.studyplatform.courseservice.entity.CourseItemHint;
import org.studyplatform.courseservice.entity.CourseItemTestCase;
import org.studyplatform.courseservice.entity.CourseModule;
import org.studyplatform.courseservice.entity.enums.TestVisibility;

import java.util.Comparator;
import java.util.List;

@Component
public class PublicCourseMapper {

    public CourseCatalogItemResponse toCatalogItem(Course course) {
        return new CourseCatalogItemResponse(
                course.getId(),
                course.getSlug(),
                course.getTitle(),
                course.getShortDescription(),
                course.getDifficulty(),
                course.getStatus(),
                course.getCoverImageUrl(),
                course.getEstimatedMinutes()
        );
    }

    public CourseDetailsResponse toCourseDetails(
            Course course,
            List<CourseModule> modules,
            List<CourseItem> items
    ) {
        List<ModuleSummaryResponse> moduleResponses = modules.stream()
                .sorted(Comparator.comparing(CourseModule::getOrderIndex))
                .map(module -> toModuleSummary(
                        module,
                        items.stream()
                                .filter(item -> item.getModule().getId().equals(module.getId()))
                                .toList()
                ))
                .toList();

        return new CourseDetailsResponse(
                course.getId(),
                course.getSlug(),
                course.getTitle(),
                course.getShortDescription(),
                course.getDescription(),
                course.getDifficulty(),
                course.getStatus(),
                course.getCoverImageUrl(),
                course.getEstimatedMinutes(),
                course.getPublishedAt(),
                moduleResponses
        );
    }

    public ModuleSummaryResponse toModuleSummary(
            CourseModule module,
            List<CourseItem> items
    ) {
        List<CourseItemSummaryResponse> itemResponses = items.stream()
                .sorted(Comparator.comparing(CourseItem::getOrderIndex))
                .map(this::toItemSummary)
                .toList();

        return new ModuleSummaryResponse(
                module.getId(),
                module.getTitle(),
                module.getDescription(),
                module.getOrderIndex(),
                itemResponses
        );
    }

    public CourseItemSummaryResponse toItemSummary(CourseItem item) {
        return new CourseItemSummaryResponse(
                item.getId(),
                item.getTitle(),
                item.getItemType(),
                item.getLanguage(),
                item.getOrderIndex()
        );
    }

    public CourseItemDetailsResponse toItemDetails(
            CourseItem item,
            List<CourseItemTestCase> testCases,
            List<CourseItemHint> hints
    ) {
        List<OpenTestCaseResponse> openTests = testCases.stream()
                .filter(testCase -> testCase.getVisibility() == TestVisibility.OPEN)
                .sorted(Comparator.comparing(CourseItemTestCase::getOrderIndex))
                .map(this::toOpenTestCase)
                .toList();

        List<CourseItemHintResponse> hintResponses = hints.stream()
                .sorted(Comparator.comparing(CourseItemHint::getOrderIndex))
                .map(this::toItemHint)
                .toList();

        return new CourseItemDetailsResponse(
                item.getId(),
                item.getModule().getId(),
                item.getTitle(),
                item.getItemType(),
                item.getStatement(),
                item.getStarterCode(),
                item.getLanguage(),
                item.getOrderIndex(),
                toItemLimits(item),
                toExecutionPolicy(item),
                toEvaluationPolicy(item),
                openTests,
                hintResponses
        );
    }

    public CourseItemLimitsResponse toItemLimits(CourseItem item) {
        return new CourseItemLimitsResponse(
                item.getTimeLimitMs(),
                item.getMemoryLimitMb(),
                item.getOutputLimitKb()
        );
    }

    public ExecutionPolicyResponse toExecutionPolicy(CourseItem item) {
        return new ExecutionPolicyResponse(
                item.getNetworkDisabled(),
                item.getReadOnlyFs()
        );
    }

    public EvaluationPolicyResponse toEvaluationPolicy(CourseItem item) {
        return new EvaluationPolicyResponse(
                item.getComparisonMode(),
                item.getNormalizeLineEndings(),
                item.getTrimTrailingWhitespaces()
        );
    }

    public OpenTestCaseResponse toOpenTestCase(CourseItemTestCase testCase) {
        return new OpenTestCaseResponse(
                testCase.getTestKey(),
                testCase.getOrderIndex(),
                testCase.getInputData()
        );
    }

    public CourseItemHintResponse toItemHint(CourseItemHint hint) {
        return new CourseItemHintResponse(
                hint.getOrderIndex(),
                hint.getText()
        );
    }
}