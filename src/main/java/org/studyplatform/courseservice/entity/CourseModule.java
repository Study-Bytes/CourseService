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
import org.studyplatform.courseservice.entity.enums.ModuleDeadlineType;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "course_modules",
        indexes = {
                @Index(name = "idx_course_modules_course_id", columnList = "course_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_course_modules_course_order",
                        columnNames = {"course_id", "order_index"}
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseModule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "deadline_type", nullable = false, length = 32)
    private ModuleDeadlineType deadlineType = ModuleDeadlineType.NONE;

    @Column(name = "deadline_at")
    private LocalDateTime deadlineAt;

    @Column(name = "time_limit_minutes")
    private Integer timeLimitMinutes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        normalizeDeadlineType();

        Instant now = Instant.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        normalizeDeadlineType();
        updatedAt = Instant.now();
    }

    private void normalizeDeadlineType() {
        if (deadlineAt != null && (deadlineType == null || deadlineType == ModuleDeadlineType.NONE)) {
            deadlineType = ModuleDeadlineType.ABSOLUTE;
            return;
        }

        if (timeLimitMinutes != null && (deadlineType == null || deadlineType == ModuleDeadlineType.NONE)) {
            deadlineType = ModuleDeadlineType.RELATIVE_FROM_START;
            return;
        }

        if (deadlineType == null) {
            deadlineType = ModuleDeadlineType.NONE;
        }
    }
}
