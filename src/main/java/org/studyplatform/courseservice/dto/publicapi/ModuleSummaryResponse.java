package org.studyplatform.courseservice.dto.publicapi;

import java.time.LocalDateTime;
import java.util.List;

public record ModuleSummaryResponse(
        Long id,
        String title,
        String description,
        Integer orderIndex,
        LocalDateTime deadlineAt,
        List<CourseItemSummaryResponse> items
) {
}
