package org.studyplatform.courseservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.studyplatform.courseservice.entity.CourseTask;

import java.util.List;

public interface CourseTaskRepository extends JpaRepository<CourseTask, Long> {

    List<CourseTask> findByModuleIdOrderByOrderIndexAsc(Long moduleId);
}