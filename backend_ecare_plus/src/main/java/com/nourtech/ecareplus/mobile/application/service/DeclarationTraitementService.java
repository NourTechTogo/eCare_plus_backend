package com.nourtech.ecareplus.mobile.application.service;

import com.nourtech.ecareplus.care.application.service.*;
import com.nourtech.ecareplus.care.domain.entity.*;
import com.nourtech.ecareplus.identity.domain.entity.Patient;
import com.nourtech.ecareplus.mobile.api.dto.DeclarationTraitementRequest;
import com.nourtech.ecareplus.shared.application.Empreintes;
import com.nourtech.ecareplus.shared.application.exception.ConflitMetierException;
import com.nourtech.ecareplus.shared.application.service.AccesPatientService;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import java.util.*;

@Service @RequiredArgsConstructor
public class DeclarationTraitementService {
    private final AccesPatientService acces;
    private final DossierPersonnelService dossiers;
    private final PlanningHistoriqueService historique;
    private final EntityManager em;
    private final ObjectMapper json;

    @Transactional
    public PrescriptionAtomiqueService.PrescriptionVue declarer(DeclarationTraitementRequest commande) {
        UUID acteur = acces.acteurId();
        Patient patient = acces.verifier(acteur, acteur);
        em.refresh(patient, LockModeType.PESSIMISTIC_WRITE);
        acces.verifier(acteur, acteur);
        String source = commande.sourceRef().toString();
        String hash = Empreintes.sha256(json.writeValueAsString(Empreintes.canonique(json.convertValue(commande, Map.class))));
        var precedentes = em.createQuery("select p from PrescriptionOperation p where p.acteurId=:acteur and p.patientId=:patient and p.sourceRef=:source", PrescriptionOperation.class)
                .setParameter("acteur", acteur).setParameter("patient", acteur).setParameter("source", source).getResultList();
        if (!precedentes.isEmpty()) {
            var precedente = precedentes.getFirst();
            if (!precedente.getEmpreinte().equals(hash)) throw new ConflitMetierException("Référence de déclaration réutilisée avec un autre contenu");
            return json.readValue(precedente.getResultat(), PrescriptionAtomiqueService.PrescriptionVue.class);
        }
        var dossier = dossiers.obtenir(acteur);
        var traitement = new Traitement(dossier, commande.libelle(), "Traitement déclaré par le patient, non validé comme prescription eCare+.",
                commande.dateDebut(), commande.dateFin(), "Déclaré par le patient", null);
        em.persist(traitement);
        var m = commande.medicament();
        var medicament = new Medicament(m.nomCommercial(), m.nomCommercial(), m.forme(), m.dosage(), m.uniteDosage());
        em.persist(medicament);
        var horaire = commande.prise();
        var prise = new PriseMedicament(traitement, medicament, horaire.quantite(), horaire.unite(), "DECLARE_PATIENT",
                horaire.heurePrevue(), null, horaire.voieAdministration(), null);
        prise.configurerPlanning(horaire.fuseau(), horaire.joursSemaine());
        em.persist(prise);
        historique.revisions(prise);
        em.flush();
        var resultat = new PrescriptionAtomiqueService.PrescriptionVue(traitement.getId(), medicament.getId(), prise.getId(),
                prise.getVersion(), prise.getFuseau(), prise.joursPlanifies());
        em.persist(new PrescriptionOperation(acteur, acteur, source, hash, json.writeValueAsString(resultat)));
        return resultat;
    }
}
