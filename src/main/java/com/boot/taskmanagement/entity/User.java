package com.boot.taskmanagement.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.Set;
import java.util.UUID;

@Entity
@Data @NoArgsConstructor @AllArgsConstructor
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(unique = true, nullable = false)
    private String username;
    
    private String password;
    
    @Enumerated(EnumType.STRING)
    private Role role;

    public boolean isAdmin() {
        return this.role == Role.ADMIN;
    }
}
