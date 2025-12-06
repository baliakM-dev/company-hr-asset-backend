package com.company.company_app.dto.employee;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EmployeeUpdateRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotNull String phoneNumber,
        @NotBlank String keycloakName
) {
}
