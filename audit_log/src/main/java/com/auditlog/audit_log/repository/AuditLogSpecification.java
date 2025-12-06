package com.auditlog.audit_log.repository;

import com.auditlog.audit_log.domain.AuditLogEntity;
import com.auditlog.audit_log.dto.AuditLogFilter;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class AuditLogSpecification {

    private AuditLogSpecification() {
    }

    public static Specification<AuditLogEntity> withFilter(AuditLogFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 2. Fulltext SEARCH (Meno OR Priezvisko OR Email)
            // Používame 'like' s lower-case pre case-insensitive vyhľadávanie (napr. "peter" nájde "Peter")
            if (StringUtils.hasText(filter.search())) {
                String searchLike = "%" + filter.search().toLowerCase() + "%";

                Predicate firstNameMatch = cb.like(cb.lower(root.get("action")), searchLike);
                Predicate lastNameMatch = cb.like(cb.lower(root.get("entityName")), searchLike);

                // Spojíme ich cez OR (nájdi v mene ALEBO v priezvisku ALEBO v emaile)
                predicates.add(cb.or(firstNameMatch, lastNameMatch));
            }

            // Výsledné predikáty spojíme cez AND (Status musí sedieť A ZÁROVEŇ text musí sedieť)
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
