package com.company.company_app.services;

import com.company.company_app.domain.Employee;
import com.company.company_app.dto.employee.CreateEmployeeRequest;
import com.company.company_app.exceptions.UserAlreadyExistsException;
import com.company.company_app.mapper.EmployeeMapper;
import com.company.company_app.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.DuplicateResourceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper employeeMapper;

    /**
     * Vytvorí zamestnanca.
     * 1. Validuje unikátnosť v lokálnej DB.
     * 2. Vytvorí konto v Keycloaku.
     * 3. Uloží dáta do DB.
     * 4. (Rollback): Ak DB zlyhá, zmaže konto v Keycloaku.
     */
    @Transactional // DB transakcia začína tu
    public UUID createEmployee(CreateEmployeeRequest request) {
        log.info("Processing creation request for employee: {}", request.keycloakName());

        // 1. Fail-Fast Validácia (ušetríme volanie na Keycloak)
        if (employeeRepository.existsByEmail(request.email()))
            throw new UserAlreadyExistsException("User with email '" + request.email() + "' already exists.");

        if (employeeRepository.existsByKeycloakName(request.keycloakName()))
            throw new UserAlreadyExistsException("Keycloak Name " + request.keycloakName() + " already exists");


        try {
            // 3. Mapovanie (DTO -> Entity)
            // MapStruct vytvorí inštanciu
            Employee employee = employeeMapper.toEntity(request);

            // Doplníme ID z externého systému a vygenerujeme naše ID
            employee.setId(UUID.randomUUID());
            employee.setKeycloakID(UUID.randomUUID().toString());

            // Adresy riešime cez helper metódu (ak nie sú null)
            if (request.addresses() != null) {
                request.addresses().forEach(addrDto -> {
                    var address = employeeMapper.toAddressEntity(addrDto);
                    address.setId(UUID.randomUUID()); // Application-Assigned ID
                    employee.addAddress(address);
                });
            }

            // 4. Uloženie (Hibernate Cascade uloží aj adresy)
            employeeRepository.save(employee);

            log.info("Employee created successfully with ID: {}", employee.getId());
            return employee.getId();

        } catch (Exception e) {
            // 🛑 KOMPENZÁCIA: Ak DB padne, musíme upratať Keycloak
            log.error("Database save failed. Rolling back Keycloak user: {}", e);
            throw e; // Prehodíme chybu ďalej, aby Spring spravil DB Rollback
        }
    }
}
