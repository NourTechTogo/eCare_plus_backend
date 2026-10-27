package com.nourtech.ecareplus.care.application.service;

import com.nourtech.ecareplus.care.domain.entity.*;
import com.nourtech.ecareplus.care.domain.enumeration.StatutTraitement;
import com.nourtech.ecareplus.care.infrastructure.persistence.*;
import com.nourtech.ecareplus.identity.domain.entity.*;
import com.nourtech.ecareplus.shared.application.service.AccesPatientService;
import com.nourtech.ecareplus.shared.application.exception.*;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional
public class PlanningTraitementService {
    private final AccesPatientService acces;private final PriseMedicamentRepository prises;private final ConfirmationPriseRepository confirmations;private final EntityManager em;
    private final com.nourtech.ecareplus.communication.application.service.ChangementTraitementService changements;
    private final PlanningHistoriqueService historique;
    public record OccurrenceVue(UUID id,UUID traitementId,UUID priseId,Instant prevueLe,Instant reporteeAu,BigDecimal quantite,String unite,String medicament,String dosage,String uniteDosage,String forme,String voieAdministration,String statut,boolean annulee,long version,String fuseau,Instant dateActionClient,Instant dateConfirmation){}
    public record TraitementPlanningVue(UUID id,String libelle,LocalDate dateDebut,LocalDate dateFin,String statut,String consignes,boolean declarationPersonnelle,List<PrisePlanningVue> prises){}
    public record PrisePlanningVue(UUID id,String frequence,LocalTime heurePrevue,String fuseau,Set<Integer> joursSemaine,boolean configurationRequise,long version){}
    public record PlanningVue(String version,LocalDate du,LocalDate au,List<TraitementPlanningVue> traitements,List<OccurrenceVue> occurrences){}
    public record PlanningCommande(long version,String fuseau,Set<Integer> joursSemaine){}
    public PlanningVue consulter(UUID acteurId,UUID patientId,LocalDate du,LocalDate au){
        verifierPeriode(du,au);acces.verifier(acteurId,patientId);em.find(Patient.class,patientId,LockModeType.PESSIMISTIC_WRITE);
        UUID structure=acces.structureClinique();var liste=prises.planningPatientStructure(patientId,structure);
        var existantes=new HashSet<String>();for(var c:confirmations.periodeStructure(patientId,structure,debut(du),fin(au)))existantes.add(c.getPriseMedicament().getId()+":"+c.getDateInitiale());
        for(var p:liste)for(var revision:historique.revisions(p)){var instantane=historique.lire(revision);
            for(LocalDate jour=du;!jour.isAfter(au);jour=jour.plusDays(1)){
                if(!instantane.prevu(jour))continue;Instant date=instantane.date(jour);
                if(revision.applicable(date)&&existantes.add(p.getId()+":"+date)){var occurrence=new ConfirmationPrise(p,date);occurrence.appliquerInstantane(instantane);confirmations.save(occurrence);}
            }
        }
        confirmations.flush();var occurrences=confirmations.periodeStructure(patientId,structure,debut(du),fin(au)).stream().filter(c->{var jour=c.getDateInitiale().atZone(ZoneId.of(Objects.requireNonNullElse(c.getFuseauPrevu(),c.getPriseMedicament().getFuseau()))).toLocalDate();return !jour.isBefore(du)&&!jour.isAfter(au);}).map(this::vue).toList();
        var groupes=new LinkedHashMap<UUID,List<PriseMedicament>>();for(var p:liste)groupes.computeIfAbsent(p.getTraitement().getId(),id->new ArrayList<>()).add(p);
        var traitements=groupes.values().stream().map(g->{var t=g.getFirst().getTraitement();return new TraitementPlanningVue(t.getId(),t.getLibelle(),t.getDateDebut(),t.getDateFin(),t.getStatut().name(),t.getConsignesGenerales(),t.getDossier().getStructure()==null,g.stream().map(p->new PrisePlanningVue(p.getId(),p.getFrequence(),p.getHeurePrevue(),p.getFuseau(),p.joursPlanifies(),p.joursPlanifies().isEmpty(),p.getVersion())).toList());}).toList();
        String version=liste.stream().map(p->p.getId()+":"+p.getVersion()+":"+p.getTraitement().getVersion()).collect(java.util.stream.Collectors.joining("|"));
        return new PlanningVue(version,du,au,traitements,occurrences);
    }
    public PrisePlanningVue configurer(UUID acteur,UUID priseId,PlanningCommande commande){
        var prise=prises.findById(priseId).orElseThrow(()->new RessourceIntrouvableException("Prise",priseId));var patient=prise.getTraitement().getDossier().getPatient().getId();
        acces.verifierDossier(acteur,prise.getTraitement().getDossier());if(!(em.find(Utilisateur.class,acteur) instanceof Medecin))throw new AccessDeniedException("Prescription réservée au médecin");
        historique.avant(prise);if(prise.getVersion()!=commande.version())throw new ConflitMetierException("Planning modifié : recharger sa version");
        boolean definitionInitiale=prise.getJoursSemaine()==null&&confirmations.countByPriseMedicamentId(prise.getId())==0;
        prise.configurerPlanning(commande.fuseau(),commande.joursSemaine());historique.apres(prise,definitionInitiale);prises.flush();changements.notifier(prise);return new PrisePlanningVue(prise.getId(),prise.getFrequence(),prise.getHeurePrevue(),prise.getFuseau(),prise.joursPlanifies(),false,prise.getVersion());
    }
    public OccurrenceVue vue(ConfirmationPrise c){var p=c.getPriseMedicament();var m=p.getMedicament();return new OccurrenceVue(c.getId(),p.getTraitement().getId(),p.getId(),c.getDateInitiale(),!c.getDatePrevue().equals(c.getDateInitiale())?c.getDatePrevue():null,c.getQuantitePrevue(),c.getUnitePrevue(),Objects.requireNonNullElse(c.getMedicamentNom(),m.getNomCommercial()),Objects.requireNonNullElse(c.getMedicamentDosage(),m.getDosage()),Objects.requireNonNullElse(c.getMedicamentUniteDosage(),m.getUniteDosage()),Objects.requireNonNullElse(c.getMedicamentForme(),m.getForme()),c.getVoiePrevue(),c.getStatut().name(),c.isAnnulee(),c.getVersion(),Objects.requireNonNullElse(c.getFuseauPrevu(),p.getFuseau()),c.getDateActionClient(),c.getDateConfirmation());}
    public static void verifierPeriode(LocalDate du,LocalDate au){if(du==null||au==null||au.isBefore(du)||java.time.temporal.ChronoUnit.DAYS.between(du,au)>89)throw new IllegalArgumentException("Période attendue : 1 à 90 jours");}
    public static Instant debut(LocalDate du){return du.atStartOfDay(ZoneOffset.UTC).minusSeconds(14*3600).toInstant();}
    public static Instant fin(LocalDate au){return au.plusDays(1).atStartOfDay(ZoneOffset.UTC).plusSeconds(14*3600).toInstant();}
}
