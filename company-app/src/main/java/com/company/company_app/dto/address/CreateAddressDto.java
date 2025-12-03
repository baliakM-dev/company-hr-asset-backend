package com.company.company_app.dto.address;

import com.company.company_app.enums.AddressType;
import jakarta.validation.constraints.*;

public record CreateAddressDto(
        @NotNull AddressType type,
        @NotBlank String street,
        @NotBlank String city,
        @NotBlank String postalCode,
        @NotBlank String country
) {}
