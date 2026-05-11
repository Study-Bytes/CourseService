package org.studyplatform.courseservice.dto.publicapi;

public record CourseItemOptionResponse(
        Long id,
        Integer orderIndex,
        String label,
        String text
) {
}
