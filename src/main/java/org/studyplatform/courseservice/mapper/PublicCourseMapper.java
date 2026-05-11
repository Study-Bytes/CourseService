package org.studyplatform.courseservice.mapper;

import org.springframework.stereotype.Component;
import org.studyplatform.courseservice.dto.publicapi.CourseCatalogItemResponse;
import org.studyplatform.courseservice.dto.publicapi.CourseDetailsResponse;
import org.studyplatform.courseservice.dto.publicapi.EvaluationPolicyResponse;
import org.studyplatform.courseservice.dto.publicapi.ExecutionPolicyResponse;
import org.studyplatform.courseservice.dto.publicapi.ModuleSummaryResponse;
import org.studyplatform.courseservice.dto.publicapi.OpenTestCaseResponse;
import org.studyplatform.courseservice.dto.publicapi.TaskDetailsResponse;
import org.studyplatform.courseservice.dto.publicapi.TaskHintResponse;
import org.studyplatform.courseservice.dto.publicapi.TaskLimitsResponse;
import org.studyplatform.courseservice.dto.publicapi.TaskSummaryResponse;
import org.studyplatform.courseservice.entity.Course;
import org.studyplatform.courseservice.entity.CourseModule;
import org.studyplatform.courseservice.entity.CourseTask;
import org.studyplatform.courseservice.entity.TaskHint;
import org.studyplatform.courseservice.entity.TaskTestCase;
import org.studyplatform.courseservice.entity.enums.TestVisibility;

import java.util.Comparator;
import java.util.List;

@Component
public class PublicCourseMapper {

    public CourseCatalogItemResponse toCatalogItem(Course course) {
        return new CourseCatalogItemResponse(
                course.getId(),
                course.getSlug(),
                course.getTitle(),
                course.getShortDescription(),
                course.getDifficulty(),
                course.getStatus(),
                course.getCoverImageUrl(),
                course.getEstimatedMinutes()
        );
    }

    public CourseDetailsResponse toCourseDetails(
            Course course,
            List<CourseModule> modules,
            List<CourseTask> tasks
    ) {
        List<ModuleSummaryResponse> moduleResponses = modules.stream()
                .sorted(Comparator.comparing(CourseModule::getOrderIndex))
                .map(module -> toModuleSummary(
                        module,
                        tasks.stream()
                                .filter(task -> task.getModule().getId().equals(module.getId()))
                                .toList()
                ))
                .toList();

        return new CourseDetailsResponse(
                course.getId(),
                course.getSlug(),
                course.getTitle(),
                course.getShortDescription(),
                course.getDescription(),
                course.getDifficulty(),
                course.getStatus(),
                course.getCoverImageUrl(),
                course.getEstimatedMinutes(),
                course.getPublishedAt(),
                moduleResponses
        );
    }

    public ModuleSummaryResponse toModuleSummary(
            CourseModule module,
            List<CourseTask> tasks
    ) {
        List<TaskSummaryResponse> taskResponses = tasks.stream()
                .sorted(Comparator.comparing(CourseTask::getOrderIndex))
                .map(this::toTaskSummary)
                .toList();

        return new ModuleSummaryResponse(
                module.getId(),
                module.getTitle(),
                module.getDescription(),
                module.getOrderIndex(),
                taskResponses
        );
    }

    public TaskSummaryResponse toTaskSummary(CourseTask task) {
        return new TaskSummaryResponse(
                task.getId(),
                task.getTitle(),
                task.getTaskType(),
                task.getLanguage(),
                task.getOrderIndex()
        );
    }

    public TaskDetailsResponse toTaskDetails(
            CourseTask task,
            List<TaskTestCase> testCases,
            List<TaskHint> hints
    ) {
        List<OpenTestCaseResponse> openTests = testCases.stream()
                .filter(testCase -> testCase.getVisibility() == TestVisibility.OPEN)
                .sorted(Comparator.comparing(TaskTestCase::getOrderIndex))
                .map(this::toOpenTestCase)
                .toList();

        List<TaskHintResponse> hintResponses = hints.stream()
                .sorted(Comparator.comparing(TaskHint::getOrderIndex))
                .map(this::toTaskHint)
                .toList();

        return new TaskDetailsResponse(
                task.getId(),
                task.getModule().getId(),
                task.getTitle(),
                task.getTaskType(),
                task.getStatement(),
                task.getStarterCode(),
                task.getLanguage(),
                task.getOrderIndex(),
                toTaskLimits(task),
                toExecutionPolicy(task),
                toEvaluationPolicy(task),
                openTests,
                hintResponses
        );
    }

    public TaskLimitsResponse toTaskLimits(CourseTask task) {
        return new TaskLimitsResponse(
                task.getTimeLimitMs(),
                task.getMemoryLimitMb(),
                task.getOutputLimitKb()
        );
    }

    public ExecutionPolicyResponse toExecutionPolicy(CourseTask task) {
        return new ExecutionPolicyResponse(
                task.getNetworkDisabled(),
                task.getReadOnlyFs()
        );
    }

    public EvaluationPolicyResponse toEvaluationPolicy(CourseTask task) {
        return new EvaluationPolicyResponse(
                task.getComparisonMode(),
                task.getNormalizeLineEndings(),
                task.getTrimTrailingWhitespaces()
        );
    }

    public OpenTestCaseResponse toOpenTestCase(TaskTestCase testCase) {
        return new OpenTestCaseResponse(
                testCase.getTestKey(),
                testCase.getOrderIndex(),
                testCase.getInputData()
        );
    }

    public TaskHintResponse toTaskHint(TaskHint hint) {
        return new TaskHintResponse(
                hint.getOrderIndex(),
                hint.getText()
        );
    }
}