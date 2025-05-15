package com.example.walletservice.service;

import com.example.walletservice.dto.TransactionDTO;
import com.example.walletservice.dto.TransferDTO;
import com.example.walletservice.dto.WalletDTO;
import com.example.walletservice.entity.Transaction;
import com.example.walletservice.entity.Wallet;
import com.example.walletservice.repository.TransactionRepository;
import com.example.walletservice.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {
    
    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final WalletService walletService;
    
    /**
     * Obtém todas as transações de uma carteira
     * @param ownerId ID do proprietário da carteira
     * @return Lista de DTOs de transações
     */
    public List<TransactionDTO> getTransactionsByOwnerId(String ownerId) {
        Wallet wallet = walletRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new RuntimeException("Carteira não encontrada para o usuário: " + ownerId));
                
        List<Transaction> transactions = transactionRepository.findByWalletIdOrderByTimestampDesc(wallet.getId());
        
        return transactions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Transfere dinheiro entre carteiras
     * @param transferDTO DTO com informações da transferência
     * @return DTO da carteira de origem atualizada
     */
    @Transactional
    public WalletDTO transfer(TransferDTO transferDTO) {
        return walletService.transfer(
                transferDTO.getSourceOwnerId(),
                transferDTO.getTargetOwnerId(),
                transferDTO.getAmount(),
                transferDTO.getDescription()
        );
    }
    
    /**
     * Converte uma entidade Transaction para TransactionDTO
     * @param transaction Entidade Transaction
     * @return DTO da transação
     */
    private TransactionDTO convertToDTO(Transaction transaction) {
        return TransactionDTO.builder()
                .id(transaction.getId())
                .walletId(transaction.getWallet().getId())
                .amount(transaction.getAmount())
                .type(transaction.getType())
                .description(transaction.getDescription())
                .timestamp(transaction.getTimestamp())
                .balanceAfterTransaction(transaction.getBalanceAfterTransaction())
                .relatedWalletId(transaction.getRelatedWalletId())
                .build();
    }
}