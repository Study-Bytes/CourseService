package org.studyplatform.courseservice.dto.admin;

import org.studyplatform.courseservice.entity.enums.TestVisibility;

public record AdminTestCaseResponse(
        Long id,
        String testKey,
        Integer orderIndex,
        TestVisibility visibility,
        String inputData,
        String expectedOutput
) {
}
