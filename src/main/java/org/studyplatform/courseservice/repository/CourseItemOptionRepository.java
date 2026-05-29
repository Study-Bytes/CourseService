package org.studyplatform.courseservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.studyplatform.courseservice.entity.CourseItemOption;

import java.util.List;

public interface CourseItemOptionRepository extends JpaRepository<CourseItemOption, Long> {

    List<CourseItemOption> findByItemIdOrderByOrderIndexAsc(Long itemId);

    @Modifying
    @Query("delete from CourseItemOption option where option.item.id = :itemId")
    void deleteByItemId(@Param("itemId") Long itemId);
}
