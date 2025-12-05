package com.company.company_app.controller;

import com.company.company_app.exceptions.UserNotFoundException;
import com.company.company_app.repository.EmployeeRepository;
import com.company.company_app.services.keycloak.KeycloakRoleService;
import com.company.company_app.services.keycloak.KeycloakUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller pre HR operácie (Human Resources).
 * <p>
 * Poskytuje endpointy pre manažment právomocí a organizačnej štruktúry,
 * ktoré vykonávajú HR manažéri.
 */
@RestController
@RequestMapping("/api/v1/hr") // Zmena URL z 'admin' na 'hr'
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGE_ROLES')") // Oprávnenie špecifické pre HR
public class HRController {

    private final KeycloakRoleService keycloakRoleService;
    private final EmployeeRepository employeeRepository;

    /**
     * Priradí zamestnanca do skupiny (napr. pri povýšení).
     * <p>
     * Tento endpoint volá HR manažér, keď chce zamestnancovi prideliť
     * novú pozíciu alebo oddelenie (napr. skupina 'MANAGER' alebo 'IT_DEPARTMENT').
     *
     * <p><strong>URL:</strong> {@code PUT /api/v1/hr/employees/{id}/groups/{groupName}}</p>
     *
     * @param id ID zamestnanca
     * @param groupName Názov skupiny (napr. "MANAGER")
     */
    @PutMapping("/employees/{id}/groups/{groupName}")
    public ResponseEntity<Void> assignGroupToEmployee(
            @PathVariable UUID id,
            @PathVariable String groupName
    ) {
        // 1. Nájdeme zamestnanca v našej DB, aby sme získali jeho Keycloak ID
        var employee = employeeRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Employee not found"));

        // 2. Zavoláme RoleService (ktorý teraz interne rieši Groups)
        // Normalizácia na veľké písmená je stále dobrá prax
        keycloakRoleService.assignGroup(employee.getKeycloakID(), groupName.toUpperCase());

        return ResponseEntity.noContent().build();
    }

    /**
     * Odoberie zamestnanca zo skupiny (napr. pri zmene oddelenia alebo degradácii).
     *
     * <p><strong>URL:</strong> {@code DELETE /api/v1/hr/employees/{id}/groups/{groupName}}</p>
     */
    @DeleteMapping("/employees/{id}/groups/{groupName}")
    public ResponseEntity<Void> removeGroupFromEmployee(
            @PathVariable UUID id,
            @PathVariable String groupName
    ) {
        var employee = employeeRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Employee not found"));

        keycloakRoleService.removeGroup(employee.getKeycloakID(), groupName.toUpperCase());

        return ResponseEntity.noContent().build();
    }
}
