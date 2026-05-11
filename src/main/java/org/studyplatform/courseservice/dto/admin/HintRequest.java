package org.studyplatform.courseservice.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record HintRequest(
        @NotNull
        Integer orderIndex,

        @NotBlank
        String text
) {
}
