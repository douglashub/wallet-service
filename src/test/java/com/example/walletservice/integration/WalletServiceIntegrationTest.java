package com.example.walletservice.integration;

import com.example.walletservice.dto.WalletDTO;
import com.example.walletservice.entity.Wallet;
import com.example.walletservice.repository.TransactionRepository;
import com.example.walletservice.repository.WalletRepository;
import com.example.walletservice.service.WalletService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@DirtiesContext // Adicionado para reiniciar o contexto após cada teste
class WalletServiceIntegrationTest {

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private final String sourceOwnerId = "integration-test-source";
    private final String targetOwnerId = "integration-test-target";

    @BeforeEach
    void setUp() {
        // Ordem corrigida: transações primeiro
        transactionRepository.deleteAll();
        walletRepository.deleteAll();
    }
    
    // Removido o método tearDown()

    @Test
    void fullWalletLifecycle() {
        // 1. Create wallets
        WalletDTO sourceWallet = walletService.createWallet(sourceOwnerId);
        WalletDTO targetWallet = walletService.createWallet(targetOwnerId);
        
        assertNotNull(sourceWallet);
        assertNotNull(targetWallet);
        assertEquals(BigDecimal.ZERO, sourceWallet.getBalance());
        assertEquals(BigDecimal.ZERO, targetWallet.getBalance());
        
        // 2. Deposit funds
        WalletDTO updatedSourceWallet = walletService.deposit(sourceOwnerId, BigDecimal.valueOf(100), "Initial deposit");
        assertEquals(0, BigDecimal.valueOf(100).compareTo(updatedSourceWallet.getBalance()));
        
        // 3. Transfer funds
        WalletDTO afterTransferSourceWallet = walletService.transfer(sourceOwnerId, targetOwnerId, BigDecimal.valueOf(30), "Test transfer");
        WalletDTO afterTransferTargetWallet = walletService.getWalletByOwnerId(targetOwnerId);
        
        assertEquals(0, BigDecimal.valueOf(70).compareTo(afterTransferSourceWallet.getBalance()));
        assertEquals(0, BigDecimal.valueOf(30).compareTo(afterTransferTargetWallet.getBalance()));
        
        // 4. Withdraw funds
        WalletDTO afterWithdrawSourceWallet = walletService.withdraw(sourceOwnerId, BigDecimal.valueOf(20), "Test withdrawal");
        assertEquals(0, BigDecimal.valueOf(50).compareTo(afterWithdrawSourceWallet.getBalance()));
        
        // 5. Verify transaction count
        Wallet sourceWalletEntity = walletRepository.findByOwnerId(sourceOwnerId).orElseThrow();
        Wallet targetWalletEntity = walletRepository.findByOwnerId(targetOwnerId).orElseThrow();
        
        // Source wallet should have 3 transactions: deposit, transfer out, withdrawal
        assertEquals(3, transactionRepository.findByWalletIdOrderByTimestampDesc(sourceWalletEntity.getId()).size());
        
        // Target wallet should have 1 transaction: transfer in
        assertEquals(1, transactionRepository.findByWalletIdOrderByTimestampDesc(targetWalletEntity.getId()).size());
    }
}