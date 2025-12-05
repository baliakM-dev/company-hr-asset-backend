package com.company.company_app.dto.employee;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record TerminateEmployeeRequest(
        @NotNull LocalDate endWork,
        @NotBlank String reason
) {}