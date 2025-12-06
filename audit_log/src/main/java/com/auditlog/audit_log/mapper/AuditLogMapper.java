package com.auditlog.audit_log.mapper;

import com.auditlog.audit_log.domain.AuditLogEntity;
import com.auditlog.audit_log.dto.AuditEventDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Komponent zodpovedný za transformáciu dát medzi perzistentnou vrstvou (Entity) a prezentačnou vrstvou (DTO).
 * <p>
 * Používame knižnicu <strong>MapStruct</strong>, ktorá generuje implementáciu tohto rozhrania
 * počas kompilácie (compile-time). To prináša dve hlavné výhody:
 * <ol>
 * <li><strong>Vysoký výkon:</strong> Žiadna reflexia za behu (ako pri ModelMapper), kód je rovnako rýchly ako ručne písané gettery/settery.</li>
 * <li><strong>Typová bezpečnosť:</strong> Ak zmeníme názov poľa v entite, kompilácia zlyhá a upozorní nás na chybu.</li>
 * </ol>
 * <p>
 * Anotácia {@code componentModel = "spring"} zabezpečí, že vygenerovaná implementácia
 * bude Spring Bean a môžeme ju injektovať pomocou @Autowired / konštruktora.
 */
@Mapper(componentModel = "spring")
public interface AuditLogMapper {

    /**
     * Konvertuje databázovú entitu na DTO (Data Transfer Object) pre potreby API odpovede.
     * <p>
     * Slúži na odhalenie len tých dát, ktoré chceme poslať klientovi (tzv. projekcia),
     * a rieši rozdiely v pomenovaní atribútov medzi databázou a externým svetom.
     *
     * @param entity Načítaný záznam z databázy.
     * @return Immutable DTO objekt pripravený na serializáciu do JSONu.
     */
    // Riešenie nekonzistencie v názvoch: V DB (JPA) používame camelCase 'keycloakId',
    // ale DTO (z Kafky/API) očakáva 'keycloakID' (s veľkým ID na konci).
    @Mapping(target = "keycloakID", source = "keycloakId")

    // Riešenie rozdielneho primárneho kľúča: V DB sa to volá 'auditId',
    // ale navonok to komunikujeme ako 'eventId' (business identifier).
    @Mapping(target = "eventId", source = "auditId")
    AuditEventDto toSummary(AuditLogEntity entity);
}