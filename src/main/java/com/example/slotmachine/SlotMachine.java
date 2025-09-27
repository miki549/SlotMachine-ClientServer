package com.example.slotmachine;

import com.example.slotmachine.client.ApiClient;
import com.example.slotmachine.server.dto.BalanceResponse;
import com.example.slotmachine.server.dto.SpinResponse;


import static com.example.slotmachine.GameSettings.*;

public class SlotMachine {

    private final int[][] generatedSymbols = new int[GRID_SIZE][GRID_SIZE]; // Csak a GUI megjelenítéshez
    private double balance;
    private int bet = DEFAULT_BET;
    private boolean isBonusMode = false;
    private int remainingFreeSpins = 0;
    private double bonusPayout = 0;
    private final ApiClient apiClient;
    private boolean isSpinning = false;
    private BalanceUpdateListener balanceUpdateListener;
    private UserBannedListener userBannedListener;
    private UserUnbannedListener userUnbannedListener;
    private UserDeletedListener userDeletedListener;
    public SlotMachine(ApiClient apiClient) {
        if (apiClient == null) {
            throw new IllegalArgumentException("ApiClient cannot be null - this is an online-only game");
        }
        this.apiClient = apiClient;
    }


    // Tét levonása és hozzáadása (lokális cache frissítés)
    public void decreaseBalance(int credit) {
        this.balance -= credit;
    }

    public void increaseBalance(double credit) {
        this.balance += credit;
    }

    public int getBalance() {
        return (int) this.balance;
    }

    // Szerver kommunikáció a spin feldolgozáshoz - új logika
    public SpinResponse processSpinOnServer(int betAmount, boolean isBonusMode) {
        try {
            SpinResponse response = apiClient.processSpin(betAmount, isBonusMode);
            if (response.isSuccess()) {
                // A szerver már hozzáadta a nyereményt, használjuk a szerver balance-t
                double serverBalance = response.getNewBalance();
                this.balance = serverBalance;
                
                // Frissítjük a lokális grid-et a szerver adataival
                if (response.getInitialGrid() != null) {
                    copyGridTo(response.getInitialGrid(), generatedSymbols);
                }
                
                System.out.println("Balance updated from server after spin: $" + this.balance);
                return response;
            } else {
                System.err.println("Spin failed: " + response.getMessage());
                return null;
            }
        } catch (Exception e) {
            // Check if it's a user banned exception
            if (e.getMessage() != null && e.getMessage().contains("Felhasználó tiltva lett")) {
                if (userBannedListener != null) {
                    userBannedListener.onUserBanned();
                }
            } else {
                System.err.println("Failed to process spin on server: " + e.getMessage());
            }
            return null;
        }
    }


    // Grid másolása
    private void copyGridTo(int[][] source, int[][] target) {
        for (int i = 0; i < GRID_SIZE && i < source.length; i++) {
            for (int j = 0; j < GRID_SIZE && j < source[i].length; j++) {
                target[i][j] = source[i][j];
            }
        }
    }

    public void updateBalanceFromServer() {
        try {
            BalanceResponse response = apiClient.getBalance();
            this.balance = response.getBalance();
            System.out.println("Balance updated from server: $" + this.balance);
        } catch (Exception e) {
            System.err.println("Failed to update balance from server: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void setBalance(double balance) {
        this.balance = balance;
    }

    public boolean isOnline() {
        return apiClient != null && apiClient.isConnected();
    }

    public boolean isSpinning() {
        return isSpinning;
    }

    public void setSpinning(boolean spinning) {
        this.isSpinning = spinning;
    }

    public int getBet() {
        return bet;
    }

    public void increaseBet() {
        if (this.bet < MAX_BET) {
            this.bet += BET_STEP;
        }
    }

    public void decreaseBet() {
        if (this.bet > MIN_BET) {
            this.bet -= BET_STEP;
        }
    }
    public int[][] getSymbols() {
        return generatedSymbols;
    }
    
    public void setBet(int s){
        bet = s;
    }

    public boolean isBonusMode() {
        return isBonusMode;
    }

    public int getRemainingFreeSpins() {
        return remainingFreeSpins;
    }

    public double getBonusPayout() {
        return bonusPayout;
    }

    public void startBonusMode() {
        isBonusMode = true;
        remainingFreeSpins = FREE_SPINS;
        bonusPayout = 0;
    }

    public void endBonusMode() {
        isBonusMode = false;
        remainingFreeSpins = 0;
        increaseBalance(bonusPayout);
        bonusPayout = 0;
    }


    public void addRetriggerSpins() {
        remainingFreeSpins += RETRIGGER_SPINS;
    }

    public void decreaseFreeSpins() {
        if (remainingFreeSpins > 0) {
            remainingFreeSpins--;
        }
    }

    public boolean hasFreeSpins() {
        return remainingFreeSpins > 0;
    }

    public void addBonusPayout(double amount) {
        bonusPayout += amount;
    }

    // Balance frissítési listener interface
    public interface BalanceUpdateListener {
        void onBalanceUpdated(double newBalance);
    }

    public void setBalanceUpdateListener(BalanceUpdateListener listener) {
        this.balanceUpdateListener = listener;
    }

    // Balance frissítés szerverről értesítéssel
    public void updateBalanceFromServerWithNotification() {
        try {
            BalanceResponse response = apiClient.getBalance();
            double oldBalance = this.balance;
            this.balance = response.getBalance();
            
            // Ha változott a balance, értesítjük a listener-t
            if (oldBalance != this.balance && balanceUpdateListener != null) {
                balanceUpdateListener.onBalanceUpdated(this.balance);
            }
            
            // If we successfully got balance, user is not banned anymore
            if (userUnbannedListener != null) {
                userUnbannedListener.onUserUnbanned();
            }
            
            System.out.println("Balance updated from server: $" + this.balance);
        } catch (Exception e) {
            // Check if it's a user banned exception
            if (e.getMessage() != null && e.getMessage().contains("Felhasználó tiltva lett")) {
                if (userBannedListener != null) {
                    userBannedListener.onUserBanned();
                }
            } else if (e.getMessage() != null && e.getMessage().contains("Felhasználó törölve lett")) {
                // Check if it's a user deleted exception
                if (userDeletedListener != null) {
                    userDeletedListener.onUserDeleted();
                }
            } else {
                System.err.println("Failed to update balance from server: " + e.getMessage());
            }
        }
    }

    // Setter for user banned listener
    public void setUserBannedListener(UserBannedListener listener) {
        this.userBannedListener = listener;
    }
    
    // Setter for user unbanned listener
    public void setUserUnbannedListener(UserUnbannedListener listener) {
        this.userUnbannedListener = listener;
    }
    
    // Setter for user deleted listener
    public void setUserDeletedListener(UserDeletedListener listener) {
        this.userDeletedListener = listener;
    }

    // Interface for user banned notifications
    public interface UserBannedListener {
        void onUserBanned();
    }
    
    // Interface for user unbanned notifications
    public interface UserUnbannedListener {
        void onUserUnbanned();
    }
    
    // Interface for user deleted notifications
    public interface UserDeletedListener {
        void onUserDeleted();
    }
}
