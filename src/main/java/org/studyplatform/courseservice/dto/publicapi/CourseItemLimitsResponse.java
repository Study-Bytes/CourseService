package org.studyplatform.courseservice.dto.publicapi;

public record CourseItemLimitsResponse(
        Integer timeLimitMs,
        Integer memoryLimitMb,
        Integer outputLimitKb
) {
}