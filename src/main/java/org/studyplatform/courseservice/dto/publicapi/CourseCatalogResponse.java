package org.studyplatform.courseservice.dto.publicapi;

import java.util.List;

public record CourseCatalogResponse(
        List<CourseCatalogItemResponse> courses
) {
}