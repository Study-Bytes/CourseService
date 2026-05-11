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
        name = "course_item_options",
        indexes = {
                @Index(name = "idx_course_item_options_item_id", columnList = "item_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_course_item_options_item_order",
                        columnNames = {"item_id", "order_index"}
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseItemOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private CourseItem item;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(name = "label", length = 20)
    private String label;

    @Column(name = "text", nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(name = "correct", nullable = false)
    private Boolean correct;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;
}
