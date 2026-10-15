package com.nourtech.ecareplus.organization.api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record TerminerAffectationRequest(@NotNull LocalDate dateFin) {
}
