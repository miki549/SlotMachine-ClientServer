package com.example.slotmachine.server.controller;

import com.example.slotmachine.server.dto.LoginRequest;
import com.example.slotmachine.server.dto.LoginResponse;
import com.example.slotmachine.server.entity.User;
import com.example.slotmachine.server.security.JwtUtil;
import com.example.slotmachine.server.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*") // Allow all origins for external access
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            Optional<User> userOpt = userService.findByUsername(loginRequest.getUsername());
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Invalid username or password");
            }

            User user = userOpt.get();
            
            if (!userService.validatePassword(user, loginRequest.getPassword())) {
                return ResponseEntity.badRequest().body("Invalid username or password");
            }

            if (!user.getActive()) {
                return ResponseEntity.badRequest().body("Account is disabled");
            }

            // Update last login
            userService.updateLastLogin(user);

            // Generate JWT token
            String token = jwtUtil.generateToken(user.getUsername());

            LoginResponse response = new LoginResponse(token, user.getUsername(), user.getBalance());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Login failed: " + e.getMessage());
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody LoginRequest registerRequest) {
        try {
            User user = userService.createUser(registerRequest.getUsername(), registerRequest.getPassword());
            String token = jwtUtil.generateToken(user.getUsername());
            
            LoginResponse response = new LoginResponse(token, user.getUsername(), user.getBalance());
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Registration failed: " + e.getMessage());
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.badRequest().body("Invalid token format");
            }

            String token = authHeader.substring(7);
            
            if (!jwtUtil.validateToken(token) || jwtUtil.isTokenExpired(token)) {
                return ResponseEntity.badRequest().body("Invalid or expired token");
            }

            String username = jwtUtil.getUsernameFromToken(token);
            Optional<User> userOpt = userService.findByUsername(username);
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("User not found");
            }

            User user = userOpt.get();
            return ResponseEntity.ok(new LoginResponse(token, user.getUsername(), user.getBalance()));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Token validation failed: " + e.getMessage());
        }
    }
}
