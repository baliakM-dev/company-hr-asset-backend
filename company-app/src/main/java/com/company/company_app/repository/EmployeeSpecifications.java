package com.company.company_app.repository;

import com.company.company_app.domain.Employee;
import com.company.company_app.dto.employee.EmployeeFilter;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility trieda pre definíciu dynamických JPA špecifikácií (Specifications) entity {@link Employee}.
 * <p>
 * Slúži na prekladanie DTO filtrov do databázových dopytov pomocou Criteria API.
 * Táto trieda by nemala byť inštancovaná.
 */
public class EmployeeSpecifications {
    /**
     * Privátny konštruktor zabraňuje inštanciácii utility triedy.
     */
    private EmployeeSpecifications() {
    }

    /**
     * Vytvorí {@link Specification} na základe vstupného filtra.
     * <p>
     * Metóda dynamicky skladá podmienky:
     * <ul>
     * <li><b>Status:</b> Presná zhoda (Equal). Aplikuje sa len, ak je v filtri prítomný.</li>
     * <li><b>Search:</b> Fulltextové vyhľadávanie (Like). Je <strong>case-insensitive</strong>
     * a hľadá zhodu v stĺpcoch <code>firstName</code>, <code>lastName</code> ALEBO <code>email</code>.</li>
     * </ul>
     * Všetky aplikované podmienky sú spojené operátorom <strong>AND</strong>.
     *
     * @param filter DTO objekt obsahujúci kritériá vyhľadávania (status, textový reťazec).
     * @return {@link Specification} pripravená pre použitie v JpaRepository, alebo prázdna špecifikácia, ak je filter prázdny.
     */
    public static Specification<Employee> withFilter(EmployeeFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Filter podľa STATUSU (Exact match)
            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("status"), filter.status()));
            }

            // 2. Fulltext SEARCH (Meno OR Priezvisko OR Email)
            // Používame 'like' s lower-case pre case-insensitive vyhľadávanie (napr. "peter" nájde "Peter")
            if (StringUtils.hasText(filter.search())) {
                String searchLike = "%" + filter.search().toLowerCase() + "%";

                Predicate firstNameMatch = cb.like(cb.lower(root.get("firstName")), searchLike);
                Predicate lastNameMatch = cb.like(cb.lower(root.get("lastName")), searchLike);
                Predicate emailMatch = cb.like(cb.lower(root.get("email")), searchLike);

                // Spojíme ich cez OR (nájdi v mene ALEBO v priezvisku ALEBO v emaile)
                predicates.add(cb.or(firstNameMatch, lastNameMatch, emailMatch));
            }

            // Výsledné predikáty spojíme cez AND (Status musí sedieť A ZÁROVEŇ text musí sedieť)
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}