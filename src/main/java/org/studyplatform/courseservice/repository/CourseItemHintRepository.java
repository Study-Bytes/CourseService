package org.studyplatform.courseservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.studyplatform.courseservice.entity.CourseItemHint;

import java.util.List;

public interface CourseItemHintRepository extends JpaRepository<CourseItemHint, Long> {

    List<CourseItemHint> findByItemIdOrderByOrderIndexAsc(Long itemId);
}