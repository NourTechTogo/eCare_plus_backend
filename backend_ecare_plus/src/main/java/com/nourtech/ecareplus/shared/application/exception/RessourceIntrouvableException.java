package com.nourtech.ecareplus.shared.application.exception;

public class RessourceIntrouvableException extends RuntimeException {

    public RessourceIntrouvableException(String type, Object identifiant) {
        super(type + " introuvable : " + identifiant);
    }
}
