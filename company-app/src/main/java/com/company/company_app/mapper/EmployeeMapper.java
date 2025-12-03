package com.company.company_app.mapper;

import com.company.company_app.domain.Address;
import com.company.company_app.domain.Employee;
import com.company.company_app.dto.address.AddressResponse;
import com.company.company_app.dto.address.CreateAddressDto;
import com.company.company_app.dto.employee.CreateEmployeeRequest;
import com.company.company_app.dto.employee.EmployeeResponse;
import org.mapstruct.*;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface EmployeeMapper {

    // Entity -> Record
    @Mapping(target = "fullName", expression = "java(e.getFirstName() + \", \" + e.getLastName())")
    @Mapping(target = "keycloakName", source = "keycloakName")
    EmployeeResponse toResponse(Employee e);

    @Mapping(target = "fullAddress", expression = "java(a.getStreet() + \", \"+ a.getCity())")
    AddressResponse toAddressResponse(Address a);

    // Record -> Entity (pre Create)
//    @Mapping(target = "id", ignore = true)
//    @Mapping(target = "addresses", ignore = true) // Adresy riešime manuálne cez helper metódy
//    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "addresses", ignore = true) // riešime ručne cez helper metódy
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "keycloakID", ignore = true) // nastaví service po Keycloak create
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    Employee toEntity(CreateEmployeeRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employee", ignore = true)
    @Mapping(target = "version", ignore = true)
    Address toAddressEntity(CreateAddressDto dto);
}