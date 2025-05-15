package com.example.walletservice.service;

import com.example.walletservice.dto.WalletDTO;
import com.example.walletservice.entity.Transaction;
import com.example.walletservice.entity.TransactionType;
import com.example.walletservice.entity.Wallet;
import com.example.walletservice.exception.InsufficientFundsException;
import com.example.walletservice.exception.WalletNotFoundException;
import com.example.walletservice.repository.TransactionRepository;
import com.example.walletservice.repository.WalletRepository;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final MeterRegistry meterRegistry;

    /**
     * Cria uma nova carteira para um usuário
     * @param ownerId ID do proprietário da carteira
     * @return DTO da carteira criada
     */
    @Transactional
    @Timed(value = "wallet.create", description = "Time taken to create a wallet")
    public WalletDTO createWallet(String ownerId) {
        if (walletRepository.existsByOwnerId(ownerId)) {
            throw new IllegalStateException("Usuário já possui uma carteira");
        }

        Wallet wallet = Wallet.builder()
                .ownerId(ownerId)
                .balance(BigDecimal.ZERO)
                .build();

        wallet = walletRepository.save(wallet);
        meterRegistry.counter("wallet.created").increment();
        return convertToDTO(wallet);
    }

    /**
     * Obtém a carteira de um usuário pelo ID do proprietário
     * @param ownerId ID do proprietário da carteira
     * @return DTO da carteira
     */
    public WalletDTO getWalletByOwnerId(String ownerId) {
        Wallet wallet = findWalletByOwnerId(ownerId);
        return convertToDTO(wallet);
    }

    /**
     * Obtém o saldo atual da carteira de um usuário
     * @param ownerId ID do proprietário da carteira
     * @return Saldo atual
     */
    public BigDecimal getWalletBalance(String ownerId) {
        Wallet wallet = findWalletByOwnerId(ownerId);
        return wallet.getBalance();
    }

    /**
     * Obtém o saldo histórico da carteira em um momento específico
     * @param ownerId ID do proprietário da carteira
     * @param timestamp Momento específico para consulta
     * @return Saldo histórico
     */
    public BigDecimal getHistoricalBalance(String ownerId, LocalDateTime timestamp) {
        Wallet wallet = findWalletByOwnerId(ownerId);
        return transactionRepository.calculateBalanceAtTime(wallet.getId(), timestamp);
    }

    /**
     * Obtém o DTO da carteira com saldo histórico em um momento específico
     * @param ownerId ID do proprietário da carteira
     * @param timestamp Momento específico para consulta
     * @return DTO da carteira com saldo histórico
     */
    public WalletDTO getHistoricalWalletDTO(String ownerId, LocalDateTime timestamp) {
        Wallet wallet = findWalletByOwnerId(ownerId);
        BigDecimal historicalBalance = transactionRepository.calculateBalanceAtTime(wallet.getId(), timestamp);
        
        Wallet historicalWallet = Wallet.builder()
                .id(wallet.getId())
                .ownerId(wallet.getOwnerId())
                .balance(historicalBalance)
                .createdAt(wallet.getCreatedAt())
                .updatedAt(timestamp)
                .build();
                
        return convertToDTO(historicalWallet);
    }

    /**
     * Deposita dinheiro na carteira de um usuário
     * @param ownerId ID do proprietário da carteira
     * @param amount Valor a ser depositado
     * @param description Descrição opcional da transação
     * @return DTO da carteira atualizada
     */
    @Transactional
    @Timed(value = "wallet.deposit", description = "Time taken to deposit money")
    public WalletDTO deposit(String ownerId, BigDecimal amount, String description) {
        Wallet wallet = findWalletByOwnerId(ownerId);
        wallet.deposit(amount);
        
        Transaction transaction = Transaction.builder()
                .wallet(wallet)
                .amount(amount)
                .type(TransactionType.DEPOSIT)
                .description(description)
                .timestamp(LocalDateTime.now())
                .balanceAfterTransaction(wallet.getBalance())
                .build();
                
        transactionRepository.save(transaction);
        walletRepository.save(wallet);
        
        return convertToDTO(wallet);
    }

    /**
     * Método de depósito sem descrição para compatibilidade com testes
     */
    @Transactional
    public BigDecimal deposit(String ownerId, BigDecimal amount) {
        WalletDTO walletDTO = deposit(ownerId, amount, null);
        return walletDTO.getBalance();
    }

    /**
     * Saca dinheiro da carteira de um usuário
     * @param ownerId ID do proprietário da carteira
     * @param amount Valor a ser sacado
     * @param description Descrição opcional da transação
     * @return DTO da carteira atualizada
     */
    @Transactional
    @Timed(value = "wallet.withdraw", description = "Time taken to withdraw money")
    public WalletDTO withdraw(String ownerId, BigDecimal amount, String description) {
        Wallet wallet = findWalletByOwnerId(ownerId);
        
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Saldo insuficiente para realizar o saque");
        }
        
        wallet.withdraw(amount);
        
        Transaction transaction = Transaction.builder()
                .wallet(wallet)
                .amount(amount.negate())
                .type(TransactionType.WITHDRAWAL)
                .description(description)
                .timestamp(LocalDateTime.now())
                .balanceAfterTransaction(wallet.getBalance())
                .build();
                
        transactionRepository.save(transaction);
        walletRepository.save(wallet);
        
        return convertToDTO(wallet);
    }

    /**
     * Método de saque sem descrição para compatibilidade com testes
     */
    @Transactional
    public BigDecimal withdraw(String ownerId, BigDecimal amount) {
        WalletDTO walletDTO = withdraw(ownerId, amount, null);
        return walletDTO.getBalance();
    }

    /**
     * Transfere dinheiro entre carteiras de usuários
     * @param sourceOwnerId ID do proprietário da carteira de origem
     * @param targetOwnerId ID do proprietário da carteira de destino
     * @param amount Valor a ser transferido
     * @param description Descrição opcional da transação
     * @return DTO da carteira de origem atualizada
     */
    @Transactional
    @Timed(value = "wallet.transfer", description = "Time taken to transfer money")
    public WalletDTO transfer(String sourceOwnerId, String targetOwnerId, BigDecimal amount, String description) {
        Wallet sourceWallet = findWalletByOwnerId(sourceOwnerId);
        Wallet targetWallet = findWalletByOwnerId(targetOwnerId);
        
        if (sourceWallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Saldo insuficiente para realizar a transferência");
        }
        
        sourceWallet.withdraw(amount);
        targetWallet.deposit(amount);
        
        Transaction sourceTransaction = Transaction.builder()
                .wallet(sourceWallet)
                .amount(amount.negate())
                .type(TransactionType.TRANSFER_OUT)
                .description(description != null ? description : "Transferência para " + targetOwnerId)
                .timestamp(LocalDateTime.now())
                .balanceAfterTransaction(sourceWallet.getBalance())
                .relatedWalletId(targetWallet.getId())
                .build();
                
        Transaction targetTransaction = Transaction.builder()
                .wallet(targetWallet)
                .amount(amount)
                .type(TransactionType.TRANSFER_IN)
                .description(description != null ? description : "Transferência de " + sourceOwnerId)
                .timestamp(LocalDateTime.now())
                .balanceAfterTransaction(targetWallet.getBalance())
                .relatedWalletId(sourceWallet.getId())
                .build();
                
        transactionRepository.save(sourceTransaction);
        transactionRepository.save(targetTransaction);
        walletRepository.save(sourceWallet);
        walletRepository.save(targetWallet);
        
        return convertToDTO(sourceWallet);
    }

    /**
     * Método de transferência sem descrição para compatibilidade com testes
     */
    @Transactional
    public void transfer(String sourceOwnerId, String targetOwnerId, BigDecimal amount) {
        transfer(sourceOwnerId, targetOwnerId, amount, null);
    }

    /**
     * Encontra uma carteira pelo ID do proprietário
     * @param ownerId ID do proprietário da carteira
     * @return Carteira encontrada
     * @throws WalletNotFoundException se a carteira não for encontrada
     */
    private Wallet findWalletByOwnerId(String ownerId) {
        return walletRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new WalletNotFoundException("Carteira não encontrada para o usuário: " + ownerId));
    }

    /**
     * Converte uma entidade Wallet para WalletDTO
     * @param wallet Entidade Wallet
     * @return DTO da carteira
     */
    private WalletDTO convertToDTO(Wallet wallet) {
        return WalletDTO.builder()
                .id(wallet.getId())
                .ownerId(wallet.getOwnerId())
                .balance(wallet.getBalance())
                .createdAt(wallet.getCreatedAt())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }
}