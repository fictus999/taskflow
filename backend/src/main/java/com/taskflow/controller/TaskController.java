package com.taskflow.controller;

import com.taskflow.dto.*;
import com.taskflow.model.Task.TaskStatus;
import com.taskflow.model.Task.Priority;
import com.taskflow.model.User;
import com.taskflow.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Task management operations")
@SecurityRequirement(name = "bearerAuth")
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    @Operation(summary = "Get paginated tasks for a project")
    public ResponseEntity<Page<TaskResponse>> getTasks(
        @PathVariable Long projectId,
        @RequestParam(required = false) TaskStatus status,
        @RequestParam(required = false) Priority priority,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) Long assigneeId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "createdAt") String sortBy,
        @RequestParam(defaultValue = "desc") String sortDir,
        @AuthenticationPrincipal User currentUser
    ) {
        Pageable pageable = PageRequest.of(page, size,
            Sort.by(sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy));
        return ResponseEntity.ok(
            taskService.getTasks(projectId, status, priority, search, assigneeId, pageable)
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a specific task by ID")
    public ResponseEntity<TaskResponse> getTask(
        @PathVariable Long projectId,
        @PathVariable Long id,
        @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(taskService.getTaskById(id, currentUser));
    }

    @PostMapping
    @Operation(summary = "Create a new task")
    public ResponseEntity<TaskResponse> createTask(
        @PathVariable Long projectId,
        @Valid @RequestBody TaskCreateRequest req,
        @AuthenticationPrincipal User currentUser
    ) {
        TaskResponse created = taskService.createTask(projectId, req, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing task")
    public ResponseEntity<TaskResponse> updateTask(
        @PathVariable Long projectId,
        @PathVariable Long id,
        @Valid @RequestBody TaskUpdateRequest req,
        @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(taskService.updateTask(id, req, currentUser));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a task")
    public ResponseEntity<Void> deleteTask(
        @PathVariable Long projectId,
        @PathVariable Long id,
        @AuthenticationPrincipal User currentUser
    ) {
        taskService.deleteTask(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    @Operation(summary = "Get task statistics for a project")
    public ResponseEntity<Map<String, Long>> getStats(@PathVariable Long projectId) {
        return ResponseEntity.ok(taskService.getProjectStats(projectId));
    }
}
