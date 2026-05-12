package org.studyplatform.courseservice.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateModuleRequest(
        @NotBlank
        @Size(max = 200)
        String title,

        String description,

        @NotNull
        Integer orderIndex
) {
}
