package org.studyplatform.courseservice.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.studyplatform.courseservice.dto.publicapi.CourseCatalogResponse;
import org.studyplatform.courseservice.dto.publicapi.CourseDetailsResponse;
import org.studyplatform.courseservice.dto.publicapi.CourseItemPreviewResponse;
import org.studyplatform.courseservice.entity.Course;
import org.studyplatform.courseservice.entity.CourseItem;
import org.studyplatform.courseservice.entity.CourseModule;
import org.studyplatform.courseservice.entity.enums.CourseAccessType;
import org.studyplatform.courseservice.entity.enums.CourseStatus;
import org.studyplatform.courseservice.exception.ResourceNotFoundException;
import org.studyplatform.courseservice.mapper.PublicCourseMapper;
import org.studyplatform.courseservice.repository.CourseItemContentBlockRepository;
import org.studyplatform.courseservice.repository.CourseItemHintRepository;
import org.studyplatform.courseservice.repository.CourseItemOptionRepository;
import org.studyplatform.courseservice.repository.CourseItemRepository;
import org.studyplatform.courseservice.repository.CourseItemTestCaseRepository;
import org.studyplatform.courseservice.repository.CourseModuleRepository;
import org.studyplatform.courseservice.repository.CourseRepository;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CoursePublicService {

    private final CourseRepository courseRepository;
    private final CourseModuleRepository courseModuleRepository;
    private final CourseItemRepository courseItemRepository;
    private final CourseItemContentBlockRepository contentBlockRepository;
    private final CourseItemTestCaseRepository testCaseRepository;
    private final CourseItemHintRepository hintRepository;
    private final CourseItemOptionRepository optionRepository;
    private final PublicCourseMapper publicCourseMapper;

    public CoursePublicService(
            CourseRepository courseRepository,
            CourseModuleRepository courseModuleRepository,
            CourseItemRepository courseItemRepository,
            CourseItemContentBlockRepository contentBlockRepository,
            CourseItemTestCaseRepository testCaseRepository,
            CourseItemHintRepository hintRepository,
            CourseItemOptionRepository optionRepository,
            PublicCourseMapper publicCourseMapper
    ) {
        this.courseRepository = courseRepository;
        this.courseModuleRepository = courseModuleRepository;
        this.courseItemRepository = courseItemRepository;
        this.contentBlockRepository = contentBlockRepository;
        this.testCaseRepository = testCaseRepository;
        this.hintRepository = hintRepository;
        this.optionRepository = optionRepository;
        this.publicCourseMapper = publicCourseMapper;
    }

    public CourseCatalogResponse getPublishedCourseCatalog() {
        List<Course> courses = courseRepository
                .findByStatusAndAccessTypeOrderByCreatedAtDesc(CourseStatus.PUBLISHED, CourseAccessType.PUBLIC);

        return new CourseCatalogResponse(
                courses.stream()
                        .map(publicCourseMapper::toCatalogItem)
                        .toList()
        );
    }

    public CourseDetailsResponse getPublishedCourseDetails(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .filter(this::isPubliclyReadableCourse)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        List<CourseModule> modules = courseModuleRepository.findByCourseIdOrderByOrderIndexAsc(course.getId());
        List<CourseItem> items = modules.stream()
                .flatMap(module -> courseItemRepository.findByModuleIdOrderByOrderIndexAsc(module.getId()).stream())
                .toList();

        return publicCourseMapper.toCourseDetails(course, modules, items);
    }

    public CourseItemPreviewResponse getPublishedCourseItemPreview(Long itemId) {
        CourseItem item = courseItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Course item not found"));

        Course course = item.getModule().getCourse();

        if (!isPubliclyReadableCourse(course)) {
            throw new ResourceNotFoundException("Course item not found");
        }

        return publicCourseMapper.toItemPreview(item);
    }

    private boolean isPubliclyReadableCourse(Course course) {
        return course.getStatus() == CourseStatus.PUBLISHED
                && (course.getAccessType() == CourseAccessType.PUBLIC
                || course.getAccessType() == CourseAccessType.UNLISTED);
    }
}
