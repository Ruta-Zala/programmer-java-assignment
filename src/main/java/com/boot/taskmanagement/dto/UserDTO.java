package com.boot.taskmanagement.dto;

import com.boot.taskmanagement.model.Role;
import java.util.UUID;

public class UserDTO {
    private UUID id;
    private String username;
    private Role role;

    public UserDTO(UUID id, String username, Role role) {
        this.id = id;
        this.username = username;
        this.role = role;
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public Role getRole() {
        return role;
    }
}
