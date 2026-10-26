package com.nourtech.ecareplus.messaging.application.service;

import com.nourtech.ecareplus.auth.infrastructure.security.AutorisationRessources;
import com.nourtech.ecareplus.identity.domain.entity.*;
import com.nourtech.ecareplus.identity.infrastructure.persistence.*;
import com.nourtech.ecareplus.messaging.domain.entity.*;
import com.nourtech.ecareplus.messaging.infrastructure.persistence.*;
import com.nourtech.ecareplus.shared.application.exception.RessourceIntrouvableException;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class MessagerieService {
    private final ConversationRepository conversations;
    private final MessageConversationRepository messages;
    private final PatientRepository patients;
    private final ProfessionnelSanteRepository professionnels;
    private final UtilisateurRepository utilisateurs;
    private final AutorisationRessources autorisations;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager em;
    @org.springframework.beans.factory.annotation.Autowired private com.nourtech.ecareplus.communication.application.service.DistributionNotificationService distribution;
    @org.springframework.beans.factory.annotation.Autowired private PieceJointeService pieces;

    public MessagerieService(ConversationRepository conversations, MessageConversationRepository messages,
            PatientRepository patients, ProfessionnelSanteRepository professionnels,
            UtilisateurRepository utilisateurs, AutorisationRessources autorisations) {
        this.conversations = conversations; this.messages = messages; this.patients = patients;
        this.professionnels = professionnels; this.utilisateurs = utilisateurs; this.autorisations = autorisations;
    }

    @Transactional
    public Conversation ouvrir(UUID patientId, UUID professionnelId) {
        Utilisateur actor=autorisations.acteur();
        if (!(actor instanceof Patient && actor.getId().equals(patientId)) && !(actor instanceof ProfessionnelSante && actor.getId().equals(professionnelId))) throw new AccessDeniedException("Participants imposés par la session");
        Patient patient = patients.findById(patientId).orElseThrow(() -> new RessourceIntrouvableException("Patient", patientId));
        ProfessionnelSante professionnel = professionnels.findById(professionnelId)
                .orElseThrow(() -> new RessourceIntrouvableException("Professionnel", professionnelId));
        verifierEchange(patient, professionnel);
        return conversations.findByPatientIdAndProfessionnelId(patientId, professionnelId)
                .orElseGet(() -> conversations.save(new Conversation(patient, professionnel)));
    }

    public List<Conversation> lister(UUID utilisateurId) {
        return conversations.findAllByPatientIdOrProfessionnelId(utilisateurId, utilisateurId).stream()
                .filter(c -> autorisations.conversationAccessible(c, utilisateurId)).toList();
    }

    public Page<MessageConversation> messages(UUID utilisateurId, UUID conversationId, Pageable pageable) {
        verifier(conversation(conversationId), utilisateurId);
        Pageable stable=pageable.isUnpaged()?Pageable.unpaged(Sort.by("envoyeLe","id")):PageRequest.of(pageable.getPageNumber(),pageable.getPageSize(),Sort.by("envoyeLe","id"));
        return messages.findAllByConversationId(conversationId, stable);
    }

    public MessageConversation dernier(UUID conversationId){return messages.findFirstByConversationIdOrderByEnvoyeLeDescIdDesc(conversationId).orElse(null);}
    public long nonLus(UUID conversationId,UUID utilisateurId){return messages.countByConversationIdAndExpediteurIdNotAndLuLeIsNull(conversationId,utilisateurId);}

    @Transactional
    public MessageConversation envoyer(UUID utilisateurId, UUID conversationId, String contenu) {
        return envoyer(utilisateurId,conversationId,contenu,List.of());
    }
    @Transactional
    public MessageConversation envoyer(UUID utilisateurId,UUID conversationId,String contenu,List<UUID> piecesJointes) {
        return envoyer(utilisateurId,conversationId,contenu,piecesJointes,null);
    }
    @Transactional
    public MessageConversation envoyer(UUID utilisateurId,UUID conversationId,String contenu,List<UUID> piecesJointes,UUID sourceRef) {
        Conversation conversation = conversation(conversationId);
        em.lock(conversation,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        verifier(conversation, utilisateurId);
        verifierEchange(conversation.getPatient(), conversation.getProfessionnel());
        Utilisateur utilisateur = utilisateurs.findById(utilisateurId)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur", utilisateurId));
        if((contenu==null||contenu.isBlank())&&piecesJointes!=null&&!piecesJointes.isEmpty())contenu="Pièce jointe";
        if(sourceRef!=null){
            var precedents=em.createQuery("select m from MessageConversation m where m.conversation.id=:c and m.expediteur.id=:a and m.sourceRef=:r",MessageConversation.class).setParameter("c",conversationId).setParameter("a",utilisateurId).setParameter("r",sourceRef).getResultList();
            if(!precedents.isEmpty()){
                var precedent=precedents.getFirst();
                var ids=piecesJointes==null?List.<UUID>of():piecesJointes;
                if(!precedent.getContenu().equals(contenu==null?null:contenu.trim()) || !new HashSet<>(precedent.getPiecesJointes().stream().map(PieceJointe::getId).toList()).equals(new HashSet<>(ids)) || new HashSet<>(ids).size()!=ids.size())throw new com.nourtech.ecareplus.shared.application.exception.ConflitMetierException("Clé de message réutilisée avec une autre demande");
                return precedent;
            }
        }
        MessageConversation message=messages.save(new MessageConversation(conversation, utilisateur, contenu));
        message.definirSource(sourceRef);
        pieces.rattacher(utilisateurId,conversationId,message,piecesJointes);
        Utilisateur destinataire=utilisateurId.equals(conversation.getPatient().getId())?conversation.getProfessionnel():conversation.getPatient();
        var notification=new com.nourtech.ecareplus.communication.domain.entity.Notification(destinataire,
                "Nouveau message", "Un nouveau message est disponible dans votre conversation sécurisée.", "MESSAGE",
                com.nourtech.ecareplus.communication.domain.enumeration.CanalNotification.APPLICATION, true);
        notification.envoyer(java.time.Instant.now());
        notification.rattacherRessource("CONVERSATION",conversation.getId());
        em.persist(notification);
        distribution.programmer(notification,conversation.getPatient().getId());
        return message;
    }

    @Transactional
    public void lire(UUID utilisateurId, UUID conversationId) {
        verifier(conversation(conversationId), utilisateurId);
        messages.findAllByConversationId(conversationId, Pageable.unpaged()).forEach(message -> {
            if (!message.getExpediteur().getId().equals(utilisateurId)) message.lire();
        });
    }

    private Conversation conversation(UUID id) {
        return conversations.findById(id).orElseThrow(() -> new RessourceIntrouvableException("Conversation", id));
    }
    private void verifier(Conversation conversation, UUID utilisateurId) {
        if (!autorisations.conversationAccessible(conversation, utilisateurId)) throw new AccessDeniedException("Accès interdit à cette conversation");
    }
    private void verifierEchange(Patient patient, ProfessionnelSante professionnel) {
        if (!autorisations.echangeAutorise(patient, professionnel)) throw new AccessDeniedException("Une affectation et un consentement valides sont obligatoires");
    }
}
