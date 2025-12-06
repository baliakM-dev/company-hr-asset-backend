package com.company.company_app.services.keycloak;

import com.company.company_app.dto.employee.CreateEmployeeRequest;
import com.company.company_app.dto.employee.EmployeeUpdateRequest;
import com.company.company_app.exceptions.UserAlreadyExistsException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Servisná trieda pre integráciu s Identity Management systémom Keycloak.
 * <p>
 * Zabezpečuje vytváranie a správu používateľských účtov v externom systéme.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakUserService {

    private final Keycloak keycloak;

    @Value("${keycloak.realm}")
    private String realm;

    /**
     * Vytvorí nového používateľa v Keycloaku.
     *
     * <p>Kroky:</p>
     * <ol>
     * <li>Pripraví {@link UserRepresentation} s menom a emailom.</li>
     * <li>Nastaví dočasné heslo.</li>
     * <li>Odošle požiadavku na Keycloak API.</li>
     * <li>Validuje HTTP status odpovede (201 Created).</li>
     * <li>Extrahuje a vráti vygenerované UUID (Keycloak ID).</li>
     * </ol>
     *
     * @param request Dáta potrebné pre vytvorenie (email, meno, priezvisko).
     * @return String reprezentácia UUID nového používateľa v Keycloaku.
     * @throws UserAlreadyExistsException ak používateľ už v Keycloaku existuje (HTTP 409).
     * @throws RuntimeException pri inej chybe komunikácie.
     */
    public String createUser(CreateEmployeeRequest request) {
        log.info("Creating Keycloak user for email: {}", request.email());

        // 1. Príprava objektu User
        UserRepresentation user = getUserRepresentation(request);

        // 3. Získanie referencie na Users Resource
        UsersResource usersResource = keycloak.realm(realm).users();

        // 4. Volanie API
        try (Response response = usersResource.create(user)) {

            // 5. Spracovanie odpovede
            if (response.getStatus() == 201) {

                String userId = CreatedResponseUtil.getCreatedId(response);
                log.info("Keycloak user created successfully. ID: {}", userId);

                // 5. ODOSLANIE EMAILU (Update Password + Verify Email)
                sendVerificationAndSetupEmail(usersResource, userId);

                return userId;
            } else if (response.getStatus() == 409) {
                log.warn("User already exists in Keycloak: {}", request.email());
                throw new UserAlreadyExistsException("User already exists in Keycloak.");
            } else {
                log.error("Failed to create Keycloak user. Status: {}, Body: {}", response.getStatus(), response.readEntity(String.class));
                throw new RuntimeException("Failed to create user in Keycloak. Status: " + response.getStatus());
            }
        } catch (Exception e) {
            log.error("Error connecting to Keycloak", e);
            throw new RuntimeException("Keycloak connection error", e);
        }
    }

    /**
     * Vyhľadá používateľa podľa emailu (užitočné pre validácie).
     */
    public List<UserRepresentation> searchByEmail(String email) {
        return keycloak.realm(realm).users().searchByEmail(email, true);
    }

    /**
     * Zmaže používateľa z Keycloaku (rollback scenár).
     *
     * @param keycloakId ID používateľa v Keycloaku.
     */
    public void deleteUser(String keycloakId) {
        log.warn("Deleting Keycloak user with ID: {}", keycloakId);
        try {
            keycloak.realm(realm).users().get(keycloakId).remove();
        } catch (Exception e) {
            log.error("Failed to delete user from Keycloak (Manual cleanup required for ID: {})", keycloakId, e);
            // Tu nevyhadzujeme exception, aby sme neprekryli pôvodnú chybu, ktorá vyvolala rollback
        }
    }

    private UserRepresentation getUserRepresentation(CreateEmployeeRequest request) {
        UserRepresentation user  = new UserRepresentation();
        user.setUsername(request.keycloakName()); // Login name
        user.setEmail(request.email());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEnabled(true);
        user.setEmailVerified(false);
        user.setRequiredActions(List.of("UPDATE_PASSWORD"));

        // 2. Nastavenie hesla (v PROD by sa malo poslať emailom alebo generovať náhodne)
        // Pre demo dávame fixné alebo z requestu (ak by tam bolo)
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue("ChangeMe123!"); // User bude vyzvany na zmenu hesla
        credential.setTemporary(true); // User si ho musí zmeniť pri prvom logine
        user.setCredentials(Collections.singletonList(credential));
        return user;
    }

    /**
     * Spustí odoslanie emailu s linkou na nastavenie hesla a overenie emailu.
     * Táto akcia je oddelená, aby jej zlyhanie nezhodilo celé vytvorenie usera, len sa zaloguje chyba.
     */
    private void sendVerificationAndSetupEmail(UsersResource usersResource, String userId) {
        try {
            log.info("Sending setup actions email to user ID: {}", userId);
            UserResource userResource = usersResource.get(userId);

            // Toto reálne odošle email cez SMTP nastavené v Keycloaku
            userResource.executeActionsEmail(List.of("UPDATE_PASSWORD", "VERIFY_EMAIL"));

        } catch (Exception e) {
            log.error("Failed to send setup email to user {}. SMTP might be misconfigured.", userId, e);
            // Nevyhadzujeme výnimku vyššie, user je vytvorený, email sa dá poslať znova manuálne cez Admin konzolu
        }
    }

    /**
     * Aktualizuje údaje používateľa v Keycloaku (Meno, Priezvisko, Username).
     *
     * @param keycloakId ID používateľa.
     * @param request Nové údaje.
     * @throws UserAlreadyExistsException Ak je nové username už obsadené.
     */
    public void updateUser(String keycloakId, EmployeeUpdateRequest request) {
        log.info("Updating Keycloak user with ID: {}", keycloakId);
        UserResource userResource = keycloak.realm(realm).users().get(keycloakId);

        // 1. Načítam aktuálnu reprezentáciu
        UserRepresentation user = userResource.toRepresentation();

        // 2. Skontrolujeme duplicitu username iba ak sa zmenilo
        if (!user.getUsername().equals(request.keycloakName())) {
            user.setUsername(request.keycloakName());
        }
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());

        try {
            // 3. Odošleme zmeny
            userResource.update(user);
            log.info("Keycloak user updated successfully.");
        } catch (ClientErrorException e) {
            // Získam status kód
            int status = e.getResponse().getStatus();

            // VALIDÁCIA (HTTP 400) - napr. krátke heslo, zlé znaky, povinné polia
            if (status == 400) {
                // Prečítame telo odpovede ako String, tam je text chyby od Keycloaku
                String errorBody = e.getResponse().readEntity(String.class);
                log.error("Validation error from Keycloak: {}", errorBody);

                // Môžeš vyhodiť vlastnú výnimku s textom z Keycloaku
                throw new IllegalArgumentException("Keycloak validation error: " + errorBody);
            }

            // DUPLICITA (HTTP 409)
            if (status == 409) {
                throw new UserAlreadyExistsException("Username or Email already exists in Keycloak.");
            }

            // Iné chyby (401, 403, 404 atď.)
            throw e;
        }
    }

    /**
     * Vráti používateľa do pôvodného stavu (Rollback).
     *
     * @param keycloakId ID používateľa.
     * @param originalState Objekt UserRepresentation stiahnutý PRED updateom.
     */
    public void revertUser(String keycloakId, UserRepresentation originalState) {
        log.warn("Reverting Keycloak user {} to original state due to transaction rollback.", keycloakId);
        try {
            UserResource userResource = keycloak.realm(realm).users().get(keycloakId);
            userResource.update(originalState);
            log.info("Keycloak rollback successful.");
        } catch (Exception e) {
            // Kritická chyba - dáta sú nekonzistentné
            log.error("CRITICAL: Failed to rollback Keycloak user {}. Data inconsistency requires manual intervention!", keycloakId, e);
        }
    }

    /**
     * Získa aktuálnu reprezentáciu používateľa (používa sa na vytvorenie zálohy pred updateom).
     */
    public UserRepresentation getUser(String keycloakId) {
        return keycloak.realm(realm).users().get(keycloakId).toRepresentation();
    }

}
