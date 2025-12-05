package com.company.company_app.controller;

import com.company.company_app.dto.employee.CreateEmployeeRequest;
import com.company.company_app.dto.employee.EmployeeFilter;
import com.company.company_app.dto.employee.EmployeeResponse;
import com.company.company_app.dto.employee.TerminateEmployeeRequest;
import com.company.company_app.services.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * REST Controller pre správu životného cyklu zamestnancov.
 * <p>
 * Poskytuje endpointy pre vytváranie, vyhľadávanie a ukončovanie pracovného pomeru.
 */
@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    /**
     * Vytvorí nového zamestnanca.
     * <p>
     * Táto operácia zabezpečuje vytvorenie záznamu v internej databáze
     * a zároveň registráciu používateľa v identifikačnom systéme (Keycloak).
     *
     * <p><strong>URL:</strong> {@code POST /api/v1/employees}</p>
     *
     * @param request    DTO obsahujúce údaje potrebné pre vytvorenie zamestnanca.
     * @param uriBuilder Builder pre dynamické zostavenie URI novovytvoreného zdroja.
     * @return {@link ResponseEntity} obsahujúce vytvoreného zamestnanca a hlavičku {@code Location}.
     * Status kód: {@code 201 Created}.
     */
    @PostMapping
    @PreAuthorize("hasRole('EMPLOYEE:CREATE')")
    public ResponseEntity<EmployeeResponse> createEmployee(
            @RequestBody @Valid CreateEmployeeRequest request,
            UriComponentsBuilder uriBuilder
    ) {
        EmployeeResponse response = employeeService.createEmployee(request);

        URI location = uriBuilder.path("/api/v1/employees/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(response);
    }

    /**
     * Ukončí pracovný pomer existujúceho zamestnanca.
     * <p>
     * Nastaví stav zamestnanca na {@code TERMINATED} a aktualizuje relevantné údaje
     * o ukončení (dátum, dôvod).
     *
     * <p><strong>URL:</strong> {@code POST /api/v1/employees/{id}/terminate}</p>
     *
     * @param id      Unikátny identifikátor zamestnanca (UUID).
     * @param request DTO s detailmi o ukončení pomeru.
     * @return {@link ResponseEntity} bez tela odpovede (Void).
     * Status kód: {@code 204 No Content}.
     */
    @PostMapping("/{id}/terminate")
    @PreAuthorize("hasRole('EMPLOYEE:TERMINATE')")
    public ResponseEntity<Void> terminateEmployee(
            @PathVariable UUID id,
            @RequestBody @Valid TerminateEmployeeRequest request
    ) {
        employeeService.terminateEmployee(id, request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Vyhľadá a vráti stránkovaný zoznam zamestnancov na základe filtra.
     * <p>
     * Podporuje filtrovanie podľa atribútov a stránkovanie výsledkov.
     *
     * <p><strong>Príklady použitia:</strong></p>
     * <ul>
     * <li>{@code GET /api/v1/employees?status=ACTIVE&search=Janko}</li>
     * <li>{@code GET /api/v1/employees?page=1&size=10&sort=lastName,desc}</li>
     * <li>{@code GET /api/v1/employees?status=TERMINATED}</li>
     * </ul>
     *
     * @param filter   Objekt obsahujúci kritériá pre filtrovanie (napr. status, fulltext search).
     * @param pageable Objekt pre stránkovanie a radenie (default: 20 položiek, radenie podľa priezviska).
     * @return {@link Page} obsahujúca zoznam nájdených zamestnancov.
     * Status kód: {@code 200 OK}.
     */
    @GetMapping
    @PreAuthorize("hasRole('EMPLOYEE:READ_ALL')")
    public ResponseEntity<Page<EmployeeResponse>> getEmployees(
            EmployeeFilter filter,
            @PageableDefault(size = 20, sort = "lastName", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(employeeService.getAllEmployees(filter, pageable));
    }

    /**
     * Vyhľadá a vráti detail konkrétneho zamestnanca podľa jeho ID.
     *
     * <p><strong>URL:</strong> {@code GET /api/v1/employees/{id}}</p>
     *
     * @param id Unikátny identifikátor zamestnanca (UUID).
     * @return {@link ResponseEntity} obsahujúce detail zamestnanca.
     * Status kód: {@code 200 OK}.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('EMPLOYEE:READ') or @employeeSecurity.isOwner(#id)")
    public ResponseEntity<EmployeeResponse> getEmployee(@PathVariable UUID id) {
        return ResponseEntity.ok(employeeService.getEmployee(id));
    }

}
