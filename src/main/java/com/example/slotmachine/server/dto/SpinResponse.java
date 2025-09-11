package com.example.slotmachine.server.dto;

public class SpinResponse {
    private boolean success;
    private String message;
    private Double newBalance;
    private Double payout;

    // Constructors
    public SpinResponse() {}

    public SpinResponse(boolean success, String message, Double newBalance, Double payout) {
        this.success = success;
        this.message = message;
        this.newBalance = newBalance;
        this.payout = payout;
    }

    // Static factory methods
    public static SpinResponse success(Double newBalance, Double payout) {
        return new SpinResponse(true, "Spin successful", newBalance, payout);
    }

    public static SpinResponse error(String message) {
        return new SpinResponse(false, message, null, null);
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Double getNewBalance() {
        return newBalance;
    }

    public void setNewBalance(Double newBalance) {
        this.newBalance = newBalance;
    }

    public Double getPayout() {
        return payout;
    }

    public void setPayout(Double payout) {
        this.payout = payout;
    }
}
