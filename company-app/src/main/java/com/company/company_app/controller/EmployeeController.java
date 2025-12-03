package com.company.company_app.controller;

import com.company.company_app.dto.employee.CreateEmployeeRequest;
import com.company.company_app.dto.employee.EmployeeResponse;
import com.company.company_app.services.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;


@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    public ResponseEntity<EmployeeResponse> createEmployee(
            @RequestBody @Valid CreateEmployeeRequest request,
            UriComponentsBuilder uriBuilder
    ) {
        // Predpokladám, že si upravil Service, aby vracal EmployeeResponse (DTO), nie len UUID.
        // Ak Service vracia len UUID, musíš tu buď vrátiť len ID, alebo zavolať get(id).
        EmployeeResponse response = employeeService.createEmployee(request);

        // 2. Dynamické zostavenie URL (Bezpečné pre Cloud/Proxy)
        URI location = uriBuilder.path("/api/v1/employees/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity
                .created(location) // 3. Nastaví hlavičku Location + Status 201
                .body(response);   // 4. Vráti JSON body
    }

}
