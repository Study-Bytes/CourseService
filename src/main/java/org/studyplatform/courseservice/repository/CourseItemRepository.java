package org.studyplatform.courseservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.studyplatform.courseservice.entity.CourseItem;

import java.util.List;

public interface CourseItemRepository extends JpaRepository<CourseItem, Long> {

    List<CourseItem> findByModuleIdOrderByOrderIndexAsc(Long moduleId);
}