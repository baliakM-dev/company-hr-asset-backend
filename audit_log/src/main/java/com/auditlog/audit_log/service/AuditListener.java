package com.auditlog.audit_log.service;

import com.auditlog.audit_log.domain.AuditLogEntity;
import com.auditlog.audit_log.dto.AuditEventDto;
import com.auditlog.audit_log.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

/**
 * Kafka Consumer zodpovedný za spracovanie auditných udalostí.
 * <p>
 * Táto služba počúva na nakonfigurovanom Kafka topicu, deserializuje správy
 * a perzistuje ich do databázy, ako nemenné auditné záznamy.
 * <p>
 * Implementuje vzor <strong>Idempotent Consumer</strong> – dokáže bezpečne spracovať
 * tú istú správu viackrát bez vytvorenia duplicity v databáze.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditListener {

    private final AuditLogRepository repository;
    private final ObjectMapper objectMapper;

    /**
     * Hlavná metóda pre príjem správ z Kafky.
     *
     * @param message Raw JSON správa prijatá z broker-a.
     * @param topic   Názov topicu, z ktorého správa prišla (pre logovanie).
     */
    @KafkaListener(
            topics = "${spring.kafka.topic.audit-log:employee-events}",
            groupId = "audit-log-group-1" // Group ID umožňuje škálovanie (load balancing) medzi inštanciami
    )
    public void processAuditLog(String message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.debug("📥 Prijatá správa z [{}]: {}", topic, message);
        try {
            // 1. Deserializácia: Prevod Stringu na Java Record (DTO)
            AuditEventDto event = objectMapper.readValue(message, AuditEventDto.class);

            // 2. Mapovanie: Prevod DTO na JPA Entitu
            AuditLogEntity entity = mapToEntity(event);

            // 3. Perzistencia: Uloženie do DB
            // Vďaka implementácii Persistable<UUID> v entite sa vykoná priamo INSERT (bez SELECTu).
            repository.save(entity);

            log.info("✅ AUDIT SAVED ► Action={} Entity={} ID={}",
                    entity.getAction(), entity.getEntityName(), entity.getAuditId());

        } catch (DataIntegrityViolationException e) {
            // IDEMPOTENCIA: Kafka má garanciu "At-Least-Once", čo znamená, že správa môže prísť dvakrát.
            // Ak sa pokúsime uložiť záznam s rovnakým ID, DB vyhodí túto výnimku.
            // Toto nepovažujeme za chybu, ale za očakávaný stav -> správu zahodíme (ACK).
            log.warn("⚠️ DUPLICITY SKIP — Event ID už v databáze existuje, ignorujeme správu.");
        } catch (Exception e) {
            // RETRY MECHANIZMUS: Pri akejkoľvek inej chybe (napr. výpadok DB) vyhodíme RuntimeException.
            // To signáluje Spring Kafka kontajneru, aby spustil Retry mechanizmus (Backoff)
            // a prípadne presunul správu do DLT (Dead Letter Topic), ak sa to nepodarí ani po X pokusoch.
            log.error("❌ Neočakávaná chyba pri spracovaní správy. Spúšťam retry...", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Pomocná metóda na mapovanie DTO -> Entity.
     * Zabezpečuje tiež serializáciu dynamického 'payload' objektu do JSON stringu pre databázu.
     */
    private AuditLogEntity mapToEntity(AuditEventDto event) {
        String payloadJson = null;
        try {
            // Objekt payload (napr. detaily zamestnanca) prevedieme na JSON String,
            // aby sa dal uložiť do stĺpca typu JSONB v PostgreSQL.
            if (event.payload() != null) {
                payloadJson = objectMapper.writeValueAsString(event.payload());
            }
        } catch (Exception ex) {
            // Ak zlyhá serializácia payloadu, nechceme zahodiť celý audit log.
            // Zalogujeme chybu a uložíme event bez detailov (alebo s chybovou poznámkou).
            log.error("❌ Chyba pri zapisovaní payload JSON - ukladám bez payloadu", ex);
        }

        return AuditLogEntity.builder()
                .auditId(event.eventId())
                .eventTime(event.eventTime() != null ? event.eventTime() : Instant.now())
                // KeycloakID sa mapuje na String (opravený názov v predchádzajúcich krokoch)
                .keycloakId(event.keycloakID())
                .entityName(event.entityName())
                .entityId(event.entityId())
                .action(event.action())
                .message(event.message())
                .sourceService(event.sourceService())
                .correlationId(event.correlationId())
                .ipAddress(event.ipAddress())
                .userAgent(event.userAgent())
                .payload(payloadJson)
                .build();
    }
}