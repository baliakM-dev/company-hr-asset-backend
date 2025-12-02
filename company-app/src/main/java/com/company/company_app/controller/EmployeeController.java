package com.company.company_app.controller;

import com.company.company_app.dto.employee.CreateEmployeeRequest;
import com.company.company_app.services.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UUID createEmployee(@RequestBody @Valid CreateEmployeeRequest request, @AuthenticationPrincipal Jwt jwt ) {
        var creatorId = UUID.fromString(jwt.getSubject());
        return employeeService.createEmployee(request);
    }

}
