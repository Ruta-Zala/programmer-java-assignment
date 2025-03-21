package com.boot.taskmanagement.controller;

import com.boot.taskmanagement.dto.UserDTO;
import com.boot.taskmanagement.exception.ResourceNotFoundException;
import com.boot.taskmanagement.model.Role;
import com.boot.taskmanagement.model.User;
import com.boot.taskmanagement.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserControllerTest {

    @InjectMocks
    private UserController userController;

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

    // Tests that an admin can successfully retrieve a list of all users with USER role
    @Test
    void testGetAllUsersSuccessAsAdmin() {
        UUID adminId = UUID.randomUUID();
        User adminUser = new User(adminId, "admin", "password", Role.ADMIN);
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        User user1 = new User(userId1, "user1", "password1", Role.USER);
        User user2 = new User(userId2, "user2", "password2", Role.USER);

        when(authentication.getName()).thenReturn("admin");
        when(authentication.getPrincipal()).thenReturn("admin");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(userRepository.findAll()).thenReturn(List.of(adminUser, user1, user2));

        List<UserDTO> result = userController.getAllUsers();

        assertEquals(2, result.size());
        assertEquals("user1", result.get(0).getUsername());
        assertEquals(Role.USER, result.get(0).getRole());
        assertEquals("user2", result.get(1).getUsername());
        assertEquals(Role.USER, result.get(1).getRole());
        verify(userRepository).findAll();
    }

    // Tests that a non-admin user is denied access when trying to retrieve all users
    @Test
    void testGetAllUsersAccessDeniedAsNonAdmin() {
        UUID userId = UUID.randomUUID();
        User nonAdminUser = new User(userId, "user", "password", Role.USER);

        when(authentication.getName()).thenReturn("user");
        when(authentication.getPrincipal()).thenReturn("user");
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(nonAdminUser));

        Exception exception = assertThrows(org.springframework.security.access.AccessDeniedException.class, 
            () -> userController.getAllUsers());
        assertEquals("Only admins can view all users", exception.getMessage());
        verify(userRepository, never()).findAll();
    }

    // Tests that an unauthenticated (anonymous) user is denied access to retrieve all users
    @Test
    void testGetAllUsersUnauthorized() {
        when(authentication.getPrincipal()).thenReturn("anonymousUser");

        Exception exception = assertThrows(org.springframework.security.access.AccessDeniedException.class, 
            () -> userController.getAllUsers());
        assertEquals("Unauthorized access", exception.getMessage());
        verify(userRepository, never()).findByUsername(anyString());
        verify(userRepository, never()).findAll();
    }

    // Tests that an admin can successfully delete a user by ID
    @Test
    void testDeleteUserSuccessAsAdmin() {
        UUID adminId = UUID.randomUUID();
        User adminUser = new User(adminId, "admin", "password", Role.ADMIN);
        UUID userId = UUID.randomUUID();
        User userToDelete = new User(userId, "userToDelete", "password", Role.USER);

        when(authentication.getName()).thenReturn("admin");
        when(authentication.getPrincipal()).thenReturn("admin");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(userId)).thenReturn(Optional.of(userToDelete));

        String result = userController.deleteUser(userId);

        assertEquals("User deleted successfully", result);
        verify(userRepository).delete(userToDelete);
    }

    // Tests that a non-admin user is denied access when trying to delete a user
    @Test
    void testDeleteUserAccessDeniedAsNonAdmin() {
        UUID userId = UUID.randomUUID();
        User nonAdminUser = new User(userId, "user", "password", Role.USER);
        UUID targetId = UUID.randomUUID();

        when(authentication.getName()).thenReturn("user");
        when(authentication.getPrincipal()).thenReturn("user");
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(nonAdminUser));

        Exception exception = assertThrows(org.springframework.security.access.AccessDeniedException.class, 
            () -> userController.deleteUser(targetId));
        assertEquals("Only admins can delete users", exception.getMessage());
        verify(userRepository, never()).findById(any(UUID.class));
        verify(userRepository, never()).delete(any(User.class));
    }

    // Tests that an admin gets a ResourceNotFoundException when trying to delete a non-existent user
    @Test
    void testDeleteUserNotFound() {
        UUID adminId = UUID.randomUUID();
        User adminUser = new User(adminId, "admin", "password", Role.ADMIN);
        UUID targetId = UUID.randomUUID();

        when(authentication.getName()).thenReturn("admin");
        when(authentication.getPrincipal()).thenReturn("admin");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(targetId)).thenReturn(Optional.empty());

        Exception exception = assertThrows(ResourceNotFoundException.class, 
            () -> userController.deleteUser(targetId));
        assertEquals("User not found", exception.getMessage());
        verify(userRepository, never()).delete(any(User.class));
    }

    // Tests that an unauthenticated (anonymous) user is denied access when trying to delete a user
    @Test
    void testDeleteUserUnauthorized() {
        UUID targetId = UUID.randomUUID();
        when(authentication.getPrincipal()).thenReturn("anonymousUser");

        Exception exception = assertThrows(org.springframework.security.access.AccessDeniedException.class, 
            () -> userController.deleteUser(targetId));
        assertEquals("Unauthorized access", exception.getMessage());
        verify(userRepository, never()).findByUsername(anyString());
        verify(userRepository, never()).findById(any(UUID.class));
        verify(userRepository, never()).delete(any(User.class));
    }
}