package com.company.company_app.repository;

import com.company.company_app.domain.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID> {
    // findAll(Pageable) je tam automaticky
    boolean existsByEmail(String email);

    boolean existsByKeycloakName(String keycloakName);
}
