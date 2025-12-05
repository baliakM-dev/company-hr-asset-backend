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

/**
 * Doménová entita reprezentujúca fyzickú adresu priradenú k zamestnancovi.
 * <p>
 * Táto entita je "vlastniacou stranou“ (owning side) relácie Many-to-One so zamestnancom.
 * Implementuje rozhranie {@link Persistable} pre efektívnu perzistenciu entít s manuálne
 * priradeným UUID (zabraňuje zbytočným SELECT dopytom pred INSERTom).
 */
@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class) // Aktivuje automatické vypĺňanie createdAt/updatedAt
@ToString(exclude = "employee") // DÔLEŽITÉ: Zabraňuje zacykleniu (StackOverflowError) pri logovaní
public class Address implements Persistable<UUID> {
    /**
     * Unikátny identifikátor adresy.
     * Nie je generovaný databázou, ale aplikáciou pred uložením.
     */
    @Id
    @Column(name = "address_id", nullable = false, updatable = false)
    private UUID id;

    /**
     * Referencia na zamestnanca, ktorému adresa patrí.
     * Používame {@code FetchType.LAZY} pre optimalizáciu výkonu (načíta sa len keď je potrebný).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    /**
     * Typ adresy (napr. HOME, MAILING, TEMPORARY, WORK).
     * Uložené, ako String pre čitateľnosť v databáze.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AddressType type = AddressType.HOME;

    private String street;
    private String city;
    private String postalCode;
    private String country;

    /**
     * Verzia záznamu pre Optimistic Locking.
     * Zabraňuje prepísaniu zmien (Lost Update), ak sa dvaja používatelia pokúsia
     * upraviť rovnakú adresu naraz.
     */
    @Version
    private Long version;

    // ---Audit Fields (Spravované automaticky Spring Data JPA) ---
    /**
     * Časová pečiatka vytvorenia záznamu.
     * Automaticky spravované cez Spring Data JPA Auditing.
     */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Časová pečiatka poslednej zmeny.
     * Automaticky spravované cez Spring Data JPA Auditing.
     */
    @LastModifiedDate
    private Instant updatedAt;

    /**
     * Príznak, či ide o novú entitu.
     * Defaultne true, aby Hibernate vykonal INSERT namiesto SELECT + INSERT.
     */
    @Transient
    private boolean isNew = true;

    @Override
    public boolean isNew() { return isNew; }

    /**
     * Callback metóda volaná po načítaní z DB alebo po uložení.
     * Nastaví príznak, že entita už v databáze existuje.
     */
    @PostLoad @PostPersist void markNotNew() { this.isNew = false; }

    /**
     * Porovnáva entity na základe ich primárneho kľúča (ID).
     * Bezpečné použitie aj s Hibernate Proxies (využíva getter a instanceof).
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Address that)) return false;
        return id != null && id.equals(that.getId());
    }

    /**
     * Vracia konštantný hash code pre triedu.
     * Nutné pre správne fungovanie v Setoch pred uložením do DB (kedy sa ID môže meniť,
     * hoci v našom prípade je ID známe vopred, je to best practice pre JPA).
     */
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}