package org.studyplatform.courseservice.mapper;

import org.springframework.stereotype.Component;
import org.studyplatform.courseservice.dto.admin.AdminContentBlockResponse;
import org.studyplatform.courseservice.dto.admin.AdminCourseItemResponse;
import org.studyplatform.courseservice.dto.admin.AdminCourseResponse;
import org.studyplatform.courseservice.dto.admin.AdminHintResponse;
import org.studyplatform.courseservice.dto.admin.AdminModuleResponse;
import org.studyplatform.courseservice.dto.admin.AdminQuizOptionResponse;
import org.studyplatform.courseservice.dto.admin.AdminTestCaseResponse;
import org.studyplatform.courseservice.entity.Course;
import org.studyplatform.courseservice.entity.CourseItem;
import org.studyplatform.courseservice.entity.CourseItemContentBlock;
import org.studyplatform.courseservice.entity.CourseItemHint;
import org.studyplatform.courseservice.entity.CourseItemOption;
import org.studyplatform.courseservice.entity.CourseItemTestCase;
import org.studyplatform.courseservice.entity.CourseModule;

import java.util.Comparator;
import java.util.List;

@Component
public class AdminCourseMapper {

    public AdminCourseResponse toCourseResponse(
            Course course,
            List<CourseModule> modules,
            List<CourseItem> items
    ) {
        List<AdminModuleResponse> moduleResponses = modules.stream()
                .sorted(Comparator.comparing(CourseModule::getOrderIndex))
                .map(module -> toModuleResponse(
                        module,
                        items.stream()
                                .filter(item -> item.getModule().getId().equals(module.getId()))
                                .toList()
                ))
                .toList();

        return new AdminCourseResponse(
                course.getId(),
                course.getSlug(),
                course.getTitle(),
                course.getShortDescription(),
                course.getDescription(),
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
                course.getReviewComment(),
                moduleResponses
        );
    }

    public AdminModuleResponse toModuleResponse(CourseModule module, List<CourseItem> items) {
        List<AdminCourseItemResponse> itemResponses = items.stream()
                .sorted(Comparator.comparing(CourseItem::getOrderIndex))
                .map(this::toItemResponse)
                .toList();

        return new AdminModuleResponse(
                module.getId(),
                module.getCourse().getId(),
                module.getTitle(),
                module.getDescription(),
                module.getOrderIndex(),
                module.getCreatedAt(),
                module.getUpdatedAt(),
                itemResponses
        );
    }

    public AdminCourseItemResponse toItemResponse(CourseItem item) {
        return toItemResponse(item, List.of(), List.of(), List.of(), List.of());
    }

    public AdminCourseItemResponse toItemResponse(
            CourseItem item,
            List<CourseItemContentBlock> contentBlocks,
            List<CourseItemHint> hints,
            List<CourseItemTestCase> testCases,
            List<CourseItemOption> options
    ) {
        return new AdminCourseItemResponse(
                item.getId(),
                item.getModule().getId(),
                item.getTitle(),
                item.getItemType(),
                item.getStatement(),
                item.getStarterCode(),
                item.getLanguage(),
                item.getOrderIndex(),
                item.getTimeLimitMs(),
                item.getMemoryLimitMb(),
                item.getOutputLimitKb(),
                item.getNetworkDisabled(),
                item.getReadOnlyFs(),
                item.getComparisonMode(),
                item.getNormalizeLineEndings(),
                item.getTrimTrailingWhitespaces(),
                item.getCreatedAt(),
                item.getUpdatedAt(),
                contentBlocks.stream()
                        .sorted(Comparator.comparing(CourseItemContentBlock::getOrderIndex))
                        .map(this::toContentBlockResponse)
                        .toList(),
                hints.stream()
                        .sorted(Comparator.comparing(CourseItemHint::getOrderIndex))
                        .map(this::toHintResponse)
                        .toList(),
                testCases.stream()
                        .sorted(Comparator.comparing(CourseItemTestCase::getOrderIndex))
                        .map(this::toTestCaseResponse)
                        .toList(),
                options.stream()
                        .sorted(Comparator.comparing(CourseItemOption::getOrderIndex))
                        .map(this::toQuizOptionResponse)
                        .toList()
        );
    }

    public AdminContentBlockResponse toContentBlockResponse(CourseItemContentBlock block) {
        return new AdminContentBlockResponse(
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

    public AdminHintResponse toHintResponse(CourseItemHint hint) {
        return new AdminHintResponse(
                hint.getId(),
                hint.getOrderIndex(),
                hint.getText()
        );
    }

    public AdminTestCaseResponse toTestCaseResponse(CourseItemTestCase testCase) {
        return new AdminTestCaseResponse(
                testCase.getId(),
                testCase.getTestKey(),
                testCase.getOrderIndex(),
                testCase.getVisibility(),
                testCase.getInputData(),
                testCase.getExpectedOutput()
        );
    }

    public AdminQuizOptionResponse toQuizOptionResponse(CourseItemOption option) {
        return new AdminQuizOptionResponse(
                option.getId(),
                option.getOrderIndex(),
                option.getLabel(),
                option.getText(),
                option.getCorrect(),
                option.getExplanation()
        );
    }
}
