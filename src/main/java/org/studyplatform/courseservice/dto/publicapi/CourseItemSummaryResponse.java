package org.studyplatform.courseservice.dto.publicapi;

import org.studyplatform.courseservice.entity.enums.CourseItemType;

public record CourseItemSummaryResponse(
        Long id,
        String title,
        CourseItemType itemType,
        String language,
        Integer orderIndex
) {
}