package org.studyplatform.courseservice.dto.publicapi;

import org.studyplatform.courseservice.entity.enums.TaskType;

public record TaskSummaryResponse(
        Long id,
        String title,
        TaskType taskType,
        String language,
        Integer orderIndex
) {
}