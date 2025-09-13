package com.example.slotmachine.server.service;

import com.example.slotmachine.server.entity.User;
import com.example.slotmachine.server.repository.UserRepository;
import com.example.slotmachine.server.repository.GameTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private GameTransactionRepository gameTransactionRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public User createUser(String username, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists");
        }
        
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setBalance(0.0);
        
        return userRepository.save(user);
    }

    public boolean validatePassword(User user, String password) {
        return passwordEncoder.matches(password, user.getPassword());
    }

    public void updateLastLogin(User user) {
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
    }

    public void updateBalance(User user, Double newBalance) {
        user.setBalance(newBalance);
        userRepository.save(user);
    }

    public void addCredits(String username, Double amount) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setBalance(user.getBalance() + amount);
        userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public void deactivateUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setActive(false);
        userRepository.save(user);
    }

    public void activateUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setActive(true);
        userRepository.save(user);
    }

    public void setUserBalance(String username, Double newBalance) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setBalance(newBalance);
        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Először töröljük a felhasználóhoz tartozó tranzakciókat
        gameTransactionRepository.deleteByUser(user);
        
        // Majd töröljük magát a felhasználót
        userRepository.delete(user);
    }

    public void renameUser(String oldUsername, String newUsername) {
        if (userRepository.existsByUsername(newUsername)) {
            throw new RuntimeException("New username already exists");
        }
        
        User user = userRepository.findByUsername(oldUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setUsername(newUsername);
        userRepository.save(user);
    }
}
