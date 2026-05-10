package org.studyplatform.courseservice.dto.publicapi;

public record ExecutionPolicyResponse(
        Boolean networkDisabled,
        Boolean readOnlyFs
) {
}