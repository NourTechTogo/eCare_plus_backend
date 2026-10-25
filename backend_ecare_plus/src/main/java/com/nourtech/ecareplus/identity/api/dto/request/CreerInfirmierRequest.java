package com.nourtech.ecareplus.identity.api.dto.request;

import com.nourtech.ecareplus.identity.domain.enumeration.Sexe;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record CreerInfirmierRequest(
        @NotBlank @Size(max = 60) String numeroProfessionnel,
        @NotBlank @Size(max = 100) String nom,
        @NotBlank @Size(max = 150) String prenoms,
        @NotNull Sexe sexe,
        @Past LocalDate dateNaissance,
        @Size(max = 30) String telephone,
        @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 12, max = 72) @jakarta.validation.constraints.Pattern(regexp="(?s)(?=.*[A-Za-z])(?=.*[0-9]).+") String motDePasse,
        @NotBlank @Size(max = 10) String languePreferee,
        @NotBlank @Size(max = 150) String specialite,
        @NotBlank @Size(max = 100) String service,
        @NotBlank @Size(max = 80) String niveauResponsabilite,
        @NotNull @PastOrPresent LocalDate datePriseFonction,
        @NotNull UUID structureId
) {
}

