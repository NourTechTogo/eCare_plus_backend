package com.nourtech.ecareplus.auth.application.exception;

public class AuthentificationException extends RuntimeException {

    public AuthentificationException() {
        super("Identifiant ou mot de passe incorrect");
    }
}
