package com.nourtech.ecareplus.mobile.api.dto;

import com.nourtech.ecareplus.care.api.dto.request.CreationPrescriptionRequest.Horaire;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.UUID;

/** A patient records an existing treatment; this request is not a prescription. */
public record DeclarationTraitementRequest(
        @NotNull UUID sourceRef,
        @NotBlank @Size(max=200) String libelle,
        @NotNull LocalDate dateDebut,
        LocalDate dateFin,
        @NotNull @Valid MedicamentDeclare medicament,
        @NotNull @Valid Horaire prise
) {
    public record MedicamentDeclare(
            @NotBlank @Size(max=150) String nomCommercial,
            @NotBlank @Size(max=40) String dosage,
            @NotBlank @Size(max=20) String uniteDosage,
            @NotBlank @Size(max=60) String forme
    ) {}
}
