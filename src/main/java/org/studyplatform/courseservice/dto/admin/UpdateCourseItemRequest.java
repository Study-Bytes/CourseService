package org.studyplatform.courseservice.dto.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.studyplatform.courseservice.entity.enums.ComparisonMode;
import org.studyplatform.courseservice.entity.enums.CourseItemType;

public record UpdateCourseItemRequest(
        @Size(max = 200)
        String title,

        CourseItemType itemType,

        String statement,

        String starterCode,

        @Size(max = 50)
        String language,

        @Min(0)
        Integer orderIndex,

        @Min(1)
        Integer timeLimitMs,

        @Min(1)
        Integer memoryLimitMb,

        @Min(1)
        Integer outputLimitKb,

        Boolean networkDisabled,

        Boolean readOnlyFs,

        ComparisonMode comparisonMode,

        Boolean normalizeLineEndings,

        Boolean trimTrailingWhitespaces
) {
}
