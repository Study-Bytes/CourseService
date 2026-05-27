package org.studyplatform.courseservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.studyplatform.courseservice.config.OpenApiConfig;
import org.studyplatform.courseservice.dto.internal.ExecutionPackageResponse;
import org.studyplatform.courseservice.dto.internal.InternalCourseAvailabilityResponse;
import org.studyplatform.courseservice.dto.internal.InternalCourseItemContentResponse;
import org.studyplatform.courseservice.dto.internal.InternalCourseOwnershipResponse;
import org.studyplatform.courseservice.service.CourseInternalService;

@RestController
@RequestMapping("/api/v1/internal")
@Tag(name = "Internal Course API", description = "Trusted backend endpoints for LearningService and BFF integrations")
@SecurityRequirement(name = OpenApiConfig.INTERNAL_API_KEY_SCHEME)
@Validated
public class InternalCourseController {

    private final CourseInternalService courseInternalService;

    public InternalCourseController(CourseInternalService courseInternalService) {
        this.courseInternalService = courseInternalService;
    }

    @Operation(
            summary = "Get course item execution package",
            description = "Returns full execution data for a course item, including hidden tests and expected outputs. Intended only for trusted backend services."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Execution package returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid internal API key"),
            @ApiResponse(responseCode = "404", description = "Course item not found")
    })
    @GetMapping("/course-items/{itemId}/execution-package")
    public ResponseEntity<ExecutionPackageResponse> getExecutionPackage(
            @Parameter(description = "Course item id", example = "2")
            @PathVariable Long itemId
    ) {
        return ResponseEntity.ok(courseInternalService.getExecutionPackage(itemId));
    }


    @Operation(
            summary = "Get enrolled-student-safe course item content",
            description = "Returns full course item content for trusted backend services after enrollment checks. "
                    + "Does not expose hidden tests, expected outputs or correct quiz answers."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Course item content returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid internal API key"),
            @ApiResponse(responseCode = "404", description = "Course item not found")
    })
    @GetMapping("/course-items/{itemId}/content")
    public ResponseEntity<InternalCourseItemContentResponse> getCourseItemContent(
            @Parameter(description = "Course item id", example = "2")
            @PathVariable Long itemId
    ) {
        return ResponseEntity.ok(courseInternalService.getCourseItemContent(itemId));
    }

    @Operation(
            summary = "Get course availability",
            description = "Returns publication, access and enrollment state for LearningService enrollment checks."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Course availability returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid internal API key"),
            @ApiResponse(responseCode = "404", description = "Course not found")
    })
    @GetMapping("/courses/{courseId}/availability")
    public ResponseEntity<InternalCourseAvailabilityResponse> getCourseAvailability(
            @Parameter(description = "Course id", example = "1")
            @PathVariable Long courseId
    ) {
        return ResponseEntity.ok(courseInternalService.getCourseAvailability(courseId));
    }

    @Operation(
            summary = "Check course ownership",
            description = "Returns whether the given user is the author of the course."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Course ownership state",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = InternalCourseOwnershipResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Missing or invalid internal API key",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Course not found",
                    content = @Content
            )
    })
    @GetMapping("/courses/{courseId}/ownership")
    public ResponseEntity<InternalCourseOwnershipResponse> getCourseOwnership(
            @Parameter(description = "Course id", example = "10")
            @PathVariable @Min(1) Long courseId,
            @Parameter(description = "User id", example = "5")
            @RequestParam @Min(1) Long userId
    ) {
        return ResponseEntity.ok(courseInternalService.getCourseOwnership(courseId, userId));
    }
}
