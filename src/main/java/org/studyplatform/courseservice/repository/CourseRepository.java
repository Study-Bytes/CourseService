package org.studyplatform.courseservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.studyplatform.courseservice.entity.Course;
import org.studyplatform.courseservice.entity.enums.CourseAccessType;
import org.studyplatform.courseservice.entity.enums.CourseStatus;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    List<Course> findByStatusOrderByCreatedAtDesc(CourseStatus status);

    List<Course> findByStatusAndAccessTypeOrderByCreatedAtDesc(
            CourseStatus status,
            CourseAccessType accessType
    );

    @Query("select c.createdByUserId from Course c where c.id = :courseId")
    Optional<Long> findCreatedByUserIdById(@Param("courseId") Long courseId);
}
