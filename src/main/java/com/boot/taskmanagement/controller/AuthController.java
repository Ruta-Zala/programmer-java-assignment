package com.boot.taskmanagement.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import com.boot.taskmanagement.model.Role;
import com.boot.taskmanagement.model.User;
import com.boot.taskmanagement.repository.UserRepository;
import com.boot.taskmanagement.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired private UserRepository userRepository;
    @Autowired private JwtUtil jwtUtil;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody User user) {
    Optional<User> existingUser = userRepository.findByUsername(user.getUsername());
    if (existingUser.isPresent()) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body("Username already exists");
    }
    user.setPassword(encoder.encode(user.getPassword()));
    userRepository.save(user);
    return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully");
}

    @PostMapping("/login")
    public String login(@RequestBody User user) {
        Optional<User> existingUser = userRepository.findByUsername(user.getUsername());

        if (existingUser.isPresent() && encoder.matches(user.getPassword(), existingUser.get().getPassword())) {
            Role role = existingUser.get().getRole();
            return jwtUtil.generateToken(user.getUsername(), role);
        }
        return "Invalid credentials";
    }
}