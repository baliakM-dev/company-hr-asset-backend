package com.company.company_app.mapper;

import com.company.company_app.domain.Address;
import com.company.company_app.domain.Employee;
import com.company.company_app.dto.employee.CreateEmployeeRequest;
import com.company.company_app.dto.employee.EmployeeUpdateRequest;
import com.company.company_app.enums.EmployeeStatus;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EmployeeMapperTest {

    // Získame inštanciu mappera (keďže v unit teste nemáme Spring kontext)
    private final EmployeeMapper mapper = Mappers.getMapper(EmployeeMapper.class);

    @Test
    void shouldMapToSummaryWithoutAddresses() {
        // Given
        Employee employee = new Employee();
        employee.setFirstName("Jan");
        employee.setLastName("Summary");
        employee.setAddresses(Collections.singleton(new Address())); // Má adresu

        // When
        var response = mapper.toSummary(employee);

        // Then
        assertThat(response.fullName()).isEqualTo("Jan, Summary");
        assertThat(response.addresses()).isNull(); // ✅ Adresy musia byť ignorované (null)
    }

    @Test
    void shouldUpdateEntityButIgnoreProtectedFields() {
        // Given (Existujúci zamestnanec)
        Employee employee = new Employee();
        UUID originalId = UUID.randomUUID();
        String originalKeycloakId = "kc-123";
        Instant originalCreatedAt = Instant.now();

        employee.setId(originalId);
        employee.setKeycloakID(originalKeycloakId);
        employee.setFirstName("OldName");
        employee.setCreatedAt(originalCreatedAt);
        employee.setStatus(EmployeeStatus.TERMINATED); // Status sa nesmie zmeniť cez DTO

        // Update Request (pokúsi sa zmeniť aj to, čo nemá)
        EmployeeUpdateRequest request = new EmployeeUpdateRequest(
                "NewName",
                "NewLast",
                "new@email.com",
                "new_kc_name"
        );

        // When
        mapper.updateEntityFromDto(request, employee);

        // Then
        // 1. Polia, ktoré sa MALI zmeniť
        assertThat(employee.getFirstName()).isEqualTo("NewName");
        assertThat(employee.getLastName()).isEqualTo("NewLast");

        // 2. Polia, ktoré sa NESMELI zmeniť (Ignored in Mapper)
        assertThat(employee.getId()).isEqualTo(originalId);
        assertThat(employee.getKeycloakID()).isEqualTo(originalKeycloakId);
        assertThat(employee.getCreatedAt()).isEqualTo(originalCreatedAt);
        assertThat(employee.getStatus()).isEqualTo(EmployeeStatus.TERMINATED); // Update request to neresetoval na ACTIVE
    }

    @Test
    void shouldMapCreateRequestToEntity() {
        // Given
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "ignored-id", // keycloakId (často ignorované ak ho generuje BE, ale musí byť v DTO)
                "Jan",
                "Test",
                "jan@test.sk",
                "0900123456", // phoneNumber
                "jantest", // keycloakName
                "password123", // keycloakPassword
                LocalDate.now(), // startedWork
                null // addresses
        );
        // When
        Employee entity = mapper.toEntity(request);

        // Then
        assertThat(entity.getFirstName()).isEqualTo("Jan");
        assertThat(entity.getStatus()).isEqualTo(EmployeeStatus.ACTIVE); // Default constant
        assertThat(entity.getId()).isNull(); // Generuje sa až v servise
    }
}