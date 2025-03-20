# Task Management System - REST API

## Objective

This project is a Spring Boot-based REST API for a Task Management System. It allows users to perform CRUD operations on tasks with authentication and authorization using Spring Security and JWT. No frontend development is included.

---

## Project Scope

### Functional Requirements

#### User Authentication & Authorization

- Users can register and log in.
- JWT-based authentication.
- Role-based access control: **Admin** and **User** roles.

#### Task Management

- **Create Task:** Users can create a task with a title, description, due date, and status.
- **Retrieve Tasks:** Users can fetch tasks (all or by specific criteria such as status or due date).
- **Update Task:** Users can update their own tasks.
- **Delete Task:** Users can delete their own tasks.
- **Admin Privileges:** Admin users can manage all tasks (CRUD operations for all users' tasks).

#### User Management (Admin Only)

- Admins can view all registered users.
- Admins can delete users.

#### Security Features

- Secure password hashing.
- JWT-based authentication.
- Role-based endpoint protection.
- Rate limiting to prevent brute force attacks (optional but preferred).

---

## Technical Requirements

### Tech Stack

- **Spring Boot** (Spring MVC, Spring Security, Spring Data JPA)
- **JWT** for authentication
- **PostgreSQL/MySQL** as the database
- **JPA/Hibernate** for ORM
- **Lombok** for reducing boilerplate code
- **Maven/Gradle** for dependency management
- **Postman/Swagger** for API testing

### API Endpoints

#### Authentication APIs

| Method | Endpoint           | Description                      |
| ------ | ------------------ | -------------------------------- |
| POST   | /api/auth/register | Register a new user              |
| POST   | /api/auth/login    | Authenticate user and return JWT |

#### User APIs (Admin Only)

| Method | Endpoint            | Description       |
| ------ | ------------------- | ----------------- |
| GET    | /api/users          | Get list of users |
| DELETE | /api/users/{userId} | Delete a user     |

#### Task APIs

| Method | Endpoint        | Description                                            |
| ------ | --------------- | ------------------------------------------------------ |
| POST   | /api/tasks      | Create a new task (User-specific)                      |
| GET    | /api/tasks      | Get all tasks (Admin can see all, Users see their own) |
| GET    | /api/tasks/{id} | Get a specific task                                    |
| PUT    | /api/tasks/{id} | Update a task (Only owner or admin)                    |
| DELETE | /api/tasks/{id} | Delete a task (Only owner or admin)                    |

---

## Implementation Guidelines

### Spring Security Configuration

- Configure JWT authentication filter.
- Define `UserDetailsService` for user authentication.
- Secure endpoints based on user roles.

### Database Schema

- **User Table:** id, username, password, role (USER/ADMIN)
- **Task Table:** id, title, description, status, due\_date, created\_by (user\_id)

### Error Handling

- Proper exception handling with meaningful HTTP status codes.
- Global exception handler using `@ControllerAdvice`.

### Logging & Monitoring

- Implement logging using SLF4J/Logback.
- Use Actuator for health checks.

---

## Setup Instructions

### Prerequisites

- Java 17+
- PostgreSQL/MySQL database
- Maven/Gradle

### Steps to Run the Project

1. Clone the repository:
   ```sh
   git clone <repository-url>
   cd task-management-system
   ```
2. Configure database credentials in `application.properties`.
3. Build the project using Maven:
   ```sh
   mvn clean install
   ```
4. Run the application:
   ```sh
   mvn spring-boot:run
   ```
5. Test the application:
   ```sh
   mvn clean test
   ```
6. Access APIs via Postman or Swagger.

---


