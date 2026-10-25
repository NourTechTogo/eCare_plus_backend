package com.nourtech.ecareplus.care.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Set;
import java.util.TreeSet;

public record CreationPrescriptionRequest(
        @NotBlank @Size(max=128) String sourceRef,
        @NotNull @Valid AjouterTraitementRequest traitement,
        @NotNull @Valid AjouterMedicamentRequest medicament,
        @NotNull @Valid Horaire prise
) {
    public record Horaire(
            @NotNull @DecimalMin(value="0",inclusive=false) BigDecimal quantite,
            @NotBlank @Size(max=20) String unite,
            @NotNull LocalTime heurePrevue,
            @Size(max=100) String voieAdministration,
            @NotBlank @Size(max=80) String fuseau,
            @NotEmpty Set<@Min(1) @Max(7) Integer> joursSemaine
    ) {
        public Horaire { if(joursSemaine!=null) joursSemaine=java.util.Collections.unmodifiableSet(new TreeSet<>(joursSemaine)); }
    }
}
