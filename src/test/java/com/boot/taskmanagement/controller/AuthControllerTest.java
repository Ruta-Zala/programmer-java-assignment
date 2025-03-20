package com.boot.taskmanagement.controller;

import com.boot.taskmanagement.model.Role;
import com.boot.taskmanagement.model.User;
import com.boot.taskmanagement.repository.UserRepository;
import com.boot.taskmanagement.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;


@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired 
    private MockMvc mockMvc;

    @MockBean 
    private UserRepository userRepository;

    @MockBean 
    private JwtUtil jwtUtil;

    @MockBean
    private BCryptPasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User(null, "testuser", "encodedPassword", Role.USER);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken("testuser", Role.USER)).thenReturn("fake-jwt-token");
        when(passwordEncoder.matches("password", "encodedPassword")).thenReturn(true);
    }


    //Test user registration: successful case.
    @Test
    void testRegisterUser_Success() throws Exception {
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"newuser\", \"password\":\"password\"}"))
                .andExpect(status().isCreated())
                .andExpect(content().string("User registered successfully"));
    }

    // Test user registration: username already exists.
    @Test
    void testRegisterUser_UsernameAlreadyExists() throws Exception {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"testuser\", \"password\":\"password\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().string("Username already exists"));
    }


    //Test login with invalid credentials.
    @Test
    void testLoginUser_InvalidCredentials() throws Exception {
        when(passwordEncoder.matches("wrongpassword", "encodedPassword")).thenReturn(false);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"testuser\", \"password\":\"wrongpassword\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Invalid credentials"));
    }

    //Test login when user does not exist.
    @Test
    void testLoginUser_UserNotFound() throws Exception {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"nonexistent\", \"password\":\"password\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Invalid credentials"));
    }
}
