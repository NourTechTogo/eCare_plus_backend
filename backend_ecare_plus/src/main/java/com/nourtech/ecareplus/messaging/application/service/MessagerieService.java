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

    public MessagerieService(ConversationRepository conversations, MessageConversationRepository messages,
            PatientRepository patients, ProfessionnelSanteRepository professionnels,
            UtilisateurRepository utilisateurs, AutorisationRessources autorisations) {
        this.conversations = conversations; this.messages = messages; this.patients = patients;
        this.professionnels = professionnels; this.utilisateurs = utilisateurs; this.autorisations = autorisations;
    }

    @Transactional
    public Conversation ouvrir(UUID patientId, UUID professionnelId) {
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
        return messages.findAllByConversationId(conversationId, pageable);
    }

    @Transactional
    public MessageConversation envoyer(UUID utilisateurId, UUID conversationId, String contenu) {
        Conversation conversation = conversation(conversationId);
        verifier(conversation, utilisateurId);
        verifierEchange(conversation.getPatient(), conversation.getProfessionnel());
        Utilisateur utilisateur = utilisateurs.findById(utilisateurId)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur", utilisateurId));
        return messages.save(new MessageConversation(conversation, utilisateur, contenu));
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
