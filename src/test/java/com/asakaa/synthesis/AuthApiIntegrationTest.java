package com.asakaa.synthesis;

import com.asakaa.synthesis.domain.dto.request.AuthRequest;
import com.asakaa.synthesis.domain.dto.request.RegisterRequest;
import com.asakaa.synthesis.domain.dto.response.AuthResponse;
import com.asakaa.synthesis.exception.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class AuthApiIntegrationTest extends BaseIntegrationTest {

    @Test
    void register_WithValidData_Returns201WithToken() {
        // Arrange
        RegisterRequest request = RegisterRequest.builder()
                .name("Dr. New Provider")
                .role("Physician")
                .clinicRegistrationCode(null)
                .region("East")
                .email("newprovider" + System.currentTimeMillis() + "@example.com")
                .password("securePassword123")
                .build();

        // Act
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/register",
                request,
                AuthResponse.class
        );

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getToken());
        assertEquals(request.getEmail(), response.getBody().getEmail());
        assertEquals("Dr. New Provider", response.getBody().getName());
        assertEquals("Physician", response.getBody().getRole());
    }

    @Test
    void login_WithValidCredentials_Returns200WithToken() {
        // Arrange - Register a provider first
        String email = "logintest" + System.currentTimeMillis() + "@example.com";
        String password = "password123";

        RegisterRequest registerRequest = RegisterRequest.builder()
                .name("Dr. Login Test")
                .role("Doctor")
                .clinicRegistrationCode(null)
                .region("North")
                .email(email)
                .password(password)
                .build();

        restTemplate.postForEntity("/api/v1/auth/register", registerRequest, AuthResponse.class);

        // Act - Login with the same credentials
        AuthRequest loginRequest = AuthRequest.builder()
                .email(email)
                .password(password)
                .build();

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/login",
                loginRequest,
                AuthResponse.class
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getToken());
        assertEquals(email, response.getBody().getEmail());
    }

    @Test
    void login_WithWrongPassword_Returns400() {
        // Arrange - Register a provider first
        String email = "wrongpass" + System.currentTimeMillis() + "@example.com";

        RegisterRequest registerRequest = RegisterRequest.builder()
                .name("Dr. Wrong Pass")
                .role("Doctor")
                .clinicRegistrationCode(null)
                .region("South")
                .email(email)
                .password("correctPassword")
                .build();

        restTemplate.postForEntity("/api/v1/auth/register", registerRequest, AuthResponse.class);

        // Act - Login with wrong password
        AuthRequest loginRequest = AuthRequest.builder()
                .email(email)
                .password("wrongPassword")
                .build();

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/login",
                loginRequest,
                ErrorResponse.class
        );

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getMessage().contains("Invalid"));
    }

    @Test
    void getPatientsWithoutToken_Returns403() {
        // Act - Try to access protected endpoint without authentication
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/patients",
                String.class
        );

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }
}
