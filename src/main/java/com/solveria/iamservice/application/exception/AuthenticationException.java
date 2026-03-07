package com.solveria.iamservice.application.exception;

import com.solveria.core.shared.exceptions.SolverException;
import java.util.Map;

public class AuthenticationException extends SolverException {
    public AuthenticationException(String message) {
        super("error.auth.unauthorized", Map.of(), message);
    }
}
