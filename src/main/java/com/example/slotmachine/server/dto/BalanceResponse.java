package com.example.slotmachine.server.dto;

public class BalanceResponse {
    private Double balance;
    private String username;

    // Constructors
    public BalanceResponse() {}

    public BalanceResponse(Double balance, String username) {
        this.balance = balance;
        this.username = username;
    }

    // Getters and Setters
    public Double getBalance() {
        return balance;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
