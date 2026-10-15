package com.nourtech.ecareplus.auth.api.dto;
import jakarta.validation.constraints.NotBlank; import jakarta.validation.constraints.Size;
public record ReinitialiserMotDePasseRequest(@NotBlank String jeton, @NotBlank @Size(min=8,max=128) String nouveauMotDePasse) {}
