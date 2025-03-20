package com.boot.taskmanagement.controller;

import java.util.stream.Collectors;
import com.boot.taskmanagement.dto.TaskDTO;
import com.boot.taskmanagement.dto.UserDTO;
import com.boot.taskmanagement.exception.ResourceNotFoundException;
import com.boot.taskmanagement.model.Task;
import com.boot.taskmanagement.model.User;
import com.boot.taskmanagement.repository.TaskRepository;
import com.boot.taskmanagement.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    @Autowired private TaskRepository taskRepository;
    @Autowired private UserRepository userRepository;

    @PostMapping
    public TaskDTO createTask(@RequestBody Task task) {
        User user = getAuthenticatedUser();
        task.setCreatedBy(user);
        Task savedTask = taskRepository.save(task);
        return convertToDTO(savedTask);
    }

    @GetMapping
    public List<TaskDTO> getTasks() {
        User user = getAuthenticatedUser();
        List<Task> tasks = user.isAdmin() ? taskRepository.findAll() : taskRepository.findByCreatedBy_Id(user.getId());
        return tasks.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public TaskDTO getTask(@PathVariable UUID id) {
        Task task = taskRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        User user = getAuthenticatedUser();

        if (!user.isAdmin() && !task.getCreatedBy().getId().equals(user.getId())) {
            throw new AccessDeniedException("Unauthorized to view this task");
        }
        return convertToDTO(task);
    }

    @PutMapping("/{id}")
    public TaskDTO updateTask(@PathVariable UUID id, @RequestBody Task updatedTask) {
        Task task = taskRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        User user = getAuthenticatedUser();

        if (!user.isAdmin() && !task.getCreatedBy().getId().equals(user.getId())) {
            throw new AccessDeniedException("Unauthorized to edit this task");
        }

        task.setTitle(updatedTask.getTitle());
        task.setDescription(updatedTask.getDescription());
        task.setDueDate(updatedTask.getDueDate());
        task.setStatus(updatedTask.getStatus());
        Task updated = taskRepository.save(task);
        return convertToDTO(updated);
    }

    @DeleteMapping("/{id}")
    public String deleteTask(@PathVariable UUID id) {
        Task task = taskRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        User user = getAuthenticatedUser();

        if (!user.isAdmin() && !task.getCreatedBy().getId().equals(user.getId())) {
            throw new AccessDeniedException("Unauthorized to delete this task");
        }

        taskRepository.delete(task);
        return "Task deleted successfully";
    }

    private TaskDTO convertToDTO(Task task) {
        return new TaskDTO(
            task.getId(),
            task.getTitle(),
            task.getDescription(),
            task.getDueDate(),
            task.getStatus().name(),
            new UserDTO(task.getCreatedBy().getId(), task.getCreatedBy().getUsername(), task.getCreatedBy().getRole())
        );
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal().equals("anonymousUser")) {
            throw new AccessDeniedException("Unauthorized access");
        }

        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
