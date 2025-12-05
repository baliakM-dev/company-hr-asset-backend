package com.company.company_app.dto.address;

import com.company.company_app.enums.AddressType;

import java.util.UUID;

public record AddressResponse(
        UUID id,
        AddressType type,
        String fullAddress // Computed field
) {}