package com.auditlog.audit_log.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

/**
 * Reprezentuje nemenný záznam o audite (Audit Log).
 * <p>
 * Táto entita slúži na ukladanie histórie zmien a dôležitých udalostí v systéme.
 * Implementuje rozhranie {@link Persistable}, aby sa optimalizoval výkon pri vkladaní (INSERT)
 * nových záznamov a predišlo sa zbytočným SELECT dopytom zo strany Hibernate.
 * <p>
 * Dáta sú optimalizované pre databázu PostgreSQL (využitie JSONB typu).
 */
@Entity
@Table(name = "audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogEntity implements Persistable<UUID> {

    /**
     * Unikátny identifikátor auditného záznamu.
     * Tento ID môže prísť už vygenerovaný z Kafky (napr. eventId), preto musíme
     * Hibernate inštruovať, ako s ním zaobchádzať (viď metóda isNew).
     */
    @Id
    @Column(name = "audit_id", nullable = false, updatable = false)
    private UUID auditId;

    /**
     * Čas, kedy udalosť nastala v zdrojovom systéme (business time).
     */
    @Column(name = "event_time", nullable = false)
    private Instant eventTime;

    /**
     * ID používateľa z Keycloaku (String), ktorý akciu vykonal.
     * Používame String, pretože Keycloak ID je externý identifikátor a jeho formát nemáme pod kontrolou.
     */
    @Column(name = "keycloak_id", length = 100)
    private String keycloakId;

    /**
     * Názov entity, ktorej sa zmena týka (napr. "EMPLOYEE", "ASSET").
     */
    @Column(name = "entity_name", nullable = false, length = 100)
    private String entityName;

    /**
     * ID konkrétnej inštancie entity, ktorá bola zmenená.
     */
    @Column(name = "entity_id")
    private UUID entityId;

    /**
     * Typ vykonanej akcie (napr. CREATE, UPDATE, DELETE, LOGIN).
     */
    @Column(name = "action", nullable = false, length = 100)
    private String action;

    /**
     * Čitateľná správa alebo popis udalosti pre administrátora.
     */
    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    /**
     * Názov mikroshužby, ktorá udalosť vygenerovala (napr. "company-service").
     */
    @Column(name = "source_service", nullable = false, length = 100)
    private String sourceService;

    /**
     * ID pre sledovanie toku požiadavky naprieč mikroshužbami (Distributed Tracing).
     */
    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    /**
     * Detailné dáta udalosti (snapshot zmenenej entity) uložené ako JSONB.
     * <p>
     * Anotácia {@code @JdbcTypeCode(SqlTypes.JSON)} zabezpečuje natívne mapovanie
     * na PostgreSQL typ JSONB, čo umožňuje efektívne indexovanie a vyhľadávanie vnútri JSONu.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private String payload;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    /**
     * Technický čas vloženia záznamu do auditnej databázy.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // --- HIBERNATE OPTIMALIZÁCIA (Persistable) ---

    /**
     * Príznak pre Hibernate, či ide o novú entitu.
     * <p>
     * Defaultne true, pretože audit logy sú "append-only" (iba sa pridávajú).
     * Ignorujeme toto pole pri perzistencii (@Transient).
     */
    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    public UUID getId() {
        return auditId;
    }

    /**
     * Kľúčová metóda pre výkon.
     * <p>
     * Keďže ID nastavujeme manuálne (často prichádza z Kafky), štandardný Hibernate by
     * najprv vykonal SELECT, aby zistil, či záznam existuje (merge).
     * <p>
     * Vrátením {@code true} vynútime priame volanie INSERT bez predchádzajúceho SELECTu.
     */
    @Override
    public boolean isNew() {
        return isNew;
    }

    /**
     * Po načítaní z DB (SELECT) alebo po uložení (INSERT) už entita nie je nová.
     */
    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    /**
     * Automatické nastavenie technických polí pred uložením, ak nie sú vyplnené.
     */
    @PrePersist
    public void onPrePersist() {
        if (this.auditId == null) this.auditId = UUID.randomUUID();
        if (this.createdAt == null) this.createdAt = Instant.now();
        // Fallback ak eventTime nepríde v správe
        if (this.eventTime == null) this.eventTime = Instant.now();
    }
}