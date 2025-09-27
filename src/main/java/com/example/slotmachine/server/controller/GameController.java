package com.example.slotmachine.server.controller;

import com.example.slotmachine.server.dto.BalanceResponse;
import com.example.slotmachine.server.dto.SpinRequest;
import com.example.slotmachine.server.dto.SpinResponse;
import com.example.slotmachine.server.entity.User;
import com.example.slotmachine.server.security.JwtUtil;
import com.example.slotmachine.server.service.GameService;
import com.example.slotmachine.server.service.SlotMachineEngine;
import com.example.slotmachine.server.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/game")
@CrossOrigin(origins = "*") // Allow all origins for external access
public class GameController {

    @Autowired
    private GameService gameService;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/balance")
    public ResponseEntity<?> getBalance(@RequestHeader("Authorization") String authHeader) {
        try {
            User user = getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.badRequest().body("Invalid token or user not found");
            }
            
            // Check if user is still active
            if (!user.getActive()) {
                return ResponseEntity.status(403).body("USER_BANNED");
            }
            
            return ResponseEntity.ok(new BalanceResponse(user.getBalance(), user.getUsername()));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to get balance: " + e.getMessage());
        }
    }

    @PostMapping("/spin")
    public ResponseEntity<?> processSpin(@RequestHeader("Authorization") String authHeader, 
                                       @RequestBody SpinRequest spinRequest) {
        try {
            User user = getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.badRequest().body("Invalid token or user not found");
            }
            
            // Check if user is still active
            if (!user.getActive()) {
                return ResponseEntity.status(403).body("USER_BANNED");
            }

            // Validate spin request
            if (spinRequest.getBetAmount() == null || spinRequest.getBetAmount() <= 0) {
                return ResponseEntity.badRequest().body("Invalid bet amount");
            }

            // Új logika: a szerver generálja a szimbólumokat és számítja a nyereményt
            SlotMachineEngine.SpinResult spinResult = gameService.processSpinNew(
                user.getUsername(), 
                spinRequest.getBetAmount(), 
                spinRequest.getIsBonusMode()
            );

            // Get updated balance - refresh user from DB
            Optional<User> userOpt = userService.findById(user.getId());
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("User not found");
            }

            User updatedUser = userOpt.get();
            
            // Convert SpinResult to SpinResponse
            List<SpinResponse.CascadeStepDto> cascadeSteps = spinResult.getCascadeSteps().stream()
                .map(step -> new SpinResponse.CascadeStepDto(
                    step.getMatchedClusters(),
                    step.getPayout(),
                    step.getGridAfterClear(),
                    step.getGridAfterRefill()
                ))
                .collect(java.util.stream.Collectors.toList());

            return ResponseEntity.ok(SpinResponse.success(
                updatedUser.getBalance(), 
                spinResult.getTotalPayout(),
                spinResult.getInitialGrid(),
                spinResult.getFinalGrid(),
                cascadeSteps,
                spinResult.isBonusTrigger(),
                spinResult.isRetrigger()
            ));

        } catch (RuntimeException e) {
            if (e.getMessage().equals("Insufficient balance")) {
                return ResponseEntity.ok(SpinResponse.error("Insufficient balance"));
            }
            return ResponseEntity.internalServerError().body("Spin processing failed: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Spin processing failed: " + e.getMessage());
        }
    }

    private User getUserFromToken(String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return null;
            }

            String token = authHeader.substring(7);
            
            if (!jwtUtil.validateToken(token) || jwtUtil.isTokenExpired(token)) {
                return null;
            }

            // Try to get user by ID first (for renamed users)
            Long userId = jwtUtil.getUserIdFromToken(token);
            if (userId != null) {
                Optional<User> userOpt = userService.findById(userId);
                if (userOpt.isPresent()) {
                    return userOpt.get();
                }
            }
            
            // Fallback to username (for backward compatibility)
            String username = jwtUtil.getUsernameFromToken(token);
            if (username != null) {
                Optional<User> userOpt = userService.findByUsername(username);
                if (userOpt.isPresent()) {
                    return userOpt.get();
                }
            }
            
            return null;

        } catch (Exception e) {
            return null;
        }
    }
    
    private String getUsernameFromToken(String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return null;
            }

            String token = authHeader.substring(7);
            
            if (!jwtUtil.validateToken(token) || jwtUtil.isTokenExpired(token)) {
                return null;
            }

            return jwtUtil.getUsernameFromToken(token);

        } catch (Exception e) {
            return null;
        }
    }
}
