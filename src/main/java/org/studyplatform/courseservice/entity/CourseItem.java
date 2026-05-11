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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.studyplatform.courseservice.entity.enums.ComparisonMode;
import org.studyplatform.courseservice.entity.enums.CourseItemType;

import java.time.Instant;

@Entity
@Table(
        name = "course_items",
        indexes = {
                @Index(name = "idx_course_items_module_id", columnList = "module_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_course_items_module_order",
                        columnNames = {"module_id", "order_index"}
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "module_id", nullable = false)
    private CourseModule module;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false, length = 32)
    private CourseItemType taskType;

    @Column(name = "statement", columnDefinition = "TEXT")
    private String statement;

    @Column(name = "starter_code", columnDefinition = "TEXT")
    private String starterCode;

    @Column(name = "language", length = 50)
    private String language;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(name = "time_limit_ms")
    private Integer timeLimitMs;

    @Column(name = "memory_limit_mb")
    private Integer memoryLimitMb;

    @Column(name = "output_limit_kb")
    private Integer outputLimitKb;

    @Column(name = "network_disabled", nullable = false)
    private Boolean networkDisabled;

    @Column(name = "read_only_fs", nullable = false)
    private Boolean readOnlyFs;

    @Enumerated(EnumType.STRING)
    @Column(name = "comparison_mode", nullable = false, length = 32)
    private ComparisonMode comparisonMode;

    @Column(name = "normalize_line_endings", nullable = false)
    private Boolean normalizeLineEndings;

    @Column(name = "trim_trailing_whitespaces", nullable = false)
    private Boolean trimTrailingWhitespaces;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }

        if (taskType == null) {
            taskType = CourseItemType.CODING;
        }

        if (language == null || language.isBlank()) {
            language = "python";
        }

        if (timeLimitMs == null) {
            timeLimitMs = 1500;
        }

        if (memoryLimitMb == null) {
            memoryLimitMb = 256;
        }

        if (outputLimitKb == null) {
            outputLimitKb = 256;
        }

        if (networkDisabled == null) {
            networkDisabled = true;
        }

        if (readOnlyFs == null) {
            readOnlyFs = true;
        }

        if (comparisonMode == null) {
            comparisonMode = ComparisonMode.EXACT;
        }

        if (normalizeLineEndings == null) {
            normalizeLineEndings = true;
        }

        if (trimTrailingWhitespaces == null) {
            trimTrailingWhitespaces = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}