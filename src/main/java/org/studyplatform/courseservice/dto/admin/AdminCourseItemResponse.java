package org.studyplatform.courseservice.dto.admin;

import org.studyplatform.courseservice.entity.enums.ComparisonMode;
import org.studyplatform.courseservice.entity.enums.CourseItemType;

import java.time.Instant;
import java.util.List;

public record AdminCourseItemResponse(
        Long id,
        Long moduleId,
        String title,
        CourseItemType itemType,
        String statement,
        String starterCode,
        String language,
        Integer orderIndex,
        Integer timeLimitMs,
        Integer memoryLimitMb,
        Integer outputLimitKb,
        Boolean networkDisabled,
        Boolean readOnlyFs,
        ComparisonMode comparisonMode,
        Boolean normalizeLineEndings,
        Boolean trimTrailingWhitespaces,
        Instant createdAt,
        Instant updatedAt,
        List<AdminContentBlockResponse> contentBlocks,
        List<AdminHintResponse> hints,
        List<AdminTestCaseResponse> testCases,
        List<AdminQuizOptionResponse> options
) {
}
