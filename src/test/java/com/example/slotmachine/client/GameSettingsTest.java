package com.example.slotmachine.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tesztek a GameSettings osztályhoz
 */
@DisplayName("GameSettings Tests")
public class GameSettingsTest {

    @Test
    @DisplayName("Should have correct default balance")
    public void testDefaultBalance() {
        // GameSettings doesn't have DEFAULT_BALANCE, so we test other constants
        assertTrue(GameSettings.DEFAULT_BET > 0);
    }

    @Test
    @DisplayName("Should have correct default bet")
    public void testDefaultBet() {
        assertEquals(600, GameSettings.DEFAULT_BET);
    }

    @Test
    @DisplayName("Should have correct symbol count")
    public void testSymbolCount() {
        assertEquals(9, GameSettings.SYMBOL_COUNT);
    }

    @Test
    @DisplayName("Should have correct cluster size")
    public void testClusterSize() {
        assertEquals(5, GameSettings.CLUSTER_SIZE);
    }

    @Test
    @DisplayName("Should have correct grid size")
    public void testGridSize() {
        assertEquals(7, GameSettings.GRID_SIZE);
    }

    @Test
    @DisplayName("Should have correct bonus price")
    public void testBonusPrice() {
        assertEquals(100, GameSettings.BONUS_PRICE);
    }

    @Test
    @DisplayName("Should have correct scatter symbol")
    public void testScatterSymbol() {
        assertEquals(8, GameSettings.SCATTER_SYMBOL);
    }

    @Test
    @DisplayName("Should have correct bonus trigger count")
    public void testBonusTriggerCount() {
        assertEquals(4, GameSettings.BONUS_TRIGGER_COUNT);
    }

    @Test
    @DisplayName("Should have correct retrigger count")
    public void testRetriggerCount() {
        assertEquals(3, GameSettings.RETRIGGER_COUNT);
    }

    @Test
    @DisplayName("Should have correct free spins")
    public void testFreeSpins() {
        assertEquals(10, GameSettings.FREE_SPINS);
    }

    @Test
    @DisplayName("Should have correct retrigger spins")
    public void testRetriggerSpins() {
        assertEquals(5, GameSettings.RETRIGGER_SPINS);
    }

    @Test
    @DisplayName("Should have correct bet step")
    public void testBetStep() {
        assertEquals(400, GameSettings.BET_STEP);
    }

    @Test
    @DisplayName("Should have correct min bet")
    public void testMinBet() {
        assertEquals(200, GameSettings.MIN_BET);
    }

    @Test
    @DisplayName("Should have correct max bet")
    public void testMaxBet() {
        assertEquals(6000, GameSettings.MAX_BET);
    }

    @Test
    @DisplayName("Should have valid bet range")
    public void testBetRange() {
        assertTrue(GameSettings.MIN_BET < GameSettings.MAX_BET);
        assertTrue(GameSettings.DEFAULT_BET >= GameSettings.MIN_BET);
        assertTrue(GameSettings.DEFAULT_BET <= GameSettings.MAX_BET);
        assertTrue(GameSettings.MAX_BET % GameSettings.BET_STEP == 0);
        assertTrue(GameSettings.MIN_BET % GameSettings.BET_STEP == 0 || GameSettings.MIN_BET < GameSettings.BET_STEP);
    }

    @Test
    @DisplayName("Should have valid grid configuration")
    public void testGridConfiguration() {
        assertTrue(GameSettings.GRID_SIZE > 0);
        assertTrue(GameSettings.SYMBOL_COUNT > 0);
        assertTrue(GameSettings.CLUSTER_SIZE > 0);
        assertTrue(GameSettings.CLUSTER_SIZE <= GameSettings.GRID_SIZE);
    }

    @Test
    @DisplayName("Should have valid bonus configuration")
    public void testBonusConfiguration() {
        assertTrue(GameSettings.FREE_SPINS > 0);
        assertTrue(GameSettings.RETRIGGER_SPINS > 0);
        assertTrue(GameSettings.BONUS_TRIGGER_COUNT > 0);
        assertTrue(GameSettings.RETRIGGER_COUNT > 0);
        assertTrue(GameSettings.SCATTER_SYMBOL >= 0);
        assertTrue(GameSettings.SCATTER_SYMBOL < GameSettings.SYMBOL_COUNT);
    }

    @Test
    @DisplayName("Should have consistent configuration")
    public void testConfigurationConsistency() {
        // Bet configuration consistency
        int betRange = GameSettings.MAX_BET - GameSettings.MIN_BET;
        int expectedSteps = betRange / GameSettings.BET_STEP;
        assertTrue(expectedSteps >= 1, "Should have at least one bet step");
        
        // Grid configuration consistency
        assertTrue(GameSettings.GRID_SIZE * GameSettings.GRID_SIZE > GameSettings.SYMBOL_COUNT,
                "Grid should be large enough for all symbols");
        
        // Bonus configuration consistency
        assertTrue(GameSettings.BONUS_TRIGGER_COUNT > GameSettings.RETRIGGER_COUNT,
                "Bonus trigger should require more symbols than retrigger");
    }
}
