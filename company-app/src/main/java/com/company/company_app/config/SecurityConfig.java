package com.company.company_app.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Povolí @PreAuthorize na metódach
@RequiredArgsConstructor // Lombok vygeneruje konštruktor pre final polia
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Vypnutie CSRF (Pre REST API s JWT to zvyčajne netreba)
                // Moderný zápis pomocou Method Reference
                .csrf(AbstractHttpConfigurer::disable)

                // 2. Pravidlá autorizácie
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints (Whitelist)
                        .requestMatchers("/api/v1/employees/**").permitAll()
                        .requestMatchers("/api/dev/jobs/**").permitAll()

                        // Všetko ostatné vyžaduje prihlásenie
                        .anyRequest().authenticated()
                )
                    .oauth2ResourceServer(oauth2 -> oauth2
                                    .jwt(Customizer.withDefaults())
                            );
                // 3. OAuth2 Resource Server (Validácia JWT)
//                .oauth2ResourceServer(oauth2 -> oauth2
//                        .jwt(jwt -> jwt
//                                // Registrácia vlastného konvertera pre Keycloak roly
//                                .jwtAuthenticationConverter(new JwtAuthConverter())
//                        )
//                        // Exception handling pre neplatné tokeny
//                        .authenticationEntryPoint(unauthorizedHandler)
//                        .accessDeniedHandler(accessDeniedHandler)
//                );

        return http.build();
    }
}
