package com.nourtech.ecareplus.care.application.service;

import com.nourtech.ecareplus.care.api.dto.request.CreationPrescriptionRequest;
import com.nourtech.ecareplus.care.api.mapper.CareMapper;
import com.nourtech.ecareplus.care.application.command.ProgrammerPriseCommand;
import com.nourtech.ecareplus.care.domain.entity.PrescriptionOperation;
import com.nourtech.ecareplus.identity.domain.entity.*;
import com.nourtech.ecareplus.portail.application.PortailService;
import com.nourtech.ecareplus.shared.application.Empreintes;
import com.nourtech.ecareplus.shared.application.exception.ConflitMetierException;
import com.nourtech.ecareplus.shared.application.service.AccesPatientService;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class PrescriptionAtomiqueService {
    private final AccesPatientService acces;
    private final PortailService portail;
    private final TraitementService traitements;
    private final MedicamentService medicaments;
    private final PriseMedicamentService prises;
    private final PlanningTraitementService planning;
    private final CareMapper mapper;
    private final EntityManager em;
    private final ObjectMapper json;
    public record PrescriptionVue(UUID traitementId,UUID medicamentId,UUID priseId,long versionPrise,String fuseau,Set<Integer> joursSemaine){}

    @Transactional public PrescriptionVue creer(UUID patientId,CreationPrescriptionRequest commande){
        UUID acteur=acces.acteurId();
        if(!(em.find(Utilisateur.class,acteur) instanceof Medecin))throw new AccessDeniedException("Prescription réservée au médecin");
        var patient=acces.verifier(acteur,patientId);
        em.refresh(patient,LockModeType.PESSIMISTIC_WRITE);
        acces.verifier(acteur,patientId);
        var dossier=portail.dossier(patient);acces.verifierDossier(acteur,dossier);
        String hash=Empreintes.sha256(json.writeValueAsString(Empreintes.canonique(json.convertValue(commande,Map.class))));
        var precedentes=em.createQuery("select p from PrescriptionOperation p where p.acteurId=:acteur and p.patientId=:patient and p.sourceRef=:source",PrescriptionOperation.class)
                .setParameter("acteur",acteur).setParameter("patient",patientId).setParameter("source",commande.sourceRef()).getResultList();
        if(!precedentes.isEmpty()){
            var precedente=precedentes.getFirst();
            if(!precedente.getEmpreinte().equals(hash))throw new ConflitMetierException("Clé de prescription réutilisée avec un autre contenu");
            return json.readValue(precedente.getResultat(),PrescriptionVue.class);
        }
        try{ZoneId.of(commande.prise().fuseau());}catch(DateTimeException e){throw new IllegalArgumentException("Fuseau IANA invalide");}
        var traitement=traitements.ajouter(mapper.toCommand(dossier.getId(),commande.traitement()));
        var medicament=medicaments.ajouter(mapper.toCommand(commande.medicament()));
        var horaire=commande.prise();
        // No occurrence exists until the explicit zone/days are configured, even
        // inside this transaction. All writes and the receipt commit together.
        var prise=prises.programmer(new ProgrammerPriseCommand(traitement.getId(),medicament.getId(),horaire.quantite(),horaire.unite(),"A_CONFIGURER",horaire.heurePrevue(),null,horaire.voieAdministration(),null));
        em.flush();
        var configured=planning.configurer(acteur,prise.getId(),new PlanningTraitementService.PlanningCommande(prise.getVersion(),horaire.fuseau(),horaire.joursSemaine()));
        var resultat=new PrescriptionVue(traitement.getId(),medicament.getId(),prise.getId(),configured.version(),configured.fuseau(),configured.joursSemaine());
        em.persist(new PrescriptionOperation(acteur,patientId,commande.sourceRef(),hash,json.writeValueAsString(resultat)));
        return resultat;
    }
}
