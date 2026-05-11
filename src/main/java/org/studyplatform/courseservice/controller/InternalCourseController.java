package org.studyplatform.courseservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.studyplatform.courseservice.dto.internal.ExecutionPackageResponse;
import org.studyplatform.courseservice.dto.internal.InternalCourseAvailabilityResponse;
import org.studyplatform.courseservice.service.CourseInternalService;

@RestController
@RequestMapping("/api/v1/internal")
@Tag(name = "Internal Course API", description = "Trusted backend endpoints for LearningService and BFF integrations")
@SecurityRequirement(name = "internalApiKey")
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
}
