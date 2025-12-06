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
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
class EmployeeRepositoryTest {

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
        employee.setId(UUID.randomUUID());
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

        employee.addAddress(address);

        // When
        employeeRepository.save(employee);
        employeeRepository.flush(); // Vynútime zápis do DB

        // Then
        var savedEmployee = employeeRepository.findById(employee.getId()).orElseThrow();

        assertThat(savedEmployee.getFirstName()).isEqualTo("Janko");
        assertThat(savedEmployee.getAddresses()).hasSize(1);
        assertThat(savedEmployee.getAddresses().iterator().next().getCity()).isEqualTo("Bratislava");

        // Auditing kontrola
        assertThat(savedEmployee.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should delete address when removed from list (Orphan Removal)")
    void shouldDeleteOrphanAddress() {
        // Given
        var employee = new Employee();
        employee.setId(UUID.randomUUID());
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

        // When
        var fetchedEmployee = employeeRepository.findById(employee.getId()).orElseThrow();
        var addressToRemove = fetchedEmployee.getAddresses().iterator().next();

        fetchedEmployee.removeAddress(addressToRemove);
        employeeRepository.saveAndFlush(fetchedEmployee);

        // Then
        var finalEmployee = employeeRepository.findById(employee.getId()).orElseThrow();
        assertThat(finalEmployee.getAddresses()).isEmpty();
    }
}