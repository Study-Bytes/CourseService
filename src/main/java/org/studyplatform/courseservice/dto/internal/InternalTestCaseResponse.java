package org.studyplatform.courseservice.dto.internal;

import org.studyplatform.courseservice.entity.enums.TestVisibility;

public record InternalTestCaseResponse(
        String testKey,
        TestVisibility visibility,
        String inputData,
        String expectedOutput,
        Integer orderIndex
) {
}
