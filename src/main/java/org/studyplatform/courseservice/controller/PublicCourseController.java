package org.studyplatform.courseservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.studyplatform.courseservice.dto.publicapi.CourseCatalogResponse;
import org.studyplatform.courseservice.dto.publicapi.CourseDetailsResponse;
import org.studyplatform.courseservice.dto.publicapi.CourseItemDetailsResponse;
import org.studyplatform.courseservice.exception.ApiErrorResponse;
import org.studyplatform.courseservice.service.CoursePublicService;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Public courses", description = "Public read endpoints for published courses and course items")
public class PublicCourseController {

    private final CoursePublicService coursePublicService;

    public PublicCourseController(CoursePublicService coursePublicService) {
        this.coursePublicService = coursePublicService;
    }

    @Operation(
            summary = "Get published course catalog",
            description = "Returns published public courses only. Private, archived and draft courses are not included."
    )
    @ApiResponse(responseCode = "200", description = "Published public course catalog")
    @GetMapping("/courses")
    public CourseCatalogResponse getPublishedCourseCatalog() {
        return coursePublicService.getPublishedCourseCatalog();
    }

    @Operation(
            summary = "Get published course details",
            description = "Returns course details with modules and course item summaries. Only published PUBLIC or UNLISTED courses are readable."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Published course details"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Course not found or not publicly readable",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    @GetMapping("/courses/{courseId}")
    public CourseDetailsResponse getPublishedCourseDetails(
            @Parameter(description = "Course identifier", example = "1")
            @PathVariable Long courseId
    ) {
        return coursePublicService.getPublishedCourseDetails(courseId);
    }

    @Operation(
            summary = "Get published course item details",
            description = "Returns course item content blocks, open tests, hints and public quiz options. "
                    + "Hidden tests, expected outputs and correct quiz answers are not exposed."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Published course item details"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Course item not found or not publicly readable",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    @GetMapping("/course-items/{itemId}")
    public CourseItemDetailsResponse getPublishedCourseItemDetails(
            @Parameter(description = "Course item identifier", example = "1")
            @PathVariable Long itemId
    ) {
        return coursePublicService.getPublishedCourseItemDetails(itemId);
    }
}
