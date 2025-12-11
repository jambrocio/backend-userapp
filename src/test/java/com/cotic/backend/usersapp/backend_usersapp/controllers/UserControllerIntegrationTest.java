package com.cotic.backend.usersapp.backend_usersapp.controllers;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.cotic.backend.usersapp.backend_usersapp.models.dto.UserDto;
import com.cotic.backend.usersapp.backend_usersapp.models.entities.User;
import com.cotic.backend.usersapp.backend_usersapp.models.request.UserRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class UserControllerIntegrationTest {

    @Autowired
    private TestRestTemplate client;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
    }

    private String login(String username, String password) throws JsonProcessingException {
        User loginUser = new User();
        loginUser.setUsername(username);
        loginUser.setPassword(password);

        HttpEntity<User> request = new HttpEntity<>(loginUser);
        ResponseEntity<String> response = client.postForEntity("/login", request, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        Map<String, String> body = objectMapper.readValue(response.getBody(), Map.class);
        return body.get("token");
    }

    @Test
    @Order(1)
    void testLogin() throws Exception {
        String token = login("admin", "123456");
        assertNotNull(token);
        assertTrue(token.length() > 10);
    }

    @Test
    @Order(2)
    void testListUsers_Public_ShouldReturn401_IfProtected() {
        // The list endpoint /users seems to be configured as protected or public
        // depending on config.
        // SpringSecurityConfig: .requestMatchers(HttpMethod.GET, "/users").permitAll()
        // So it should be public.

        ResponseEntity<UserDto[]> response = client.getForEntity("/users", UserDto[].class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        UserDto[] users = response.getBody();
        assertNotNull(users);
        assertTrue(users.length >= 2); // default users
    }

    @Test
    @Order(3)
    void testCreateUser_Admin() throws Exception {
        String token = login("admin", "123456");

        User newUser = new User();
        newUser.setUsername("testuser");
        newUser.setPassword("password");
        newUser.setEmail("test@email.com");
        newUser.setAdmin(false);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        HttpEntity<User> request = new HttpEntity<>(newUser, headers);

        // POST /users requires ADMIN role.
        ResponseEntity<User> response = client.exchange("/users", HttpMethod.POST, request, User.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        User createdUser = response.getBody();
        assertNotNull(createdUser);
        assertNotNull(createdUser.getId());
        assertEquals("testuser", createdUser.getUsername());
    }

    @Test
    @Order(4)
    void testGetUser_Admin() throws Exception {
        // User created has ID 4 likely (1,2,3 are seeded)
        String token = login("admin", "123456");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        // GET /users/{id} requires "USER" or "ADMIN"
        ResponseEntity<UserDto> response = client.exchange("/users/1", HttpMethod.GET, request, UserDto.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        UserDto user = response.getBody();
        assertEquals("admin", user.getUsername());
    }

    @Test
    @Order(5)
    void testUpdateUser_Admin() throws Exception {
        // Update user 4 (created in testCreateUser_Admin)
        String token = login("admin", "123456");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        UserRequest updateReq = new UserRequest();
        updateReq.setUsername("testup"); // Must be between 4 and 8 chars
        updateReq.setEmail("testup@email.com");
        updateReq.setAdmin(false);

        HttpEntity<UserRequest> request = new HttpEntity<>(updateReq, headers);

        // PUT /users/{id} requires ADMIN
        // Assuming ID 4 exists from test 3.
        ResponseEntity<UserDto> response = client.exchange("/users/4", HttpMethod.PUT, request, UserDto.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        UserDto user = response.getBody();
        assertEquals("testup", user.getUsername());
        assertEquals("testup@email.com", user.getEmail());
    }

    @Test
    @Order(6)
    void testDeleteUser_Admin() throws Exception {
        // Create a user to delete first to avoid breaking other tests or delete the one
        // created in test 3
        // We know id=4 was created in test 3.

        String token = login("admin", "123456");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        // DELETE /users/{id} requires ADMIN
        // Assuming ID 4 exists from test 3.
        ResponseEntity<Void> response = client.exchange("/users/4", HttpMethod.DELETE, request, Void.class);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

        // Verify it is gone
        ResponseEntity<UserDto> check = client.exchange("/users/4", HttpMethod.GET, request, UserDto.class);
        assertEquals(HttpStatus.NOT_FOUND, check.getStatusCode());
    }

    @Test
    @Order(7)
    void testValidateToken() throws Exception {
        String token = login("admin", "123456");

        UserController.TokenRequest tokenRequest = new UserController.TokenRequest();
        tokenRequest.setToken("Bearer " + token);

        HttpEntity<UserController.TokenRequest> request = new HttpEntity<>(tokenRequest);
        ResponseEntity<Map> response = client.postForEntity("/users/validate-token", request, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map body = response.getBody();
        assertTrue((Boolean) body.get("valid"));
        assertEquals("admin", body.get("username"));
    }

    @Test
    @Order(8)
    void testValidateToken_Invalid() {
        String token = "invalid.token.garbage";

        UserController.TokenRequest tokenRequest = new UserController.TokenRequest();
        tokenRequest.setToken(token);

        HttpEntity<UserController.TokenRequest> request = new HttpEntity<>(tokenRequest);
        ResponseEntity<Map> response = client.postForEntity("/users/validate-token", request, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map body = response.getBody();
        assertFalse((Boolean) body.get("valid"));
    }

    @Test
    @Order(9)
    void testGetUser_NotFound() throws Exception {
        String token = login("admin", "123456");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<UserDto> response = client.exchange("/users/99999", HttpMethod.GET, request, UserDto.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @Order(10)
    void testUpdateUser_NotFound() throws Exception {
        String token = login("admin", "123456");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        UserRequest updateReq = new UserRequest();
        updateReq.setUsername("ghost"); // Valid length
        updateReq.setEmail("ghost@email.com");
        updateReq.setAdmin(false);

        HttpEntity<UserRequest> request = new HttpEntity<>(updateReq, headers);

        ResponseEntity<UserDto> response = client.exchange("/users/99999", HttpMethod.PUT, request, UserDto.class);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    @Order(11)
    void testDeleteUser_NotFound() throws Exception {
        String token = login("admin", "123456");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Void> response = client.exchange("/users/99999", HttpMethod.DELETE, request, Void.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @Order(12)
    void testCreateUser_ValidationErrors() throws Exception {
        String token = login("admin", "123456");

        User newUser = new User();
        newUser.setUsername("abc"); // Invalid: min 4
        newUser.setPassword(""); // Invalid: NotBlank
        newUser.setEmail("not-an-email"); // Invalid: Email

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        HttpEntity<User> request = new HttpEntity<>(newUser, headers);

        ResponseEntity<Map> response = client.exchange("/users", HttpMethod.POST, request, Map.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map body = response.getBody();
        assertNotNull(body);
        assertTrue(body.containsKey("username"));
        assertTrue(body.containsKey("password"));
        assertTrue(body.containsKey("email"));
    }
}
