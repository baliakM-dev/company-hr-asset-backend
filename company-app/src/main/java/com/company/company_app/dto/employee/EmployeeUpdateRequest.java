package com.company.company_app.dto.employee;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmployeeUpdateRequest(
        @NotBlank
        @Size(min = 2, max = 50)
        @Pattern(
                // 1) len povolené znaky: písmená, čísla, medzery, apostrof, pomlčka
                // 2) (?!\\d+$) = zakáž reťazec, ktorý je TVORENÝ IBA číslicami
                regexp = "^(?!\\d+$)[\\p{L}\\d\\-' ]+$",
                message = "First name must not be only digits and may contain letters, digits, spaces, apostrophe and hyphen"
        )
        String firstName,

        @NotBlank
        @Size(min = 2, max = 50)
        @Pattern(
                regexp = "^(?!\\d+$)[\\p{L}\\d\\-' ]+$",
                message = "Last name must not be only digits and may contain letters, digits, spaces, apostrophe and hyphen"
        )
        String lastName,

        @NotNull
        String phoneNumber,

        @NotBlank
        String keycloakName
) {
}
