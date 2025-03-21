package com.boot.taskmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import com.boot.taskmanagement.model.Status;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
public class TaskDTO {
    private UUID id;
    private String title;
    private String description;
    private LocalDateTime dueDate;
    private String status;
    private UserDTO createdBy;
}
