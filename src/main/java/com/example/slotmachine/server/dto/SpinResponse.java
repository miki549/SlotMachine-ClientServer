package com.example.slotmachine.server.dto;

import java.util.List;
import java.util.Map;

public class SpinResponse {
    private boolean success;
    private String message;
    private Double newBalance;
    private Double totalPayout;
    
    // Új mezők a teljes játéklogika támogatásához
    private int[][] initialGrid;
    private int[][] finalGrid;
    private List<CascadeStepDto> cascadeSteps;
    private boolean bonusTrigger;
    private boolean retrigger;

    // Constructors
    public SpinResponse() {}

    public SpinResponse(boolean success, String message, Double newBalance, Double totalPayout) {
        this.success = success;
        this.message = message;
        this.newBalance = newBalance;
        this.totalPayout = totalPayout;
    }

    // Static factory methods
    public static SpinResponse success(Double newBalance, Double totalPayout, int[][] initialGrid, 
                                     int[][] finalGrid, List<CascadeStepDto> cascadeSteps,
                                     boolean bonusTrigger, boolean retrigger) {
        SpinResponse response = new SpinResponse(true, "Spin successful", newBalance, totalPayout);
        response.setInitialGrid(initialGrid);
        response.setFinalGrid(finalGrid);
        response.setCascadeSteps(cascadeSteps);
        response.setBonusTrigger(bonusTrigger);
        response.setRetrigger(retrigger);
        return response;
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

    public Double getTotalPayout() {
        return totalPayout;
    }

    public void setTotalPayout(Double totalPayout) {
        this.totalPayout = totalPayout;
    }

    public int[][] getInitialGrid() {
        return initialGrid;
    }

    public void setInitialGrid(int[][] initialGrid) {
        this.initialGrid = initialGrid;
    }

    public int[][] getFinalGrid() {
        return finalGrid;
    }

    public void setFinalGrid(int[][] finalGrid) {
        this.finalGrid = finalGrid;
    }

    public List<CascadeStepDto> getCascadeSteps() {
        return cascadeSteps;
    }

    public void setCascadeSteps(List<CascadeStepDto> cascadeSteps) {
        this.cascadeSteps = cascadeSteps;
    }

    public boolean isBonusTrigger() {
        return bonusTrigger;
    }

    public void setBonusTrigger(boolean bonusTrigger) {
        this.bonusTrigger = bonusTrigger;
    }

    public boolean isRetrigger() {
        return retrigger;
    }

    public void setRetrigger(boolean retrigger) {
        this.retrigger = retrigger;
    }


    /**
     * Cascade lépés DTO
     */
    public static class CascadeStepDto {
        private Map<Integer, List<int[]>> matchedClusters;
        private double payout;
        private int[][] gridAfterClear;
        private int[][] gridAfterRefill;

        // Constructors
        public CascadeStepDto() {}

        public CascadeStepDto(Map<Integer, List<int[]>> matchedClusters, double payout, 
                            int[][] gridAfterClear, int[][] gridAfterRefill) {
            this.matchedClusters = matchedClusters;
            this.payout = payout;
            this.gridAfterClear = gridAfterClear;
            this.gridAfterRefill = gridAfterRefill;
        }

        // Getters and setters
        public Map<Integer, List<int[]>> getMatchedClusters() { return matchedClusters; }
        public void setMatchedClusters(Map<Integer, List<int[]>> matchedClusters) { this.matchedClusters = matchedClusters; }

        public double getPayout() { return payout; }
        public void setPayout(double payout) { this.payout = payout; }

        public int[][] getGridAfterClear() { return gridAfterClear; }
        public void setGridAfterClear(int[][] gridAfterClear) { this.gridAfterClear = gridAfterClear; }

        public int[][] getGridAfterRefill() { return gridAfterRefill; }
        public void setGridAfterRefill(int[][] gridAfterRefill) { this.gridAfterRefill = gridAfterRefill; }
    }
}
