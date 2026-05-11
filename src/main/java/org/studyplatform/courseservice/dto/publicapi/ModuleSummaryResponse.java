package org.studyplatform.courseservice.dto.publicapi;

import java.util.List;

public record ModuleSummaryResponse(
        Long id,
        String title,
        String description,
        Integer orderIndex,
        List<CourseItemSummaryResponse> items
) {
}