package org.studyplatform.courseservice.dto.publicapi;

import org.studyplatform.courseservice.entity.enums.ComparisonMode;
import org.studyplatform.courseservice.entity.enums.TaskType;

import java.util.List;

public record TaskDetailsResponse(
        Long id,
        Long moduleId,
        String title,
        TaskType taskType,
        String statement,
        String starterCode,
        String language,
        Integer orderIndex,
        TaskLimitsResponse limits,
        ExecutionPolicyResponse executionPolicy,
        EvaluationPolicyResponse evaluationPolicy,
        List<OpenTestCaseResponse> openTests,
        List<TaskHintResponse> hints
) {
}