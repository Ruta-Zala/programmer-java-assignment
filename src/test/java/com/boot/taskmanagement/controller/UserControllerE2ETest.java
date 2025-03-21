package com.boot.taskmanagement.controller;

import com.boot.taskmanagement.dto.UserDTO;
import com.boot.taskmanagement.model.Role;
import com.boot.taskmanagement.model.User;
import com.boot.taskmanagement.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserControllerE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String baseUrl;
    private UUID adminId;
    private UUID userId;
    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        baseUrl = "http://localhost:" + port + "/api/users";
        registerUser("admin", "adminPass", "ADMIN");
        registerUser("user", "userPass", "USER");
        adminToken = getJwtToken("admin", "adminPass");
        userToken = getJwtToken("user", "userPass");
        adminId = userRepository.findByUsername("admin").orElseThrow().getId();
        userId = userRepository.findByUsername("user").orElseThrow().getId();
    }

    private void registerUser(String username, String password, String role) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> user = new HashMap<>();
        user.put("username", username);
        user.put("password", password);
        user.put("role", role);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(user, headers);
        ResponseEntity<String> response = restTemplate.postForEntity("/api/auth/register", request, String.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    private String getJwtToken(String username, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String loginBody = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        HttpEntity<String> request = new HttpEntity<>(loginBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/auth/login",
            request,
            String.class
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());
        String token = response.getBody();
        assertNotEquals("Invalid credentials", token);
        return token;
    }

    private HttpEntity<?> getAuthenticatedRequest(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        return new HttpEntity<>(headers);
    }

    // Tests that an admin can successfully retrieve all users
    @Test
    @Order(1)
    void testGetAllUsers_Success_Admin() {
        ResponseEntity<UserDTO[]> response = restTemplate.exchange(
            baseUrl,
            HttpMethod.GET,
            getAuthenticatedRequest(adminToken),
            UserDTO[].class
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());
        UserDTO[] users = response.getBody();
        assertNotNull(users);
        assertEquals(1, users.length);
        assertEquals("user", users[0].getUsername());
        assertEquals(Role.USER, users[0].getRole());
    }

    // Tests that a non-admin user is denied access to retrieve all users
    @Test
    @Order(2)
    void testGetAllUsers_AccessDenied_NonAdmin() {
        ResponseEntity<String> response = restTemplate.exchange(
            baseUrl,
            HttpMethod.GET,
            getAuthenticatedRequest(userToken),
            String.class
        );
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    // Tests that an admin can successfully delete a user
    @Test
    @Order(3)
    void testDeleteUser_Success_Admin() {
        ResponseEntity<String> response = restTemplate.exchange(
            baseUrl + "/" + userId,
            HttpMethod.DELETE,
            getAuthenticatedRequest(adminToken),
            String.class
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("User deleted successfully", response.getBody());
        assertFalse(userRepository.findById(userId).isPresent());
    }

    // Tests that a non-admin user is denied permission to delete a user
    @Test
    @Order(4)
    void testDeleteUser_AccessDenied_NonAdmin() {
        ResponseEntity<String> response = restTemplate.exchange(
            baseUrl + "/" + userId,
            HttpMethod.DELETE,
            getAuthenticatedRequest(userToken),
            String.class
        );
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertTrue(userRepository.findById(userId).isPresent());
    }

    // Tests that attempting to delete a non-existent user returns a not found response
    @Test
    @Order(5)
    void testDeleteUser_UserNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        ResponseEntity<String> response = restTemplate.exchange(
            baseUrl + "/" + nonExistentId,
            HttpMethod.DELETE,
            getAuthenticatedRequest(adminToken),
            String.class
        );
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // Tests that an unauthenticated request to retrieve all users is forbidden
    @Test
    @Order(6)
    void testGetAllUsers_Unauthenticated() {
        ResponseEntity<String> response = restTemplate.exchange(
            baseUrl,
            HttpMethod.GET,
            new HttpEntity<>(new HttpHeaders()),
            String.class
        );
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }
}