package org.studyplatform.courseservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.studyplatform.courseservice.entity.CourseItemContentBlock;

import java.util.List;

public interface CourseItemContentBlockRepository extends JpaRepository<CourseItemContentBlock, Long> {

    List<CourseItemContentBlock> findByItemIdOrderByOrderIndexAsc(Long itemId);

    @Modifying
    @Query("delete from CourseItemContentBlock block where block.item.id = :itemId")
    void deleteByItemId(@Param("itemId") Long itemId);
}
