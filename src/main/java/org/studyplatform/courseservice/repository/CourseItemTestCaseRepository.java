package org.studyplatform.courseservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.studyplatform.courseservice.entity.CourseItemTestCase;
import org.studyplatform.courseservice.entity.enums.TestVisibility;

import java.util.List;

public interface CourseItemTestCaseRepository extends JpaRepository<CourseItemTestCase, Long> {

    List<CourseItemTestCase> findByItemIdOrderByOrderIndexAsc(Long itemId);

    List<CourseItemTestCase> findByItemIdAndVisibilityOrderByOrderIndexAsc(
            Long itemId,
            TestVisibility visibility
    );

    @Modifying
    @Query("delete from CourseItemTestCase testCase where testCase.item.id = :itemId")
    void deleteByItemId(@Param("itemId") Long itemId);
}
