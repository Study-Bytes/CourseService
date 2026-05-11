package org.studyplatform.courseservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.studyplatform.courseservice.entity.CourseItemOption;

import java.util.List;

public interface CourseItemOptionRepository extends JpaRepository<CourseItemOption, Long> {

    List<CourseItemOption> findByItemIdOrderByOrderIndexAsc(Long itemId);
}
