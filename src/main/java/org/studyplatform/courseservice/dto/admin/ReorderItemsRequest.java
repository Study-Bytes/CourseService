package org.studyplatform.courseservice.dto.admin;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ReorderItemsRequest(
        @NotEmpty
        List<Long> orderedItemIds
) {
}
