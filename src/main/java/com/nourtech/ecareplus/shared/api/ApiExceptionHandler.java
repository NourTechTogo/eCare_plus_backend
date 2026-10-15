package com.nourtech.ecareplus.shared.api;

import com.nourtech.ecareplus.auth.application.exception.AuthentificationException;
import com.nourtech.ecareplus.shared.application.exception.ConflitMetierException;
import com.nourtech.ecareplus.shared.application.exception.RessourceIntrouvableException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(AuthentificationException.class)
    ProblemDetail authentification(AuthentificationException exception, HttpServletRequest request) {
        return probleme(HttpStatus.UNAUTHORIZED, "Échec de l'authentification", exception.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail accesRefuse(AccessDeniedException exception, HttpServletRequest request) {
        return probleme(HttpStatus.FORBIDDEN, "Accès refusé", exception.getMessage(), request);
    }

    @ExceptionHandler(RessourceIntrouvableException.class)
    ProblemDetail ressourceIntrouvable(
            RessourceIntrouvableException exception,
            HttpServletRequest request
    ) {
        return probleme(HttpStatus.NOT_FOUND, "Ressource introuvable", exception.getMessage(), request);
    }

    @ExceptionHandler({ConflitMetierException.class, IllegalStateException.class})
    ProblemDetail conflit(RuntimeException exception, HttpServletRequest request) {
        return probleme(HttpStatus.CONFLICT, "Conflit métier", exception.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail argumentInvalide(IllegalArgumentException exception, HttpServletRequest request) {
        return probleme(HttpStatus.BAD_REQUEST, "Requête invalide", exception.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        ProblemDetail detail = probleme(
                HttpStatus.BAD_REQUEST,
                "Données invalides",
                "Un ou plusieurs champs ne respectent pas le contrat de l'API.",
                request
        );
        List<Map<String, String>> erreurs = exception.getBindingResult().getFieldErrors().stream()
                .map(erreur -> Map.of(
                        "champ", erreur.getField(),
                        "message", erreur.getDefaultMessage() == null ? "valeur invalide" : erreur.getDefaultMessage()
                ))
                .toList();
        detail.setProperty("erreurs", erreurs);
        return detail;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail contrainte(ConstraintViolationException exception, HttpServletRequest request) {
        return probleme(HttpStatus.BAD_REQUEST, "Données invalides", exception.getMessage(), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail corpsIllisible(HttpMessageNotReadableException exception, HttpServletRequest request) {
        return probleme(
                HttpStatus.BAD_REQUEST,
                "Corps de requête invalide",
                "Le JSON est absent, mal formé ou contient une valeur incompatible.",
                request
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail integrite(DataIntegrityViolationException exception, HttpServletRequest request) {
        return probleme(
                HttpStatus.CONFLICT,
                "Conflit de données",
                "L'opération viole une contrainte d'intégrité.",
                request
        );
    }

    private static ProblemDetail probleme(
            HttpStatus statut,
            String titre,
            String message,
            HttpServletRequest request
    ) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(statut, message);
        detail.setTitle(titre);
        detail.setInstance(URI.create(request.getRequestURI()));
        return detail;
    }
}
