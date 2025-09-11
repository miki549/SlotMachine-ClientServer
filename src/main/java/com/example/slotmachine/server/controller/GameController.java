package com.example.slotmachine.server.controller;

import com.example.slotmachine.server.dto.BalanceResponse;
import com.example.slotmachine.server.dto.SpinRequest;
import com.example.slotmachine.server.dto.SpinResponse;
import com.example.slotmachine.server.entity.User;
import com.example.slotmachine.server.security.JwtUtil;
import com.example.slotmachine.server.service.GameService;
import com.example.slotmachine.server.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/game")
@CrossOrigin(origins = "*") // Development only
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
            String username = getUsernameFromToken(authHeader);
            if (username == null) {
                return ResponseEntity.badRequest().body("Invalid token");
            }

            Optional<User> userOpt = userService.findByUsername(username);
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("User not found");
            }

            User user = userOpt.get();
            return ResponseEntity.ok(new BalanceResponse(user.getBalance(), user.getUsername()));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to get balance: " + e.getMessage());
        }
    }

    @PostMapping("/spin")
    public ResponseEntity<?> processSpin(@RequestHeader("Authorization") String authHeader, 
                                       @RequestBody SpinRequest spinRequest) {
        try {
            String username = getUsernameFromToken(authHeader);
            if (username == null) {
                return ResponseEntity.badRequest().body("Invalid token");
            }

            // Validate spin request
            if (spinRequest.getBetAmount() == null || spinRequest.getBetAmount() <= 0) {
                return ResponseEntity.badRequest().body("Invalid bet amount");
            }

            if (spinRequest.getSymbols() == null || spinRequest.getSymbols().length != 7 || 
                spinRequest.getSymbols()[0].length != 7) {
                return ResponseEntity.badRequest().body("Invalid symbols grid");
            }

            if (spinRequest.getPayout() == null || spinRequest.getPayout() < 0) {
                return ResponseEntity.badRequest().body("Invalid payout");
            }

            boolean success = gameService.processSpin(
                username, 
                spinRequest.getBetAmount(), 
                spinRequest.getSymbols(), 
                spinRequest.getPayout()
            );

            if (!success) {
                return ResponseEntity.ok(SpinResponse.error("Insufficient balance"));
            }

            // Get updated balance
            Optional<User> userOpt = userService.findByUsername(username);
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("User not found");
            }

            User user = userOpt.get();
            return ResponseEntity.ok(SpinResponse.success(user.getBalance(), spinRequest.getPayout()));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Spin processing failed: " + e.getMessage());
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
