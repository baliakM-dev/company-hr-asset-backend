package com.company.company_app.services.keycloak;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Servisná trieda pre správu rolí a oprávnení v Keycloaku.
 * <p>
 * Vyčlenená z KeycloakService pre lepšiu granularitu a prehľadnosť.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakRoleService {
    private final Keycloak keycloak;

    @Value("${keycloak.realm}")
    private String realm;

    /**
     * Pridá používateľa do skupiny (Group).
     *
     * @param keycloakId ID používateľa v Keycloaku.
     * @param groupName Názov skupiny (napr. "MANAGER", "HR").
     */
    public void assignGroup(String keycloakId, String groupName) {
        log.info("Adding user '{}' to group '{}'", keycloakId, groupName);

        // 1. Získame referenciu na používateľa
        UserResource userResource = keycloak.realm(realm).users().get(keycloakId);

        // 2. Nájdeme ID skupiny podľa názvu
        String groupId = findGroupIdByName(groupName);

        // 3. Pridáme používateľa do skupiny
        userResource.joinGroup(groupId);
    }

    /**
     * Odoberie používateľa zo skupiny.
     *
     * @param keycloakId ID používateľa v Keycloaku.
     * @param groupName Názov skupiny (napr. "MANAGER").
     */
    public void removeGroup(String keycloakId, String groupName) {
        log.info("Removing user '{}' from group '{}'", keycloakId, groupName);

        UserResource userResource = keycloak.realm(realm).users().get(keycloakId);

        // 2. Nájdeme ID skupiny
        String groupId = findGroupIdByName(groupName);

        // 3. Odoberieme používateľa zo skupiny
        userResource.leaveGroup(groupId);
    }

    /**
     * Pomocná metóda na nájdenie ID skupiny podľa jej názvu.
     * Keycloak API neumožňuje pridať do skupiny priamo podľa názvu, potrebujeme UUID.
     */
    private String findGroupIdByName(String groupName) {
        // Vyhľadáme skupiny, ktoré sa volajú (alebo obsahujú) daný názov
        List<GroupRepresentation> groups = keycloak.realm(realm).groups()
                .groups(groupName, 0, 1, false);

        if (groups.isEmpty()) {
            throw new RuntimeException("Group not found in Keycloak: " + groupName);
        }

        // Pre istotu nájdeme presnú zhodu (API môže vrátiť aj čiastočné zhody)
        return groups.stream()
                .filter(g -> g.getName().equalsIgnoreCase(groupName))
                .findFirst()
                .map(GroupRepresentation::getId)
                .orElseThrow(() -> new RuntimeException("Group not found in Keycloak: " + groupName));
    }
}
