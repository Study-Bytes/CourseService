package org.studyplatform.courseservice.dto.publicapi;

import org.studyplatform.courseservice.entity.enums.ComparisonMode;

public record EvaluationPolicyResponse(
        ComparisonMode comparisonMode,
        Boolean normalizeLineEndings,
        Boolean trimTrailingWhitespaces
) {
}