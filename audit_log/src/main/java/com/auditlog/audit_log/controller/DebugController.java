package com.auditlog.audit_log.controller;

import com.auditlog.audit_log.dto.AuditEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
public class DebugController {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    // Načítame názov topicu z configu (aby sme nepísali hardcode stringy)
    @Value("${spring.kafka.topic.audit-log:employee-events}")
    private String topicName;

    @PostMapping("/send")
    public ResponseEntity<String> sendManualLog(@RequestBody AuditEventDto request) {
        try {
            // 1. Doplníme dáta, aby sa nám to ľahšie testovalo (ak chýbajú v requeste)
            AuditEventDto eventToSend = new AuditEventDto(
                    request.eventId() != null ? request.eventId() : UUID.randomUUID(),
                    request.eventTime() != null ? request.eventTime() : Instant.now(),
                    request.keycloakID() != null ? request.keycloakID() : "test-user-kc",
                    request.entityName() != null ? request.entityName() : "TEST_ENTITY",
                    request.entityId() != null ? request.entityId() : UUID.randomUUID(),
                    request.action() != null ? request.action() : "MANUAL_TEST",
                    request.message() != null ? request.message() : "Manuálne poslaný log cez Controller",
                    "audit-service-debug-controller", // Source service
                    request.correlationId() != null ? request.correlationId() : UUID.randomUUID().toString(),
                    request.payload(), // Payload necháme tak, ako prišiel
                    "127.0.0.1",
                    "Postman/Curl"
            );

            // 2. Serializácia na JSON String (simulujeme, ako to robia iné microservices)
            String jsonMessage = objectMapper.writeValueAsString(eventToSend);

            // 3. Odoslanie do Kafky
            // key: eventId (aby správy s rovnakým ID išli do rovnakej partície, ak by sme mali cluster)
            kafkaTemplate.send(topicName, eventToSend.eventId().toString(), jsonMessage);

            log.info("🚀 DEBUG: Odoslaná správa do topicu '{}': {}", topicName, eventToSend.eventId());

            return ResponseEntity.ok("✅ Správa odoslaná. ID: " + eventToSend.eventId());

        } catch (Exception e) {
            log.error("❌ Chyba pri odosielaní: ", e);
            return ResponseEntity.internalServerError().body("Chyba: " + e.getMessage());
        }
    }
}