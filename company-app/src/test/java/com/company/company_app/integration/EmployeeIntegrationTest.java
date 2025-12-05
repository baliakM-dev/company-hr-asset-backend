package com.company.company_app.integration;


import com.company.company_app.config.JpaConfig;
import com.company.company_app.domain.Address;
import com.company.company_app.domain.Employee;
import com.company.company_app.enums.AddressType;
import com.company.company_app.repository.EmployeeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers // Zapne podporu pre kontajnery
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // Nepoužívaj H2, použi Docker
@Import(JpaConfig.class) // Dôležité: Aby fungoval @CreatedDate (Auditing)
class EmployeeRepositoryTest {

    // ✅ Spring Boot 4 Magic: Toto automaticky nastaví datasource.url, username, password
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private EmployeeRepository employeeRepository;

    @Test
    @DisplayName("Should save employee with addresses and generate audit fields")
    void shouldSaveEmployeeWithAddresses() {
        // Given
        var employee = new Employee();
        employee.setId(UUID.randomUUID()); // ID generujeme manuálne (podľa našej stratégie)
        employee.setKeycloakID("keycloak-123");
        employee.setFirstName("Janko");
        employee.setLastName("Hrasko");
        employee.setEmail("janko@hrasko.sk");
        employee.setKeycloakName("jhrasko");
        employee.setStartedWork(LocalDate.now());

        var address = new Address();
        address.setId(UUID.randomUUID());
        address.setStreet("Hlavná 1");
        address.setCity("Bratislava");
        address.setPostalCode("81101");
        address.setCountry("Slovakia");
        address.setType(AddressType.HOME);

        // Prepojenie
        employee.addAddress(address);

        // When
        employeeRepository.save(employee);

        // Then (Flush a Clear, aby sme si vynútili načítanie z DB, nie z cache)
        employeeRepository.flush();

        // Získame ho z DB
        var savedEmployee = employeeRepository.findById(employee.getId()).orElseThrow();

        // 1. Overíme dáta
        assertThat(savedEmployee.getFirstName()).isEqualTo("Janko");

        // 2. Overíme Cascade (Adresa sa musela uložiť sama)
        assertThat(savedEmployee.getAddresses()).hasSize(1);
        var savedAddress = savedEmployee.getAddresses().iterator().next();
        assertThat(savedAddress.getCity()).isEqualTo("Bratislava");

        // 3. Overíme Auditing (CreatedDate by malo byť vyplnené)
        assertThat(savedEmployee.getCreatedAt()).isNotNull();
        assertThat(savedAddress.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should delete address when removed from list (Orphan Removal)")
    void shouldDeleteOrphanAddress() {
        // Given (Uložený zamestnanec s adresou)
        var employee = new Employee();
        employee.setId(UUID.randomUUID());
        // ... vyplniť povinné polia (v teste si môžeš spraviť helper metódu) ...
        employee.setKeycloakID("k-999");
        employee.setFirstName("Test");
        employee.setLastName("User");
        employee.setEmail("test@user.sk");
        employee.setKeycloakName("testuser");
        employee.setStartedWork(LocalDate.now());

        var address = new Address();
        address.setId(UUID.randomUUID());
        address.setStreet("Old Street");
        address.setCity("Old City");
        address.setPostalCode("00000");
        address.setCountry("SK");
        address.setType(AddressType.HOME);

        employee.addAddress(address);
        employeeRepository.saveAndFlush(employee);

        // When (Vyhodíme adresu)
        var fetchedEmployee = employeeRepository.findById(employee.getId()).orElseThrow();
        var addressToRemove = fetchedEmployee.getAddresses().iterator().next();

        fetchedEmployee.removeAddress(addressToRemove); // Helper metóda
        employeeRepository.saveAndFlush(fetchedEmployee);

        // Then
        var finalEmployee = employeeRepository.findById(employee.getId()).orElseThrow();
        assertThat(finalEmployee.getAddresses()).isEmpty();
    }
}