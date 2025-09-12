package com.example.slotmachine;

import com.example.slotmachine.client.ApiClient;
import com.example.slotmachine.server.dto.BalanceResponse;
import com.example.slotmachine.server.dto.SpinResponse;

import java.util.*;

import static com.example.slotmachine.GameSettings.*;

public class SlotMachine {

    private final int[][] generatedSymbols = new int[GRID_SIZE][GRID_SIZE];
    private double balance;
    private int bet = DEFAULT_BET;
    private final Random random = new Random();
    private boolean isBonusMode = false;
    private int remainingFreeSpins = 0;
    private double bonusPayout = 0;
    private final ApiClient apiClient;
    private boolean isOnline = false;
    private BalanceUpdateListener balanceUpdateListener;
    private UserBannedListener userBannedListener;
    private UserUnbannedListener userUnbannedListener;

    // Szimbólum valószínűségek és szorzók
    private final double[] symbolProbabilities = {12,12,12,14,14,16,15,4,1};  // Összesen 100%
    private final double[][] payoutMultipliers = {
            // 5 szimbólum - legalacsonyabb klaszterméret
            {0.20, 0.25, 0.30, 0.35, 0.40, 0.5, 0.75, 1.00, 2.5},   // 5 szimbólum
            {0.25, 0.30, 0.40, 0.45, 0.50, 0.75, 1.0, 1.50, 3.5},   // 6 szimbólum
            {0.30, 0.40, 0.50, 0.55, 0.74, 1.00, 1.25, 1.75, 4.5},   // 7 szimbólum
            {0.40, 0.50, 0.75, 0.80, 1.00, 1.25, 1.50, 2.00, 6.0},   // 8 szimbólum
            {0.50, 0.75, 1.00, 1.20, 0.25, 1.50, 2.00, 2.50, 6.5},   // 9 szimbólum
            {1.00, 1.25, 1.50, 1.70, 2.00, 3.00, 4.00, 5.00, 7.0},    // 10 szimbólum
            {1.50, 2.00, 2.50, 2.75, 3.00, 4.50, 6.00, 7.50, 10.0},   // 11 szimbólum
            {2.50, 3.00, 3.50, 4.50, 5.00, 10.00, 12.50, 15.00, 20.0},   // 12 szimbólum
            {5.00, 6.00, 8.00, 9.00, 10.00, 20.00, 30.00, 35.00, 40.0},    // 13 szimbólum
            {10.00, 12.00, 15.00, 18.00, 10.00, 40.00, 60.00, 70.00, 80.0},    // 14 szimbólum
            {20.00, 25.00, 30.00, 35.00, 40.00, 60.00, 100.00, 150.00, 160.0},    // 15 szimbólum vagy több
    };
    private double spinPayout = 0;
    // RTP követéséhez szükséges változók
    private int totalBets = 0;
    private double totalPayouts = 0;
    public SlotMachine(ApiClient apiClient) {
        this.apiClient = apiClient;
        this.isOnline = apiClient != null;
        if (!isOnline) {
            // Offline mód - lokális balance betöltése
            loadBalance();
        }
    }

    public SlotMachine() {
        this(null); // Backward compatibility - offline mode
    }

    // Save the balance with hash (only for offline mode)
    private void saveBalance() {
        if (isOnline) {
            return; // Online módban a szerver kezeli a balance-t
        }
        
        try {
            String balanceStr = String.valueOf(balance);
            String hash = calculateHash(balanceStr);
            String dataToSave = balanceStr + ":" + hash;

            java.nio.file.Files.write(java.nio.file.Paths.get("balance.dat"), dataToSave.getBytes());
        } catch (java.io.IOException e) {
            System.err.println("Error saving balance: " + e.getMessage());
        }
    }
    
    // Load the balance and verify hash (only for offline mode)
    private void loadBalance() {
        try {
            if (!java.nio.file.Files.exists(java.nio.file.Paths.get("balance.dat"))) {
                balance = DEFAULT_BALANCE;
                saveBalance();
                return;
            }

            String fileContent = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("balance.dat")));
            String[] parts = fileContent.split(":");

            if (parts.length != 2) {
                balance = DEFAULT_BALANCE;
                saveBalance();
                return;
            }

            String balanceStr = parts[0];
            String storedHash = parts[1];
            String calculatedHash = calculateHash(balanceStr);

            if (!calculatedHash.equals(storedHash)) {
                System.err.println("Balance file has been tampered with! Resetting to default.");
                balance = DEFAULT_BALANCE;
                saveBalance();
                return;
            }

            balance = Double.parseDouble(balanceStr);
        } catch (java.io.IOException e) {
            System.err.println("Error loading balance: " + e.getMessage());
            balance = DEFAULT_BALANCE;
            saveBalance();
        }
    }

    // Calculate SHA-256 hash of the balance
    private String calculateHash(String input) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
    public void addBalance(int amount) {
        if (!isOnline) {
            this.balance += amount;
            saveBalance();
        }
        // Online módban ezt nem használjuk - a szerver kezeli
    }

    // Tét levonása és hozzáadása az összesített téthez
    public void decreaseBalance(int credit) {
        if (!isOnline) {
            this.balance -= credit;
            this.totalBets += credit;  // Hozzáadjuk a tétek összegéhez
            saveBalance();
        }
        // Online módban ezt a szerver kezeli
    }

    public void increaseBalance(double credit) {
        if (!isOnline) {
            this.balance += credit;
            saveBalance();
        }
        // Online módban ezt a szerver kezeli
    }

    public int getBalance() {
        if (isOnline) {
            try {
                BalanceResponse response = apiClient.getBalance();
                this.balance = response.getBalance();
                return (int) this.balance;
            } catch (Exception e) {
                System.err.println("Failed to get balance from server: " + e.getMessage());
                return 0; // Vagy dobjon kivételt
            }
        }
        return (int)balance;
    }

    // Új metódus a szerver kommunikációhoz
    public boolean processSpinOnServer(int betAmount, double payout) {
        if (!isOnline) {
            return false;
        }
        
        try {
            SpinResponse response = apiClient.processSpin(betAmount, generatedSymbols, payout);
            if (response.isSuccess()) {
                this.balance = response.getNewBalance();
                return true;
            } else {
                System.err.println("Spin failed: " + response.getMessage());
                return false;
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
            return false;
        }
    }

    public void updateBalanceFromServer() {
        if (isOnline) {
            try {
                BalanceResponse response = apiClient.getBalance();
                this.balance = response.getBalance();
                System.out.println("Balance updated from server: $" + this.balance);
            } catch (Exception e) {
                System.err.println("Failed to update balance from server: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    public void setBalance(double balance) {
        this.balance = balance;
    }

    public boolean isOnline() {
        return isOnline && apiClient != null && apiClient.isConnected();
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
    public Map<Integer, List<int[]>> checkForMatches() {
        Map<Integer, List<int[]>> matchedClusters = new HashMap<>();
        boolean[][] visited = new boolean[GRID_SIZE][GRID_SIZE];

        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                if (!visited[row][col]) {
                    List<int[]> cluster = new ArrayList<>();
                    findCluster(row, col, generatedSymbols[row][col], visited, cluster);

                    if (cluster.size() >= CLUSTER_SIZE) {
                        matchedClusters
                                .computeIfAbsent(generatedSymbols[row][col], _ -> new ArrayList<>())
                                .addAll(cluster);
                    }
                }
            }
        }
        return matchedClusters;
    }

public int[][] generateSymbols() {
    // Track scatter symbols per column
    boolean[] columnHasScatter = new boolean[GRID_SIZE];
    int scatterCount = 0;
    
    for (int row = 0; row < GRID_SIZE; row++) {
        for (int col = 0; col < GRID_SIZE; col++) {
            int symbol;
            if (random.nextDouble() < 0.3) {
                List<Integer> neighbors = new ArrayList<>();
                // Fölötte lévő szomszéd
                if (row > 0) neighbors.add(generatedSymbols[row - 1][col]);
                // Alatta lévő szomszéd
                if (row < GRID_SIZE - 1) neighbors.add(generatedSymbols[row + 1][col]);
                // Balra lévő szomszéd
                if (col > 0) neighbors.add(generatedSymbols[row][col - 1]);
                // Jobbra lévő szomszéd
                if (col < GRID_SIZE - 1) neighbors.add(generatedSymbols[row][col + 1]);
                if (!neighbors.isEmpty()) {
                    symbol = neighbors.get(random.nextInt(neighbors.size()));
                } else {
                    symbol = generateSymbol();
                }
            } else {
                symbol = generateSymbol();
            }
            
            // Check if this is a scatter symbol
            if (symbol == SCATTER_SYMBOL) {
                // Only allow scatter if:
                // 1. We haven't reached the maximum scatter count
                // 2. This column doesn't already have a scatter
                if (scatterCount >= BONUS_TRIGGER_COUNT || columnHasScatter[col]) {
                    // Generate a different symbol instead
                    symbol = generateNonScatterSymbol();
                } else {
                    // This is a valid scatter placement
                    scatterCount++;
                    columnHasScatter[col] = true;
                }
            }
            
            generatedSymbols[row][col] = symbol;
        }
    }
    return generatedSymbols;
}

// Helper method to generate a non-scatter symbol
private int generateNonScatterSymbol() {
    int symbol;
    do {
        symbol = generateSymbol();
    } while (symbol == SCATTER_SYMBOL);
    return symbol;
}

    // Módosított metódus a véletlenszerű szimbólum generálásához a valószínűségek alapján
    public int generateSymbol() {
        int randomValue = random.nextInt(100);
        double cumulativeProbability = 0;
        for (int i = 0; i < symbolProbabilities.length; i++) {
            cumulativeProbability += symbolProbabilities[i];
            if (randomValue < cumulativeProbability) {
                return i;
            }
        }
        return symbolProbabilities.length - 1;
    }

    private void findCluster(int row, int col, int symbol, boolean[][] visited, List<int[]> cluster) {
        if (row < 0 || row >= GRID_SIZE || col < 0 || col >= GRID_SIZE || visited[row][col] || generatedSymbols[row][col] != symbol) {
            return;
        }
        visited[row][col] = true;
        cluster.add(new int[]{row, col});

        findCluster(row + 1, col, symbol, visited, cluster);
        findCluster(row - 1, col, symbol, visited, cluster);
        findCluster(row, col + 1, symbol, visited, cluster);
        findCluster(row, col - 1, symbol, visited, cluster);
    }

    // Nyeremény kiszámítása és klaszterek törlése
    public double clearMatchedSymbols(Map<Integer, List<int[]>> matchedClusters) {
        double hitPayout = 0;

        for (Map.Entry<Integer, List<int[]>> entry : matchedClusters.entrySet()) {
            int symbol = entry.getKey();
            int clusterSize = entry.getValue().size();
            Pair<Double, Integer> multiplierInfo = getPayoutMultiplier(symbol, clusterSize);
            double payout = bet * multiplierInfo.first;
            hitPayout += payout;

            for (int[] position : entry.getValue()) {
                generatedSymbols[position[0]][position[1]] = -1;  // Üresítjük a matched szimbólumokat
            }
        }
        spinPayout += hitPayout;
        increaseBalance(hitPayout);
        totalPayouts += hitPayout;  // Nyeremény hozzáadása az összes nyereményhez
        return hitPayout;
    }

    public void resetSpinPayout() {
        spinPayout = 0;
    }

    // Üres helyek feltöltése új szimbólumokkal a legfelső sorban
    public void dropAndRefillSymbols() {
        for (int col = 0; col < GRID_SIZE; col++) {
            int emptyRow = GRID_SIZE - 1;

            // Az oszlop végigjárása alulról felfelé
            for (int row = GRID_SIZE - 1; row >= 0; row--) {
                if (generatedSymbols[row][col] != -1) {
                    generatedSymbols[emptyRow][col] = generatedSymbols[row][col];
                    if (emptyRow != row) {
                        generatedSymbols[row][col] = -1;
                    }
                    emptyRow--;
                }
            }

            // Üres helyek feltöltése új szimbólumokkal a legfelső sorban
            while (emptyRow >= 0) {
                if (random.nextDouble() < 0.2) {  // 20% esély a klaszterformációra
                    generatedSymbols[emptyRow][col] = suggestClusterSymbol(col, emptyRow);
                } else {
                    generatedSymbols[emptyRow][col] = generateSymbol();  // Véletlenszerű szimbólum
                }
                emptyRow--;
            }
        }
    }
    private int suggestClusterSymbol(int col, int row) {
        List<Integer> possibleSymbols = new ArrayList<>();

        // Nézd meg az összes szomszédot
        /*if (row > 0 && generatedSymbols[row - 1][col] != -1) {  // Fölötte
            possibleSymbols.add(generatedSymbols[row - 1][col]);
        }*/
        /*if (row < GRID_SIZE - 1 && generatedSymbols[row + 1][col] != -1) {  // Alatta
            possibleSymbols.add(generatedSymbols[row + 1][col]);
        }*/
        if (col > 0 && generatedSymbols[row][col - 1] != -1) {  // Balra
            possibleSymbols.add(generatedSymbols[row][col - 1]);
        }
        if (col < GRID_SIZE - 1 && generatedSymbols[row][col + 1] != -1) {  // Jobbra
            possibleSymbols.add(generatedSymbols[row][col + 1]);
        }

        // Ha van lehetséges szimbólum, válassz közülük nagyobb eséllyel
        if (!possibleSymbols.isEmpty() && random.nextDouble() < 0.8) {  // 80% esély a szomszéd másolására
            return possibleSymbols.get(random.nextInt(possibleSymbols.size()));
        }

        // Ha nincs releváns szomszéd, vagy nem másolunk, generáljunk egy véletlenszerű szimbólumot
        return generateSymbol();
    }


    // RTP kiszámítása
    public double getRTP() {
        if (totalBets == 0) {
            return 0;
        }
        return ((double) totalPayouts / totalBets) * 100;
    }

    public Pair<Double, Integer> getPayoutMultiplier(int symbol, int clusterSize) {
        int multiplierIndex = Math.min(clusterSize-CLUSTER_SIZE, payoutMultipliers.length-1);
        double multiplier = payoutMultipliers[multiplierIndex][symbol];
        return new Pair<>(multiplier, clusterSize);
    }

    // Getter metódusok az összes tét és nyeremény lekérdezéséhez
    public int getTotalBets() {
        return totalBets;
    }

    public double getTotalPayouts() {
        return totalPayouts;
    }
    public int[][] getSymbols() {
        return generatedSymbols;
    }
    public void setBet(int s){
        bet = s;
    }

    public double getSpinPayout() {
        return spinPayout;
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

    public boolean checkForBonusTrigger() {
        int scatterCount = 0;
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                if (generatedSymbols[row][col] == SCATTER_SYMBOL) {
                    scatterCount++;
                }
            }
        }
        return scatterCount >= BONUS_TRIGGER_COUNT;
    }

    public boolean checkForRetrigger() {
        int scatterCount = 0;
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                if (generatedSymbols[row][col] == SCATTER_SYMBOL) {
                    scatterCount++;
                }
            }
        }
        return scatterCount >= RETRIGGER_COUNT;
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

    // Módosított updateBalanceFromServer - most értesíti a listener-t
    public void updateBalanceFromServerWithNotification() {
        if (isOnline) {
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
                } else {
                    System.err.println("Failed to update balance from server: " + e.getMessage());
                }
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

    // Interface for user banned notifications
    public interface UserBannedListener {
        void onUserBanned();
    }
    
    // Interface for user unbanned notifications
    public interface UserUnbannedListener {
        void onUserUnbanned();
    }
}
