package com.nourtech.ecareplus.mobile.api.dto;

import com.nourtech.ecareplus.care.domain.enumeration.TypeMesure;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public record CreerMesureMobileRequest(
        @NotNull TypeMesure typeMesure,
        @NotNull BigDecimal valeurPrincipale,
        BigDecimal valeurSecondaire,
        @NotBlank String unite,
        @NotNull Instant dateMesure,
        String contexteMesure,
        String commentaire,
        @jakarta.validation.constraints.Size(max=120) String sourceRef
) {
    public CreerMesureMobileRequest(TypeMesure t, BigDecimal a, BigDecimal b, String u, Instant d, String c, String note) {this(t,a,b,u,d,c,note,null);}
}
