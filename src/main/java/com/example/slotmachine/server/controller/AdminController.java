package com.example.slotmachine.server.controller;

import com.example.slotmachine.server.entity.GameTransaction;
import com.example.slotmachine.server.entity.User;
import com.example.slotmachine.server.service.GameService;
import com.example.slotmachine.server.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*") // Allow all origins for external access
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private GameService gameService;

    @PostMapping("/add-credits")
    public ResponseEntity<?> addCredits(@RequestBody Map<String, Object> request) {
        try {
            String username = (String) request.get("username");
            Double amount = Double.valueOf(request.get("amount").toString());

            if (username == null || amount == null || amount <= 0) {
                return ResponseEntity.badRequest().body("Invalid username or amount");
            }

            userService.addCredits(username, amount);
            return ResponseEntity.ok("Credits added successfully");

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to add credits: " + e.getMessage());
        }
    }

    @GetMapping("/transactions/{username}")
    public ResponseEntity<?> getUserTransactions(@PathVariable("username") String username) {
        try {
            List<GameTransaction> transactions = gameService.getUserTransactions(username);
            return ResponseEntity.ok(transactions);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to get transactions: " + e.getMessage());
        }
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        try {
            List<User> users = userService.getAllUsers();
            return ResponseEntity.ok(users);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to get users: " + e.getMessage());
        }
    }

    @PostMapping("/user/deactivate")
    public ResponseEntity<?> deactivateUser(@RequestBody Map<String, Object> request) {
        try {
            String username = (String) request.get("username");

            if (username == null || username.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Invalid username");
            }

            userService.deactivateUser(username);
            return ResponseEntity.ok("User deactivated successfully");

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to deactivate user: " + e.getMessage());
        }
    }

    @PostMapping("/user/activate")
    public ResponseEntity<?> activateUser(@RequestBody Map<String, Object> request) {
        try {
            String username = (String) request.get("username");

            if (username == null || username.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Invalid username");
            }

            userService.activateUser(username);
            return ResponseEntity.ok("User activated successfully");

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to activate user: " + e.getMessage());
        }
    }

    @PostMapping("/user/update-balance")
    public ResponseEntity<?> updateUserBalance(@RequestBody Map<String, Object> request) {
        try {
            String username = (String) request.get("username");
            Double newBalance = Double.valueOf(request.get("balance").toString());

            if (username == null || newBalance == null || newBalance < 0) {
                return ResponseEntity.badRequest().body("Invalid username or balance");
            }

            userService.setUserBalance(username, newBalance);
            return ResponseEntity.ok("Balance updated successfully");

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to update balance: " + e.getMessage());
        }
    }

    @DeleteMapping("/user/{username}")
    public ResponseEntity<?> deleteUser(@PathVariable("username") String username) {
        try {
            userService.deleteUser(username);
            return ResponseEntity.ok("User deleted successfully");

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to delete user: " + e.getMessage());
        }
    }

    @PostMapping("/user/rename")
    public ResponseEntity<?> renameUser(@RequestBody Map<String, Object> request) {
        try {
            String oldUsername = (String) request.get("oldUsername");
            String newUsername = (String) request.get("newUsername");

            if (oldUsername == null || newUsername == null || 
                oldUsername.trim().isEmpty() || newUsername.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Invalid usernames");
            }

            userService.renameUser(oldUsername, newUsername);
            return ResponseEntity.ok("User renamed successfully");

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to rename user: " + e.getMessage());
        }
    }
}
