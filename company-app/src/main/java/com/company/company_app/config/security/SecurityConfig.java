package com.company.company_app.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Hlavná bezpečnostná konfigurácia aplikácie.
 * <p>
 * Nastavuje:
 * <ul>
 * <li>CSRF ochranu (vypnutá pre REST API).</li>
 * <li>Pravidlá autorizácie (kto má prístup kam).</li>
 * <li>OAuth2 Resource Server (validácia JWT tokenov z Keycloaku).</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Povolí @PreAuthorize na metódach
@RequiredArgsConstructor // Lombok vygeneruje konštruktor pre final polia
public class SecurityConfig {

    private final JwtAuthConverter jwtAuthConverter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Vypnutie CSRF (Pre bezstavové REST API s JWT to zvyčajne netreba)
                .csrf(AbstractHttpConfigurer::disable)

                // 2. Pravidlá autorizácie URL
                .authorizeHttpRequests(auth -> auth
                        // Whitelist: Verejne dostupné endpointy (bez prihlásenia)
                        .requestMatchers("/api/dev/jobs/**").permitAll()
                        // Swagger UI (voliteľné, ak používaš)
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // Všetky ostatné požiadavky vyžadujú platný token
                        .anyRequest().authenticated()
                )

                // 3. Konfigurácia OAuth2 Resource Servera
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                // Registrácia nášho vlastného konvertera, ktorý extrahuje Keycloak roly
                                // Dôležité: Používame injektovaný bean, nie 'new JwtAuthConverter()'
                                .jwtAuthenticationConverter(jwtAuthConverter)
                        )
                );

        return http.build();
    }
}