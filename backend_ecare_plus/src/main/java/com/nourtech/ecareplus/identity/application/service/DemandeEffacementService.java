package com.nourtech.ecareplus.identity.application.service;
import com.nourtech.ecareplus.identity.domain.entity.*;
import com.nourtech.ecareplus.shared.application.service.AccesPatientService;
import com.nourtech.ecareplus.shared.application.exception.RessourceIntrouvableException;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import java.time.Instant;
import java.util.UUID;
@Service @RequiredArgsConstructor @Transactional
public class DemandeEffacementService {
    private final EntityManager em;private final AccesPatientService acces;
    public record DemandeVue(UUID id,String statut,Instant dateCreation,String information){}
    public DemandeVue demander(String motif){UUID acteur=acces.acteurId();if(motif!=null&&motif.length()>1000)throw new IllegalArgumentException("Motif trop long");em.find(Utilisateur.class,acteur,LockModeType.PESSIMISTIC_WRITE);
        var precedentes=em.createQuery("select d from DemandeEffacement d where d.utilisateurId=:id and d.statut='EN_ATTENTE'",DemandeEffacement.class).setParameter("id",acteur).getResultList();
        if(!precedentes.isEmpty())return vue(precedentes.getFirst());var demande=new DemandeEffacement(acteur,motif);em.persist(demande);em.flush();return vue(demande);
    }
    public DemandeVue consulter(UUID id){var demande=em.find(DemandeEffacement.class,id);if(demande==null)throw new RessourceIntrouvableException("Demande",id);if(!demande.getUtilisateurId().equals(acces.acteurId()))throw new AccessDeniedException("Demande privée");return vue(demande);}
    private DemandeVue vue(DemandeEffacement d){return new DemandeVue(d.getId(),d.getStatut(),d.getDateCreation(),"Demande enregistrée ; traitement selon les règles de conservation applicables, sans effacement automatique immédiat.");}
}
