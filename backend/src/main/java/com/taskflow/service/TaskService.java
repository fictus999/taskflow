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
            "/topic/projects/" + projectId + "/tas
