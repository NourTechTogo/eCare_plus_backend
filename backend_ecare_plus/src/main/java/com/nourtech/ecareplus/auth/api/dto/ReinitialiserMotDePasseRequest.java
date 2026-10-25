package com.nourtech.ecareplus.auth.api.dto;
import jakarta.validation.constraints.NotBlank; import jakarta.validation.constraints.Size;
public record ReinitialiserMotDePasseRequest(@NotBlank String jeton, @NotBlank @Size(min=12,max=72) @jakarta.validation.constraints.Pattern(regexp="(?s)(?=.*[A-Za-z])(?=.*[0-9]).+") String nouveauMotDePasse) {}

