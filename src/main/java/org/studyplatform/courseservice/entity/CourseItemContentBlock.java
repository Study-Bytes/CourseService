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
import org.studyplatform.courseservice.entity.enums.ContentBlockType;

@Entity
@Table(
        name = "course_item_content_blocks",
        indexes = {
                @Index(name = "idx_course_item_content_blocks_item_id", columnList = "item_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_course_item_content_blocks_item_order",
                        columnNames = {"item_id", "order_index"}
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseItemContentBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private CourseItem item;

    @Enumerated(EnumType.STRING)
    @Column(name = "block_type", nullable = false, length = 32)
    private ContentBlockType blockType;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "text_content", columnDefinition = "TEXT")
    private String textContent;

    @Column(name = "url", length = 1000)
    private String url;

    @Column(name = "language", length = 50)
    private String language;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;
}
