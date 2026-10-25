package com.nourtech.ecareplus.portail.application;

import com.nourtech.ecareplus.identity.domain.enumeration.Sexe;
import jakarta.validation.Validator;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.*;

/** Validates only submitted PATCH fields while preserving explicit null values. */
@Component
public class ProfilValidation {
    private final Validator validator;
    public ProfilValidation(Validator validator) { this.validator = validator; }

    public void verifier(Map<String, Object> input) {
        for (String key : List.of("nom", "prenoms", "sexe", "dateNaissance", "telephone", "email", "languePreferee",
                "adresse", "ville", "pays", "personneAContacter", "telephoneUrgence", "groupeSanguin", "allergies",
                "antecedents", "pathologie", "motDePasse", "professionnelId", "versionDocument")) {
            if (input.get(key) != null && !(input.get(key) instanceof String)) {
                throw new IllegalArgumentException(key + " doit être du texte");
            }
        }
        for (String key : List.of("nom", "prenoms", "languePreferee")) {
            if (input.containsKey(key) && (input.get(key) == null || input.get(key).toString().isBlank())) {
                throw new IllegalArgumentException(key + " est obligatoire");
            }
        }
        for (String key : List.of("tailleCm", "poidsKg")) {
            if (input.get(key) != null && !(input.get(key) instanceof Number)) throw new IllegalArgumentException(key + " doit être numérique");
        }
        if (input.get("sexe") != null) Sexe.valueOf(input.get("sexe").toString());
        LocalDate naissance = input.get("dateNaissance") == null ? null : LocalDate.parse(input.get("dateNaissance").toString());
        var fields = new Fields(text(input,"nom"),text(input,"prenoms"),text(input,"telephone"),text(input,"email"),
                text(input,"languePreferee"),text(input,"adresse"),text(input,"ville"),text(input,"pays"),
                text(input,"personneAContacter"),text(input,"telephoneUrgence"),text(input,"groupeSanguin"),
                text(input,"allergies"),text(input,"antecedents"),text(input,"versionDocument"),naissance);
        var failures = validator.validate(fields);
        if (!failures.isEmpty()) throw new jakarta.validation.ConstraintViolationException(failures);
    }

    private static String text(Map<String,Object> input,String key) {
        Object value = input.get(key);
        return value == null ? null : value.toString().trim();
    }

    public record Fields(@Size(max=100) String nom, @Size(max=150) String prenoms,
            @Size(max=30) String telephone, @Email @Size(max=254) String email,
            @Size(min=2,max=10) String languePreferee, @Size(max=300) String adresse,
            @Size(max=100) String ville, @Size(max=100) String pays,
            @Size(max=200) String personneAContacter, @Size(max=30) String telephoneUrgence,
            @Size(max=10) String groupeSanguin, @Size(max=10000) String allergies,
            @Size(max=10000) String antecedents, @Size(max=40) String versionDocument,
            @PastOrPresent LocalDate dateNaissance) {}
}
