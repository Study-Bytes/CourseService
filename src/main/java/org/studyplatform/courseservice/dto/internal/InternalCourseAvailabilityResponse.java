package org.studyplatform.courseservice.dto.internal;

import org.studyplatform.courseservice.entity.enums.CourseAccessType;
import org.studyplatform.courseservice.entity.enums.CourseStatus;

public record InternalCourseAvailabilityResponse(
        Long courseId,
        CourseStatus status,
        CourseAccessType accessType,
        Boolean enrollmentEnabled,
        Boolean availableForEnrollment
) {
}
