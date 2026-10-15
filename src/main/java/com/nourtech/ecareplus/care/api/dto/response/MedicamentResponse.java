package com.nourtech.ecareplus.care.api.dto.response;

import java.util.UUID;

public record MedicamentResponse(
        UUID id,
        String nomCommercial,
        String denomination,
        String forme,
        String dosage,
        String uniteDosage,
        long version
) {
}
