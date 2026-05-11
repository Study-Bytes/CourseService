package org.studyplatform.courseservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.studyplatform.courseservice.entity.Course;
import org.studyplatform.courseservice.entity.enums.CourseStatus;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Course> findByStatusOrderByCreatedAtDesc(CourseStatus status);
}