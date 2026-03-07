package com.solveria.iamservice.application.exception;

import com.solveria.core.shared.exceptions.BusinessRuleViolationException;

public class UserAlreadyExistsException extends BusinessRuleViolationException {
    public UserAlreadyExistsException(String email) {
        super("email.already.taken");
    }
}
