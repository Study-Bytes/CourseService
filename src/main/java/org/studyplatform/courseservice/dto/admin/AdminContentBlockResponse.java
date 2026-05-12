package org.studyplatform.courseservice.dto.admin;

import org.studyplatform.courseservice.entity.enums.ContentBlockType;

public record AdminContentBlockResponse(
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
