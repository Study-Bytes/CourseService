package org.studyplatform.courseservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.studyplatform.courseservice.entity.CourseItem;

import java.util.List;
import java.util.Optional;

public interface CourseItemRepository extends JpaRepository<CourseItem, Long> {

    List<CourseItem> findByModuleIdOrderByOrderIndexAsc(Long moduleId);

    @Query("select i.module.course.createdByUserId from CourseItem i where i.id = :itemId")
    Optional<Long> findCourseOwnerIdByItemId(@Param("itemId") Long itemId);
}
