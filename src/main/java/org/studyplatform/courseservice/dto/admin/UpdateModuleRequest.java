package org.studyplatform.courseservice.dto.admin;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class UpdateModuleRequest {

    @Size(max = 200)
    private String title;

    private String description;

    @Min(0)
    private Integer orderIndex;

    private LocalDateTime deadlineAt;

    @JsonIgnore
    private boolean deadlineAtProvided;

    public String title() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String description() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer orderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    public LocalDateTime deadlineAt() {
        return deadlineAt;
    }

    @JsonSetter(value = "deadlineAt", nulls = Nulls.SET)
    public void setDeadlineAt(LocalDateTime deadlineAt) {
        this.deadlineAt = deadlineAt;
        this.deadlineAtProvided = true;
    }

    @JsonIgnore
    public boolean deadlineAtProvided() {
        return deadlineAtProvided;
    }
}
