package org.studyplatform.courseservice.dto.internal;

import org.studyplatform.courseservice.entity.enums.CourseItemType;

import java.util.List;

public record QuizEvaluationPackageResponse(
        Long itemId,
        Long moduleId,
        Long courseId,
        CourseItemType itemType,
        String title,
        List<QuizEvaluationOptionResponse> options
) {
    public record QuizEvaluationOptionResponse(
            Long id,
            Integer orderIndex,
            String label,
            String text,
            Boolean correct,
            String explanation
    ) {
    }
}
