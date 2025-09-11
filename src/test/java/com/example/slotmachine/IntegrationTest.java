package com.example.slotmachine;

import com.example.slotmachine.client.ApiClient;
import com.example.slotmachine.server.dto.BalanceResponse;
import com.example.slotmachine.server.dto.LoginResponse;
import com.example.slotmachine.server.dto.SpinResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;

import static org.junit.jupiter.api.Assertions.*;

@Disabled("Integration tests require running server")
public class IntegrationTest {
    
    private ApiClient apiClient;
    
    @BeforeEach
    void setUp() {
        apiClient = new ApiClient();
    }
    
    @Test
    void testUserRegistrationAndLogin() throws Exception {
        String username = "testuser_" + System.currentTimeMillis();
        String password = "testpass123";
        
        // Regisztráció
        LoginResponse registerResponse = apiClient.register(username, password);
        assertNotNull(registerResponse);
        assertEquals(username, registerResponse.getUsername());
        assertNotNull(registerResponse.getToken());
        assertEquals(0.0, registerResponse.getBalance());
        
        // Bejelentkezés
        ApiClient newClient = new ApiClient();
        LoginResponse loginResponse = newClient.login(username, password);
        assertNotNull(loginResponse);
        assertEquals(username, loginResponse.getUsername());
        assertNotNull(loginResponse.getToken());
    }
    
    @Test
    void testBalanceRetrieval() throws Exception {
        String username = "balancetest_" + System.currentTimeMillis();
        String password = "testpass123";
        
        // Regisztráció
        apiClient.register(username, password);
        
        // Balance lekérés
        BalanceResponse balanceResponse = apiClient.getBalance();
        assertNotNull(balanceResponse);
        assertEquals(username, balanceResponse.getUsername());
        assertEquals(0.0, balanceResponse.getBalance());
    }
    
    @Test
    void testSpinWithInsufficientBalance() throws Exception {
        String username = "spintest_" + System.currentTimeMillis();
        String password = "testpass123";
        
        // Regisztráció
        apiClient.register(username, password);
        
        // Pörgetés próbálkozás elégségtelen balance-szel
        int[][] symbols = new int[7][7]; // Üres rács
        SpinResponse spinResponse = apiClient.processSpin(100, symbols, 0.0);
        
        assertNotNull(spinResponse);
        assertFalse(spinResponse.isSuccess());
        assertTrue(spinResponse.getMessage().contains("Insufficient balance") || 
                  spinResponse.getMessage().contains("insufficient"));
    }
}
