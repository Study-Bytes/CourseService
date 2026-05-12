package org.studyplatform.courseservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.studyplatform.courseservice.dto.admin.AdminCourseItemResponse;
import org.studyplatform.courseservice.dto.admin.AdminCourseResponse;
import org.studyplatform.courseservice.dto.admin.AdminModuleResponse;
import org.studyplatform.courseservice.dto.admin.CreateCourseItemRequest;
import org.studyplatform.courseservice.dto.admin.CreateCourseRequest;
import org.studyplatform.courseservice.dto.admin.CreateModuleRequest;
import org.studyplatform.courseservice.dto.admin.ReplaceContentBlocksRequest;
import org.studyplatform.courseservice.dto.admin.ReplaceHintsRequest;
import org.studyplatform.courseservice.dto.admin.ReplaceQuizOptionsRequest;
import org.studyplatform.courseservice.dto.admin.ReplaceTestCasesRequest;
import org.studyplatform.courseservice.dto.admin.UpdateCourseItemRequest;
import org.studyplatform.courseservice.dto.admin.UpdateCourseRequest;
import org.studyplatform.courseservice.dto.admin.UpdateModuleRequest;
import org.studyplatform.courseservice.service.CourseAdminService;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Course API", description = "Teacher/admin API for creating and editing course content")
public class AdminCourseController {

    private final CourseAdminService adminService;

    @PostMapping("/courses")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create course", description = "Creates a draft course with base metadata.")
    public AdminCourseResponse createCourse(@Valid @RequestBody CreateCourseRequest request) {
        return adminService.createCourse(request);
    }

    @GetMapping("/courses/{courseId}")
    @Operation(summary = "Get admin course details", description = "Returns course metadata with modules and item summaries.")
    public AdminCourseResponse getCourse(@PathVariable Long courseId) {
        return adminService.getCourse(courseId);
    }

    @PutMapping("/courses/{courseId}")
    @Operation(summary = "Update course", description = "Updates course metadata.")
    public AdminCourseResponse updateCourse(
            @PathVariable Long courseId,
            @Valid @RequestBody UpdateCourseRequest request
    ) {
        return adminService.updateCourse(courseId, request);
    }

    @PostMapping("/courses/{courseId}/publish")
    @Operation(summary = "Publish course", description = "Marks a course as published and sets publishedAt if missing.")
    public AdminCourseResponse publishCourse(@PathVariable Long courseId) {
        return adminService.publishCourse(courseId);
    }

    @PostMapping("/courses/{courseId}/archive")
    @Operation(summary = "Archive course", description = "Marks a course as archived.")
    public AdminCourseResponse archiveCourse(@PathVariable Long courseId) {
        return adminService.archiveCourse(courseId);
    }

    @PostMapping("/courses/{courseId}/modules")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create module", description = "Creates a module inside a course.")
    public AdminModuleResponse createModule(
            @PathVariable Long courseId,
            @Valid @RequestBody CreateModuleRequest request
    ) {
        return adminService.createModule(courseId, request);
    }

    @PutMapping("/modules/{moduleId}")
    @Operation(summary = "Update module", description = "Updates module metadata and order.")
    public AdminModuleResponse updateModule(
            @PathVariable Long moduleId,
            @Valid @RequestBody UpdateModuleRequest request
    ) {
        return adminService.updateModule(moduleId, request);
    }

    @DeleteMapping("/modules/{moduleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete module", description = "Deletes a module and all its course items.")
    public void deleteModule(@PathVariable Long moduleId) {
        adminService.deleteModule(moduleId);
    }

    @PostMapping("/modules/{moduleId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create course item", description = "Creates a course item inside a module.")
    public AdminCourseItemResponse createItem(
            @PathVariable Long moduleId,
            @Valid @RequestBody CreateCourseItemRequest request
    ) {
        return adminService.createItem(moduleId, request);
    }

    @GetMapping("/course-items/{itemId}")
    @Operation(summary = "Get admin course item details", description = "Returns course item with content blocks, hints, tests and quiz options.")
    public AdminCourseItemResponse getItem(@PathVariable Long itemId) {
        return adminService.getItem(itemId);
    }

    @PutMapping("/course-items/{itemId}")
    @Operation(summary = "Update course item", description = "Updates base metadata and execution/evaluation settings for a course item.")
    public AdminCourseItemResponse updateItem(
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCourseItemRequest request
    ) {
        return adminService.updateItem(itemId, request);
    }

    @DeleteMapping("/course-items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete course item", description = "Deletes a course item and its content blocks, hints, tests and quiz options.")
    public void deleteItem(@PathVariable Long itemId) {
        adminService.deleteItem(itemId);
    }

    @PutMapping("/course-items/{itemId}/content-blocks")
    @Operation(summary = "Replace content blocks", description = "Replaces all rich content blocks for a course item.")
    public AdminCourseItemResponse replaceContentBlocks(
            @PathVariable Long itemId,
            @Valid @RequestBody ReplaceContentBlocksRequest request
    ) {
        return adminService.replaceContentBlocks(itemId, request);
    }

    @PutMapping("/course-items/{itemId}/hints")
    @Operation(summary = "Replace hints", description = "Replaces all hints for a course item.")
    public AdminCourseItemResponse replaceHints(
            @PathVariable Long itemId,
            @Valid @RequestBody ReplaceHintsRequest request
    ) {
        return adminService.replaceHints(itemId, request);
    }

    @PutMapping("/course-items/{itemId}/test-cases")
    @Operation(summary = "Replace test cases", description = "Replaces all test cases for a course item. Admin response includes expected output.")
    public AdminCourseItemResponse replaceTestCases(
            @PathVariable Long itemId,
            @Valid @RequestBody ReplaceTestCasesRequest request
    ) {
        return adminService.replaceTestCases(itemId, request);
    }

    @PutMapping("/course-items/{itemId}/options")
    @Operation(summary = "Replace quiz options", description = "Replaces all quiz options for a course item. Admin response includes correct flags.")
    public AdminCourseItemResponse replaceOptions(
            @PathVariable Long itemId,
            @Valid @RequestBody ReplaceQuizOptionsRequest request
    ) {
        return adminService.replaceOptions(itemId, request);
    }
}
