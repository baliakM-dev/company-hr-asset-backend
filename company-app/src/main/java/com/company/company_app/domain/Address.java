package com.company.company_app.domain;

import com.company.company_app.enums.AddressType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@ToString(exclude = "employee")
public class Address implements Persistable<UUID> {

    @Id
    @Column(name = "address_id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AddressType type = AddressType.HOME;

    private String street;
    private String city;
    private String postalCode;
    private String country;

    @Version
    private Long version;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @Transient
    private boolean isNew = true;

    @Override
    public boolean isNew() { return isNew; }

    @PostLoad @PostPersist void markNotNew() { this.isNew = false; }

    // Equals/HashCode pre Set
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Address that)) return false;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}