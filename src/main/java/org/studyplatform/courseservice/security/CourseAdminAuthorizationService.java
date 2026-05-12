package org.studyplatform.courseservice.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.studyplatform.courseservice.repository.CourseItemRepository;
import org.studyplatform.courseservice.repository.CourseModuleRepository;
import org.studyplatform.courseservice.repository.CourseRepository;

import java.util.Optional;

@Service("courseAdminAuthorizationService")
@RequiredArgsConstructor
public class CourseAdminAuthorizationService {

    private final CurrentUserService currentUserService;
    private final CourseRepository courseRepository;
    private final CourseModuleRepository moduleRepository;
    private final CourseItemRepository itemRepository;

    public boolean canCreateCourseFor(Long createdByUserId) {
        if (currentUserService.isAdmin()) {
            return true;
        }

        if (!currentUserService.isTeacher() || createdByUserId == null) {
            return false;
        }

        return currentUserService.getCurrentUserId()
                .map(createdByUserId::equals)
                .orElse(false);
    }

    public boolean canManageCourse(Long courseId) {
        if (currentUserService.isAdmin()) {
            return true;
        }

        if (!currentUserService.isTeacher()) {
            return false;
        }

        Optional<Long> ownerId = courseRepository.findCreatedByUserIdById(courseId);
        return ownerId.isEmpty() || isCurrentUser(ownerId.get());
    }

    public boolean canManageModule(Long moduleId) {
        if (currentUserService.isAdmin()) {
            return true;
        }

        if (!currentUserService.isTeacher()) {
            return false;
        }

        Optional<Long> ownerId = moduleRepository.findCourseOwnerIdByModuleId(moduleId);
        return ownerId.isEmpty() || isCurrentUser(ownerId.get());
    }

    public boolean canManageItem(Long itemId) {
        if (currentUserService.isAdmin()) {
            return true;
        }

        if (!currentUserService.isTeacher()) {
            return false;
        }

        Optional<Long> ownerId = itemRepository.findCourseOwnerIdByItemId(itemId);
        return ownerId.isEmpty() || isCurrentUser(ownerId.get());
    }

    private boolean isCurrentUser(Long ownerId) {
        return currentUserService.getCurrentUserId()
                .map(ownerId::equals)
                .orElse(false);
    }
}
