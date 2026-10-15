package com.nourtech.ecareplus.auth.api.dto;
import jakarta.validation.constraints.Email; import jakarta.validation.constraints.NotBlank;
public record DemanderOtpRequest(@NotBlank @Email String email) {}
