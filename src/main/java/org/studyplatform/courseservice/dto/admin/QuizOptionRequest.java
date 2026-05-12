package org.studyplatform.courseservice.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record QuizOptionRequest(
        @NotNull
        Integer orderIndex,

        @Size(max = 20)
        String label,

        @NotBlank
        String text,

        @NotNull
        Boolean correct,

        String explanation
) {
}
