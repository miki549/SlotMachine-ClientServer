package com.example.slotmachine.server.dto;

public class LoginResponse {
    private String token;
    private String username;
    private Double balance;

    // Constructors
    public LoginResponse() {}

    public LoginResponse(String token, String username, Double balance) {
        this.token = token;
        this.username = username;
        this.balance = balance;
    }

    // Getters and Setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Double getBalance() {
        return balance;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }
}
