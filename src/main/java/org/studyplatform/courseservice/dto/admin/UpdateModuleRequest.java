package org.studyplatform.courseservice.dto.admin;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.studyplatform.courseservice.entity.enums.ModuleDeadlineType;

public class UpdateModuleRequest {

    @Size(max = 200)
    private String title;

    private String description;

    @Min(0)
    private Integer orderIndex;

    private ModuleDeadlineType deadlineType;

    private String deadlineAt;

    @Min(1)
    private Integer timeLimitMinutes;

    @JsonIgnore
    private boolean deadlineTypeProvided;

    @JsonIgnore
    private boolean deadlineAtProvided;

    @JsonIgnore
    private boolean timeLimitMinutesProvided;

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

    public ModuleDeadlineType deadlineType() {
        return deadlineType;
    }

    @JsonSetter(value = "deadlineType", nulls = Nulls.SET)
    public void setDeadlineType(ModuleDeadlineType deadlineType) {
        this.deadlineType = deadlineType;
        this.deadlineTypeProvided = true;
    }

    public String deadlineAt() {
        return deadlineAt;
    }

    @JsonSetter(value = "deadlineAt", nulls = Nulls.SET)
    public void setDeadlineAt(String deadlineAt) {
        this.deadlineAt = deadlineAt;
        this.deadlineAtProvided = true;
    }

    public Integer timeLimitMinutes() {
        return timeLimitMinutes;
    }

    @JsonSetter(value = "timeLimitMinutes", nulls = Nulls.SET)
    public void setTimeLimitMinutes(Integer timeLimitMinutes) {
        this.timeLimitMinutes = timeLimitMinutes;
        this.timeLimitMinutesProvided = true;
    }

    @JsonIgnore
    public boolean deadlineTypeProvided() {
        return deadlineTypeProvided;
    }

    @JsonIgnore
    public boolean deadlineAtProvided() {
        return deadlineAtProvided;
    }

    @JsonIgnore
    public boolean timeLimitMinutesProvided() {
        return timeLimitMinutesProvided;
    }

    @JsonIgnore
    public boolean hasDeadlineChanges() {
        return deadlineTypeProvided || deadlineAtProvided || timeLimitMinutesProvided;
    }
}
