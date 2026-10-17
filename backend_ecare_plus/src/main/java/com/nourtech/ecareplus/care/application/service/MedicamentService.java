package com.nourtech.ecareplus.care.application.service;

import com.nourtech.ecareplus.care.application.command.AjouterMedicamentCommand;
import com.nourtech.ecareplus.care.domain.entity.Medicament;
import com.nourtech.ecareplus.care.infrastructure.persistence.MedicamentRepository;
import com.nourtech.ecareplus.shared.application.exception.RessourceIntrouvableException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class MedicamentService {

    private final MedicamentRepository medicamentRepository;

    public MedicamentService(MedicamentRepository medicamentRepository) {
        this.medicamentRepository = medicamentRepository;
    }

    public Medicament ajouter(AjouterMedicamentCommand commande) {
        return medicamentRepository.save(new Medicament(
                commande.nomCommercial(),
                commande.denomination(),
                commande.forme(),
                commande.dosage(),
                commande.uniteDosage()
        ));
    }

    public Medicament modifier(UUID medicamentId, String nomCommercial, String denomination, String forme, String dosage, String uniteDosage) {
        Medicament medicament = charger(medicamentId);
        medicament.modifier(nomCommercial, denomination, forme, dosage, uniteDosage);
        return medicament;
    }

    public Medicament consulter(UUID medicamentId) {
        return charger(medicamentId);
    }

    private Medicament charger(UUID medicamentId) {
        return medicamentRepository.findById(medicamentId)
                .orElseThrow(() -> new RessourceIntrouvableException("Medicament", medicamentId));
    }
}
