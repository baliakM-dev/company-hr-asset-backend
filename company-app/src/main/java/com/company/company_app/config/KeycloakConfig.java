package com.company.company_app.config;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Konfigurácia pre Keycloak Admin Client.
 * <p>
 * Vytvára inštanciu {@link Keycloak}, ktorá slúži na komunikáciu s REST API Keycloaku
 * (vytváranie používateľov, správa rolí, atď.).
 */
@Configuration
public class KeycloakConfig {
    @Value("${keycloak.server-url}")
    private String serverUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    /**
     * Vytvorí Bean pre Keycloak klienta.
     * Používa flow {@code client_credentials}, čo znamená, že aplikácia sa autentifikuje
     * ako servisný účet (Service Account) s admin právami, nie ako konkrétny používateľ.
     */
    @Bean
    public Keycloak keycloak() {
        return KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(realm)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .build();
    }
}
