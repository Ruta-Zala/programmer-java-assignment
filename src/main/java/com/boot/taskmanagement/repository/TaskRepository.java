package com.boot.taskmanagement.repository;

import com.boot.taskmanagement.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {
    List<Task> findByCreatedBy_Id(UUID userId);
}
