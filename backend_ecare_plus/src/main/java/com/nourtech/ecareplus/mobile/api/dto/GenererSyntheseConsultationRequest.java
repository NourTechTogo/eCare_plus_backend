package com.nourtech.ecareplus.mobile.api.dto;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.UUID;
public record GenererSyntheseConsultationRequest(@NotNull UUID sourceRef,@NotNull LocalDate du,@NotNull LocalDate au,
        @NotBlank @Size(max=80) String fuseau){}
