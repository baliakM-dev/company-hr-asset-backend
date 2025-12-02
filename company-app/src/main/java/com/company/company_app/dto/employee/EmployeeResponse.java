package com.company.company_app.dto.employee;

import com.company.company_app.dto.address.AddressResponse;
import com.company.company_app.enums.EmployeeStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record EmployeeResponse(
        UUID id,
        String fullName, // Computed field
        String email,
        EmployeeStatus status,
        LocalDate startedWork,
        List<AddressResponse> addresses
) {}