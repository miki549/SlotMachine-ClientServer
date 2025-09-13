package com.example.slotmachine.server.repository;

import com.example.slotmachine.server.entity.GameTransaction;
import com.example.slotmachine.server.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GameTransactionRepository extends JpaRepository<GameTransaction, Long> {
    List<GameTransaction> findByUserOrderByCreatedAtDesc(User user);
    
    @Query("SELECT SUM(t.amount) FROM GameTransaction t WHERE t.user = :user AND t.type = 'BET' AND t.createdAt >= :since")
    Double getTotalBetsForUserSince(User user, LocalDateTime since);
    
    @Query("SELECT SUM(t.amount) FROM GameTransaction t WHERE t.user = :user AND t.type = 'WIN' AND t.createdAt >= :since")
    Double getTotalWinsForUserSince(User user, LocalDateTime since);
    
    void deleteByUser(User user);
}
