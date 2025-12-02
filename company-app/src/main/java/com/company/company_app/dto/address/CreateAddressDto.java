package com.company.company_app.dto.address;

import com.company.company_app.enums.AddressType;

public record CreateAddressDto(
        AddressType type,
        String street,
        String city,
        String postalCode,
        String country
) {}
