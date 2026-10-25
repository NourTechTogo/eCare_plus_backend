package com.nourtech.ecareplus.care.application.service;
import com.nourtech.ecareplus.care.domain.entity.*;
import com.nourtech.ecareplus.care.infrastructure.persistence.NoteCibleeRepository;
import com.nourtech.ecareplus.alert.domain.entity.Alerte;
import com.nourtech.ecareplus.messaging.domain.entity.*;
import com.nourtech.ecareplus.auth.infrastructure.security.AutorisationRessources;
import com.nourtech.ecareplus.shared.application.service.AccesPatientService;
import com.nourtech.ecareplus.shared.application.exception.*;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.domain.*;
import java.time.Instant;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional
public class NoteCibleeService {
    private final NoteCibleeRepository notes;private final AccesPatientService acces;private final AutorisationRessources autorisations;private final EntityManager em;
    public record CreationNote(String typeCible,UUID cibleId,String contenu,String visibilite){}
    public record ModificationNote(String contenu,String visibilite,long version){}
    public record NoteVue(UUID id,UUID auteurId,String typeCible,UUID cibleId,String contenu,String visibilite,Instant dateCreation,Instant dateModification,long version){}
    public NoteVue creer(UUID acteur,CreationNote commande){var patient=cible(acteur,commande.typeCible(),commande.cibleId());return vue(notes.saveAndFlush(new NoteCiblee(acteur,patient,commande.typeCible(),commande.cibleId(),commande.contenu(),commande.visibilite())));}
    public Page<NoteVue> lister(UUID acteur,String type,UUID id,Pageable page){var patient=cible(acteur,type,id);return notes.visibles(type,id,acteur,patient,acteur.equals(patient),page).map(this::vue);}
    public NoteVue modifier(UUID acteur,UUID id,ModificationNote commande){var note=exiger(acteur,id);if(note.getVersion()!=commande.version())throw new ConflitMetierException("Note modifiée : recharger sa version");note.modifier(commande.contenu(),commande.visibilite());notes.flush();return vue(note);}
    public void supprimer(UUID acteur,UUID id){exiger(acteur,id).supprimer();}
    private NoteCiblee exiger(UUID acteur,UUID id){var note=notes.findById(id).orElseThrow(()->new RessourceIntrouvableException("Note",id));if(note.getSupprimeLe()!=null)throw new RessourceIntrouvableException("Note",id);if(!note.getAuteurId().equals(acteur))throw new AccessDeniedException("Modification réservée à l'auteur");cible(acteur,note.getTypeCible(),note.getCibleId());return note;}
    private UUID cible(UUID acteur,String type,UUID id){if(type==null||id==null)throw new IllegalArgumentException("Cible requise");UUID patient;
        switch(type){
            case "MESURE" -> {var m=em.find(MesureSante.class,id);if(m==null)throw new RessourceIntrouvableException("Mesure",id);acces.verifierDossier(acteur,m.getDossier());patient=m.getPatient().getId();}
            case "ALERTE" -> {var a=em.find(Alerte.class,id);if(a==null)throw new RessourceIntrouvableException("Alerte",id);if(a.getMesure()!=null)acces.verifierDossier(acteur,a.getMesure().getDossier());else if(acces.structureClinique()!=null&&a.getProfessionnel()!=null&&!acces.structureClinique().equals(a.getProfessionnel().getStructure().getId()))throw new AccessDeniedException("Alerte d'une autre structure");patient=a.getPatient().getId();}
            case "MESSAGE" -> {var m=em.find(MessageConversation.class,id);if(m==null)throw new RessourceIntrouvableException("Message",id);if(!autorisations.conversationAccessible(m.getConversation(),acteur))throw new AccessDeniedException("Échange inaccessible");patient=m.getConversation().getPatient().getId();}
            case "APPEL" -> {var a=em.find(com.nourtech.ecareplus.calling.domain.entity.SessionAppel.class,id);if(a==null)throw new RessourceIntrouvableException("Appel",id);if(!autorisations.conversationAccessible(a.getConversation(),acteur))throw new AccessDeniedException("Appel inaccessible");patient=a.getConversation().getPatient().getId();}
            default -> throw new IllegalArgumentException("Type de cible non pris en charge");
        }acces.verifier(acteur,patient);return patient;
    }
    private NoteVue vue(NoteCiblee n){return new NoteVue(n.getId(),n.getAuteurId(),n.getTypeCible(),n.getCibleId(),n.getContenu(),n.getVisibilite(),n.getDateCreation(),n.getDateModification(),n.getVersion());}
}
