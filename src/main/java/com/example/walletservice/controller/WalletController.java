package com.example.walletservice.controller;

import com.example.walletservice.dto.TransactionDTO;
import com.example.walletservice.dto.TransferDTO;
import com.example.walletservice.dto.WalletDTO;
import com.example.walletservice.service.TransactionService;
import com.example.walletservice.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
@Validated
@Tag(name = "Wallet API", description = "API para gerenciamento de carteiras digitais")
public class WalletController {
    
    private final WalletService walletService;
    private final TransactionService transactionService;
    
    @PostMapping
    @Operation(summary = "Criar uma nova carteira", description = "Cria uma nova carteira para um usuário")
    public ResponseEntity<WalletDTO> createWallet(@RequestParam @NotBlank String ownerId) {
        WalletDTO wallet = walletService.createWallet(ownerId);
        return new ResponseEntity<>(wallet, HttpStatus.CREATED);
    }
    
    @GetMapping("/{ownerId}")
    @Operation(summary = "Obter saldo atual", description = "Obtém o saldo atual da carteira de um usuário")
    public ResponseEntity<WalletDTO> getWallet(@PathVariable @NotBlank String ownerId) {
        WalletDTO wallet = walletService.getWalletByOwnerId(ownerId);
        return ResponseEntity.ok(wallet);
    }
    
    @GetMapping("/{ownerId}/historical")
    @Operation(summary = "Obter saldo histórico", description = "Obtém o saldo da carteira em um momento específico do passado")
    public ResponseEntity<WalletDTO> getHistoricalBalance(
            @PathVariable @NotBlank String ownerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime timestamp) {
        WalletDTO wallet = walletService.getHistoricalWalletDTO(ownerId, timestamp);
        return ResponseEntity.ok(wallet);
    }
    
    @PostMapping("/{ownerId}/deposit")
    @Operation(summary = "Depositar fundos", description = "Deposita dinheiro na carteira de um usuário")
    public ResponseEntity<WalletDTO> deposit(
            @PathVariable @NotBlank String ownerId,
            @RequestParam @NotNull @DecimalMin("0.01") BigDecimal amount,
            @RequestParam(required = false) String description) {
        
        WalletDTO wallet = walletService.deposit(ownerId, amount, description);
        return ResponseEntity.ok(wallet);
    }
    
    @PostMapping("/{ownerId}/withdraw")
    @Operation(summary = "Sacar fundos", description = "Saca dinheiro da carteira de um usuário")
    public ResponseEntity<WalletDTO> withdraw(
            @PathVariable @NotBlank String ownerId,
            @RequestParam @NotNull @DecimalMin("0.01") BigDecimal amount,
            @RequestParam(required = false) String description) {
        
        WalletDTO wallet = walletService.withdraw(ownerId, amount, description);
        return ResponseEntity.ok(wallet);
    }
    
    @PostMapping("/transfer")
    @Operation(summary = "Transferir fundos", description = "Transfere dinheiro entre carteiras de usuários")
    public ResponseEntity<WalletDTO> transfer(@RequestBody @Valid TransferDTO transferDTO) {
        WalletDTO sourceWallet = transactionService.transfer(transferDTO);
        return ResponseEntity.ok(sourceWallet);
    }
    
    @PostMapping("/{ownerId}/transfer")
    @Operation(summary = "Transferir fundos (endpoint alternativo)", description = "Transfere dinheiro entre carteiras de usuários")
    public ResponseEntity<WalletDTO> transferFromOwner(
            @PathVariable @NotBlank String ownerId,
            @RequestBody @NotNull Map<String, Object> request) {
        
        // Validate required fields
        if (!request.containsKey("targetOwnerId") || !request.containsKey("amount")) {
            throw new IllegalArgumentException("targetOwnerId and amount are required");
        }
        
        String targetOwnerId = (String) request.get("targetOwnerId");
        if (targetOwnerId == null || targetOwnerId.trim().isEmpty()) {
            throw new IllegalArgumentException("targetOwnerId cannot be null or empty");
        }
        
        BigDecimal amount;
        try {
            amount = new BigDecimal(request.get("amount").toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid amount format");
        }
        
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        
        String description = (String) request.get("description");
        
        TransferDTO transferDTO = TransferDTO.builder()
                .sourceOwnerId(ownerId)
                .targetOwnerId(targetOwnerId)
                .amount(amount)
                .description(description)
                .build();
        
        WalletDTO sourceWallet = transactionService.transfer(transferDTO);
        return ResponseEntity.ok(sourceWallet);
    }
    
    @GetMapping("/{ownerId}/transactions")
    @Operation(summary = "Listar transações", description = "Lista todas as transações de uma carteira")
    public ResponseEntity<List<TransactionDTO>> getTransactions(@PathVariable @NotBlank String ownerId) {
        List<TransactionDTO> transactions = transactionService.getTransactionsByOwnerId(ownerId);
        return ResponseEntity.ok(transactions);
    }
}