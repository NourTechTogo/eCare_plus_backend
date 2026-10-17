package com.nourtech.ecareplus.identity.api.dto.request;

import com.nourtech.ecareplus.identity.domain.enumeration.Sexe;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreerPatientRequest(
        @NotBlank @Size(max = 40) String numeroPatient,
        @NotBlank @Size(max = 100) String nom,
        @NotBlank @Size(max = 150) String prenoms,
        @NotNull Sexe sexe,
        @Past LocalDate dateNaissance,
        @Size(max = 30) String telephone,
        @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 12, max = 128) String motDePasse,
        @NotBlank @Size(max = 10) String languePreferee
) {
}
