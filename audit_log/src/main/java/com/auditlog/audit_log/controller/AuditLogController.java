package com.auditlog.audit_log.controller;

import com.auditlog.audit_log.dto.AuditEventDto;
import com.auditlog.audit_log.dto.AuditLogFilter;
import com.auditlog.audit_log.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller pre správu Audit Logu.
 * <p>
 * Poskytuje endpoint na rpezeranie logov pre admina.
 * Dalsia mozost je pouzit kafkaUI
 */
@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    /**
     * Vyhľadá a vráti stránkovaný zoznam audit logov na základe filtra.
     * <p>
     * Podporuje filtrovanie podľa atribútov a stránkovanie výsledkov.
     *
     * <p><strong>Príklady použitia:</strong></p>
     * <ul>
     * <li>{@code GET http://localhost:8083/api/v1/audit}</li>
     * <li>{@code GET http://localhost:8083/api/v1/employees?page=1&size=10&sort=eventTime,desc}</li>
     * <li>{@code GET http://localhost:8083/api/v1/audit?page=0&size=5&sort=eventTime,desc}</li>
     * </ul>
     *
     * @param filter   Objekt obsahujúci kritériá pre filtrovanie (napr. action, fulltext search).
     * @param pageable Objekt pre stránkovanie a radenie (default: 20 položiek, radenie podľa eventTime).
     * @return {@link Page} obsahujúca zoznam nájdených audit logov.
     * Status kód: {@code 200 OK}.
     */
    @GetMapping
    @PreAuthorize("hasRole('AUDIT_LOG:READ_ALL')")
    public ResponseEntity<Page<AuditEventDto>> getEmployees(
            AuditLogFilter filter,
            @PageableDefault(size = 20, sort = "eventTime", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(auditLogService.getAllAuditLog(filter, pageable));
    }
}
