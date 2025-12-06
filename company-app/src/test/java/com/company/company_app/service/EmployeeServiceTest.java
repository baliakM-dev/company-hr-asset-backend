package com.company.company_app.service;

import com.company.company_app.domain.Employee;
import com.company.company_app.dto.employee.CreateEmployeeRequest;
import com.company.company_app.dto.employee.EmployeeResponse;
import com.company.company_app.exceptions.UserAlreadyExistsException;
import com.company.company_app.mapper.EmployeeMapper;
import com.company.company_app.repository.EmployeeRepository;
import com.company.company_app.services.EmployeeService;
import com.company.company_app.services.keycloak.KeycloakUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock // Simulujeme Repository
    private EmployeeRepository employeeRepository;

    @Mock // Simulujeme Mapper
    private EmployeeMapper employeeMapper;

    @Mock // Simulujeme Keycloak (Wrapper, nie samotného klienta)
    private KeycloakUserService keycloakService;

    @InjectMocks // Toto je testovaná trieda, do ktorej sa vstreknú mocky vyššie
    private EmployeeService employeeService;

    @Test
    @DisplayName("Should create employee successfully when Keycloak and DB succeed")
    void shouldCreateEmployeeSuccessfully() {
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
        Employee mappedEntity = new Employee();
        mappedEntity.setFirstName("Jan");

        Employee savedEntity = new Employee();
        savedEntity.setId(UUID.randomUUID());
        savedEntity.setKeycloakID("kc-uuid-123");
        savedEntity.setFirstName("Jan");

        // Mockovanie správania
        when(employeeRepository.existsByEmail(any())).thenReturn(false);
        when(employeeRepository.existsByKeycloakName(any())).thenReturn(false);
        when(keycloakService.createUser(any())).thenReturn("kc-uuid-123"); // Keycloak vráti ID
        when(employeeMapper.toEntity(any())).thenReturn(mappedEntity);
        when(employeeRepository.save(any(Employee.class))).thenReturn(savedEntity);
        when(employeeMapper.toResponse(any())).thenReturn(new EmployeeResponse(
                savedEntity.getId(), "Jan, Test", "jan@test.sk", "jantest", null, null, null
        ));

        // When
        EmployeeResponse response = employeeService.createEmployee(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.keycloakName()).isEqualTo("jantest");

        // Overíme, že sa volal save a NEVOLAL sa deleteUser (rollback)
        verify(employeeRepository).save(any(Employee.class));
        verify(keycloakService, never()).deleteUser(anyString());
    }

    @Test
    @DisplayName("Should ROLLBACK Keycloak user when DB save fails")
    void shouldRollbackKeycloakUserWhenDbSaveFails() {
        // Given
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "ignored-id",
                "Fail",
                "User",
                "fail@db.sk",
                "0900000000",
                "failuser",
                "password123",
                LocalDate.now(),
                null
        );
        String createdKeycloakId = "kc-uuid-fail";

        // Mockovanie
        when(employeeRepository.existsByEmail(any())).thenReturn(false);
        when(keycloakService.createUser(any())).thenReturn(createdKeycloakId); // 1. Keycloak vytvorí usera
        when(employeeMapper.toEntity(any())).thenReturn(new Employee());

        // 2. DB vyhodí chybu pri ukladaní
        when(employeeRepository.save(any())).thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        assertThatThrownBy(() -> employeeService.createEmployee(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database connection failed");

        // 3. CRITICAL CHECK: Overíme, či sa zavolala kompenzačná metóda deleteUser
        verify(keycloakService).deleteUser(createdKeycloakId);
    }

    @Test
    @DisplayName("Should throw exception immediately if user exists in local DB")
    void shouldFailFastIfUserExistsInDb() {
        // Given
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "ignored-id",
                "Exist",
                "User",
                "exist@db.sk",
                "0900000000",
                "exist",
                "password123",
                LocalDate.now(),
                null
        );


        when(employeeRepository.existsByEmail("exist@db.sk")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> employeeService.createEmployee(request))
                .isInstanceOf(UserAlreadyExistsException.class);

        // Overíme, že sme ani nevolali Keycloak (šetríme performance)
        verify(keycloakService, never()).createUser(any());
    }
}