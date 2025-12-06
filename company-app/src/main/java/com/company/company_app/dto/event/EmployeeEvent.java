package com.company.company_app.dto.event;

import java.time.Instant;
import java.util.UUID;

public record EmployeeEvent(
    UUID eventId,
    Instant eventTime,
    UUID keycloakID,            // ID usera, ktorý akciu vykonal (napr. admin)
    String entityName,      // "EMPLOYEE"
    UUID entityId,          // ID zamestnanca
    String action,          // "CREATE", "UPDATE", "TERMINATE"
    String sourceService,   // "company-service"
    Object payload,          // Detaily (napr. JSON s menom atď.)
    String ipAddress,       // Meta dáta
    String userAgent        // Meta dáta
) {}