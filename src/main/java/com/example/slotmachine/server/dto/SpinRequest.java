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

}
