package com.example.slotmachine.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.slotmachine.server.dto.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

// Custom exception for banned users
class UserBannedException extends RuntimeException {
    public UserBannedException(String message) {
        super(message);
    }
}

// Custom exception for deleted users
class UserDeletedException extends RuntimeException {
    public UserDeletedException(String message) {
        super(message);
    }
}

public class ApiClient {
    private static final String PC_SERVER_URL = "http://46.139.211.149:8081/api";
    private static final String LAPTOP_SERVER_URL = "http://46.139.211.149:8082/api";
    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private String authToken;

    public ApiClient() {
        this(getPreferredServerUrl());
    }
    
    private static String getPreferredServerUrl() {
        // Try PC server first (port 8081)
        if (isServerAvailable(PC_SERVER_URL)) {
            return PC_SERVER_URL;
        }
        // Fallback to laptop server (port 8082)
        if (isServerAvailable(LAPTOP_SERVER_URL)) {
            return LAPTOP_SERVER_URL;
        }
        // Default to PC server if neither is available
        return PC_SERVER_URL;
    }
    
    private static boolean isServerAvailable(String serverUrl) {
        try {
            HttpClient testClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .build();
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl + "/auth/health"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            
            HttpResponse<String> response = testClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    public ApiClient(String serverUrl) {
        this.baseUrl = serverUrl.endsWith("/api") ? serverUrl : serverUrl + "/api";
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public LoginResponse login(String username, String password) throws IOException, InterruptedException {
        LoginRequest loginRequest = new LoginRequest(username, password);
        String jsonBody = objectMapper.writeValueAsString(loginRequest);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/auth/login"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            LoginResponse loginResponse = objectMapper.readValue(response.body(), LoginResponse.class);
            this.authToken = loginResponse.getToken();
            return loginResponse;
        } else {
            throw new RuntimeException("Login failed: " + response.body());
        }
    }

    public LoginResponse register(String username, String password) throws IOException, InterruptedException {
        LoginRequest registerRequest = new LoginRequest(username, password);
        String jsonBody = objectMapper.writeValueAsString(registerRequest);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/auth/register"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            LoginResponse loginResponse = objectMapper.readValue(response.body(), LoginResponse.class);
            this.authToken = loginResponse.getToken();
            return loginResponse;
        } else {
            throw new RuntimeException("Registration failed: " + response.body());
        }
    }

    public BalanceResponse getBalance() throws IOException, InterruptedException {
        if (authToken == null) {
            throw new RuntimeException("Not authenticated");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/game/balance"))
                .header("Authorization", "Bearer " + authToken)
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), BalanceResponse.class);
        } else if (response.statusCode() == 403 && "USER_BANNED".equals(response.body())) {
            throw new UserBannedException("Felhasználó tiltva lett");
        } else if (response.statusCode() == 400 && response.body().contains("Invalid token or user not found")) {
            throw new UserDeletedException("Felhasználó törölve lett");
        } else {
            throw new RuntimeException("Failed to get balance: " + response.body());
        }
    }

    /**
     * Új spin feldolgozás - a szerver generálja a szimbólumokat és számítja a nyereményt
     */
    public SpinResponse processSpin(Integer betAmount, Boolean isBonusMode) throws IOException, InterruptedException {
        if (authToken == null) {
            throw new RuntimeException("Not authenticated");
        }

        SpinRequest spinRequest = new SpinRequest(betAmount, isBonusMode);
        String jsonBody = objectMapper.writeValueAsString(spinRequest);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/game/spin"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + authToken)
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), SpinResponse.class);
        } else if (response.statusCode() == 403 && "USER_BANNED".equals(response.body())) {
            throw new UserBannedException("Felhasználó tiltva lett");
        } else {
            throw new RuntimeException("Spin failed: " + response.body());
        }
    }


    public boolean isConnected() {
        try {
            // Use health endpoint for connection testing (no authentication required)
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/auth/health"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    public void setAuthToken(String token) {
        this.authToken = token;
    }

    public String getAuthToken() {
        return authToken;
    }

    public String getServerUrl() {
        return baseUrl.replace("/api", "");
    }
}
