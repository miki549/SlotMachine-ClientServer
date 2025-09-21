package com.example.slotmachine.server.service;

import com.example.slotmachine.Pair;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.example.slotmachine.GameSettings.*;

/**
 * Szerver-oldali slot machine játékmotor
 * Tartalmazza a teljes játéklogikát: szimbólum generálás, klaszter keresés, nyeremény számítás
 */
@Component
public class SlotMachineEngine {

    private final Random random = new Random();

    // Szimbólum valószínűségek és szorzók (átmásolva a SlotMachine.java-ból)
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

    /**
     * Teljes spin feldolgozása
     */
    public SpinResult processSpin(int betAmount, boolean isBonusMode) {
        SpinResult result = new SpinResult();
        result.setBetAmount(betAmount);
        result.setInitialGrid(generateSymbols());
        
        // Feldolgozzuk az összes klasztert (cascade mechanizmus)
        List<CascadeStep> cascadeSteps = new ArrayList<>();
        int[][] currentGrid = copyGrid(result.getInitialGrid());
        double totalPayout = 0;
        
        while (true) {
            Map<Integer, List<int[]>> matchedClusters = checkForMatches(currentGrid);
            if (matchedClusters.isEmpty()) {
                break;
            }
            
            CascadeStep step = new CascadeStep();
            step.setMatchedClusters(matchedClusters);
            
            double stepPayout = calculatePayout(matchedClusters, betAmount);
            step.setPayout(stepPayout);
            totalPayout += stepPayout;
            
            // Töröljük a matched szimbólumokat
            clearMatchedSymbols(currentGrid, matchedClusters);
            step.setGridAfterClear(copyGrid(currentGrid));
            
            // Feltöltjük új szimbólumokkal
            dropAndRefillSymbols(currentGrid);
            step.setGridAfterRefill(copyGrid(currentGrid));
            
            cascadeSteps.add(step);
        }
        
        result.setCascadeSteps(cascadeSteps);
        result.setFinalGrid(currentGrid);
        result.setTotalPayout(totalPayout);
        
        // Bonus trigger ellenőrzése
        result.setBonusTrigger(checkForBonusTrigger(result.getInitialGrid()));
        result.setRetrigger(isBonusMode && checkForRetrigger(result.getInitialGrid()));
        
        return result;
    }

    /**
     * Szimbólumok generálása
     */
    public int[][] generateSymbols() {
        int[][] generatedSymbols = new int[GRID_SIZE][GRID_SIZE];
        
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

    /**
     * Véletlenszerű szimbólum generálása a valószínűségek alapján
     */
    private int generateSymbol() {
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

    /**
     * Non-scatter szimbólum generálása
     */
    private int generateNonScatterSymbol() {
        int symbol;
        do {
            symbol = generateSymbol();
        } while (symbol == SCATTER_SYMBOL);
        return symbol;
    }

    /**
     * Klaszterek keresése
     */
    public Map<Integer, List<int[]>> checkForMatches(int[][] grid) {
        Map<Integer, List<int[]>> matchedClusters = new HashMap<>();
        boolean[][] visited = new boolean[GRID_SIZE][GRID_SIZE];

        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                if (!visited[row][col]) {
                    List<int[]> cluster = new ArrayList<>();
                    findCluster(row, col, grid[row][col], visited, cluster, grid);

                    if (cluster.size() >= CLUSTER_SIZE) {
                        matchedClusters
                                .computeIfAbsent(grid[row][col], k -> new ArrayList<>())
                                .addAll(cluster);
                    }
                }
            }
        }
        return matchedClusters;
    }

    /**
     * Klaszter keresése rekurzívan
     */
    private void findCluster(int row, int col, int symbol, boolean[][] visited, List<int[]> cluster, int[][] grid) {
        if (row < 0 || row >= GRID_SIZE || col < 0 || col >= GRID_SIZE || 
            visited[row][col] || grid[row][col] != symbol) {
            return;
        }
        visited[row][col] = true;
        cluster.add(new int[]{row, col});

        findCluster(row + 1, col, symbol, visited, cluster, grid);
        findCluster(row - 1, col, symbol, visited, cluster, grid);
        findCluster(row, col + 1, symbol, visited, cluster, grid);
        findCluster(row, col - 1, symbol, visited, cluster, grid);
    }

    /**
     * Nyeremény számítása
     */
    private double calculatePayout(Map<Integer, List<int[]>> matchedClusters, int betAmount) {
        double totalPayout = 0;

        for (Map.Entry<Integer, List<int[]>> entry : matchedClusters.entrySet()) {
            int symbol = entry.getKey();
            int clusterSize = entry.getValue().size();
            Pair<Double, Integer> multiplierInfo = getPayoutMultiplier(symbol, clusterSize);
            double payout = betAmount * multiplierInfo.first;
            totalPayout += payout;
        }

        return totalPayout;
    }

    /**
     * Szorzó lekérése
     */
    public Pair<Double, Integer> getPayoutMultiplier(int symbol, int clusterSize) {
        int multiplierIndex = Math.min(clusterSize - CLUSTER_SIZE, payoutMultipliers.length - 1);
        double multiplier = payoutMultipliers[multiplierIndex][symbol];
        return new Pair<>(multiplier, clusterSize);
    }

    /**
     * Matched szimbólumok törlése
     */
    private void clearMatchedSymbols(int[][] grid, Map<Integer, List<int[]>> matchedClusters) {
        for (Map.Entry<Integer, List<int[]>> entry : matchedClusters.entrySet()) {
            for (int[] position : entry.getValue()) {
                grid[position[0]][position[1]] = -1;  // Üresítjük a matched szimbólumokat
            }
        }
    }

    /**
     * Üres helyek feltöltése új szimbólumokkal
     */
    private void dropAndRefillSymbols(int[][] grid) {
        for (int col = 0; col < GRID_SIZE; col++) {
            int emptyRow = GRID_SIZE - 1;

            // Az oszlop végigjárása alulról felfelé
            for (int row = GRID_SIZE - 1; row >= 0; row--) {
                if (grid[row][col] != -1) {
                    grid[emptyRow][col] = grid[row][col];
                    if (emptyRow != row) {
                        grid[row][col] = -1;
                    }
                    emptyRow--;
                }
            }

            // Üres helyek feltöltése új szimbólumokkal a legfelső sorban
            while (emptyRow >= 0) {
                if (random.nextDouble() < 0.2) {  // 20% esély a klaszterformációra
                    grid[emptyRow][col] = suggestClusterSymbol(col, emptyRow, grid);
                } else {
                    grid[emptyRow][col] = generateSymbol();  // Véletlenszerű szimbólum
                }
                emptyRow--;
            }
        }
    }

    /**
     * Klaszter formáció javaslata
     */
    private int suggestClusterSymbol(int col, int row, int[][] grid) {
        List<Integer> possibleSymbols = new ArrayList<>();

        // Nézd meg az összes szomszédot
        if (col > 0 && grid[row][col - 1] != -1) {  // Balra
            possibleSymbols.add(grid[row][col - 1]);
        }
        if (col < GRID_SIZE - 1 && grid[row][col + 1] != -1) {  // Jobbra
            possibleSymbols.add(grid[row][col + 1]);
        }

        // Ha van lehetséges szimbólum, válassz közülük nagyobb eséllyel
        if (!possibleSymbols.isEmpty() && random.nextDouble() < 0.8) {  // 80% esély a szomszéd másolására
            return possibleSymbols.get(random.nextInt(possibleSymbols.size()));
        }

        // Ha nincs releváns szomszéd, vagy nem másolunk, generáljunk egy véletlenszerű szimbólumot
        return generateSymbol();
    }

    /**
     * Bonus trigger ellenőrzése
     */
    public boolean checkForBonusTrigger(int[][] grid) {
        int scatterCount = 0;
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                if (grid[row][col] == SCATTER_SYMBOL) {
                    scatterCount++;
                }
            }
        }
        return scatterCount >= BONUS_TRIGGER_COUNT;
    }

    /**
     * Retrigger ellenőrzése
     */
    public boolean checkForRetrigger(int[][] grid) {
        int scatterCount = 0;
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                if (grid[row][col] == SCATTER_SYMBOL) {
                    scatterCount++;
                }
            }
        }
        return scatterCount >= RETRIGGER_COUNT;
    }

    /**
     * Grid másolása
     */
    private int[][] copyGrid(int[][] original) {
        int[][] copy = new int[GRID_SIZE][GRID_SIZE];
        for (int i = 0; i < GRID_SIZE; i++) {
            System.arraycopy(original[i], 0, copy[i], 0, GRID_SIZE);
        }
        return copy;
    }

    /**
     * Spin eredménye
     */
    public static class SpinResult {
        private int betAmount;
        private int[][] initialGrid;
        private int[][] finalGrid;
        private List<CascadeStep> cascadeSteps;
        private double totalPayout;
        private boolean bonusTrigger;
        private boolean retrigger;

        // Getters and setters
        public int getBetAmount() { return betAmount; }
        public void setBetAmount(int betAmount) { this.betAmount = betAmount; }

        public int[][] getInitialGrid() { return initialGrid; }
        public void setInitialGrid(int[][] initialGrid) { this.initialGrid = initialGrid; }

        public int[][] getFinalGrid() { return finalGrid; }
        public void setFinalGrid(int[][] finalGrid) { this.finalGrid = finalGrid; }

        public List<CascadeStep> getCascadeSteps() { return cascadeSteps; }
        public void setCascadeSteps(List<CascadeStep> cascadeSteps) { this.cascadeSteps = cascadeSteps; }

        public double getTotalPayout() { return totalPayout; }
        public void setTotalPayout(double totalPayout) { this.totalPayout = totalPayout; }

        public boolean isBonusTrigger() { return bonusTrigger; }
        public void setBonusTrigger(boolean bonusTrigger) { this.bonusTrigger = bonusTrigger; }

        public boolean isRetrigger() { return retrigger; }
        public void setRetrigger(boolean retrigger) { this.retrigger = retrigger; }
    }

    /**
     * Cascade lépés
     */
    public static class CascadeStep {
        private Map<Integer, List<int[]>> matchedClusters;
        private double payout;
        private int[][] gridAfterClear;
        private int[][] gridAfterRefill;

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
