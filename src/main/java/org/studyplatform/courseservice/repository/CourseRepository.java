package org.studyplatform.courseservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.studyplatform.courseservice.entity.Course;
import org.studyplatform.courseservice.entity.enums.CourseStatus;
import org.studyplatform.courseservice.entity.enums.CourseAccessType;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Course> findByStatusOrderByCreatedAtDesc(CourseStatus status);

    List<Course> findByStatusAndAccessTypeOrderByCreatedAtDesc(
            CourseStatus status,
            CourseAccessType accessType
    );
}