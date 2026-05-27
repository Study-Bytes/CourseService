package org.studyplatform.courseservice.dto.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.studyplatform.courseservice.entity.enums.ModuleDeadlineType;

public record CreateModuleRequest(
        @NotBlank
        @Size(max = 200)
        String title,

        String description,

        @NotNull
        @Min(0)
        Integer orderIndex,

        ModuleDeadlineType deadlineType,

        String deadlineAt,

        @Min(1)
        Integer timeLimitMinutes
) {
}
