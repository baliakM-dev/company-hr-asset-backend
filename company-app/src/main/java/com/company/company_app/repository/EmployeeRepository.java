package com.company.company_app.repository;

import com.company.company_app.domain.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.net.http.HttpHeaders;
import java.util.UUID;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID>, JpaSpecificationExecutor<Employee> {
    boolean existsByEmail(String email);
    boolean existsByKeycloakName(String keycloakName);
}
