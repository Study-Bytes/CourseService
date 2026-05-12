package org.studyplatform.courseservice.dto.internal;

import org.studyplatform.courseservice.dto.publicapi.ContentBlockResponse;
import org.studyplatform.courseservice.dto.publicapi.CourseItemHintResponse;
import org.studyplatform.courseservice.dto.publicapi.CourseItemLimitsResponse;
import org.studyplatform.courseservice.dto.publicapi.CourseItemOptionResponse;
import org.studyplatform.courseservice.dto.publicapi.EvaluationPolicyResponse;
import org.studyplatform.courseservice.dto.publicapi.ExecutionPolicyResponse;
import org.studyplatform.courseservice.dto.publicapi.OpenTestCaseResponse;
import org.studyplatform.courseservice.entity.enums.CourseItemType;

import java.util.List;

public record InternalCourseItemContentResponse(
        Long itemId,
        Long moduleId,
        Long courseId,
        String title,
        CourseItemType itemType,
        String statement,
        String starterCode,
        String language,
        Integer orderIndex,
        CourseItemLimitsResponse limits,
        ExecutionPolicyResponse executionPolicy,
        EvaluationPolicyResponse evaluationPolicy,
        List<ContentBlockResponse> contentBlocks,
        List<OpenTestCaseResponse> openTests,
        List<CourseItemHintResponse> hints,
        List<CourseItemOptionResponse> options
) {
}
