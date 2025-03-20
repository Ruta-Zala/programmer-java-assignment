package com.boot.taskmanagement.controller;

import com.boot.taskmanagement.dto.TaskDTO;
import com.boot.taskmanagement.exception.ResourceNotFoundException;
import com.boot.taskmanagement.model.Role;
import com.boot.taskmanagement.model.Status;
import com.boot.taskmanagement.model.Task;
import com.boot.taskmanagement.model.User;
import com.boot.taskmanagement.repository.TaskRepository;
import com.boot.taskmanagement.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TaskControllerTest {

    @InjectMocks
    private TaskController taskController;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
    }

    // Tests that an authenticated user can successfully create a new task
    @Test
    void testCreateTaskSuccess() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "user", "password", Role.USER);
        Task task = new Task(null, "Test Task", "Description", LocalDateTime.now(), Status.PENDING, null);
        Task savedTask = new Task(UUID.randomUUID(), "Test Task", "Description", LocalDateTime.now(), Status.PENDING, user);

        when(authentication.getName()).thenReturn("user");
        when(authentication.getPrincipal()).thenReturn("user");
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        TaskDTO result = taskController.createTask(task);

        assertNotNull(result.getId());
        assertEquals("Test Task", result.getTitle());
        assertEquals("user", result.getCreatedBy().getUsername());
        verify(taskRepository).save(any(Task.class));
    }

    // Tests that an unauthenticated (anonymous) user is denied access when trying to create a task
    @Test
    void testCreateTaskUnauthorized() {
        Task task = new Task(null, "Test Task", "Description", LocalDateTime.now(), Status.PENDING, null);
        when(authentication.getPrincipal()).thenReturn("anonymousUser");

        Exception exception = assertThrows(org.springframework.security.access.AccessDeniedException.class, 
            () -> taskController.createTask(task));
        assertEquals("Unauthorized access", exception.getMessage());
        verify(taskRepository, never()).save(any(Task.class));
    }

    // Tests that an admin can retrieve all tasks in the system
    @Test
    void testGetTasksAsAdmin() {
        UUID adminId = UUID.randomUUID();
        User admin = new User(adminId, "admin", "password", Role.ADMIN);
        Task task1 = new Task(UUID.randomUUID(), "Task 1", "Desc 1", LocalDateTime.now(), Status.PENDING, admin);
        Task task2 = new Task(UUID.randomUUID(), "Task 2", "Desc 2", LocalDateTime.now(), Status.IN_PROGRESS, admin);

        when(authentication.getName()).thenReturn("admin");
        when(authentication.getPrincipal()).thenReturn("admin");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(taskRepository.findAll()).thenReturn(List.of(task1, task2));

        List<TaskDTO> result = taskController.getTasks();

        assertEquals(2, result.size());
        assertEquals("Task 1", result.get(0).getTitle());
        assertEquals("Task 2", result.get(1).getTitle());
        verify(taskRepository).findAll();
    }

    // Tests that a regular user can retrieve only their own tasks
    @Test
    void testGetTasksAsUser() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "user", "password", Role.USER);
        Task task = new Task(UUID.randomUUID(), "Task 1", "Desc 1", LocalDateTime.now(), Status.PENDING, user);

        when(authentication.getName()).thenReturn("user");
        when(authentication.getPrincipal()).thenReturn("user");
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(taskRepository.findByCreatedBy_Id(userId)).thenReturn(List.of(task));

        List<TaskDTO> result = taskController.getTasks();

        assertEquals(1, result.size());
        assertEquals("Task 1", result.get(0).getTitle());
        verify(taskRepository).findByCreatedBy_Id(userId);
    }

    // Tests that a user can successfully retrieve a task they own
    @Test
    void testGetTaskSuccessAsOwner() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "user", "password", Role.USER);
        UUID taskId = UUID.randomUUID();
        Task task = new Task(taskId, "Task 1", "Desc 1", LocalDateTime.now(), Status.PENDING, user);

        when(authentication.getName()).thenReturn("user");
        when(authentication.getPrincipal()).thenReturn("user");
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        TaskDTO result = taskController.getTask(taskId);

        assertEquals("Task 1", result.getTitle());
        assertEquals("user", result.getCreatedBy().getUsername());
        verify(taskRepository).findById(taskId);
    }

    // Tests that an admin can retrieve any task, even if they don't own it
    @Test
    void testGetTaskSuccessAsAdmin() {
        UUID adminId = UUID.randomUUID();
        User admin = new User(adminId, "admin", "password", Role.ADMIN);
        UUID otherUserId = UUID.randomUUID();
        User otherUser = new User(otherUserId, "other", "password", Role.USER);
        UUID taskId = UUID.randomUUID();
        Task task = new Task(taskId, "Task 1", "Desc 1", LocalDateTime.now(), Status.PENDING, otherUser);

        when(authentication.getName()).thenReturn("admin");
        when(authentication.getPrincipal()).thenReturn("admin");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        TaskDTO result = taskController.getTask(taskId);

        assertEquals("Task 1", result.getTitle());
        assertEquals("other", result.getCreatedBy().getUsername());
        verify(taskRepository).findById(taskId);
    }

    // Tests that a non-owner, non-admin user is denied access when trying to retrieve someone else's task
    @Test
    void testGetTaskAccessDenied() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "user", "password", Role.USER);
        UUID otherUserId = UUID.randomUUID();
        User otherUser = new User(otherUserId, "other", "password", Role.USER);
        UUID taskId = UUID.randomUUID();
        Task task = new Task(taskId, "Task 1", "Desc 1", LocalDateTime.now(), Status.PENDING, otherUser);

        when(authentication.getName()).thenReturn("user");
        when(authentication.getPrincipal()).thenReturn("user");
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        Exception exception = assertThrows(org.springframework.security.access.AccessDeniedException.class, 
            () -> taskController.getTask(taskId));
        assertEquals("Unauthorized to view this task", exception.getMessage());
    }

    // Tests that a user can successfully update a task they own
    @Test
    void testUpdateTaskSuccessAsOwner() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "user", "password", Role.USER);
        UUID taskId = UUID.randomUUID();
        Task existingTask = new Task(taskId, "Old Title", "Old Desc", LocalDateTime.now(), Status.PENDING, user);
        Task updatedTask = new Task(null, "New Title", "New Desc", LocalDateTime.now(), Status.IN_PROGRESS, null);

        when(authentication.getName()).thenReturn("user");
        when(authentication.getPrincipal()).thenReturn("user");
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(existingTask));
        when(taskRepository.save(any(Task.class))).thenReturn(existingTask);

        TaskDTO result = taskController.updateTask(taskId, updatedTask);

        assertEquals("New Title", result.getTitle());
        assertEquals("New Desc", result.getDescription());
        verify(taskRepository).save(any(Task.class));
    }

    // Tests that updating a non-existent task results in a ResourceNotFoundException
    @Test
    void testUpdateTaskNotFound() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "user", "password", Role.USER);
        UUID taskId = UUID.randomUUID();
        Task updatedTask = new Task(null, "New Title", "New Desc", LocalDateTime.now(), Status.IN_PROGRESS, null);

        when(authentication.getName()).thenReturn("user");
        when(authentication.getPrincipal()).thenReturn("user");
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        Exception exception = assertThrows(ResourceNotFoundException.class, 
            () -> taskController.updateTask(taskId, updatedTask));
        assertEquals("Task not found", exception.getMessage());
        verify(taskRepository, never()).save(any(Task.class));
    }

    // Tests that an admin can successfully delete any task, even if they don't own it
    @Test
    void testDeleteTaskSuccessAsAdmin() {
        UUID adminId = UUID.randomUUID();
        User admin = new User(adminId, "admin", "password", Role.ADMIN);
        UUID otherUserId = UUID.randomUUID();
        User otherUser = new User(otherUserId, "other", "password", Role.USER);
        UUID taskId = UUID.randomUUID();
        Task task = new Task(taskId, "Task 1", "Desc 1", LocalDateTime.now(), Status.PENDING, otherUser);

        when(authentication.getName()).thenReturn("admin");
        when(authentication.getPrincipal()).thenReturn("admin");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        String result = taskController.deleteTask(taskId);

        assertEquals("Task deleted successfully", result);
        verify(taskRepository).delete(task);
    }

    // Tests that a non-owner, non-admin user is denied access when trying to delete someone else's task
    @Test
    void testDeleteTaskAccessDenied() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "user", "password", Role.USER);
        UUID otherUserId = UUID.randomUUID();
        User otherUser = new User(otherUserId, "other", "password", Role.USER);
        UUID taskId = UUID.randomUUID();
        Task task = new Task(taskId, "Task 1", "Desc 1", LocalDateTime.now(), Status.PENDING, otherUser);

        when(authentication.getName()).thenReturn("user");
        when(authentication.getPrincipal()).thenReturn("user");
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        Exception exception = assertThrows(org.springframework.security.access.AccessDeniedException.class, 
            () -> taskController.deleteTask(taskId));
        assertEquals("Unauthorized to delete this task", exception.getMessage());
        verify(taskRepository, never()).delete(any(Task.class));
    }
}