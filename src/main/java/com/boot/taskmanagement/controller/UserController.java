package com.boot.taskmanagement.controller;

import com.boot.taskmanagement.dto.UserDTO;
import com.boot.taskmanagement.exception.ResourceNotFoundException;
import com.boot.taskmanagement.model.User;
import com.boot.taskmanagement.model.Role;
import com.boot.taskmanagement.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired 
    private UserRepository userRepository;

    // Get All Users (Admin Only)
    @GetMapping
    public List<UserDTO> getAllUsers() {
        String currentUsername = getAuthenticatedUsername();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!currentUser.isAdmin()) {
            throw new AccessDeniedException("Only admins can view all users");
        }

        return userRepository.findAll().stream()
            .filter(user -> user.getRole() == Role.USER)
            .map(user -> new UserDTO(user.getId(), user.getUsername(), user.getRole()))
            .toList();

    }

    // Delete User (Admin Only)
    @DeleteMapping("/{id}")
    public String deleteUser(@PathVariable UUID id) {
        String currentUsername = getAuthenticatedUsername();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!currentUser.isAdmin()) {
            throw new AccessDeniedException("Only admins can delete users");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        userRepository.delete(user);
        return "User deleted successfully";
    }

    // Helper method to get the currently authenticated username
    private String getAuthenticatedUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal().equals("anonymousUser")) {
            throw new AccessDeniedException("Unauthorized access");
        }
        return authentication.getName();
    }
}
