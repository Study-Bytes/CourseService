package org.studyplatform.courseservice.dto.publicapi;

import java.time.LocalDateTime;
import java.util.List;
import org.studyplatform.courseservice.entity.enums.ModuleDeadlineType;

public record ModuleSummaryResponse(
        Long id,
        String title,
        String description,
        Integer orderIndex,
        ModuleDeadlineType deadlineType,
        LocalDateTime deadlineAt,
        Integer timeLimitMinutes,
        List<CourseItemSummaryResponse> items
) {
}
