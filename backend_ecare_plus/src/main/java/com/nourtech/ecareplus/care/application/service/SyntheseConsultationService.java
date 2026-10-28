package com.nourtech.ecareplus.care.application.service;
import com.nourtech.ecareplus.mobile.api.dto.*;
import com.nourtech.ecareplus.mobile.api.dto.SyntheseConsultationVue.*;
import com.nourtech.ecareplus.care.domain.entity.*;
import com.nourtech.ecareplus.alert.domain.entity.Alerte;
import com.nourtech.ecareplus.alert.api.dto.response.RegleAppliqueeVue;
import com.nourtech.ecareplus.communication.domain.entity.RendezVous;
import com.nourtech.ecareplus.identity.domain.entity.Patient;
import com.nourtech.ecareplus.shared.application.service.AccesPatientService;
import com.nourtech.ecareplus.shared.application.Empreintes;
import com.nourtech.ecareplus.shared.application.exception.*;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.domain.*;
import tools.jackson.databind.ObjectMapper;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional
public class SyntheseConsultationService {
    private final EntityManager em;private final AccesPatientService acces;private final DossierPersonnelService dossiers;
    private final PlanningTraitementService planning;private final ObjectMapper json;
    public record ResumeVue(UUID id,String titre,LocalDate du,LocalDate au,String fuseau,Instant genereLe,String format,String statut){}
    public SyntheseConsultationVue generer(GenererSyntheseConsultationRequest r){
        UUID acteur=acces.acteurId();Patient patient=acces.verifier(acteur,acteur);em.refresh(patient,LockModeType.PESSIMISTIC_WRITE);acces.verifier(acteur,acteur);
        ZoneId zone=zone(r.fuseau());long jours=ChronoUnit.DAYS.between(r.du(),r.au())+1;
        if(jours<1||jours>90||r.du().getYear()<1||r.au().getYear()>9999||r.au().isAfter(LocalDate.now(zone)))throw new IllegalArgumentException("Période attendue : 1 à 90 jours, sans date future");
        String hash=Empreintes.sha256(r.du()+":"+r.au()+":"+r.fuseau());
        var precedent=em.createQuery("select r from RapportSuivi r where r.dossier.patient.id=:patient and r.sourceRefSynthese=:source",RapportSuivi.class).setParameter("patient",acteur).setParameter("source",r.sourceRef()).getResultList();
        if(!precedent.isEmpty()){
            var rapport=precedent.getFirst();if(!hash.equals(rapport.getEmpreinteSynthese()))throw new ConflitMetierException("Référence de rapport réutilisée avec une autre période");
            return contenu(rapport);
        }
        Instant debut=r.du().atStartOfDay(zone).toInstant(),fin=r.au().plusDays(1).atStartOfDay(zone).toInstant(),maintenant=Instant.now();
        var mesures=borne(em.createQuery("select m from MesureSante m where m.patient.id=:patient and m.dateMesure>=:du and m.dateMesure<:au and m.dateMesure<=:maintenant order by m.dateMesure,m.id",MesureSante.class).setParameter("patient",acteur).setParameter("du",debut).setParameter("au",fin).setParameter("maintenant",maintenant).setMaxResults(5001).getResultList(),5000);
        var traitements=borne(em.createQuery("select t from Traitement t where t.dossier.patient.id=:patient and t.dateDebut<=:au and (t.dateFin is null or t.dateFin>=:du) order by t.dateDebut,t.id",Traitement.class).setParameter("patient",acteur).setParameter("du",r.du()).setParameter("au",r.au()).setMaxResults(501).getResultList(),500);
        var prises=borne(em.createQuery("select p from PriseMedicament p join fetch p.traitement t join fetch p.medicament where t.dossier.patient.id=:patient and t.dateDebut<=:au and (t.dateFin is null or t.dateFin>=:du) order by p.id",PriseMedicament.class).setParameter("patient",acteur).setParameter("du",r.du()).setParameter("au",r.au()).setMaxResults(5001).getResultList(),5000);
        var reponses=borne(em.createQuery("select c from ConfirmationPrise c where c.priseMedicament.traitement.dossier.patient.id=:patient and c.dateInitiale>=:du and c.dateInitiale<:au and c.statut in ('PRISE','OUBLIEE','REPORTEE') order by c.dateInitiale,c.id",ConfirmationPrise.class).setParameter("patient",acteur).setParameter("du",debut).setParameter("au",fin).setMaxResults(5001).getResultList(),5000);
        var alertes=borne(em.createQuery("select a from Alerte a left join fetch a.mesure m where a.patient.id=:patient and ((m is not null and m.dateMesure>=:du and m.dateMesure<:au and m.dateMesure<=:maintenant) or (m is null and a.dateCreation>=:du and a.dateCreation<:au and a.dateCreation<=:maintenant)) order by a.dateCreation,a.id",Alerte.class).setParameter("patient",acteur).setParameter("du",debut).setParameter("au",fin).setParameter("maintenant",maintenant).setMaxResults(5001).getResultList(),5000);
        var rdvs=borne(em.createQuery("select r from RendezVous r where r.patient.id=:patient and r.personnel=true and r.dateHeure>=:du and r.dateHeure<:au order by r.dateHeure,r.id",RendezVous.class).setParameter("patient",acteur).setParameter("du",debut).setParameter("au",fin).setMaxResults(5001).getResultList(),5000);
        // Materialize every lazy relation before persisting the report: its JSON columns are insert-only.
        var series=StatistiquesConsultation.calculer(mesures,r.du(),r.au(),zone);
        var traitementsVues=traitements.stream().map(t->new TraitementVue(t.getId(),t.getLibelle(),t.getDateDebut(),t.getDateFin(),t.getStatut().name(),t.getDossier().getStructure()==null,prises.stream().filter(p->p.getTraitement().getId().equals(t.getId())).map(p->new HoraireVue(p.getId(),p.getMedicament().getNomCommercial(),p.getMedicament().getDosage(),p.getMedicament().getUniteDosage(),p.getQuantite(),p.getUnite(),p.getHeurePrevue(),p.getFuseau(),p.joursPlanifies(),p.isActif())).toList())).toList();
        var reponsesVues=reponses.stream().map(planning::vue).toList();
        var alertesVues=alertes.stream().map(a0->new AlerteVue(a0.getId(),a0.getMesure()==null?a0.getDateCreation():a0.getMesure().getDateMesure(),a0.getOrigine().name(),a0.getTitre(),a0.getNiveauPriorite().name(),a0.getStatut().name(),RegleAppliqueeVue.depuis(a0.getRegleAppliquee()))).sorted(Comparator.comparing(AlerteVue::dateEvenement).thenComparing(AlerteVue::id)).toList();
        var rendezVousVues=rdvs.stream().map(v->new RendezVousVue(v.getId(),v.getMotif(),v.getDateHeure(),v.getFuseau(),v.getStatut().name(),v.getLieu(),v.getProfessionnelNom())).toList();
        var identite=new PatientVue(patient.getId(),patient.getNom(),patient.getPrenoms(),patient.getDateNaissance());
        long joursSansSaisie=jours-StatistiquesConsultation.joursAvecSaisie(mesures,zone);
        var rapport=new RapportSuivi(dossiers.obtenir(acteur),"Préparer ma consultation",r.du(),r.au(),"Synthèse descriptive du suivi personnel", "JSON");
        // Store the content before persist; the stable row UUID is added to the API view when reading.
        var resultat=new SyntheseConsultationVue(null,1,identite,r.du(),r.au(),r.fuseau(),rapport.getDateGeneration(),mesures.size(),joursSansSaisie,series,traitementsVues,reponsesVues,alertesVues,rendezVousVues,
            List.of("Synthèse de données enregistrées, sans diagnostic ni recommandation thérapeutique automatique.","Un jour sans saisie ne signifie pas qu'une mesure ou un traitement a été oublié.","Les traitements et horaires décrivent la configuration enregistrée à la génération ; leurs modifications passées ne sont pas reconstituées ici.","Les réponses aux prises concernent seulement les réponses déjà enregistrées ; aucun taux d'observance n'est déduit.","Les alertes liées à une mesure suivent sa date ; les alertes sans mesure suivent leur date de création.","L'agenda personnel ne constitue pas une réservation médicale confirmée.","Des saisies simultanées peuvent n'apparaître qu'au rapport suivant ; la synthèse décrit les données recueillies lors de sa génération."));
        rapport.publierSynthese(r.sourceRef(),hash,r.fuseau(),json.writeValueAsString(resultat));em.persist(rapport);em.flush();return resultat.avecIdentifiant(rapport.getId());
    }
    @Transactional(readOnly=true) public SyntheseConsultationVue consulter(UUID id){UUID patient=acces.acteurId();acces.verifier(patient,patient);var r=em.find(RapportSuivi.class,id);if(r==null)throw new RessourceIntrouvableException("Rapport",id);if(!r.getDossier().getPatient().getId().equals(patient))throw new AccessDeniedException("Rapport personnel d'un autre compte");if(r.getContenuSynthese()==null)throw new RessourceIntrouvableException("Synthèse",id);return contenu(r);}
    @Transactional(readOnly=true) public Page<ResumeVue> lister(Pageable page){
        UUID patient=acces.acteurId();acces.verifier(patient,patient);var p=PageRequest.of(page.isPaged()?page.getPageNumber():0,page.isPaged()?Math.min(50,page.getPageSize()):20);
        var liste=em.createQuery("select r from RapportSuivi r where r.dossier.patient.id=:p and r.contenuSynthese is not null order by r.dateGeneration desc,r.id",RapportSuivi.class).setParameter("p",patient).setFirstResult((int)Math.min(p.getOffset(),Integer.MAX_VALUE)).setMaxResults(p.getPageSize()).getResultList();
        long total=em.createQuery("select count(r) from RapportSuivi r where r.dossier.patient.id=:p and r.contenuSynthese is not null",Long.class).setParameter("p",patient).getSingleResult();
        return new PageImpl<>(liste.stream().map(r->new ResumeVue(r.getId(),r.getTitre(),r.getDateDebut(),r.getDateFin(),r.getFuseauSynthese(),r.getDateGeneration(),r.getFormat(),r.getStatut().name())).toList(),p,total);
    }
    private SyntheseConsultationVue contenu(RapportSuivi r){return json.readValue(r.getContenuSynthese(),SyntheseConsultationVue.class).avecIdentifiant(r.getId());}
    private static ZoneId zone(String fuseau){if(!ZoneId.getAvailableZoneIds().contains(fuseau))throw new IllegalArgumentException("Fuseau IANA invalide");return ZoneId.of(fuseau);}
    private static <T> List<T> borne(List<T> liste,int maximum){if(liste.size()>maximum)throw new IllegalArgumentException("Trop de données pour cette période ; choisir une période plus courte");return liste;}
}
