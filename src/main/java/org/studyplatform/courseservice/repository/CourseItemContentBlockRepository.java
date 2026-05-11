package org.studyplatform.courseservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.studyplatform.courseservice.entity.CourseItemContentBlock;

import java.util.List;

public interface CourseItemContentBlockRepository extends JpaRepository<CourseItemContentBlock, Long> {

    List<CourseItemContentBlock> findByItemIdOrderByOrderIndexAsc(Long itemId);
}
