package com.nourtech.ecareplus.care.application.service;
import com.nourtech.ecareplus.care.infrastructure.persistence.*;
import com.nourtech.ecareplus.care.domain.entity.*;
import com.nourtech.ecareplus.care.domain.enumeration.StatutPrise;
import com.nourtech.ecareplus.shared.application.service.AccesPatientService;
import com.nourtech.ecareplus.shared.application.exception.*;
import com.nourtech.ecareplus.identity.domain.entity.Patient;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional
public class ObservanceService {
    private final AccesPatientService acces;private final PlanningTraitementService planning;private final ConfirmationPriseRepository confirmations;
    private final ReponsePriseOperationRepository operations;private final EntityManager em;private final ObjectMapper json;
    private final NotificationPriseService rappels;
    public record ReponsePriseCommande(String action,Instant reporteeAu,Instant dateAction,String sourceRef,long version){}
    public record ObservanceVue(long prisesPrevues,long prisesConfirmees,long prisesIgnorees,long prisesNonConfirmees,Double taux){}
    public PlanningTraitementService.OccurrenceVue confirmer(UUID acteur,UUID occurrenceId,ReponsePriseCommande commande){
        var occurrence=confirmations.findById(occurrenceId).orElseThrow(()->new RessourceIntrouvableException("Occurrence",occurrenceId));
        UUID patient=occurrence.getPriseMedicament().getTraitement().getDossier().getPatient().getId();
        acces.verifier(acteur,patient);if(!acteur.equals(patient))throw new org.springframework.security.access.AccessDeniedException("Réponse réservée au patient");
        em.find(Patient.class,patient,LockModeType.PESSIMISTIC_WRITE);em.refresh(occurrence);
        if(commande.sourceRef()==null||commande.sourceRef().isBlank()||commande.sourceRef().length()>128)throw new IllegalArgumentException("sourceRef requise (128 caractères maximum)");
        String requete=occurrenceId+":"+json.writeValueAsString(commande);var operation=operations.findByActeurIdAndSourceRef(acteur,commande.sourceRef());
        if(operation.isPresent()){if(!operation.get().getRequete().equals(requete))throw new ConflitMetierException("sourceRef réutilisée avec une autre réponse");return json.readValue(operation.get().getResultat(),PlanningTraitementService.OccurrenceVue.class);}
        if(occurrence.getVersion()!=commande.version())throw new ConflitMetierException("Occurrence modifiée : recharger sa version");
        if(commande.dateAction()==null||commande.dateAction().isAfter(Instant.now().plusSeconds(300)))throw new IllegalArgumentException("Horodatage de réponse invalide");
        switch(Objects.requireNonNullElse(commande.action(),"")){
            case "PRIS" -> occurrence.confirmer(null,Instant.now());
            case "IGNORER" -> occurrence.signalerOubli(null);
            case "REPORTER" -> occurrence.reporter(commande.reporteeAu());
            default -> throw new IllegalArgumentException("Action attendue : PRIS, REPORTER ou IGNORER");
        }
        rappels.invalider(occurrence);
        occurrence.actionClient(commande.dateAction());confirmations.flush();var resultat=planning.vue(occurrence);
        operations.save(new ReponsePriseOperation(acteur,commande.sourceRef(),requete,json.writeValueAsString(resultat)));return resultat;
    }
    public PlanningTraitementService.OccurrenceVue consulterOccurrence(UUID acteur,UUID id){
        var c=confirmations.findById(id).orElseThrow(()->new RessourceIntrouvableException("Occurrence",id));
        UUID patient=c.getPriseMedicament().getTraitement().getDossier().getPatient().getId();
        acces.verifier(acteur,patient);
        if(!acteur.equals(patient))throw new org.springframework.security.access.AccessDeniedException("Lecture réservée au patient");
        return planning.vue(c);
    }
    public List<PlanningTraitementService.OccurrenceVue> historique(UUID acteur,UUID patient,LocalDate du,LocalDate au){return planning.consulter(acteur,patient,du,au).occurrences();}
    public ObservanceVue calculer(UUID acteur,UUID patient,LocalDate du,LocalDate au){
        var liste=historique(acteur,patient,du,au).stream().filter(c->!c.annulee()&&!c.prevueLe().isAfter(Instant.now())).toList();
        long prevues=liste.size(),prises=liste.stream().filter(c->c.statut().equals(StatutPrise.PRISE.name())).count(),ignorees=liste.stream().filter(c->c.statut().equals(StatutPrise.OUBLIEE.name())).count();
        return new ObservanceVue(prevues,prises,ignorees,prevues-prises-ignorees,prevues==0?null:100d*prises/prevues);
    }
}
