package org.studyplatform.courseservice.dto.publicapi;

import org.studyplatform.courseservice.entity.enums.CourseItemType;

import java.util.List;

public record CourseItemDetailsResponse(
        Long id,
        Long moduleId,
        String title,
        CourseItemType itemType,
        String statement,
        String starterCode,
        String language,
        Integer orderIndex,
        CourseItemLimitsResponse limits,
        ExecutionPolicyResponse executionPolicy,
        EvaluationPolicyResponse evaluationPolicy,
        List<OpenTestCaseResponse> openTests,
        List<CourseItemHintResponse> hints
) {
}