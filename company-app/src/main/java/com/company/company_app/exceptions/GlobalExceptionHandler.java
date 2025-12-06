package com.company.company_app.exceptions;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Centrálny handler pre správu výnimiek v celej aplikácii.
 * <p>
 * Táto trieda zachytáva výnimky vyhodené z Controllerov a transformuje ich na
 * štandardizovanú odpoveď podľa špecifikácie <strong>RFC 7807: Problem Details for HTTP APIs</strong>.
 * <p>
 * Použitie triedy {@link ProblemDetail} (novinka v Spring Boot 3 / Spring Framework 6)
 * zabezpečuje konzistentnú štruktúru chybových odpovedí pre klientov API.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Spracováva biznis výnimku, keď sa pokúšame vytvoriť zdroj, ktorý už existuje.
     *
     * @param ex Výnimka obsahujúca detail konfliktu.
     * @return {@link ProblemDetail} so statusom {@code 409 Conflict}.
     */
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ProblemDetail handleUserAlreadyExists(UserAlreadyExistsException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());

        problem.setTitle("User Already Exists");
        problem.setType(URI.create("urn:problem-type:conflict"));
        problem.setProperty("timestamp", Instant.now());

        return problem;
    }

    /**
     * Spracováva validačné chyby vzniknuté pri anotácii {@code @Valid} na vstupných DTO.
     * <p>
     * Extrahuje zoznam chybných polí a pridáva ich do sekcie {@code invalid-params}
     * v odpovedi, čo umožňuje klientovi presne identifikovať chyby vo formulári.
     *
     * @param ex Výnimka obsahujúca výsledky validácie (BindingResult).
     * @return {@link ProblemDetail} so statusom {@code 400 Bad Request}.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "One or more fields are invalid.");

        problem.setTitle("Validation Failed");
        problem.setType(URI.create("urn:problem-type:validation-error"));
        problem.setProperty("timestamp", Instant.now());

        Map<String, String> invalidParams = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            invalidParams.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        problem.setProperty("invalid-params", invalidParams);

        return problem;
    }

    /**
     * Catch-all handler pre neočakávané chyby (NullPointerException, a pod.).
     * <p>
     * Z bezpečnostných dôvodov <strong>nevracia</strong> stack trace ani detailnú správu chyby,
     * aby nedošlo k úniku citlivých informácií o infraštruktúre.
     *
     * @param ex Akákoľvek neošetrená výnimka.
     * @return {@link ProblemDetail} so statusom {@code 500 Internal Server Error}.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error occurred.");

        problem.setTitle("Internal Server Error");
        problem.setType(URI.create("urn:problem-type:internal-error"));
        problem.setProperty("timestamp", Instant.now());

        return problem;
    }

    /**
     * Spracováva chyby integrity databázy (Unique constraint, Foreign key, Not Null).
     * <p>
     * Táto metóda analyzuje správu z databázového ovládača a prekladá ju na
     * zrozumiteľnejší HTTP status a správu.
     * <p>
     * <em>Poznámka: Logika parsovania správ je závislá na použitom DB ovládači (tu prispôsobené pre PostgreSQL).</em>
     *
     * @param ex Výnimka vyhodená Hibernate/JPA vrstvou pri porušení integrity.
     * @return {@link ProblemDetail} so statusom {@code 409 Conflict} alebo {@code 400 Bad Request}.
     */    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDatabaseViolation(DataIntegrityViolationException ex) {
        // Získame detailnú správu z databázy (Postgres drivera)
        String rootMsg = ex.getMostSpecificCause().getMessage();

        HttpStatus status = HttpStatus.CONFLICT;
        String detail = "Database constraint violation.";

        if (rootMsg != null) {
            // Detekcia duplicity (ak by sme to nezachytili v servise)
            if (rootMsg.contains("duplicate key value")) {
                status = HttpStatus.CONFLICT;
                detail = "A record with this unique key already exists.";
            }
            // Detekcia chýbajúcich povinných polí (NOT NULL)
            else if (rootMsg.contains("violates not-null constraint")) {
                status = HttpStatus.BAD_REQUEST;
                detail = "Required database field is missing.";
            }
            // Detekcia cudzích kľúčov (Foreign Key)
            else if (rootMsg.contains("violates foreign key constraint")) {
                status = HttpStatus.BAD_REQUEST;
                detail = "Referenced record does not exist.";
            }
        }

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle("Data Integrity Violation");
        problem.setType(URI.create("urn:problem-type:data-integrity"));
        // Pre istotu pošleme aj root message (v DEV prostredí užitočné, v PROD možno skryť)
        problem.setProperty("db_error", rootMsg);
        problem.setProperty("timestamp", Instant.now());

        return problem;
    }

    /**
     * Spracováva prípad, keď požadovaný zdroj nebol nájdený.
     *
     * @param ex Výnimka obsahujúca správu, čo sa nenašlo.
     * @return {@link ProblemDetail} so statusom {@code 404 Not Found}.
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(UserNotFoundException ex) {
        // Vytvoríme štandardnú 404 odpoveď
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );

        problem.setTitle("User Not Found");
        problem.setType(URI.create("urn:problem-type:resource-not-found"));
        problem.setProperty("timestamp", Instant.now());

        return problem;
    }

    /**
     * Spracováva bezpečnostné výnimky (pokus o prístup bez oprávnenia).
     *
     * @param ex Výnimka vyhodená Spring Security.
     * @return {@link ProblemDetail} so statusom {@code 403 Forbidden}.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                "You do not have permission to access this resource."
        );
        problem.setTitle("Access Denied");
        problem.setType(URI.create("urn:problem-type:access-denied"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    /**
     * Spracováva chyby pri deserializácii JSONu (napr. malformovaný JSON, zlý dátový typ).
     *
     * @param ex Výnimka vyhodená Jackson mapperom.
     * @return {@link ProblemDetail} so statusom {@code 400 Bad Request}.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleJsonError(HttpMessageNotReadableException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Invalid JSON request body."
        );
        problem.setTitle("JSON Parse Error");
        problem.setType(URI.create("urn:problem-type:bad-request"));
        // Pre debug pošli aj správu (v PROD opatrne)
        problem.setProperty("error_detail", ex.getMessage());
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    /**
     * Spracováva chyby prichádzajúce z externých klientov (napr. Keycloak Admin Client).
     * <p>
     * Ak Keycloak vráti 400 (Bad Request) alebo 409 (Conflict), táto metóda
     * extrahuje správu z tela odpovede a pošle ju klientovi.
     */
    @ExceptionHandler(jakarta.ws.rs.ClientErrorException.class)
    public ProblemDetail handleKeycloakClientError(jakarta.ws.rs.ClientErrorException ex) {
        int statusCode = ex.getResponse().getStatus();

        // Skúsime prečítať detailnú správu z Keycloaku (napr. "Password length too short")
        String errorMsg = "External system error";
        try {
            // Pozor: readEntity spotrebuje stream, dá sa prečítať len raz
            errorMsg = ex.getResponse().readEntity(String.class);
        } catch (Exception e) {
            // Ak sa nepodarí prečítať body, použijeme default správu
            errorMsg = ex.getMessage();
        }

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.valueOf(statusCode),
                errorMsg != null && !errorMsg.isBlank() ? errorMsg : "Error from identity provider"
        );

        problem.setTitle("Identity Provider Error");
        problem.setType(URI.create("urn:problem-type:external-service-error"));
        problem.setProperty("service", "Keycloak");
        problem.setProperty("timestamp", Instant.now());

        return problem;
    }

    /**
     * Spracováva manuálne validácie a logické chyby vyhodené zo Service vrstvy.
     * Napr. throw new IllegalArgumentException("Dátum ukončenia musí byť po dátume začiatku");
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        assert problem.getDetail() != null;
        if (problem.getDetail().contains("username")){
            problem.setTitle("Invalid Username");
            problem.setType(URI.create("urn:problem-type:invalid-username"));
            problem.setProperty("timestamp", Instant.now());
            problem.setDetail("Username must be at least 3 characters long and contain only letters, numbers and underscores.");
        }
        problem.setTitle("Invalid Argument");
        problem.setType(URI.create("urn:problem-type:illegal-argument"));
        problem.setProperty("timestamp", Instant.now());

        return problem;
    }

    /**
     * Spracováva chyby, keď klient pošle parameter v zlom formáte.
     * Napríklad: ID má byť UUID, ale príde text "abc".
     */
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex) {
        String detail = String.format("Parameter '%s' has invalid format. Expected type: '%s'.",
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                detail
        );

        problem.setTitle("Invalid Parameter Format");
        problem.setType(URI.create("urn:problem-type:type-mismatch"));
        problem.setProperty("parameter", ex.getName());
        problem.setProperty("timestamp", Instant.now());

        return problem;
    }
}