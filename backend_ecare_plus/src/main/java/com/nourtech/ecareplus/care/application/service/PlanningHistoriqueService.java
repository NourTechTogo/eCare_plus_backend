package com.nourtech.ecareplus.care.application.service;
import com.nourtech.ecareplus.care.domain.entity.*;
import com.nourtech.ecareplus.care.domain.entity.RevisionPlanningPrise.Instantane;
import com.nourtech.ecareplus.care.infrastructure.persistence.ConfirmationPriseRepository;
import com.nourtech.ecareplus.identity.domain.entity.Patient;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional
public class PlanningHistoriqueService {
    private final NotificationPriseService rappels;
    private final EntityManager em;private final ObjectMapper json;private final ConfirmationPriseRepository confirmations;
    public List<RevisionPlanningPrise> revisions(PriseMedicament prise){
        var liste=em.createQuery("select r from RevisionPlanningPrise r where r.priseId=:id order by r.valideDepuis,r.id",RevisionPlanningPrise.class).setParameter("id",prise.getId()).getResultList();
        if(liste.isEmpty()){var initiale=new RevisionPlanningPrise(prise.getId(),prise.getTraitement().getDateDebut().atStartOfDay(ZoneId.of(prise.getFuseau())).toInstant(),json.writeValueAsString(Instantane.depuis(prise)));em.persist(initiale);return List.of(initiale);}return liste;
    }
    public Instantane lire(RevisionPlanningPrise revision){return json.readValue(revision.getContenu(),Instantane.class);}
    public void avant(PriseMedicament prise){verrouiller(prise.getTraitement());em.refresh(prise,LockModeType.PESSIMISTIC_WRITE);revisions(prise);}
    public void avant(Traitement traitement){verrouiller(traitement);for(var p:prises(traitement))revisions(p);}
    private void verrouiller(Traitement traitement){em.find(Patient.class,traitement.getDossier().getPatient().getId(),LockModeType.PESSIMISTIC_WRITE);em.refresh(traitement,LockModeType.PESSIMISTIC_WRITE);}
    public void apres(Traitement traitement){for(var p:prises(traitement))apres(p,false);}
    public void apres(PriseMedicament prise){apres(prise,false);}
    public void apres(PriseMedicament prise,boolean definitionInitiale){
        var liste=revisions(prise);var derniere=liste.stream().filter(r->r.getValideJusqua()==null).findFirst().orElseThrow(()->new IllegalStateException("Aucune révision active"));String contenu=json.writeValueAsString(Instantane.depuis(prise));
        if(!derniere.getContenu().equals(contenu)){
            confirmations.findAllByPriseMedicamentId(prise.getId()).forEach(rappels::invalider);
            Instant maintenant=Instant.now().truncatedTo(ChronoUnit.MICROS);
            if(definitionInitiale&&liste.size()==1&&confirmations.countByPriseMedicamentId(prise.getId())==0)derniere.definirInitial(prise.getTraitement().getDateDebut().atStartOfDay(ZoneId.of(prise.getFuseau())).toInstant(),contenu);
            else{if(maintenant.equals(derniere.getValideDepuis()))maintenant=maintenant.plusNanos(1000);derniere.clore(maintenant);em.flush();em.persist(new RevisionPlanningPrise(prise.getId(),maintenant,contenu));}
        }
        actualiserFutures(prise);
    }
    private void actualiserFutures(PriseMedicament prise){
        var actuelle=Instantane.depuis(prise);Instant maintenant=Instant.now();
        for(var c:confirmations.findAllByPriseMedicamentId(prise.getId())){
            if(c.getDateInitiale().isBefore(maintenant)&&c.getDatePrevue().isBefore(maintenant))continue;
            LocalDate jour=c.getDateInitiale().atZone(ZoneId.of(actuelle.fuseau())).toLocalDate();
            if(actuelle.prevu(jour)&&actuelle.date(jour).equals(c.getDateInitiale())){c.retablirOccurrence();if(c.getStatut()==com.nourtech.ecareplus.care.domain.enumeration.StatutPrise.ATTENTE||c.getStatut()==com.nourtech.ecareplus.care.domain.enumeration.StatutPrise.REPORTEE)c.appliquerInstantane(actuelle);}else c.annulerOccurrence();
        }em.flush();
    }
    private List<PriseMedicament> prises(Traitement traitement){return em.createQuery("select p from PriseMedicament p where p.traitement.id=:id order by p.id",PriseMedicament.class).setParameter("id",traitement.getId()).getResultList();}
    public void avant(Medicament medicament){for(var p:prises(medicament))avant(p);}
    public void apres(Medicament medicament){for(var p:prises(medicament))apres(p);}
    private List<PriseMedicament> prises(Medicament medicament){return em.createQuery("select p from PriseMedicament p where p.medicament.id=:id order by p.traitement.dossier.patient.id,p.id",PriseMedicament.class).setParameter("id",medicament.getId()).getResultList();}
}
