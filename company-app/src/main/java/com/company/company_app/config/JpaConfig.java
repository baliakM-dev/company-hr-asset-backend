package com.company.company_app.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider") // ✅ Aktivácia auditu
public class JpaConfig {

    @Bean
    public AuditorAware<UUID> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated() || Objects.equals(authentication.getPrincipal(), "anonymousUser")) {
                return Optional.empty();
            }

            try {
                // Predpokladáme, že Principal (name) je UUID (z Keycloaku/JWT)
                return Optional.of(UUID.fromString(authentication.getName()));
            } catch (IllegalArgumentException e) {
                // Fallback alebo logovanie, ak user ID nie je UUID
                return Optional.empty();
            }
        };
    }
}