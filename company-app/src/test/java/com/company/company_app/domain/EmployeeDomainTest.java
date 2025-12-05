package com.company.company_app.domain;

import com.company.company_app.enums.AddressType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EmployeeDomainTest {

    @Test
    void shouldAddAddressAndSetReference() {
        // Given
        Employee employee = new Employee();
        employee.setId(UUID.randomUUID());

        Address address = new Address();
        address.setId(UUID.randomUUID());
        address.setType(AddressType.HOME);

        // When
        employee.addAddress(address);

        // Then
        assertThat(employee.getAddresses()).contains(address);
        assertThat(address.getEmployee()).isEqualTo(employee);
    }

    @Test
    void shouldRemoveAddressAndClearReference() {
        // Given
        Employee employee = new Employee();
        Address address = new Address();
        employee.addAddress(address);

        // When
        employee.removeAddress(address);

        // Then
        assertThat(employee.getAddresses()).doesNotContain(address);
        assertThat(address.getEmployee()).isNull();
    }

    @Test
    void shouldVerifyEqualityBasedOnId() {
        // Given
        UUID id = UUID.randomUUID();

        Employee emp1 = new Employee();
        emp1.setId(id);

        Employee emp2 = new Employee();
        emp2.setId(id);

        Employee emp3 = new Employee();
        emp3.setId(UUID.randomUUID());

        // Then
        assertThat(emp1).isEqualTo(emp2);
        assertThat(emp1).isNotEqualTo(emp3);
        assertThat(emp1.hashCode()).isEqualTo(emp2.hashCode());
    }
}