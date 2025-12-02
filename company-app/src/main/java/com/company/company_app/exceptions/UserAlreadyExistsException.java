package com.company.company_app.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// Voliteľné: @ResponseStatus povie Springu, že ak táto chyba vyletí a nikto ju nechytí, má dať 409.
// Ale my to aj tak odchytíme v Handleri pre krajšiu odpoveď.
@ResponseStatus(HttpStatus.CONFLICT)
public class UserAlreadyExistsException extends RuntimeException {

    public UserAlreadyExistsException(String message) {
        super(message);
    }
}