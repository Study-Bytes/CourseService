package org.studyplatform.courseservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.studyplatform.courseservice.entity.CourseItemHint;

import java.util.List;

public interface CourseItemHintRepository extends JpaRepository<CourseItemHint, Long> {

    List<CourseItemHint> findByItemIdOrderByOrderIndexAsc(Long itemId);

    @Modifying
    @Query("delete from CourseItemHint hint where hint.item.id = :itemId")
    void deleteByItemId(@Param("itemId") Long itemId);
}
