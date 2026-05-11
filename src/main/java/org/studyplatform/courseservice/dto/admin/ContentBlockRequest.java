package org.studyplatform.courseservice.dto.admin;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.studyplatform.courseservice.entity.enums.ContentBlockType;

public record ContentBlockRequest(
        @NotNull
        ContentBlockType blockType,

        @NotNull
        Integer orderIndex,

        @Size(max = 200)
        String title,

        String textContent,

        @Size(max = 1000)
        String url,

        @Size(max = 50)
        String language,

        String metadataJson
) {
}
