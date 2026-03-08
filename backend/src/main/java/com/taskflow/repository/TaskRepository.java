package com.taskflow.repository;

import com.taskflow.model.Task;
import com.taskflow.model.Task.TaskStatus;
import com.taskflow.model.Task.Priority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    Page<Task> findByProjectId(Long projectId, Pageable pageable);

    Page<Task> findByAssigneeId(Long userId, Pageable pageable);

    Page<Task> findByProjectIdAndStatus(Long projectId, TaskStatus status, Pageable pageable);

    @Query("""
        SELECT t FROM Task t
        WHERE t.project.id = :projectId
        AND (:status IS NULL OR t.status = :status)
        AND (:priority IS NULL OR t.priority = :priority)
        AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:assigneeId IS NULL OR t.assignee.id = :assigneeId)
        ORDER BY t.priority DESC, t.createdAt DESC
        """)
    Page<Task> searchTasks(
        @Param("projectId") Long projectId,
        @Param("status") TaskStatus status,
        @Param("priority") Priority priority,
        @Param("search") String search,
        @Param("assigneeId") Long assigneeId,
        Pageable pageable
    );

    @Query("SELECT t FROM Task t WHERE t.assignee.id = :userId AND t.dueDate < :now AND t.status != 'DONE'")
    List<Task> findOverdueTasks(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.project.id = :projectId AND t.status = :status")
    long countByProjectIdAndStatus(@Param("projectId") Long projectId, @Param("status") TaskStatus status);

    @Query("""
        SELECT t.status, COUNT(t) FROM Task t
        WHERE t.project.id = :projectId
        GROUP BY t.status
        """)
    List<Object[]> getStatusDistribution(@Param("projectId") Long projectId);
}
