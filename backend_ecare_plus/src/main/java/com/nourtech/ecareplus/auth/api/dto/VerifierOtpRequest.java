package com.nourtech.ecareplus.auth.api.dto;
import jakarta.validation.constraints.Email; import jakarta.validation.constraints.NotBlank; import jakarta.validation.constraints.Pattern;
public record VerifierOtpRequest(@NotBlank @Email String email, @Pattern(regexp="\\d{4}") String code) {}
