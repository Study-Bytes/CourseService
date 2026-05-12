package org.studyplatform.courseservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.studyplatform.courseservice.entity.CourseModule;

import java.util.List;
import java.util.Optional;

public interface CourseModuleRepository extends JpaRepository<CourseModule, Long> {

    List<CourseModule> findByCourseIdOrderByOrderIndexAsc(Long courseId);

    @Query("select m.course.createdByUserId from CourseModule m where m.id = :moduleId")
    Optional<Long> findCourseOwnerIdByModuleId(@Param("moduleId") Long moduleId);
}
