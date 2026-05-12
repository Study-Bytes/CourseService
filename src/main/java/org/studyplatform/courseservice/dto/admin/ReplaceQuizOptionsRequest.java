package org.studyplatform.courseservice.dto.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ReplaceQuizOptionsRequest(
        @NotNull
        List<@Valid QuizOptionRequest> options
) {
}
