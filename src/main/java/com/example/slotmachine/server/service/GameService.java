package com.example.slotmachine.server.service;

import com.example.slotmachine.server.entity.GameTransaction;
import com.example.slotmachine.server.entity.User;
import com.example.slotmachine.server.repository.GameTransactionRepository;
import com.example.slotmachine.server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional
public class GameService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private GameTransactionRepository transactionRepository;

    public boolean processSpin(String username, Integer betAmount, int[][] symbols, Double payout) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Ellenőrizzük, hogy van-e elég balance
        if (user.getBalance() < betAmount) {
            return false;
        }

        Double balanceBefore = user.getBalance();
        
        // Tét levonása
        user.setBalance(user.getBalance() - betAmount);
        
        // Tét tranzakció rögzítése
        GameTransaction betTransaction = new GameTransaction(
                user, 
                GameTransaction.TransactionType.BET, 
                -betAmount.doubleValue(), 
                balanceBefore, 
                user.getBalance(),
                "Spin bet"
        );
        transactionRepository.save(betTransaction);

        // Ha van nyeremény, hozzáadjuk
        if (payout > 0) {
            Double balanceBeforeWin = user.getBalance();
            
            // Szerver oldali nyeremény ellenőrzés (egyszerűsített)
            Double serverPayout = calculateServerPayout(symbols, betAmount);
            
            // Biztonsági ellenőrzés: a kliens által küldött nyeremény ne legyen nagyobb a szerveren számítottnál
            Double actualPayout = Math.min(payout, serverPayout);
            
            user.setBalance(user.getBalance() + actualPayout);
            
            // Nyeremény tranzakció rögzítése
            GameTransaction winTransaction = new GameTransaction(
                    user,
                    GameTransaction.TransactionType.WIN,
                    actualPayout,
                    balanceBeforeWin,
                    user.getBalance(),
                    "Spin win"
            );
            transactionRepository.save(winTransaction);
        }

        userRepository.save(user);
        return true;
    }

    // Egyszerűsített szerver oldali nyeremény számítás (biztonsági ellenőrzéshez)
    private Double calculateServerPayout(int[][] symbols, Integer betAmount) {
        // Itt implementálhatjuk a játéklogika egy egyszerűsített verzióját
        // Most csak egy alapvető ellenőrzést végzünk
        
        // Ellenőrizzük a klasztereket (egyszerűsített verzió)
        Map<Integer, Integer> symbolCounts = countSymbols(symbols);
        
        double totalPayout = 0;
        for (Map.Entry<Integer, Integer> entry : symbolCounts.entrySet()) {
            int symbol = entry.getKey();
            int count = entry.getValue();
            
            if (count >= 5) { // Minimum klaszter méret
                // Egyszerűsített szorzó számítás
                double multiplier = getSimplifiedMultiplier(symbol, count);
                totalPayout += betAmount * multiplier;
            }
        }
        
        return totalPayout;
    }

    private Map<Integer, Integer> countSymbols(int[][] symbols) {
        Map<Integer, Integer> counts = new java.util.HashMap<>();
        for (int[] row : symbols) {
            for (int symbol : row) {
                counts.put(symbol, counts.getOrDefault(symbol, 0) + 1);
            }
        }
        return counts;
    }

    private double getSimplifiedMultiplier(int symbol, int count) {
        // Egyszerűsített szorzó táblázat (biztonsági célokra)
        double baseMultiplier = switch (symbol) {
            case 0, 1, 2 -> 0.2; // Alacsony értékű szimbólumok
            case 3, 4 -> 0.3;    // Közepes értékű szimbólumok
            case 5, 6 -> 0.5;    // Magas értékű szimbólumok
            case 7 -> 1.0;       // Prémium szimbólum
            case 8 -> 2.0;       // Scatter
            default -> 0.1;
        };
        
        // Klaszter méret alapján növeljük a szorzót
        return baseMultiplier * Math.min(count / 5.0, 3.0); // Maximum 3x szorzó
    }

    public List<GameTransaction> getUserTransactions(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return transactionRepository.findByUserOrderByCreatedAtDesc(user);
    }
}
