package org.studyplatform.courseservice.dto.internal;

import io.swagger.v3.oas.annotations.media.Schema;

public record InternalCourseOwnershipResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
        Long courseId,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "5")
        Long userId,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
        Boolean owner
) {
}
