package com.company.company_app.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Custom konvertor, ktorý mapuje Keycloak JWT token na Spring Security Authentication token.
 * <p>
 * Jeho hlavnou zodpovednosťou je extrahovať Keycloak-špecifické roly (Realm a Resource roly)
 * z claimov tokenu a "sploštiť" ich do kolekcie {@link GrantedAuthority}.
 * <p>
 * Toto umožňuje v aplikácii používať štandardné anotácie ako {@code @PreAuthorize("hasRole('ADMIN')")}.
 */
@Component
public class JwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String ROLE_PREFIX = "ROLE_";
    private static final String CLAIM_REALM_ACCESS = "realm_access";
    private static final String CLAIM_RESOURCE_ACCESS = "resource_access";
    private static final String CLAIM_ROLES = "roles";
    private static final String PRINCIPAL_ATTRIBUTE = "preferred_username";

    private final JwtGrantedAuthoritiesConverter defaultGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
    private final String resourceId;

    /**
     * @param resourceId ID klienta v Keycloaku (napr. "company-app").
     * Hodnota je injektovaná z properties, s fallbackom na "company-app".
     */
    public JwtAuthConverter(@Value("${keycloak.resource:company-app}") String resourceId) {
        this.resourceId = resourceId;
    }

    /**
     * Konvertuje zdrojový {@link Jwt} na {@link AbstractAuthenticationToken}.
     *
     * @param jwt Validovaný JSON Web Token.
     * @return Autentifikačný token obsahujúci principála a všetky kombinované autority (roly + scopes).
     */
    @Override
    public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
        // Spojíme všetky zdroje autorít do jedného streamu pre čistejší a funkcionálny prístup
        Set<GrantedAuthority> authorities = Stream.of(
                        defaultGrantedAuthoritiesConverter.convert(jwt),
                        extractRealmRoles(jwt),
                        extractResourceRoles(jwt)
                )
                .filter(Objects::nonNull) // Ignorujeme null kolekcie
                .flatMap(Collection::stream) // Sploštenie streamov
                .collect(Collectors.toSet());

        return new JwtAuthenticationToken(
                jwt,
                authorities,
                getPrincipalClaimName(jwt)
        );
    }

    /**
     * Určuje meno principála.
     * Preferuje 'preferred_username' (napr. 'janko.hrasko') pre lepšiu čitateľnosť,
     * inak použije štandardný 'sub' (UUID).
     */
    private String getPrincipalClaimName(Jwt jwt) {
        String claimName = jwt.getClaimAsString(PRINCIPAL_ATTRIBUTE);
        return (claimName != null) ? claimName : jwt.getSubject();
    }

    /**
     * Parsuje 'realm_access' claim pre extrakciu globálnych rolí.
     * <p>
     * Štruktúra JSON:
     * {@code "realm_access": { "roles": ["admin", "user"] }}
     */
    @SuppressWarnings("unchecked")
    private Collection<? extends GrantedAuthority> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim(CLAIM_REALM_ACCESS);

        if (realmAccess == null || realmAccess.isEmpty()) {
            return Collections.emptySet();
        }

        Collection<String> roles = (Collection<String>) realmAccess.get(CLAIM_ROLES);
        if (roles == null) {
            return Collections.emptySet();
        }

        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role.toUpperCase()))
                .collect(Collectors.toSet());
    }

    /**
     * Parsuje 'resource_access' claim pre extrakciu rolí špecifických pre tento klient (aplikáciu).
     * <p>
     * Štruktúra JSON:
     * {@code "resource_access": { "company-app": { "roles": ["editor"] } }}
     */
    @SuppressWarnings("unchecked")
    private Collection<? extends GrantedAuthority> extractResourceRoles(Jwt jwt) {
        Map<String, Object> resourceAccess = jwt.getClaim(CLAIM_RESOURCE_ACCESS);

        if (resourceAccess == null || resourceAccess.isEmpty()) {
            return Collections.emptySet();
        }

        // Vyhľadáme konfiguráciu pre konkrétneho klienta (resourceId)
        Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess.get(resourceId);
        if (clientAccess == null || clientAccess.isEmpty()) {
            return Collections.emptySet();
        }

        Collection<String> roles = (Collection<String>) clientAccess.get(CLAIM_ROLES);
        if (roles == null) {
            return Collections.emptySet();
        }

        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role.toUpperCase()))
                .collect(Collectors.toSet());
    }
}