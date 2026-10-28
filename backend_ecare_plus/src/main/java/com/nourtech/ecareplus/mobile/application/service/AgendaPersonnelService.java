package com.nourtech.ecareplus.mobile.application.service;

import com.nourtech.ecareplus.communication.domain.entity.*;
import com.nourtech.ecareplus.communication.domain.enumeration.*;
import com.nourtech.ecareplus.communication.infrastructure.persistence.RendezVousRepository;
import com.nourtech.ecareplus.identity.domain.entity.Patient;
import com.nourtech.ecareplus.mobile.api.dto.AgendaPersonnelRequest;
import com.nourtech.ecareplus.shared.application.Empreintes;
import com.nourtech.ecareplus.shared.application.exception.*;
import com.nourtech.ecareplus.shared.application.service.AccesPatientService;
import com.nourtech.ecareplus.communication.application.service.RappelAgendaService;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional
public class AgendaPersonnelService {
    private final AccesPatientService acces;
    private final RendezVousRepository rendezVous;
    private final EntityManager em;
    private final ObjectMapper json;
    private final RappelAgendaService rappels;

    public record Vue(UUID id,UUID patientId,String motif,Instant dateHeure,String fuseau,String lieu,
                      String professionnelNom,String commentaire,String statut,boolean personnel,long version,
                      Integer rappelMinutes,UUID rappelId,Instant rappelLe) {}

    @Transactional(readOnly=true)
    public Page<Vue> lister(Instant du,Instant au,boolean inclureAnnules,Pageable page) {
        UUID patient=acces.acteurId();acces.verifier(patient,patient);
        if((du==null)!=(au==null)||du!=null&&(!du.isBefore(au)||Duration.between(du,au).compareTo(Duration.ofDays(366))>0))
            throw new IllegalArgumentException("Période attendue : deux instants croissants, 366 jours au maximum");
        var bounded=PageRequest.of(page.isPaged()?page.getPageNumber():0,page.isPaged()?Math.min(100,page.getPageSize()):100,
                Sort.by("dateHeure","id"));
        return rendezVous.findAll((r,q,cb)->{
            var conditions=new ArrayList<jakarta.persistence.criteria.Predicate>();
            conditions.add(cb.equal(r.get("patient").get("id"),patient));conditions.add(cb.isTrue(r.get("personnel")));
            if(!inclureAnnules)conditions.add(cb.notEqual(r.get("statut"),StatutRendezVous.ANNULE));
            if(du!=null){conditions.add(cb.greaterThanOrEqualTo(r.get("dateHeure"),du));conditions.add(cb.lessThan(r.get("dateHeure"),au));}
            return cb.and(conditions.toArray(jakarta.persistence.criteria.Predicate[]::new));
        },bounded).map(this::vue);
    }

    @Transactional(readOnly=true)
    public Vue consulter(UUID id) { UUID patient=acces.acteurId();acces.verifier(patient,patient);return vue(charger(patient,id)); }

    public Vue creer(AgendaPersonnelRequest commande) {
        Patient patient=verrouiller();
        String hash=empreinte("CREER",null,commande);
        Vue precedente=recu(patient.getId(),commande.sourceRef(),hash);if(precedente!=null)return precedente;
        if(commande.version()!=null)throw new IllegalArgumentException("La version est réservée aux modifications");
        valider(commande);
        var rdv=RendezVous.personnel(patient,commande.motif(),commande.dateHeure(),commande.fuseau(),
                commande.lieu(),commande.professionnelNom(),commande.commentaire());
        em.persist(rdv);em.flush();
        planifier(rdv,commande.rappelMinutes());em.flush();
        return enregistrer(patient.getId(),commande.sourceRef(),hash,rdv);
    }

    public Vue modifier(UUID id,AgendaPersonnelRequest commande) {
        Patient patient=verrouiller();var rdv=charger(patient.getId(),id);
        String hash=empreinte("MODIFIER",id,commande);Vue precedente=recu(patient.getId(),commande.sourceRef(),hash);
        if(precedente!=null)return precedente;
        verifierVersion(rdv,commande.version());valider(commande);
        rappels.invalider(rdv);
        rdv.modifierPersonnel(commande.motif(),commande.dateHeure(),commande.fuseau(),commande.lieu(),
                commande.professionnelNom(),commande.commentaire());
        planifier(rdv,commande.rappelMinutes());em.flush();
        return enregistrer(patient.getId(),commande.sourceRef(),hash,rdv);
    }

    public Vue annuler(UUID id,UUID sourceRef,Long version) {
        Patient patient=verrouiller();var rdv=charger(patient.getId(),id);
        String hash=empreinte("ANNULER",id,Map.of("sourceRef",sourceRef,"version",version));
        Vue precedente=recu(patient.getId(),sourceRef,hash);if(precedente!=null)return precedente;
        verifierVersion(rdv,version);rdv.annuler();rappels.invalider(rdv);em.flush();
        return enregistrer(patient.getId(),sourceRef,hash,rdv);
    }

    private void planifier(RendezVous rdv,Integer avance) {
        rdv.rattacherRappel(null,null);
        if(avance==null)return;
        var rappel=new Rappel(rdv.getPatient(),null,TypeRappel.RENDEZ_VOUS,"Rappel de rendez-vous",
                "Un rappel de votre agenda personnel est disponible.",rdv.getDateHeure().minusSeconds(avance.longValue()*60),
                RecurrenceRappel.AUCUNE,CanalNotification.APPLICATION,null);
        rappel.rattacherAgendaPersonnel(rdv.getId());em.persist(rappel);rdv.rattacherRappel(rappel,avance);
    }
    private Patient verrouiller() {
        UUID id=acces.acteurId();Patient patient=acces.verifier(id,id);
        em.refresh(patient,LockModeType.PESSIMISTIC_WRITE);acces.verifier(id,id);return patient;
    }
    private RendezVous charger(UUID patient,UUID id) {
        var rdv=rendezVous.findById(id).orElseThrow(()->new RessourceIntrouvableException("Rendez-vous",id));
        if(!rdv.isPersonnel()||!rdv.getPatient().getId().equals(patient))throw new AccessDeniedException("Agenda personnel d'un autre compte");
        return rdv;
    }
    private static void verifierVersion(RendezVous rdv,Long version) {
        if(version==null)throw new IllegalArgumentException("Version du rendez-vous requise");
        if(rdv.getVersion()!=version)throw new ConflitMetierException("Rendez-vous modifié : recharger sa version");
    }
    private static void valider(AgendaPersonnelRequest r) {
        if(!ZoneId.getAvailableZoneIds().contains(r.fuseau()))throw new IllegalArgumentException("Fuseau IANA invalide");
        int annee=r.dateHeure().atZone(ZoneOffset.UTC).getYear();
        if(annee<1||annee>9999)throw new IllegalArgumentException("Date hors du format calendaire pris en charge");
    }
    private String empreinte(String action,UUID id,Object commande) {
        return Empreintes.sha256(action+":"+id+":"+json.writeValueAsString(Empreintes.canonique(json.convertValue(commande,Map.class))));
    }
    private Vue recu(UUID patient,UUID source,String hash) {
        var liste=em.createQuery("select o from OperationAgendaPatient o where o.patientId=:patient and o.sourceRef=:source",OperationAgendaPatient.class)
                .setParameter("patient",patient).setParameter("source",source).getResultList();
        if(liste.isEmpty())return null;
        if(!liste.getFirst().getEmpreinte().equals(hash))throw new ConflitMetierException("Référence d'agenda réutilisée avec une autre commande");
        return json.readValue(liste.getFirst().getResultat(),Vue.class);
    }
    private Vue enregistrer(UUID patient,UUID source,String hash,RendezVous rdv) {
        var resultat=vue(rdv);em.persist(new OperationAgendaPatient(patient,source,hash,json.writeValueAsString(resultat)));return resultat;
    }
    private Vue vue(RendezVous r) {
        var rappel=r.getRappel();
        return new Vue(r.getId(),r.getPatient().getId(),r.getMotif(),r.getDateHeure(),r.getFuseau(),r.getLieu(),
                r.getProfessionnelNom(),r.getCommentaire(),r.getStatut().name(),r.isPersonnel(),r.getVersion(),r.getRappelMinutes(),
                rappel==null?null:rappel.getId(),rappel==null?null:rappel.getDateHeurePrevue());
    }
}
