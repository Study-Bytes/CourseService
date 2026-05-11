package org.studyplatform.courseservice.dto.publicapi;

public record TaskLimitsResponse(
        Integer timeLimitMs,
        Integer memoryLimitMb,
        Integer outputLimitKb
) {
}