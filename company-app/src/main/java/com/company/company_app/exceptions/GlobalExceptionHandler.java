package com.company.company_app.exceptions;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ProblemDetail handleUserAlreadyExists(UserAlreadyExistsException ex) {
        // 1. Vytvoríme štandardizovanú odpoveď 409 Conflict
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());

        // 2. Môžeme pridať metadáta (napr. typ chyby, čas)
        problem.setTitle("User Already Exists");
        problem.setType(URI.create("urn:problem-type:conflict"));
        problem.setProperty("timestamp", Instant.now());

        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "One or more fields are invalid.");

        problem.setTitle("Validation Failed");
        problem.setType(URI.create("urn:problem-type:validation-error"));
        problem.setProperty("timestamp", Instant.now());

        // RFC 9457 štandard: invalid-params
        Map<String, String> invalidParams = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            invalidParams.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        problem.setProperty("invalid-params", invalidParams);

        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error occurred.");

        problem.setTitle("Internal Server Error");
        problem.setType(URI.create("urn:problem-type:internal-error"));
        problem.setProperty("timestamp", Instant.now());

        return problem;
    }

    // 3. Ošetrenie DB Constraintov (Unikátnosť, Not Null, Foreign Key)
    @ExceptionHandler(DataIntegrityViolationException.class)
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
}