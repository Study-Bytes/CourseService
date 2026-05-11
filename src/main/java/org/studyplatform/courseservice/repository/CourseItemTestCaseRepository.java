package org.studyplatform.courseservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.studyplatform.courseservice.entity.CourseItemTestCase;
import org.studyplatform.courseservice.entity.enums.TestVisibility;

import java.util.List;

public interface CourseItemTestCaseRepository extends JpaRepository<CourseItemTestCase, Long> {

    List<CourseItemTestCase> findByItemIdOrderByOrderIndexAsc(Long itemId);

    List<CourseItemTestCase> findByItemIdAndVisibilityOrderByOrderIndexAsc(
            Long itemId,
            TestVisibility visibility
    );
}