package org.studyplatform.courseservice.dto.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.studyplatform.courseservice.entity.enums.CourseAccessType;
import org.studyplatform.courseservice.entity.enums.CourseDifficulty;

public record UpdateCourseRequest(
        @Size(max = 120)
        String slug,

        @Size(max = 200)
        String title,

        @Size(max = 500)
        String shortDescription,

        String description,

        CourseDifficulty difficulty,

        CourseAccessType accessType,

        Boolean enrollmentEnabled,

        @Size(max = 1000)
        String coverImageUrl,

        @Min(0)
        Integer estimatedMinutes
) {
}
