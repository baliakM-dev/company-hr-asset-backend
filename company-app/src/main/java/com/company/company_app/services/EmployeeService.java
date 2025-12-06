package com.company.company_app.services;

import com.company.company_app.domain.Employee;
import com.company.company_app.dto.employee.*;
import com.company.company_app.exceptions.UserAlreadyExistsException;
import com.company.company_app.exceptions.UserNotFoundException;
import com.company.company_app.mapper.EmployeeMapper;
import com.company.company_app.repository.EmployeeRepository;
import com.company.company_app.repository.EmployeeSpecifications;
import com.company.company_app.services.keycloak.KeycloakUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Servisná vrstva zodpovedná za biznis logiku a orchestráciu operácií nad entitou {@link Employee}.
 * <p>
 * Táto trieda sprostredkúva komunikáciu medzi Controllerom, Repository a externými systémami (Keycloak).
 * Zabezpečuje transakčnosť operácií a integritu dát.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper employeeMapper;
    private final KeycloakUserService keycloakUserService;

    /**
     * Vytvorí a perzistuje nového zamestnanca.
     * <p>
     * Proces zahŕňa:
     * <ol>
     * <li>Validáciu unikátnosti emailu a Keycloak mena (Fail-Fast).</li>
     * <li>Mapovanie DTO na doménovú entitu.</li>
     * <li>Generovanie interných identifikátorov.</li>
     * <li>Perzistenciu do databázy.</li>
     * </ol>
     * Metóda je transakčná. V prípade zlyhania uloženia do DB nastane rollback.
     *
     * @param request DTO s údajmi pre vytvorenie zamestnanca.
     * @return {@link EmployeeResponse} reprezentácia vytvoreného zamestnanca.
     * @throws UserAlreadyExistsException ak zamestnanec s daným emailom alebo keycloakName už existuje.
     * @throws RuntimeException ak nastane chyba pri ukladaní (spustí rollback).
     */
    @Transactional // DB transakcia začína tu
    public EmployeeResponse createEmployee(CreateEmployeeRequest request) {
        log.info("Processing creation request for employee email={}, keycloakName={}", request.email(), request.keycloakName());

        // 1. Fail-Fast Validácia (ušetríme volanie na Keycloak)
        if (employeeRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException(
                    "User with email '" + request.email() + "' already exists.");
        }

        if (employeeRepository.existsByKeycloakName(request.keycloakName())) {
            throw new UserAlreadyExistsException(
                    "Keycloak name '" + request.keycloakName() + "' already exists.");
        }

        String keycloakId = null;
        try {
            // 2. Najprv vytvorime usera v keycloaku
            keycloakId = keycloakUserService.createUser(request);

            // 3. Mapovanie (DTO -> Entity)
            Employee employee = employeeMapper.toEntity(request);

            // Generovanie identifikátorov (Simulácia ID z externého systému)
            employee.setId(UUID.randomUUID());
            employee.setKeycloakID(keycloakId);

            // 3. Spracovanie adries (ak existujú)
            if (request.addresses() != null) {
                request.addresses().forEach(addrDto -> {
                    var address = employeeMapper.toAddressEntity(addrDto);
                    address.setId(UUID.randomUUID()); // Application-Assigned ID
                    employee.addAddress(address);
                });
            }

            // 4. Uloženie (Hibernate Cascade uloží aj adresy)
            Employee saved = employeeRepository.save(employee);
            log.info("Employee created successfully with ID={} and keycloakId={}", saved.getId(), saved.getKeycloakID());

            return employeeMapper.toResponse(saved);
        } catch (RuntimeException ex) {
            // 🛑 KOMPENZÁCIA: Ak DB padne, musíme upratať Keycloak
            log.error("Database save failed. Rolling back Keycloak user: {}", ex.getMessage());
            if (keycloakId != null) keycloakUserService.deleteUser(keycloakId);

            throw ex; // Prehodíme chybu ďalej, aby Spring spravil DB Rollback
        }
    }

    /**
     * Ukončí pracovný pomer zamestnanca na základe zadanej požiadavky.
     * <p>
     * Metóda načíta entitu, aplikuje doménovú logiku ukončenia (zmena statusu, nastavenie dátumu)
     * a uloží zmeny.
     *
     * @param employeeId Unikátny identifikátor zamestnanca (UUID).
     * @param request DTO obsahujúce dátum a dôvod ukončenia.
     * @throws UserNotFoundException ak zamestnanec so zadaným ID neexistuje.
     */
    @Transactional
    public void terminateEmployee(UUID employeeId, TerminateEmployeeRequest request) {
        // 1. Načítanie Entity s kontrolou existencie
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new UserNotFoundException("Employee not found"));

        // 2. Vykonanie Biznis Logiky (Volanie metódy na entite - Rich Domain Model)
        employee.terminate(request.endWork(), request.reason());

        // 3. Uloženie zmien
        // Poznámka: Vďaka @Transactional by Hibernate vykonal update aj bez explicitného save(),
        // ale pre čitateľnosť je vhodné ho ponechať.
        employeeRepository.save(employee);
    }

    /**
     * Vráti stránkovaný zoznam zamestnancov filtrovaný podľa zadaných kritérií.
     * <p>
     * Používa {@code readOnly} transakciu pre optimalizáciu výkonu (Hibernate nemusí sledovať
     * zmeny v entitách-dirty checking).
     *
     * @param filter Kritériá pre filtrovanie (status, fulltext search).
     * @param pageable Informácie o stránkovaní a radení.
     * @return {@link Page} obsahujúca {@link EmployeeResponse} objekty.
     * @see EmployeeSpecifications#withFilter(EmployeeFilter)
     */
    @Transactional(readOnly = true)
    public Page<EmployeeResponse> getAllEmployees(EmployeeFilter filter, Pageable pageable) {
        // 1. Vytvoríme Specification (WHERE klauzula)
        var spec = EmployeeSpecifications.withFilter(filter);

        // 2. Načítanie stránky entít z DB
        Page<Employee> page = employeeRepository.findAll(spec, pageable);

        // 3. Mapovanie na DTO
        return page.map(employeeMapper::toSummary);
    }

    /**
     * Vyhľadá detail zamestnanca podľa unikátneho identifikátora.
     * <p>
     * Používa {@code readOnly} transakciu, keďže ide len o čítanie dát.
     *
     * @param id Unikátny identifikátor zamestnanca (UUID).
     * @return {@link EmployeeResponse} s údajmi o zamestnancovi.
     * @throws UserNotFoundException ak zamestnanec so zadaným ID neexistuje.
     */
    @Transactional(readOnly = true)
    public EmployeeResponse getEmployee(UUID id) {
        return employeeRepository.findById(id)
                .map(employeeMapper::toResponse)
                .orElseThrow(() -> new UserNotFoundException("Employee not found with ID: " + id));
    }

    /**
     * Úprava existujúceho zamestnanca.
     *
     * * <p><strong>URL:</strong> {@code PUT /api/v1/employees/{id}}</p>
     *
     * @param id Unikátny identifikátor zamestnanca (UUID).
     * Status kód: {@code 200 OK}.
     * @return {@link ResponseEntity} obsahujúce detail zamestnanca.
     */
    @Transactional
    public EmployeeResponse updateEmployee(UUID id, EmployeeUpdateRequest request) {
        log.info("Processing update request for employee with ID={}", id);
        // 1. Najdem uzivatela v internej databaze podla id
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Employee not found with ID: " + id));

        // 2. Vytvorenie zálohy (Snapshot) Keycloak stavu PRED zmenou
        UserRepresentation originalKeycloakUser = keycloakUserService.getUser(employee.getKeycloakID());

        try {
            // 3. Update v Keycloaku (Externý systém)
            keycloakUserService.updateUser(employee.getKeycloakID(), request);

            // 4. Update v internej DB
            employeeMapper.updateEntityFromDto(request, employee);

            // Explicitný save (hoci @Transactional by to flushol na konci, save je bezpečnejší pre vyvolanie DB chýb hneď)
            Employee saved = employeeRepository.save(employee);

            log.info("Employee updated successfully (DB + Keycloak).");
            return employeeMapper.toResponse(saved);

        } catch (Exception ex) {
            // 🛑 KOMPENZÁCIA (ROLLBACK)
            log.error("Database update failed after Keycloak update. Initiating Keycloak rollback. Error: {}", ex.getMessage());

            // Vrátime Keycloak do pôvodného stavu zo zálohy
            keycloakUserService.revertUser(employee.getKeycloakID(), originalKeycloakUser);

            throw ex; // Prehodíme chybu ďalej, aby Spring spravil DB Rollback
        }
    }
}
