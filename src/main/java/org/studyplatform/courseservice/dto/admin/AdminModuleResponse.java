package org.studyplatform.courseservice.dto.admin;

import java.time.Instant;
import java.util.List;

public record AdminModuleResponse(
        Long id,
        Long courseId,
        String title,
        String description,
        Integer orderIndex,
        Instant createdAt,
        Instant updatedAt,
        List<AdminCourseItemResponse> items
) {
}
