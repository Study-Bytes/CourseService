package org.studyplatform.courseservice.dto.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record HintRequest(
        @NotNull
        @Min(0)
        Integer orderIndex,

        @NotBlank
        String text
) {
}
