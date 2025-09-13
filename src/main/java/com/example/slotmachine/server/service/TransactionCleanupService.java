package com.example.slotmachine.server.service;

import com.example.slotmachine.server.entity.GameTransaction;
import com.example.slotmachine.server.entity.User;
import com.example.slotmachine.server.repository.GameTransactionRepository;
import com.example.slotmachine.server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TransactionCleanupService {
    
    private static final int MAX_TRANSACTIONS_PER_USER = 1000;
    
    @Autowired
    private GameTransactionRepository transactionRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Minden nap hajnali 2:00-kor futtatja a cleanup job-ot
     * Törli a régi tranzakciókat, hogy minden felhasználónál maximum 1000 tranzakció maradjon
     */
    @Scheduled(cron = "0 0 2 * * ?") // Minden nap 2:00-kor
    @Transactional
    public void cleanupOldTransactions() {
        System.out.println("🧹 Tranzakció cleanup indítása...");
        
        List<User> allUsers = userRepository.findAll();
        int totalDeleted = 0;
        
        for (User user : allUsers) {
            int deletedCount = cleanupTransactionsForUser(user);
            totalDeleted += deletedCount;
            
            if (deletedCount > 0) {
                System.out.println("👤 " + user.getUsername() + ": " + deletedCount + " régi tranzakció törölve");
            }
        }
        
        System.out.println("✅ Tranzakció cleanup befejezve. Összesen " + totalDeleted + " tranzakció törölve.");
    }
    
    /**
     * Egy adott felhasználó tranzakcióinak cleanup-ja
     * @param user A felhasználó
     * @return Törölt tranzakciók száma
     */
    private int cleanupTransactionsForUser(User user) {
        // Összes tranzakció lekérése dátum szerint csökkenő sorrendben
        List<GameTransaction> allTransactions = transactionRepository.findByUserOrderByCreatedAtDesc(user);
        
        // Ha 1000-nél kevesebb van, nem kell törölni
        if (allTransactions.size() <= MAX_TRANSACTIONS_PER_USER) {
            return 0;
        }
        
        // A legutóbbi 1000 tranzakció megtartása, a többi törlése
        List<GameTransaction> transactionsToDelete = allTransactions.subList(MAX_TRANSACTIONS_PER_USER, allTransactions.size());
        
        // Tranzakciók törlése
        transactionRepository.deleteAll(transactionsToDelete);
        
        return transactionsToDelete.size();
    }
    
    /**
     * Manuális cleanup futtatása (admin használatra)
     * @return Törölt tranzakciók száma
     */
    @Transactional
    public int manualCleanup() {
        System.out.println("🧹 Manuális tranzakció cleanup indítása...");
        
        List<User> allUsers = userRepository.findAll();
        int totalDeleted = 0;
        
        for (User user : allUsers) {
            int deletedCount = cleanupTransactionsForUser(user);
            totalDeleted += deletedCount;
            
            if (deletedCount > 0) {
                System.out.println("👤 " + user.getUsername() + ": " + deletedCount + " régi tranzakció törölve");
            }
        }
        
        System.out.println("✅ Manuális tranzakció cleanup befejezve. Összesen " + totalDeleted + " tranzakció törölve.");
        return totalDeleted;
    }
}
