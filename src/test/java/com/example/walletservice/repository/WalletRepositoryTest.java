package com.example.walletservice.repository;

import com.example.walletservice.entity.Wallet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class WalletRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private WalletRepository walletRepository;

    @Test
    void findByOwnerId_Success() {
        // Given
        String ownerId = "test-user-123";
        Wallet wallet = Wallet.builder()
                .ownerId(ownerId)
                .balance(BigDecimal.valueOf(100))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        entityManager.persist(wallet);
        entityManager.flush();
        
        // When
        Optional<Wallet> found = walletRepository.findByOwnerId(ownerId);
        
        // Then
        assertTrue(found.isPresent());
        assertEquals(ownerId, found.get().getOwnerId());
        assertEquals(0, BigDecimal.valueOf(100).compareTo(found.get().getBalance()));
    }
    
    @Test
    void findByOwnerId_NotFound() {
        // When
        Optional<Wallet> found = walletRepository.findByOwnerId("non-existent-user");
        
        // Then
        assertFalse(found.isPresent());
    }
    
    @Test
    void existsByOwnerId_True() {
        // Given
        String ownerId = "test-user-456";
        Wallet wallet = Wallet.builder()
                .ownerId(ownerId)
                .balance(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        entityManager.persist(wallet);
        entityManager.flush();
        
        // When
        boolean exists = walletRepository.existsByOwnerId(ownerId);
        
        // Then
        assertTrue(exists);
    }
    
    @Test
    void existsByOwnerId_False() {
        // When
        boolean exists = walletRepository.existsByOwnerId("non-existent-user");
        
        // Then
        assertFalse(exists);
    }
}