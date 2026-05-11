package org.studyplatform.courseservice.dto.internal;

import org.studyplatform.courseservice.entity.enums.ComparisonMode;
import org.studyplatform.courseservice.entity.enums.CourseItemType;

import java.util.List;

public record ExecutionPackageResponse(
        Long itemId,
        Long moduleId,
        Long courseId,
        CourseItemType itemType,
        String title,
        String language,
        String starterCode,
        ExecutionLimits limits,
        ExecutionPolicy executionPolicy,
        EvaluationPolicy evaluationPolicy,
        List<InternalTestCaseResponse> tests
) {
    public record ExecutionLimits(
            Integer timeLimitMs,
            Integer memoryLimitMb,
            Integer outputLimitKb
    ) {
    }

    public record ExecutionPolicy(
            Boolean networkDisabled,
            Boolean readOnlyFs
    ) {
    }

    public record EvaluationPolicy(
            ComparisonMode comparisonMode,
            Boolean normalizeLineEndings,
            Boolean trimTrailingWhitespaces
    ) {
    }
}
