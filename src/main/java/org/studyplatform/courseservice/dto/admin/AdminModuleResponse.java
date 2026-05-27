package org.studyplatform.courseservice.dto.admin;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.studyplatform.courseservice.entity.enums.ModuleDeadlineType;

public record AdminModuleResponse(
        Long id,
        Long courseId,
        String title,
        String description,
        Integer orderIndex,
        ModuleDeadlineType deadlineType,
        LocalDateTime deadlineAt,
        Integer timeLimitMinutes,
        Instant createdAt,
        Instant updatedAt,
        List<AdminCourseItemResponse> items
) {
}
