package com.example.slotmachine.server.service;

import com.example.slotmachine.server.dto.SpinResponse;
import com.example.slotmachine.server.entity.GameTransaction;
import com.example.slotmachine.server.entity.User;
import com.example.slotmachine.server.repository.GameTransactionRepository;
import com.example.slotmachine.server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class GameService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private GameTransactionRepository transactionRepository;

    @Autowired
    private SlotMachineEngine slotMachineEngine;

    /**
     * Új spin feldolgozás - a szerver generálja a szimbólumokat és számítja a nyereményt
     */
    public SlotMachineEngine.SpinResult processSpinNew(String username, Integer betAmount, Boolean isBonusMode) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Ellenőrizzük, hogy van-e elég balance
        if (user.getBalance() < betAmount) {
            throw new RuntimeException("Insufficient balance");
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

        // Spin feldolgozása a játékmotor segítségével
        SlotMachineEngine.SpinResult spinResult = slotMachineEngine.processSpin(betAmount, isBonusMode != null ? isBonusMode : false);

        // Ha van nyeremény, hozzáadjuk
        if (spinResult.getTotalPayout() > 0) {
            Double balanceBeforeWin = user.getBalance();
            
            user.setBalance(user.getBalance() + spinResult.getTotalPayout());
            
            // Nyeremény tranzakció rögzítése
            GameTransaction winTransaction = new GameTransaction(
                    user,
                    GameTransaction.TransactionType.WIN,
                    spinResult.getTotalPayout(),
                    balanceBeforeWin,
                    user.getBalance(),
                    "Spin win"
            );
            transactionRepository.save(winTransaction);
        }

        userRepository.save(user);
        return spinResult;
    }


    public List<GameTransaction> getUserTransactions(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return transactionRepository.findByUserOrderByCreatedAtDesc(user);
    }
}
