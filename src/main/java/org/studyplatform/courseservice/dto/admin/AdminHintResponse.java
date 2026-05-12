package org.studyplatform.courseservice.dto.admin;

public record AdminHintResponse(
        Long id,
        Integer orderIndex,
        String text
) {
}
