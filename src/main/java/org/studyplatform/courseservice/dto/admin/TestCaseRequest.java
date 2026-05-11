package org.studyplatform.courseservice.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.studyplatform.courseservice.entity.enums.TestVisibility;

public record TestCaseRequest(
        @NotBlank
        @Size(max = 120)
        String testKey,

        @NotNull
        Integer orderIndex,

        @NotNull
        TestVisibility visibility,

        String inputData,

        String expectedOutput
) {
}
