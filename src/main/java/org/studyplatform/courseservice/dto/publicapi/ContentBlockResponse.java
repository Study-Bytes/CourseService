package org.studyplatform.courseservice.dto.publicapi;

import org.studyplatform.courseservice.entity.enums.ContentBlockType;

public record ContentBlockResponse(
        Long id,
        ContentBlockType blockType,
        Integer orderIndex,
        String title,
        String textContent,
        String url,
        String language,
        String metadataJson
) {
}
