package com.example.slotmachine.server.dto;

public class SpinRequest {
    private Integer betAmount;
    private Boolean isBonusMode; // Bonus mód jelzése

    // Constructors
    public SpinRequest() {}

    public SpinRequest(Integer betAmount, Boolean isBonusMode) {
        this.betAmount = betAmount;
        this.isBonusMode = isBonusMode != null ? isBonusMode : false;
    }

    // Backward compatibility constructor (régi kliens támogatáshoz)
    public SpinRequest(Integer betAmount, int[][] symbols, Double payout) {
        this.betAmount = betAmount;
        this.isBonusMode = false;
        // Régi paramétereket figyelmen kívül hagyjuk, a szerver generálja a szimbólumokat
    }

    // Getters and Setters
    public Integer getBetAmount() {
        return betAmount;
    }

    public void setBetAmount(Integer betAmount) {
        this.betAmount = betAmount;
    }

    public Boolean getIsBonusMode() {
        return isBonusMode != null ? isBonusMode : false;
    }

    public void setIsBonusMode(Boolean isBonusMode) {
        this.isBonusMode = isBonusMode;
    }

    // Backward compatibility getters (régi kliens támogatáshoz)
    @Deprecated
    public int[][] getSymbols() {
        return null; // Már nem használjuk
    }

    @Deprecated
    public void setSymbols(int[][] symbols) {
        // Már nem használjuk
    }

    @Deprecated
    public Double getPayout() {
        return null; // Már nem használjuk
    }

    @Deprecated
    public void setPayout(Double payout) {
        // Már nem használjuk
    }
}
