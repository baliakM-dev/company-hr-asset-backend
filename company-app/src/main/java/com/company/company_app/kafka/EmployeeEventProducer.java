package com.company.company_app.kafka;

import com.company.company_app.dto.event.EmployeeEvent;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topic.employee:employee-events}")
    private String topicName;
    private static final UUID SYSTEM_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    /**
     * Odošle event do Kafky, ale AŽ PO tom, čo DB transakcia úspešne prebehne (COMMIT).
     */
    public void sendEvent(UUID entityId, String action, Object payload) {

// 1. Získame kontext (User ID, IP, User-Agent)
        UUID currentUserId = getCurrentUserId();
        String ipAddress = getClientIp();
        String userAgent = getUserAgent();

        // Zostavíme event
        EmployeeEvent event = new EmployeeEvent(
                UUID.randomUUID(),
                Instant.now(),
                currentUserId,
                "EMPLOYEE",
                entityId,
                action,
                "company-service",
                payload,
                ipAddress,
                userAgent
        );

        // SENIOR TRIK: Registrujeme "callback“, ktorý sa spustí až po commite DB transakcie.
        // Tým zabránime situácii, že pošleme event "CREATED“, ale DB rollbackne a user reálne neexistuje.
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendToKafka(event);
                }
            });
        } else {
            // Ak nie sme v transakcii, pošleme hneď
            sendToKafka(event);
        }
    }

    private void sendToKafka(EmployeeEvent event) {
        try {
            log.info("📤 Publishing event: {} for entity: {}", event.action(), event.entityId());
            kafkaTemplate.send(topicName, event.eventId().toString(), event);
        } catch (Exception e) {
            log.error("❌ Failed to publish Kafka event", e);
            // Tu by sme teoreticky mohli uložiť event do "Outbox" tabuľky na neskoršie odoslanie
        }
    }

    /**
     * Vytiahne ID prihláseného používateľa zo Security Contextu.
     * Ak bežíme v kontexte bez usera (napr. Scheduler), vráti SYSTEM_USER_ID.
     */
    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 1. Skontrolujeme, či máme autentifikáciu a či je Principal typu JWT
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            try {
                // 2. Vytiahneme hodnotu "sub" (Subject)
                // Spring Security to automaticky mapuje z claimu "sub", ktorý vidíš na screenshote
                String subject = jwt.getSubject();

                // Alternatívne, ak by getSubject() nešlo, môžeš použiť:
                // String subject = jwt.getClaimAsString("sub");

                return UUID.fromString(subject);
            } catch (IllegalArgumentException e) {
                log.warn("JWT 'sub' claim nie je validné UUID: {}. Používam System ID.", jwt.getSubject());
            }
        }

        // Fallback pre systémové volania (Scheduler)
        return SYSTEM_USER_ID;
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                // Niekedy je IP schovaná za proxy (X-Forwarded-For)
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0];
                }
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            // Ignorujeme (napr. v scheduler threadoch)
        }
        return "unknown";
    }

    private String getUserAgent() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                return attrs.getRequest().getHeader("User-Agent");
            }
        } catch (Exception e) {
            // Ignorujeme
        }
        return "unknown";
    }
}