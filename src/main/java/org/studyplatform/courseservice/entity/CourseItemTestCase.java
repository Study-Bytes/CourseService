package org.studyplatform.courseservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.studyplatform.courseservice.entity.enums.TestVisibility;

@Entity
@Table(
        name = "course_item_test_cases",
        indexes = {
                @Index(name = "idx_course_item_test_cases_item_id", columnList = "item_id"),
                @Index(name = "idx_course_item_test_cases_visibility", columnList = "visibility")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_course_item_test_cases_item_key",
                        columnNames = {"item_id", "test_key"}
                ),
                @UniqueConstraint(
                        name = "uk_course_item_test_cases_item_order",
                        columnNames = {"item_id", "order_index"}
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseItemTestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private CourseItem item;

    @Column(name = "test_key", nullable = false, length = 120)
    private String testKey;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 32)
    private TestVisibility visibility;

    @Column(name = "input_data", columnDefinition = "TEXT")
    private String inputData;

    @Column(name = "expected_output", columnDefinition = "TEXT")
    private String expectedOutput;
}