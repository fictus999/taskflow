package com.taskflow.service;

import com.taskflow.dto.TaskCreateRequest;
import com.taskflow.dto.TaskResponse;
import com.taskflow.dto.TaskUpdateRequest;
import com.taskflow.model.Task;
import com.taskflow.model.Task.TaskStatus;
import com.taskflow.model.Task.Priority;
import com.taskflow.model.User;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;
import com.taskflow.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final EmailService emailService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    @Cacheable(value = "tasks", key = "#projectId + '-' + #pageable.pageNumber")
    public Page<TaskResponse> getTasks(Long projectId, TaskStatus status, Priority priority,
                                        String search, Long assigneeId, Pageable pageable) {
        return taskRepository
            .searchTasks(projectId, status, priority, search, assigneeId, pageable)
            .map(TaskResponse::from);
    }

    @Transactional(readOnly = true)
    public TaskResponse getTaskById(Long id, User currentUser) {
        Task task = findTaskOrThrow(id);
        assertProjectAccess(task.getProject().getId(), currentUser);
        return TaskResponse.from(task);
    }

    @CacheEvict(value = "tasks", allEntries = true)
    public TaskResponse createTask(Long projectId, TaskCreateRequest req, User creator) {
        var project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));

        User assignee = req.getAssigneeId() != null
            ? userRepository.findById(req.getAssigneeId()).orElse(null)
            : null;

        Task task = Task.builder()
            .title(req.getTitle())
            .description(req.getDescription())
            .status(TaskStatus.TODO)
            .priority(req.getPriority() != null ? req.getPriority() : Priority.MEDIUM)
            .dueDate(req.getDueDate())
            .storyPoints(req.getStoryPoints())
            .estimatedHours(req.getEstimatedHours())
            .project(project)
            .assignee(assignee)
            .creator(creator)
            .build();

        Task saved = taskRepository.save(task);
        log.info("Task created: id={}, title='{}', project={}", saved.getId(), saved.getTitle(), projectId);

        if (assignee != null) {
            emailService.sendTaskAssignedEmail(assignee, saved);
        }

        messagingTemplate.convertAndSend(
            "/topic/projects/" + projectId + "/tasks",
            Map.of("event", "TASK_CREATED", "task", TaskResponse.from(saved))
        );

        auditService.log("TASK", saved.getId(), "CREATE", creator, null, saved);
        return TaskResponse.from(saved);
    }

    @CacheEvict(value = "tasks", allEntries = true)
    public TaskResponse updateTask(Long id, TaskUpdateRequest req, User currentUser) {
        Task task = findTaskOrThrow(id);
        assertProjectAccess(task.getProject().getId(), currentUser);

        if (req.getTitle() != null) task.setTitle(req.getTitle());
        if (req.getDescription() != null) task.setDescription(req.getDescription());
        if (req.getStatus() != null) task.setStatus(req.getStatus());
        if (req.getPriority() != null) task.setPriority(req.getPriority());
        if (req.getDueDate() != null) task.setDueDate(req.getDueDate());
        if (req.getStoryPoints() != null) task.setStoryPoints(req.getStoryPoints());
        if (req.getLoggedHours() != null) task.setLoggedHours(req.getLoggedHours());

        if (req.getAssigneeId() != null) {
            User newAssignee = userRepository.findById(req.getAssigneeId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            task.setAssignee(newAssignee);
            emailService.sendTaskAssignedEmail(newAssignee, task);
        }

        Task saved = taskRepository.save(task);

        messagingTemplate.convertAndSend(
            "/topic/projects/" + task.getProject().getId() + "/tasks",
            Map.of("event", "TASK_UPDATED", "task", TaskResponse.from(saved))
        );

        auditService.log("TASK", saved.getId(), "UPDATE", currentUser, null, saved);
        return TaskResponse.from(saved);
    }

    @CacheEvict(value = "tasks", allEntries = true)
    public void deleteTask(Long id, User currentUser) {
        Task task = findTaskOrThrow(id);

        if (!currentUser.getRole().equals(User.Role.ADMIN) &&
            !task.getCreator().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Insufficient permissions to delete this task");
        }

        Long projectId = task.getProject().getId();
        taskRepository.delete(task);

        messagingTemplate.convertAndSend(
            "/topic/projects/" + projectId + "/tasks",
            Map.of("event", "TASK_DELETED", "taskId", id)
        );

        auditService.log("TASK", id, "DELETE", currentUser, task, null);
        log.info("Task deleted: id={} by user={}", id, currentUser.getId());
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getProjectStats(Long projectId) {
        return Map.of(
            "total", taskRepository.count(),
            "todo", taskRepository.countByProjectIdAndStatus(projectId, TaskStatus.TODO),
            "inProgress", taskRepository.countByProjectIdAndStatus(projectId, TaskStatus.IN_PROGRESS),
            "done", taskRepository.countByProjectIdAndStatus(projectId, TaskStatus.DONE)
        );
    }

    private Task findTaskOrThrow(Long id) {
        return taskRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + id));
    }

    private void assertProjectAccess(Long projectId, User user) {
        if (user.getRole() == User.Role.ADMIN) return;
        var project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        boolean isMember = project.getMembers().stream()
            .anyMatch(m -> m.getId().equals(user.getId()));
        boolean isOwner = project.getOwner().getId().equals(user.getId());
        if (!isMember && !isOwner) {
            throw new AccessDeniedException("You are not a member of this project");
        }
    }
}
