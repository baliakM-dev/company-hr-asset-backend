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
    EmployeeResponse toResponse(Employee e);

    @Mapping(target = "fullAddress", expression = "java(a.getStreet() + \", \"+ a.getCity())")
    AddressResponse toAddressResponse(Address a);

    // Record -> Entity (pre Create)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "addresses", ignore = true) // Adresy riešime manuálne cez helper metódy
    @Mapping(target = "status", constant = "ACTIVE")
    Employee toEntity(CreateEmployeeRequest request);

    Address toAddressEntity(CreateAddressDto dto);
}