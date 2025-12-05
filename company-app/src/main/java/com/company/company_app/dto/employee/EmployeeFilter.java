package com.company.company_app.dto.employee;

import com.company.company_app.enums.EmployeeStatus;

public record EmployeeFilter(
        String search,       // Hľadá v mene, priezvisku, emaile
        EmployeeStatus status
) {}
