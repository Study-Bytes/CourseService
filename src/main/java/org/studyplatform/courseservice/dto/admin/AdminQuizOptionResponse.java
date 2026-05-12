package org.studyplatform.courseservice.dto.admin;

public record AdminQuizOptionResponse(
        Long id,
        Integer orderIndex,
        String label,
        String text,
        Boolean correct,
        String explanation
) {
}
