package com.example.walletservice.health;

import com.example.walletservice.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WalletServiceHealthIndicator implements HealthIndicator {

    private final WalletRepository walletRepository;

    @Override
    public Health health() {
        try {
            long walletCount = walletRepository.count();
            return Health.up()
                    .withDetail("walletCount", walletCount)
                    .withDetail("status", "Wallet service is running correctly")
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}