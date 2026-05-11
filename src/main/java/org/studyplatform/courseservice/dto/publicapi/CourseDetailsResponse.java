package org.studyplatform.courseservice.dto.publicapi;

import org.studyplatform.courseservice.entity.enums.CourseAccessType;
import org.studyplatform.courseservice.entity.enums.CourseDifficulty;
import org.studyplatform.courseservice.entity.enums.CourseStatus;

import java.time.Instant;
import java.util.List;

public record CourseDetailsResponse(
        Long id,
        String slug,
        String title,
        String shortDescription,
        String description,
        CourseDifficulty difficulty,
        CourseStatus status,
        CourseAccessType accessType,
        Boolean enrollmentEnabled,
        String coverImageUrl,
        Integer estimatedMinutes,
        Instant publishedAt,
        List<ModuleSummaryResponse> modules
) {
}
