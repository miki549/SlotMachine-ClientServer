package com.example.slotmachine.server.dto;

public class SpinRequest {
    private Integer betAmount;
    private int[][] symbols; // A kliens által generált szimbólumok
    private Double payout; // A kliens által számított nyeremény

    // Constructors
    public SpinRequest() {}

    public SpinRequest(Integer betAmount, int[][] symbols, Double payout) {
        this.betAmount = betAmount;
        this.symbols = symbols;
        this.payout = payout;
    }

    // Getters and Setters
    public Integer getBetAmount() {
        return betAmount;
    }

    public void setBetAmount(Integer betAmount) {
        this.betAmount = betAmount;
    }

    public int[][] getSymbols() {
        return symbols;
    }

    public void setSymbols(int[][] symbols) {
        this.symbols = symbols;
    }

    public Double getPayout() {
        return payout;
    }

    public void setPayout(Double payout) {
        this.payout = payout;
    }
}
