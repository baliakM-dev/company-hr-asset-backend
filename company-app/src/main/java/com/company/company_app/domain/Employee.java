package com.company.company_app.domain;


import com.company.company_app.enums.EmployeeStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.NaturalId;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "employees")
@Getter @Setter // ✅ MapStruct použije tieto settery
@NoArgsConstructor // ✅ MapStruct (aj Hibernate) použije tento konštruktor
@EntityListeners(AuditingEntityListener.class) // 1. Aktivuje auditing
@ToString(exclude = "addresses")
public class Employee implements Persistable<UUID> {

    @Id
    @Column(name = "employee_id", nullable = false, updatable = false)
    private UUID id;

    @NaturalId(mutable = true)
    @Column(name = "keycloak_id", nullable = false, unique = true)
    private String keycloakID;

    private String firstName;
    private String lastName;
    private String email;

    @Column(name = "phone")
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmployeeStatus status = EmployeeStatus.ACTIVE;

    private String keycloakName;
    private LocalDate startedWork;
    private LocalDate endWork;
    private String terminationReason;

    @Version
    private Long version;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Address> addresses = new HashSet<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @CreatedBy
    @Column(updatable = false)
    private UUID createdBy;

    @LastModifiedBy
    private UUID updatedBy;

    // --- Persistable Logic ---
    @Transient
    private boolean isNew = true; // ✅ Default funguje

    @Override
    public boolean isNew() { return isNew; }

    @PostLoad @PostPersist
    void markNotNew() { this.isNew = false; }

    // Helper pre adresy (stále užitočný)
    public void addAddress(Address address) {
        addresses.add(address);
        address.setEmployee(this);
    }

    public void removeAddress(Address address) {
        addresses.remove(address);
        address.setEmployee(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Employee that)) return false;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}