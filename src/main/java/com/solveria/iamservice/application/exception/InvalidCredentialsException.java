package com.solveria.iamservice.application.exception;

public class InvalidCredentialsException extends AuthenticationException {
    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
