package org.studyplatform.courseservice.dto.admin;

import org.studyplatform.courseservice.entity.enums.CourseAccessType;
import org.studyplatform.courseservice.entity.enums.CourseDifficulty;
import org.studyplatform.courseservice.entity.enums.CourseStatus;

import java.time.Instant;

public record AdminCourseSummaryResponse(
        Long id,
        String slug,
        String title,
        String shortDescription,
        CourseDifficulty difficulty,
        CourseStatus status,
        CourseAccessType accessType,
        Boolean enrollmentEnabled,
        String coverImageUrl,
        Integer estimatedMinutes,
        Long createdByUserId,
        Instant createdAt,
        Instant updatedAt,
        Instant publishedAt,
        Instant submittedForReviewAt,
        Instant reviewedAt,
        Long reviewedByUserId,
        String reviewComment
) {
}
