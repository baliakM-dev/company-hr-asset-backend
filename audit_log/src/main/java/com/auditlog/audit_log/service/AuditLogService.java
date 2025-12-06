package com.auditlog.audit_log.service;

import com.auditlog.audit_log.domain.AuditLogEntity;
import com.auditlog.audit_log.dto.AuditEventDto;
import com.auditlog.audit_log.dto.AuditLogFilter;
import com.auditlog.audit_log.mapper.AuditLogMapper;
import com.auditlog.audit_log.repository.AuditLogRepository;
import com.auditlog.audit_log.repository.AuditLogSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

Java
        package com.auditlog.audit_log.service;

import com.auditlog.audit_log.domain.AuditLogEntity;
import com.auditlog.audit_log.dto.AuditEventDto;
import com.auditlog.audit_log.dto.AuditLogFilter;
import com.auditlog.audit_log.mapper.AuditLogMapper;
import com.auditlog.audit_log.repository.AuditLogRepository;
import com.auditlog.audit_log.repository.AuditLogSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servisná vrstva zodpovedná za čítanie a vyhľadávanie v auditných logoch.
 * <p>
 * Táto trieda slúži ako sprostredkovateľ medzi REST Controllerom a databázovou vrstvou (Repository).
 * Jej hlavnou úlohou je aplikovať biznis logiku filtrovania a transformovať databázové entity
 * na bezpečné DTO objekty pre frontend.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;

    /**
     * Získa stránkovaný a filtrovaný zoznam auditných záznamov.
     * <p>
     * Metóda je označená, ako {@code @Transactional(readOnly = true)}, čo prináša významné výkonnostné výhody:
     * <ul>
     * <li>Hibernate nevykonáva "Dirty Checking" (nekontroluje zmeny v entitách).</li>
     * <li>JDBC driver môže optimalizovať sieťovú komunikáciu pre čítanie.</li>
     * <li>Databáza môže použiť snapshot isolation level (v závislosti od nastavenia).</li>
     * </ul>
     *
     * @param filter   Kritériá vyhľadávania (fulltext search, typ akcie, atď.).
     * @param pageable Informácie o stránkovaní (číslo strany, veľkosť, radenie).
     * @return Stránka (Page) obsahujúca DTO objekty (nie Entity!).
     */
    @Transactional(readOnly = true)
    public Page<AuditEventDto> getAllAuditLog(AuditLogFilter filter, Pageable pageable) {
        log.debug("Fetching audit logs based on filter: {}", filter);

        // 1. Vytvorenie dynamickej WHERE klauzuly pomocou JPA Specification
        // Toto nám umožňuje skladať SQL dotaz programovo na základe toho, čo používateľ vyplnil vo filtri.
        var spec = AuditLogSpecification.withFilter(filter);

        // 2. Vykonanie DB dotazu (SELECT … FROM audit_log WHERE … LIMIT … OFFSET …)
        // Repository vráti Page<AuditLogEntity>, čo je stále pripojené na DB kontext.
        Page<AuditLogEntity> page = auditLogRepository.findAll(spec, pageable);

        // 3. Projekcia / Mapovanie (Entity -> DTO)
        // Je kritické nevracať Entitu priamo na Controller, aby sme neodhalili internú štruktúru DB
        // a vyhli sa problémom s Lazy Loadingom alebo cyklickými závislosťami pri JSON serializácii.
        return page.map(auditLogMapper::toSummary);
    }
}