package com.company.company_app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Definuje Bean {@link AuditorAware}, ktorý slúži na získanie identifikátora aktuálneho používateľa (audítora).
 * <p>
 * Táto implementácia prepája JPA so Spring Security kontextom.
 * Logika získania ID:
 * <ol>
 * <li>Skontroluje, či existuje {@link Authentication} a či je používateľ autentifikovaný.</li>
 * <li>Ignoruje anonymných používateľov ({@code anonymousUser}).</li>
 * <li>Pokúsi sa získať meno principála ({@code authentication.getName()}) a konvertovať ho na {@link UUID}.</li>
 * </ol>
 *
 * @return {@link Optional} obsahujúci UUID aktuálneho používateľa, alebo {@code Optional.empty()}
 * ak používateľ nie je prihlásený alebo ID nie je v platnom UUID formáte.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider") // ✅ Aktivácia auditu
public class JpaConfig {

    @Bean
    public AuditorAware<UUID> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            // 1. Validácia kontextu a autentifikácie
            if (authentication == null ||
                    !authentication.isAuthenticated() ||
                    Objects.equals(authentication.getPrincipal(), "anonymousUser")) {
                return Optional.empty();
            }

            try {
                // 2. Extrakcia UUID (Predpoklad: Keycloak/JWT posiela UUID ako principal name)
                return Optional.of(UUID.fromString(authentication.getName()));
            } catch (IllegalArgumentException e) {
                // Fallback: Ak principal nie je UUID (napr. pri systémových procesoch), vráti empty
                // Tu by sa hodilo logovanie warningu v produkcii
                return Optional.empty();
            }
        };
    }
}