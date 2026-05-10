package org.studyplatform.courseservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.studyplatform.courseservice.entity.TaskHint;

import java.util.List;

public interface TaskHintRepository extends JpaRepository<TaskHint, Long> {

    List<TaskHint> findByTaskIdOrderByOrderIndexAsc(Long taskId);
}