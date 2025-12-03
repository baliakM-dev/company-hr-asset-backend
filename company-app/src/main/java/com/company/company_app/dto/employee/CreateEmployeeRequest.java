package com.company.company_app.dto.employee;

import com.company.company_app.dto.address.CreateAddressDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

public record CreateEmployeeRequest(
        @NotBlank String keycloakId,
        @NotBlank String firstName,
        @NotBlank String lastName,
        @Email @NotBlank String email,
        @NotNull String phoneNumber,
        @NotBlank String keycloakName,
        @NotNull String keycloakPassword,
        LocalDate startedWork,
        @Valid List<@Valid CreateAddressDto> addresses
) {
}