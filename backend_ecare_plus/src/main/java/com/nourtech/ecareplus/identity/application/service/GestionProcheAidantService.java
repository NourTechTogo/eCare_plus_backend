package com.nourtech.ecareplus.identity.application.service;

import com.nourtech.ecareplus.identity.application.command.CreerProcheAidantCommand;
import com.nourtech.ecareplus.identity.domain.entity.ProcheAidant;
import com.nourtech.ecareplus.identity.infrastructure.persistence.ProcheAidantRepository;
import com.nourtech.ecareplus.shared.application.exception.RessourceIntrouvableException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class GestionProcheAidantService {

    private final ProcheAidantRepository procheAidantRepository;
    private final VerificationUniciteCompteService uniciteCompteService;

    public GestionProcheAidantService(
            ProcheAidantRepository procheAidantRepository,
            VerificationUniciteCompteService uniciteCompteService
    ) {
        this.procheAidantRepository = procheAidantRepository;
        this.uniciteCompteService = uniciteCompteService;
    }

    public ProcheAidant creer(CreerProcheAidantCommand commande) {
        uniciteCompteService.verifier(commande.email(), commande.telephone());
        return procheAidantRepository.save(new ProcheAidant(
                commande.nom(),
                commande.prenoms(),
                commande.sexe(),
                commande.dateNaissance(),
                commande.telephone(),
                commande.email(),
                commande.motDePasseHash(),
                commande.languePreferee(),
                commande.lienAvecPatient()
        ));
    }

    @Transactional(readOnly = true)
    public ProcheAidant consulter(UUID procheAidantId) {
        return procheAidantRepository.findById(procheAidantId)
                .orElseThrow(() -> new RessourceIntrouvableException("ProcheAidant", procheAidantId));
    }
}
