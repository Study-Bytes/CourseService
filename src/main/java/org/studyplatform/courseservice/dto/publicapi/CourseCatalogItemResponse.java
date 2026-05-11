package org.studyplatform.courseservice.dto.publicapi;

import org.studyplatform.courseservice.entity.enums.CourseDifficulty;
import org.studyplatform.courseservice.entity.enums.CourseStatus;

public record CourseCatalogItemResponse(
        Long id,
        String slug,
        String title,
        String shortDescription,
        CourseDifficulty difficulty,
        CourseStatus status,
        String coverImageUrl,
        Integer estimatedMinutes
) {
}