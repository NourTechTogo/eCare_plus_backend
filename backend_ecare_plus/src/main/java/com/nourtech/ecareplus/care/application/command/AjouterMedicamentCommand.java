package com.nourtech.ecareplus.care.application.command;

public record AjouterMedicamentCommand(
        String nomCommercial,
        String denomination,
        String forme,
        String dosage,
        String uniteDosage
) {
}
