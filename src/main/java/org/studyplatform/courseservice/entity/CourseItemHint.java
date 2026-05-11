package org.studyplatform.courseservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(
        name = "course_item_hints",
        indexes = {
                @Index(name = "idx_course_item_hints_task_id", columnList = "task_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_course_item_hints_task_order",
                        columnNames = {"task_id", "order_index"}
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseItemHint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private CourseItem task;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(name = "text", nullable = false, columnDefinition = "TEXT")
    private String text;
}