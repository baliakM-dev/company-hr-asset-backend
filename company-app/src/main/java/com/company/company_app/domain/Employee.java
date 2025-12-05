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

/**
 * Hlavná doménová entita reprezentujúca zamestnanca spoločnosti.
 * <p>
 * Slúži, ako koreň agregátu (Aggregate Root) pre súvisiace entity, ako sú {@link Address}.
 * Implementuje rozhranie {@link Persistable} pre optimalizáciu ukladania pri manuálne
 * generovaných ID.
 */
@Entity
@Table(name = "employees")
@Getter @Setter // ✅ MapStruct použije tieto settery
@NoArgsConstructor // ✅ MapStruct (aj Hibernate) použije tento konštruktor
@EntityListeners(AuditingEntityListener.class) // Aktivuje automatické vypĺňanie audit polí (createdAt, atď.)
@ToString(exclude = "addresses") // Prevencia cyklických závislostí pri logovaní
public class Employee implements Persistable<UUID> {

    /**
     * Primárny kľúč entity.
     * Je generovaný aplikačne (nie databázou), čo umožňuje získať ID ešte pred uložením.
     */
    @Id
    @Column(name = "employee_id", nullable = false, updatable = false)
    private UUID id;

    /**
     * Prirodzený identifikátor prepojený na externý IAM systém (Keycloak).
     * <p>
     * Anotácia {@code @NaturalId} umožňuje Hibernate optimalizovať načítanie entity
     * a ukladanie do L2 cache. Atribút je meniteľný (`mutable = true`) pre prípad migrácie používateľov.
     */
    @NaturalId(mutable = true)
    @Column(name = "keycloak_id", nullable = false, unique = true)
    private String keycloakID;

    private String firstName;
    private String lastName;
    private String email;

    @Column(name = "phone")
    private String phoneNumber;

    /**
     * Aktuálny stav životného cyklu zamestnanca.
     * Uložený, ako String (EnumType.STRING) pre lepšiu čitateľnosť v DB a odolnosť voči re-orderingu enumov.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmployeeStatus status = EmployeeStatus.ACTIVE;

    private String keycloakName;
    private LocalDate startedWork;
    private LocalDate endWork;
    private String terminationReason;

    /**
     * Verzia záznamu pre Optimistic Locking.
     * Chráni pred prepísaním zmien pri súbežnej úprave viacerými používateľmi.
     */
    @Version
    private Long version;

    /**
     * Zoznam adries zamestnanca.
     * <p>
     * <ul>
     * <li>{@code CascadeType.ALL}: Všetky operácie (persist, remove…) sa propagujú na adresy.</li>
     * <li>{@code orphanRemoval = true}: Odstránenie adresy zo zoznamu ju fyzicky zmaže z databázy.</li>
     * </ul>
     */
    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Address> addresses = new HashSet<>();

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

    @CreatedBy
    @Column(updatable = false)
    private UUID createdBy;

    @LastModifiedBy
    private UUID updatedBy;

    /**
     * Príznak, či ide o novú entitu, ktorá ešte nie je v databáze.
     * Defaultne {@code true}, aby Hibernate vykonal priamo INSERT bez zbytočného SELECTu.
     */
    @Transient
    private boolean isNew = true;

    @Override
    public boolean isNew() { return isNew; }

    /**
     * Callback po načítaní z DB alebo uložení-entita už nie je nová.
     */
    @PostLoad @PostPersist
    void markNotNew() { this.isNew = false; }

    /**
     * Pridá adresu zamestnancovi a zároveň nastaví spätnú referenciu.
     * Zabezpečuje konzistenciu obojsmernej relácie v pamäti.
     *
     * @param address Adresa na pridanie.
     */
    public void addAddress(Address address) {
        addresses.add(address);
        address.setEmployee(this);
    }

    /**
     * Odstráni adresu zo zoznamu a zruší spätnú referenciu.
     *
     * @param address Adresa na odstránenie.
     */
    public void removeAddress(Address address) {
        addresses.remove(address);
        address.setEmployee(null);
    }

    /**
     * Vykoná proces ukončenia pracovného pomeru.
     * <p>
     * Metóda zapuzdruje biznis pravidlá pre zmenu stavu:
     * <ul>
     * <li>Kontroluje, či zamestnanec už nie je ukončený.</li>
     * <li>Validuje časovú postupnosť dátumov.</li>
     * <li>Nastavuje stav, dátum a dôvod ukončenia.</li>
     * </ul>
     *
     * @param endDate Dátum ukončenia pracovného pomeru.
     * @param reason Textové odôvodnenie ukončenia.
     * @throws IllegalStateException ak je zamestnanec už v stave {@code TERMINATED}.
     * @throws IllegalArgumentException ak je dátum ukončenia pred dátumom nástupu.
     */
    public void terminate(LocalDate endDate, String reason) {
        if (this.status == EmployeeStatus.TERMINATED) {
            throw new IllegalStateException("Employee is already terminated."); // Alebo tvoja BusinessException
        }

        // Validácia dátumov (napr. nemôžeš ukončiť pomer v minulosti hlbšej, ako X)
        if (endDate.isBefore(this.startedWork)) {
            throw new IllegalArgumentException("End date cannot be before start date.");
        }

        this.status = EmployeeStatus.TERMINATED;
        this.endWork = endDate;
        this.terminationReason = reason;

        // Tu pride este vratenie majetku a pod. ked budu dalsie tabulky.
    }

    /**
     * Porovnáva entity na základe ich primárneho kľúča (ID).
     * Bezpečné použitie aj s Hibernate Proxies (využíva getter a instanceof).
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Employee that)) return false;
        return id != null && id.equals(that.getId());
    }

    /**
     * Vracia konštantný hash code pre triedu.
     * Nutné pre správne fungovanie v Setoch pred uložením do DB.
     */
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}