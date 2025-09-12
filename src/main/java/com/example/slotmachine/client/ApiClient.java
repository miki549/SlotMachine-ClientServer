package com.example.slotmachine.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.slotmachine.server.dto.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class ApiClient {
    private static final String DEFAULT_BASE_URL = "http://localhost:8080/api";
    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private String authToken;

    public ApiClient() {
        this(DEFAULT_BASE_URL);
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
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), BalanceResponse.class);
        } else {
            throw new RuntimeException("Failed to get balance: " + response.body());
        }
    }

    public SpinResponse processSpin(Integer betAmount, int[][] symbols, Double payout) throws IOException, InterruptedException {
        if (authToken == null) {
            throw new RuntimeException("Not authenticated");
        }

        SpinRequest spinRequest = new SpinRequest(betAmount, symbols, payout);
        String jsonBody = objectMapper.writeValueAsString(spinRequest);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/game/spin"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + authToken)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), SpinResponse.class);
        } else {
            throw new RuntimeException("Spin failed: " + response.body());
        }
    }

    public boolean isConnected() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/auth/validate"))
                    .header("Authorization", "Bearer " + (authToken != null ? authToken : ""))
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
