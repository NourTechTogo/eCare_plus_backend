package com.nourtech.ecareplus.calling.application.service;
import com.nourtech.ecareplus.calling.domain.entity.SessionAppel;
import com.nourtech.ecareplus.messaging.domain.entity.Conversation;
import com.nourtech.ecareplus.communication.domain.entity.Notification;
import com.nourtech.ecareplus.communication.domain.enumeration.CanalNotification;
import com.nourtech.ecareplus.communication.application.service.DistributionNotificationService;
import com.nourtech.ecareplus.identity.domain.entity.Utilisateur;
import com.nourtech.ecareplus.identity.domain.enumeration.StatutCompte;
import com.nourtech.ecareplus.auth.infrastructure.security.AutorisationRessources;
import com.nourtech.ecareplus.shared.application.exception.*;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.domain.*;
import java.time.*;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional
public class AppelTransactions {
    private final EntityManager em;private final AutorisationRessources autorisations;private final DistributionNotificationService notifications;
    private final com.nourtech.ecareplus.calling.application.port.FournisseurAppel fournisseur;
    public record AppelVue(UUID id,UUID conversationId,UUID initiateurId,UUID destinataireId,String type,String statut,Instant dateCreation,Instant expireLe,Instant debutLe,Instant finLe,Long dureeSecondes,boolean fermetureConfirmee,long version){}
    public record CreationPreparation(AppelVue vue,String salle,boolean creer){}
    public record SalleInfo(UUID id,String salle,UUID initiateur,UUID destinataire,String type){}
    public CreationPreparation preparer(UUID acteur,UUID conversationId,String type,String ref){
        if(!Set.of("AUDIO","VIDEO").contains(Objects.requireNonNullElse(type,""))||ref==null||ref.isBlank()||ref.length()>128)throw new IllegalArgumentException("Type d'appel et sourceRef requis");
        var c=conversation(acteur,conversationId);em.lock(c,LockModeType.PESSIMISTIC_WRITE);
        var existants=em.createQuery("select s from SessionAppel s where s.initiateurId=:a and s.sourceRef=:r",SessionAppel.class).setParameter("a",acteur).setParameter("r",ref).getResultList();
        if(!existants.isEmpty()){var s=existants.getFirst();if(!s.getConversation().getId().equals(conversationId)||!s.getType().equals(type))throw new ConflitMetierException("Clé d'appel réutilisée avec une autre demande");return new CreationPreparation(vue(s),s.getSalle(),s.getStatut().equals("PREPARATION"));}
        var enCours=em.createQuery("select count(s) from SessionAppel s where s.conversation.id=:c and s.statut in ('PREPARATION','EN_ATTENTE','ACCEPTE')",Long.class).setParameter("c",conversationId).getSingleResult();if(enCours>0)throw new ConflitMetierException("Un appel est déjà en cours");
        UUID destinataire=acteur.equals(c.getPatient().getId())?c.getProfessionnel().getId():c.getPatient().getId();var s=new SessionAppel(c,acteur,destinataire,type,ref);em.persist(s);em.flush();return new CreationPreparation(vue(s),s.getSalle(),true);
    }
    public AppelVue pret(UUID id){var s=em.find(SessionAppel.class,id,LockModeType.PESSIMISTIC_WRITE);if(!s.getStatut().equals("PREPARATION"))return vue(s);s.pret();var n=new Notification(em.find(Utilisateur.class,s.getDestinataireId()),"Appel entrant","Un appel vous attend dans eCare+.","APPEL_ENTRANT",CanalNotification.APPLICATION,true);n.envoyer(Instant.now());n.rattacherRessource("APPEL",s.getId());em.persist(n);notifications.programmer(n,s.getConversation().getPatient().getId());em.flush();return vue(s);}
    public void echec(UUID id){em.find(SessionAppel.class,id,LockModeType.PESSIMISTIC_WRITE).terminer("ECHEC");}
    public AppelVue action(UUID acteur,UUID id,String action){var s=exiger(acteur,id);expirer(s);
        switch(action){
            case "ACCEPTER" -> {if(!s.getDestinataireId().equals(acteur))throw new AccessDeniedException("Acceptation réservée au destinataire");s.accepter();}
            case "REFUSER" -> {if(!s.getDestinataireId().equals(acteur))throw new AccessDeniedException("Refus réservé au destinataire");if(!s.getStatut().equals("EN_ATTENTE")&&!s.getStatut().equals("REFUSE"))throw new ConflitMetierException("Appel déjà traité");s.terminer("REFUSE");}
            case "FIN" -> {if(s.getStatut().equals("ACCEPTE"))s.terminer("TERMINE");else if(s.getInitiateurId().equals(acteur)&&s.getStatut().equals("EN_ATTENTE"))s.terminer("ANNULE");else if(s.actif())throw new ConflitMetierException("Transition d'appel incompatible");}
            default -> throw new IllegalArgumentException("Action d'appel inconnue");
        }em.flush();return vue(s);
    }
    public com.nourtech.ecareplus.calling.application.port.FournisseurAppel.AccesAppel connexion(UUID acteur,UUID id){var s=exiger(acteur,id);expirer(s);if(!s.getStatut().equals("ACCEPTE"))throw new ConflitMetierException("Appel non accepté ou terminé");return fournisseur.autoriserParticipant(s.getSalle(),acteur,s.getType());}
    public SalleInfo fermeture(UUID id){var s=em.find(SessionAppel.class,id);return s!=null&&!s.actif()&&!s.isFermetureConfirmee()?salle(s):null;}
    public void confirmerFermeture(UUID id){var s=em.find(SessionAppel.class,id,LockModeType.PESSIMISTIC_WRITE);s.fermer();}
    public AppelVue consulter(UUID acteur,UUID id){var s=exiger(acteur,id);expirer(s);return vue(s);}
    public Page<AppelVue> historique(UUID acteur,UUID conversationId,Pageable page){conversation(acteur,conversationId);long total=em.createQuery("select count(s) from SessionAppel s where s.conversation.id=:c",Long.class).setParameter("c",conversationId).getSingleResult();var query=em.createQuery("select s from SessionAppel s where s.conversation.id=:c order by s.dateCreation desc,s.id",SessionAppel.class).setParameter("c",conversationId);if(page.isPaged())query.setFirstResult((int)page.getOffset()).setMaxResults(page.getPageSize());return new PageImpl<>(query.getResultList().stream().map(this::vue).toList(),page,total);}
    public List<UUID> entretenir(){var liste=em.createQuery("select s from SessionAppel s where s.statut in ('PREPARATION','EN_ATTENTE','ACCEPTE')",SessionAppel.class).getResultList();
        for(var s:liste){expirer(s);var c=s.getConversation();if(c.getPatient().getStatutCompte()!=StatutCompte.ACTIF||!autorisations.echangeAutorise(c.getPatient(),c.getProfessionnel()))s.terminer("ANNULE");}
        return em.createQuery("select s.id from SessionAppel s where s.fermetureConfirmee=false and s.statut not in ('PREPARATION','EN_ATTENTE','ACCEPTE') order by s.dateCreation",UUID.class).setMaxResults(100).getResultList();
    }
    public List<UUID> terminerUtilisateur(UUID acteur){
        var appels=em.createQuery("select s from SessionAppel s where (s.initiateurId=:a or s.destinataireId=:a) and s.statut in ('PREPARATION','EN_ATTENTE','ACCEPTE') order by s.id",SessionAppel.class).setParameter("a",acteur).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
        for(var s:appels)s.terminer("TERMINE");
        return appels.stream().map(SessionAppel::getId).toList();
    }
    private void expirer(SessionAppel s){if((s.getStatut().equals("EN_ATTENTE")||s.getStatut().equals("PREPARATION"))&&!s.getExpireLe().isAfter(Instant.now()))s.terminer("EXPIRE");if(s.getStatut().equals("ACCEPTE")&&s.getDateCreation().isBefore(Instant.now().minusSeconds(14400)))s.terminer("TERMINE");}
    private Conversation conversation(UUID acteur,UUID id){var c=em.find(Conversation.class,id);if(c==null)throw new RessourceIntrouvableException("Conversation",id);if(!autorisations.conversationAccessible(c,acteur))throw new AccessDeniedException("Conversation inaccessible");return c;}
    private SessionAppel exiger(UUID acteur,UUID id){var s=em.find(SessionAppel.class,id,LockModeType.PESSIMISTIC_WRITE);if(s==null)throw new RessourceIntrouvableException("Appel",id);conversation(acteur,s.getConversation().getId());return s;}
    private SalleInfo salle(SessionAppel s){return new SalleInfo(s.getId(),s.getSalle(),s.getInitiateurId(),s.getDestinataireId(),s.getType());}
    private AppelVue vue(SessionAppel s){return new AppelVue(s.getId(),s.getConversation().getId(),s.getInitiateurId(),s.getDestinataireId(),s.getType(),s.getStatut(),s.getDateCreation(),s.getExpireLe(),s.getDebutLe(),s.getFinLe(),s.getDureeSecondes(),s.isFermetureConfirmee(),s.getVersion());}
}
