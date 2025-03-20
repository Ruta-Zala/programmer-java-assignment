package com.boot.taskmanagement.controller;

import com.boot.taskmanagement.model.Task;
import com.boot.taskmanagement.model.Status;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import com.fasterxml.jackson.core.type.TypeReference;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class TaskControllerE2ETest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private String userToken;
    private String adminToken;
    private String taskId;

    @BeforeEach
    public void setup() {
      restTemplate.delete("/api/tasks/clear");
        // Clear any existing data if needed
        registerUser("user1", "pass123", "USER");
        registerUser("admin1", "admin123", "ADMIN");
    }

    private void registerUser(String username, String password, String role) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> user = new HashMap<>();
        user.put("username", username);
        user.put("password", password);
        user.put("role", role);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(user, headers);
        restTemplate.postForEntity("/api/auth/register", request, String.class);
    }

    @Test
    public void testTaskLifecycle() throws Exception {
        // Step 1: User Login
        userToken = login("user1", "pass123");
        assertNotNull(userToken, "User token should not be null");

        // Step 2: Create Task
        Map<String, Object> newTask = new HashMap<>();
        newTask.put("title", "Complete Project Report");
        newTask.put("description", "Finish the quarterly report");
        newTask.put("dueDate", "2025-03-25T10:00:00");
        newTask.put("status", "PENDING");

        HttpHeaders userHeaders = new HttpHeaders();
        userHeaders.setBearerAuth(userToken);
        HttpEntity<Map<String, Object>> createRequest = new HttpEntity<>(newTask, userHeaders);

        ResponseEntity<String> createResponse = restTemplate.postForEntity("/api/tasks", createRequest, String.class);
        assertEquals(HttpStatus.OK, createResponse.getStatusCode());
        Map<String, Object> taskResponse = objectMapper.readValue(createResponse.getBody(), Map.class);
        taskId = taskResponse.get("id").toString();
        assertEquals("Complete Project Report", taskResponse.get("title"));

        // Step 3: Get All Tasks (Non-Admin)
       ResponseEntity<String> getAllResponse = restTemplate.exchange("/api/tasks", HttpMethod.GET, 
        new HttpEntity<>(userHeaders), String.class);
        assertEquals(HttpStatus.OK, getAllResponse.getStatusCode());

        // ✅ Corrected: Deserialize as List of Maps instead of String[]
        List<Map<String, Object>> tasks = objectMapper.readValue(getAllResponse.getBody(), new TypeReference<List<Map<String, Object>>>() {});
        assertEquals(1, tasks.size());

        // Step 4: Get Specific Task
        ResponseEntity<String> getTaskResponse = restTemplate.exchange("/api/tasks/" + taskId, HttpMethod.GET, 
                new HttpEntity<>(userHeaders), String.class);
        assertEquals(HttpStatus.OK, getTaskResponse.getStatusCode());
        Map<String, Object> task = objectMapper.readValue(getTaskResponse.getBody(), Map.class);
        assertEquals("Complete Project Report", task.get("title"));

        // Step 5: Update Task
        Map<String, Object> updatedTask = new HashMap<>();
        updatedTask.put("title", "Complete Updated Report");
        updatedTask.put("description", "Finish the updated quarterly report");
        updatedTask.put("dueDate", "2025-03-26T10:00:00");
        updatedTask.put("status", "IN_PROGRESS");

        HttpEntity<Map<String, Object>> updateRequest = new HttpEntity<>(updatedTask, userHeaders);
        ResponseEntity<String> updateResponse = restTemplate.exchange("/api/tasks/" + taskId, HttpMethod.PUT, 
                updateRequest, String.class);
        assertEquals(HttpStatus.OK, updateResponse.getStatusCode());
        Map<String, Object> updatedTaskResponse = objectMapper.readValue(updateResponse.getBody(), Map.class);
        assertEquals("Complete Updated Report", updatedTaskResponse.get("title"));
        assertEquals("IN_PROGRESS", updatedTaskResponse.get("status"));

        // Step 6: Verify Updated Task
        ResponseEntity<String> verifyUpdateResponse = restTemplate.exchange("/api/tasks/" + taskId, HttpMethod.GET, 
                new HttpEntity<>(userHeaders), String.class);
        assertEquals(HttpStatus.OK, verifyUpdateResponse.getStatusCode());
        Map<String, Object> verifiedTask = objectMapper.readValue(verifyUpdateResponse.getBody(), Map.class);
        assertEquals("IN_PROGRESS", verifiedTask.get("status"));

        // Step 7: Delete Task
        ResponseEntity<String> deleteResponse = restTemplate.exchange("/api/tasks/" + taskId, HttpMethod.DELETE, 
                new HttpEntity<>(userHeaders), String.class);
        assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
        assertEquals("Task deleted successfully", deleteResponse.getBody());

        // Step 8: Verify Task Deletion (Non-Admin)
        ResponseEntity<String> verifyDeleteResponse = restTemplate.exchange("/api/tasks/" + taskId, HttpMethod.GET, 
                new HttpEntity<>(userHeaders), String.class);
        assertEquals(HttpStatus.NOT_FOUND, verifyDeleteResponse.getStatusCode());

        // Step 9: Admin Login
        adminToken = login("admin1", "admin123");
        assertNotNull(adminToken, "Admin token should not be null");

        // Step 10: Admin Verifies Task Deletion
        HttpHeaders adminHeaders = new HttpHeaders();
        adminHeaders.setBearerAuth(adminToken);
        ResponseEntity<String> adminVerifyResponse = restTemplate.exchange("/api/tasks/" + taskId, HttpMethod.GET, 
                new HttpEntity<>(adminHeaders), String.class);
        assertEquals(HttpStatus.NOT_FOUND, adminVerifyResponse.getStatusCode());

        // Step 11: Admin Retrieves All Tasks
        ResponseEntity<String> adminGetAllResponse = restTemplate.exchange("/api/tasks", HttpMethod.GET, 
                new HttpEntity<>(adminHeaders), String.class);
        assertEquals(HttpStatus.OK, adminGetAllResponse.getStatusCode());
        String[] adminTasks = objectMapper.readValue(adminGetAllResponse.getBody(), String[].class);
        assertTrue(adminTasks.length == 0 || !containsTaskId(adminTasks, taskId));
    }

    private String login(String username, String password) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", username);
        loginRequest.put("password", password);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(loginRequest, headers);

        ResponseEntity<String> response = restTemplate.postForEntity("/api/auth/login", entity, String.class);

        if (response.getStatusCode() != HttpStatus.OK) {
            fail("Login failed for " + username + ": " + response.getBody());
        }

        String responseBody = response.getBody();
        
        // ✅ Since login returns plain text, we check if it's "Invalid credentials"
        if ("Invalid credentials".equals(responseBody)) {
            fail("Login failed due to incorrect credentials");
            return null;
        }

        return responseBody; // ✅ Now returning the token correctly
    }




    private boolean containsTaskId(String[] tasks, String taskId) throws Exception {
        for (String taskJson : tasks) {
            Map<String, Object> task = objectMapper.readValue(taskJson, Map.class);
            if (task.get("id").toString().equals(taskId)) {
                return true;
            }
        }
        return false;
    }
}