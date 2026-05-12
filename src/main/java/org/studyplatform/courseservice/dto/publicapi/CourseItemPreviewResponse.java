package org.studyplatform.courseservice.dto.publicapi;

import org.studyplatform.courseservice.entity.enums.CourseItemType;

public record CourseItemPreviewResponse(
        Long id,
        Long moduleId,
        Long courseId,
        String title,
        CourseItemType itemType,
        String language,
        Integer orderIndex
) {
}
