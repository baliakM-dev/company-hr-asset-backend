package com.company.company_app.mapper;

import com.company.company_app.domain.Address;
import com.company.company_app.domain.Employee;
import com.company.company_app.dto.address.AddressResponse;
import com.company.company_app.dto.address.CreateAddressDto;
import com.company.company_app.dto.employee.CreateEmployeeRequest;
import com.company.company_app.dto.employee.EmployeeResponse;
import com.company.company_app.dto.employee.EmployeeUpdateRequest;
import org.mapstruct.*;

/**
 * Komponent zodpovedný za mapovanie medzi DTO objektmi a doménovými entitami.
 * <p>
 * Využíva knižnicu <strong>MapStruct</strong> pre automatické generovanie implementácie
 * mapovacích metód počas kompilácie.
 * <p>
 * Konfigurácia:
 * <ul>
 * <li>{@code componentModel = "spring"}: Mapper je registrovaný ako Spring Bean a je možné ho injektovať (@Autowired).</li>
 * <li>{@code unmappedTargetPolicy = ReportingPolicy.IGNORE}: Ignoruje cieľové polia, ktoré nemajú zdroj v DTO (napr. auditné polia), aby sa predišlo chybám pri builde.</li>
 * </ul>
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface EmployeeMapper {

    /**
     * Konvertuje entitu zamestnanca na odpoveď (DTO) pre klienta.
     * <p>
     * Obsahuje custom logiku pre formátovanie mena.
     *
     * @param e Zdrojová entita {@link Employee}.
     * @return {@link EmployeeResponse} s naformátovanými údajmi.
     */
    @Mapping(target = "fullName", expression = "java(e.getFirstName() + \", \" + e.getLastName())")
    @Mapping(target = "keycloakName", source = "keycloakName")
    EmployeeResponse toResponse(Employee e);

    /**
     * SUMMARY RESPONSE: Používa sa pre zoznamy (ignoruje adresy pre optimalizáciu).
     * <p>
     * Zoznam adries bude v JSONe null.
     */
    @Mapping(target = "fullName", expression = "java(e.getFirstName() + \", \" + e.getLastName())")
    @Mapping(target = "keycloakName", source = "keycloakName")
    @Mapping(target = "addresses", ignore = true) // ✅ Tu ignorujeme adresy
    EmployeeResponse toSummary(Employee e);

    /**
     * Konvertuje entitu adresy na DTO.
     * <p>
     * Spája ulicu a mesto do jedného reťazca pre zobrazenie vo výpisoch.
     *
     * @param a Zdrojová entita {@link Address}.
     * @return {@link AddressResponse}.
     */
    @Mapping(target = "fullAddress", expression = "java(a.getStreet() + \", \"+ a.getCity())")
    AddressResponse toAddressResponse(Address a);

    /**
     * Konvertuje požiadavku na vytvorenie zamestnanca na novú entitu.
     * <p>
     * Nastavuje predvolené hodnoty a ignoruje polia spravované systémom:
     * <ul>
     * <li>{@code id}, {@code keycloakID}: Generované v servise alebo DB.</li>
     * <li>{@code addresses}: Riešené manuálne cez helper metódy entity pre zachovanie konzistencie (relationship management).</li>
     * <li>{@code status}: Nový zamestnanec je vždy {@code ACTIVE}.</li>
     * <li>Audit polia (createdBy, atď.): Spravuje JPA Auditing.</li>
     * </ul>
     *
     * @param request Vstupné DTO {@link CreateEmployeeRequest}.
     * @return Nová inštancia {@link Employee} pripravená na uloženie (okrem relácií).
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "addresses", ignore = true) // riešime ručne cez helper metódy
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "keycloakID", ignore = true) // nastaví service po Keycloak create
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    Employee toEntity(CreateEmployeeRequest request);

    /**
     * Konvertuje DTO novej adresy na entitu.
     * <p>
     * Referencia na rodiča ({@code employee}) sa nastavuje manuálne v servise.
     *
     * @param dto Vstupné dáta adresy.
     * @return Entita {@link Address}.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employee", ignore = true)
    @Mapping(target = "version", ignore = true)
    Address toAddressEntity(CreateAddressDto dto);

    /**
     * Aktualizuje údaje existujúceho zamestnanca na základe DTO.
     * <p>
     * Metóda využíva {@code @MappingTarget} na priamu modifikáciu načítanej entity (Dirty Checking).
     * <p>
     * <strong>Bezpečnostné opatrenie:</strong> Explicitne ignoruje citlivé a systémové polia,
     * ktoré sa nesmú meniť cez tento endpoint (ID, KeycloakID, Status, Audit polia),
     * aby sa zachovala konzistencia a integrita dát.
     *
     * @param request DTO s aktualizovanými údajmi (meno, priezvisko, email...).
     * @param employee Cieľová entita, ktorá bude modifikovaná.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "addresses", ignore = true) // Adresy neriešime v tomto update
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "keycloakID", ignore = true)
    @Mapping(target = "startedWork", ignore = true)
    @Mapping(target = "endWork", ignore = true)
    @Mapping(target = "terminationReason", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(EmployeeUpdateRequest request, @MappingTarget Employee employee);
}