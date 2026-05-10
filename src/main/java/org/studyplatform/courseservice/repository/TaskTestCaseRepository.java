package org.studyplatform.courseservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.studyplatform.courseservice.entity.TaskTestCase;
import org.studyplatform.courseservice.entity.enums.TestVisibility;

import java.util.List;

public interface TaskTestCaseRepository extends JpaRepository<TaskTestCase, Long> {

    List<TaskTestCase> findByTaskIdOrderByOrderIndexAsc(Long taskId);

    List<TaskTestCase> findByTaskIdAndVisibilityOrderByOrderIndexAsc(
            Long taskId,
            TestVisibility visibility
    );
}