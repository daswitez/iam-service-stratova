package com.solveria.iamservice.application.exception;

public class InactiveUserException extends AuthenticationException {
    public InactiveUserException() {
        super("User account is inactive");
    }
}
