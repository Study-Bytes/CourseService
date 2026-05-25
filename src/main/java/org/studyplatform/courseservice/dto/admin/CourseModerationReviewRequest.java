package org.studyplatform.courseservice.dto.admin;

import jakarta.validation.constraints.NotBlank;

public record CourseModerationReviewRequest(
        @NotBlank String reviewComment
) {
}
