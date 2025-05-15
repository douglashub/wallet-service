package com.example.walletservice.repository;

import com.example.walletservice.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    
    Optional<Wallet> findByOwnerId(String ownerId);
    
    boolean existsByOwnerId(String ownerId);
}