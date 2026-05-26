package org.studyplatform.courseservice.dto.admin;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public record AdminModuleResponse(
        Long id,
        Long courseId,
        String title,
        String description,
        Integer orderIndex,
        LocalDateTime deadlineAt,
        Instant createdAt,
        Instant updatedAt,
        List<AdminCourseItemResponse> items
) {
}
