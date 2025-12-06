package com.auditlog.audit_log.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable DTO (Data Transfer Object) pre prenos dát z Kafky.
 * Používame Java Record (od Java 14+), čo je najefektívnejší spôsob pre nosiče dát.
 */
@JsonIgnoreProperties(ignoreUnknown = true) // KĽÚČOVÉ: Ak producent pridá nové pole, my nespadneme.
public record AuditEventDto(
        UUID eventId,           // Unikátne ID správy (zabezpečí idempotenciu)
        Instant eventTime,      // Kedy sa to stalo (nie kedy sme to prijali)

        String keycloakID,      // Kto to spravil (Keycloak ID)

        String entityName,      // Čoho sa to týka (napr. "EMPLOYEE")
        UUID entityId,          // ID zmenenej entity

        String action,          // Čo sa stalo (napr. "CREATE", "UPDATE")
        String message,         // Ľudsky čitateľný popis

        String sourceService,   // Kto správu poslal (napr. "employee-service")
        String correlationId,   // ID requestu pre tracing (prepojenie logov naprieč systémami)

        Object payload,         // 💡 TRIK: Prijmeme akýkoľvek JSON objekt (Mapu), v servise ho hodíme do Stringu

        String ipAddress,       // Meta dáta
        String userAgent        // Meta dáta
) {}