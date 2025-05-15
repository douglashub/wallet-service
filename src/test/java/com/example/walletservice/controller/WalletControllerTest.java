
package com.example.walletservice.controller;

import com.example.walletservice.dto.WalletDTO;
import com.example.walletservice.entity.Wallet;
import com.example.walletservice.exception.InsufficientFundsException;
import com.example.walletservice.exception.WalletNotFoundException;
import com.example.walletservice.repository.TransactionRepository;
import com.example.walletservice.repository.WalletRepository;
import com.example.walletservice.service.WalletService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {
    
    @Mock
    private WalletRepository walletRepository;
    
    @Mock
    private MeterRegistry meterRegistry;
    
    @Mock
    private Counter counter;
    
    @Mock
    private TransactionRepository transactionRepository;
    
    @InjectMocks
    private WalletService walletService;
    
    private Wallet testWallet;
    private final String ownerId = "test-user-123";

    @BeforeEach
    void setUp() {
        // Mock meter registry to return the counter whenever requested
        lenient().when(meterRegistry.counter(anyString())).thenReturn(counter);
        
        testWallet = Wallet.builder()
                .id(1L)
                .ownerId(ownerId)
                .balance(BigDecimal.valueOf(100))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createWallet_Success() {
        // Create a wallet with zero balance for the return value
        Wallet newWallet = Wallet.builder()
                .id(1L)
                .ownerId(ownerId)
                .balance(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        when(walletRepository.existsByOwnerId(ownerId)).thenReturn(false);
        when(walletRepository.save(any(Wallet.class))).thenReturn(newWallet);

        WalletDTO result = walletService.createWallet(ownerId);

        assertNotNull(result);
        assertEquals(ownerId, result.getOwnerId());
        assertEquals(BigDecimal.ZERO, result.getBalance());
        verify(walletRepository).save(any(Wallet.class));
        verify(counter).increment();
    }

    @Test
    void createWallet_OwnerAlreadyHasWallet() {
        when(walletRepository.existsByOwnerId(ownerId)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> walletService.createWallet(ownerId));
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    void getWalletBalance_Success() {
        when(walletRepository.findByOwnerId(ownerId)).thenReturn(Optional.of(testWallet));

        BigDecimal balance = walletService.getWalletBalance(ownerId);

        assertEquals(BigDecimal.valueOf(100), balance);
    }

    @Test
    void getWalletBalance_WalletNotFound() {
        when(walletRepository.findByOwnerId(ownerId)).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class, () -> walletService.getWalletBalance(ownerId));
    }

    @Test
    void getHistoricalBalance_Success() {
        LocalDateTime timestamp = LocalDateTime.now().minusDays(1);
        BigDecimal historicalBalance = BigDecimal.valueOf(50);
        
        when(walletRepository.findByOwnerId(ownerId)).thenReturn(Optional.of(testWallet));
        when(transactionRepository.calculateBalanceAtTime(eq(1L), eq(timestamp)))
                .thenReturn(historicalBalance);

        BigDecimal result = walletService.getHistoricalBalance(ownerId, timestamp);

        assertEquals(historicalBalance, result);
    }

    @Test
    void getHistoricalBalance_WalletNotFound() {
        LocalDateTime timestamp = LocalDateTime.now().minusDays(1);
        when(walletRepository.findByOwnerId(ownerId)).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class, 
                () -> walletService.getHistoricalBalance(ownerId, timestamp));
    }

    @Test
    void deposit_Success() {
        BigDecimal depositAmount = BigDecimal.valueOf(50);
        BigDecimal expectedBalance = BigDecimal.valueOf(150);
        
        when(walletRepository.findByOwnerId(ownerId)).thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);

        WalletDTO result = walletService.deposit(ownerId, depositAmount, "Test deposit");

        assertEquals(expectedBalance, testWallet.getBalance());
        verify(transactionRepository).save(any());
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    void deposit_WalletNotFound() {
        when(walletRepository.findByOwnerId(ownerId)).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class, 
                () -> walletService.deposit(ownerId, BigDecimal.TEN, null));
        
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void withdraw_Success() {
        BigDecimal withdrawAmount = BigDecimal.valueOf(50);
        BigDecimal expectedBalance = BigDecimal.valueOf(50);
        
        when(walletRepository.findByOwnerId(ownerId)).thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);

        WalletDTO result = walletService.withdraw(ownerId, withdrawAmount, "Test withdrawal");

        assertEquals(expectedBalance, testWallet.getBalance());
        verify(transactionRepository).save(any());
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    void withdraw_WalletNotFound() {
        when(walletRepository.findByOwnerId(ownerId)).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class, 
                () -> walletService.withdraw(ownerId, BigDecimal.TEN, null));
        
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void withdraw_InsufficientFunds() {
        BigDecimal withdrawAmount = BigDecimal.valueOf(150);
        
        when(walletRepository.findByOwnerId(ownerId)).thenReturn(Optional.of(testWallet));

        assertThrows(InsufficientFundsException.class, 
                () -> walletService.withdraw(ownerId, withdrawAmount, null));
        
        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transfer_Success() {
        String recipientId = "user456";
        BigDecimal transferAmount = BigDecimal.valueOf(50);
        
        Wallet recipientWallet = Wallet.builder()
                .id(2L)
                .ownerId(recipientId)
                .balance(BigDecimal.valueOf(20))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        when(walletRepository.findByOwnerId(ownerId)).thenReturn(Optional.of(testWallet));
        when(walletRepository.findByOwnerId(recipientId)).thenReturn(Optional.of(recipientWallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet).thenReturn(recipientWallet);

        WalletDTO result = walletService.transfer(ownerId, recipientId, transferAmount, "Test transfer");

        assertEquals(BigDecimal.valueOf(50), testWallet.getBalance());
        assertEquals(BigDecimal.valueOf(70), recipientWallet.getBalance());
        verify(transactionRepository, times(2)).save(any());
        verify(walletRepository, times(2)).save(any(Wallet.class));
    }

    @Test
    void transfer_SenderWalletNotFound() {
        String recipientId = "user456";
        
        when(walletRepository.findByOwnerId(ownerId)).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class, 
                () -> walletService.transfer(ownerId, recipientId, BigDecimal.TEN, null));
        
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transfer_RecipientWalletNotFound() {
        String recipientId = "user456";
        
        when(walletRepository.findByOwnerId(ownerId)).thenReturn(Optional.of(testWallet));
        when(walletRepository.findByOwnerId(recipientId)).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class, 
                () -> walletService.transfer(ownerId, recipientId, BigDecimal.TEN, null));
        
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transfer_InsufficientFunds() {
        String recipientId = "user456";
        BigDecimal transferAmount = BigDecimal.valueOf(150);
        
        Wallet recipientWallet = Wallet.builder()
                .id(2L)
                .ownerId(recipientId)
                .balance(BigDecimal.valueOf(20))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        when(walletRepository.findByOwnerId(ownerId)).thenReturn(Optional.of(testWallet));
        when(walletRepository.findByOwnerId(recipientId)).thenReturn(Optional.of(recipientWallet));

        assertThrows(InsufficientFundsException.class, 
                () -> walletService.transfer(ownerId, recipientId, transferAmount, null));
        
        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }
}