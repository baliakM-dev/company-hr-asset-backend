package com.company.company_app.config.security;

import com.company.company_app.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Custom Security Bean pre overovanie prístupu k dátam zamestnancov.
 * Používa sa v @PreAuthorize anotáciách cez SpEL (napr. @employeeSecurity.isOwner(#id)).
 */
@Slf4j
@Component("employeeSecurity")
@RequiredArgsConstructor
public class EmployeeSecurity {

    private final EmployeeRepository employeeRepository;

    /**
     * Skontroluje, či aktuálne prihlásený používateľ je vlastníkom záznamu zamestnanca s daným ID.
     *
     * @param requestedEmployeeId Interné DB ID zamestnanca, ku ktorému sa pristupuje (z URL).
     * @return true, ak sa Keycloak ID prihláseného používateľa zhoduje s Keycloak ID zamestnanca v DB.
     */
    public boolean isOwner(UUID requestedEmployeeId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 1. Validácia: Musí to byť JWT token
        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            log.warn("Access denied: Principal is not a JWT token.");
            return false;
        }

        // 2. Získame 'sub' (Subject ID) z tokenu - to je Keycloak UUID prihláseného užívateľa
        String currentKeycloakId = jwt.getSubject();

        // 3. Načítanie zamestnanca z DB podľa ID z URL
        return employeeRepository.findById(requestedEmployeeId)
                .map(employee -> {
                    // Porovnáme: "Patrí tento záznam (requestedId) prihlásenému užívateľovi (currentKeycloakId)?"
                    // Používame equalsIgnoreCase pre istotu, hoci UUID by malo byť štandardizované
                    boolean isMatch = currentKeycloakId.equalsIgnoreCase(employee.getKeycloakID());

                    if (!isMatch) {
                        log.warn("Access denied: User {} tried to access employee profile {}", currentKeycloakId, requestedEmployeeId);
                    }
                    return isMatch;
                })
                .orElse(false); // Ak zamestnanec s týmto ID neexistuje, vrátime false (neprezradíme 404 vs 403)
    }
}