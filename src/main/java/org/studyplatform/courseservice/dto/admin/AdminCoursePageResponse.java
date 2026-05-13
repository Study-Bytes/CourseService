package org.studyplatform.courseservice.dto.admin;

import java.util.List;

public record AdminCoursePageResponse(
        List<AdminCourseSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
