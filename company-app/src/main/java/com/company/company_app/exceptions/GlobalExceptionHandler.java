package com.company.company_app.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

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
}