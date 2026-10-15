package com.nourtech.ecareplus.auth.infrastructure.security;

import com.nourtech.ecareplus.alert.domain.entity.*;
import com.nourtech.ecareplus.care.domain.entity.*;
import com.nourtech.ecareplus.communication.domain.entity.*;
import com.nourtech.ecareplus.consent.domain.entity.*;
import com.nourtech.ecareplus.consent.domain.enumeration.StatutConsentement;
import com.nourtech.ecareplus.consent.infrastructure.persistence.ConsentementRepository;
import com.nourtech.ecareplus.identity.domain.entity.*;
import com.nourtech.ecareplus.identity.domain.enumeration.StatutCompte;
import com.nourtech.ecareplus.messaging.domain.entity.Conversation;
import com.nourtech.ecareplus.organization.domain.entity.*;
import com.nourtech.ecareplus.organization.domain.enumeration.StatutAffectation;
import com.nourtech.ecareplus.organization.domain.enumeration.StatutStructure;
import com.nourtech.ecareplus.organization.infrastructure.persistence.AffectationPatientRepository;
import com.nourtech.ecareplus.shared.application.exception.RessourceIntrouvableException;
import com.nourtech.ecareplus.subscription.domain.entity.AbonnementStructure;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.lang.reflect.*;
import java.time.*;
import java.util.*;

/** Explicit resource ownership checks shared by the HTTP controllers. Unknown references fail closed. */
@Component
@Transactional(readOnly = true)
public class AutorisationRessources {
    private static final Set<String> ADMINISTRATION = Set.of("AdministrateurStructureController",
            "ProfessionnelSanteController", "AffectationPatientController", "StructureSanteController",
            "AbonnementStructureController");
    private static final Set<String> CLINIQUE = Set.of("DossierSuiviController", "MesureSanteController",
            "PathologieController", "TraitementController", "PriseMedicamentController", "ConfirmationPriseController",
            "ObservationSuiviController", "RapportSuiviController", "DeclarationSymptomeController", "AlerteController",
            "RappelController", "RendezVousController", "NotificationController");
    private static final Map<String, Class<?>> REFERENCES = Map.ofEntries(
            Map.entry("patientId", Patient.class), Map.entry("structureId", StructureSante.class),
            Map.entry("professionnelId", ProfessionnelSante.class), Map.entry("medecinId", Medecin.class),
            Map.entry("auteurId", Medecin.class), Map.entry("utilisateurId", Utilisateur.class),
            Map.entry("procheAidantId", ProcheAidant.class), Map.entry("dossierId", DossierSuivi.class),
            Map.entry("mesureId", MesureSante.class), Map.entry("pathologieId", Pathologie.class),
            Map.entry("traitementId", Traitement.class), Map.entry("priseId", PriseMedicament.class),
            Map.entry("priseMedicamentId", PriseMedicament.class), Map.entry("confirmationId", ConfirmationPrise.class),
            Map.entry("observationId", ObservationSuivi.class), Map.entry("rapportId", RapportSuivi.class),
            Map.entry("declarationId", DeclarationSymptome.class), Map.entry("alerteId", Alerte.class),
            Map.entry("interventionId", InterventionSuivi.class), Map.entry("rappelId", Rappel.class),
            Map.entry("rendezVousId", RendezVous.class), Map.entry("notificationId", Notification.class),
            Map.entry("consentementId", Consentement.class), Map.entry("affectationId", AffectationPatient.class),
            Map.entry("abonnementId", AbonnementStructure.class), Map.entry("contenuId", ContenuEducatif.class),
            Map.entry("regleId", RegleAlerte.class), Map.entry("medicamentId", Medicament.class));
    private final EntityManager em;
    private final AffectationPatientRepository affectations;
    private final ConsentementRepository consentements;

    public AutorisationRessources(EntityManager em, AffectationPatientRepository affectations,
                                  ConsentementRepository consentements) {
        this.em = em; this.affectations = affectations; this.consentements = consentements;
    }

    public void verifier(Method method, Object[] args) {
        Utilisateur actor = acteur();
        String controller = method.getDeclaringClass().getSimpleName();
        // These controllers derive ownership from the JWT and validate membership themselves.
        if (Set.of("EspacePatientController", "CommunicationPatientController", "MessagerieController").contains(controller)) return;
        boolean read = method.isAnnotationPresent(GetMapping.class);
        boolean onboarding = controller.equals("DossierSuiviController") && method.getName().equals("ouvrir");
        if (ADMINISTRATION.contains(controller) || onboarding
                || (Set.of("PatientController", "ProcheAidantController").contains(controller) && !read)) {
            require(actor instanceof AdministrateurStructure);
        } else if (CLINIQUE.contains(controller)) {
            require(actor instanceof ProfessionnelSante);
        } else if (Set.of("RegleAlerteController", "MedicamentController", "ContenuEducatifController").contains(controller)) {
            require(read ? actor instanceof ProfessionnelSante || actor instanceof AdministrateurStructure : actor instanceof Medecin);
        } else if (controller.equals("ConsentementController")) {
            require(actor instanceof ProfessionnelSante || actor instanceof AdministrateurStructure);
        } else if (!Set.of("PatientController", "ProcheAidantController", "SynchronisationController").contains(controller)) {
            throw denied();
        }
        if (actor instanceof ProfessionnelSante pro) {
            require("ACTIF".equals(pro.getStatutProfessionnel()) && pro.getStructure().getStatut() == StatutStructure.ACTIVE);
        }
        Map<String, UUID> refs = references(method, args);
        boolean clinical = CLINIQUE.contains(controller) && !onboarding;
        for (var ref : refs.entrySet()) {
            String name = ref.getKey(); UUID id = ref.getValue();
            // Initial assignment is the explicit administrative patient admission operation.
            if (controller.equals("AffectationPatientController") && method.getName().equals("affecter") && name.equals("patientId")) continue;
            // A new sharing authorization links an existing caregiver to an already scoped patient.
            if (controller.equals("ConsentementController") && method.getName().equals("autoriserProche") && name.equals("procheAidantId")) continue;
            if (controller.equals("SynchronisationController")) require(actor.getId().equals(id));
            if (name.equals("auteurId") || name.equals("medecinId")
                    || (name.equals("professionnelId") && method.getName().equals("ajouterObservation"))) {
                require(actor.getId().equals(id));
            }
            verifierReference(name, id, actor, clinical, read);
        }
        // A structure administrator cannot provision an unrelated new tenant via the tenant API.
        if (controller.equals("StructureSanteController") && method.getName().equals("creer")) throw denied();
    }

    public Object filtrer(Method method, Object result) {
        if (!(result instanceof List<?> values)) return result;
        String controller = method.getDeclaringClass().getSimpleName();
        String ref = switch (controller) {
            case "AlerteController" -> "alerteId";
            case "RappelController" -> "rappelId";
            default -> null;
        };
        if (ref == null) return result;
        Utilisateur actor = acteur();
        return values.stream().filter(value -> {
            try {
                UUID id = (UUID) value.getClass().getMethod("id").invoke(value);
                verifierReference(ref, id, actor, true, true);
                return true;
            } catch (AccessDeniedException denied) { return false; }
            catch (ReflectiveOperationException error) { throw new IllegalStateException("Réponse sans identifiant de ressource", error); }
        }).toList();
    }

    public boolean conversationAccessible(Conversation conversation, UUID utilisateurId) {
        Utilisateur actor = acteur();
        if (!actor.getId().equals(utilisateurId)) return false;
        if (actor instanceof Patient) return conversation.getPatient().getId().equals(utilisateurId);
        return actor instanceof ProfessionnelSante && conversation.getProfessionnel().getId().equals(utilisateurId)
                && echangeAutorise(conversation.getPatient(), conversation.getProfessionnel());
    }

    public boolean echangeAutorise(Patient patient, ProfessionnelSante professionnel) {
        return professionnel.getStatutCompte() == StatutCompte.ACTIF
                && "ACTIF".equals(professionnel.getStatutProfessionnel())
                && professionnel.getStructure().getStatut() == StatutStructure.ACTIVE
                && patientAccessible(patient, professionnel, true);
    }

    private void verifierReference(String name, UUID id, Utilisateur actor, boolean clinical, boolean read) {
        Class<?> type = REFERENCES.get(name);
        if (type == null) throw denied();
        Object entity = em.find(type, id);
        if (entity == null) throw new RessourceIntrouvableException(type.getSimpleName(), id);
        verifierEntite(Hibernate.unproxy(entity), actor, clinical, read);
    }

    private void verifierEntite(Object entity, Utilisateur actor, boolean clinical, boolean read) {
        switch (entity) {
            case DossierSuivi d -> { structure(d.getStructure(), actor); patient(d.getPatient(), actor, clinical); }
            case MesureSante m -> verifierEntite(m.getDossier(), actor, true, read);
            case Pathologie p -> verifierEntite(p.getDossier(), actor, true, read);
            case Traitement t -> verifierEntite(t.getDossier(), actor, true, read);
            case PriseMedicament p -> verifierEntite(p.getTraitement(), actor, true, read);
            case ConfirmationPrise c -> verifierEntite(c.getPriseMedicament(), actor, true, read);
            case ObservationSuivi o -> verifierEntite(o.getDossier(), actor, true, read);
            case RapportSuivi r -> verifierEntite(r.getDossier(), actor, true, read);
            case DeclarationSymptome d -> patient(d.getPatient(), actor, true);
            case Alerte a -> {
                patient(a.getPatient(), actor, true);
                if (a.getMesure() != null) verifierEntite(a.getMesure(), actor, true, read);
            }
            case InterventionSuivi i -> verifierEntite(i.getAlerte(), actor, true, read);
            case Rappel r -> {
                patient(r.getPatient(), actor, true);
                if (r.getPriseMedicament() != null) verifierEntite(r.getPriseMedicament(), actor, true, read);
            }
            case RendezVous r -> patient(r.getPatient(), actor, true);
            case Notification n -> verifierEntite(Hibernate.unproxy(n.getUtilisateur()), actor, true, read);
            case Patient p -> patient(p, actor, clinical);
            case ProfessionnelSante p -> structure(p.getStructure(), actor);
            case AdministrateurStructure a -> structure(a.getStructure(), actor);
            case StructureSante s -> structure(s, actor);
            case Consentement c -> patient(c.getPatient(), actor, false);
            case AffectationPatient a -> structure(a.getStructure(), actor);
            case AbonnementStructure a -> structure(a.getStructure(), actor);
            case ContenuEducatif c -> { if (!read) require(c.getAuteur().getId().equals(actor.getId())); }
            case ProcheAidant p -> {
                List<AutorisationPartage> links = em.createQuery("select a from AutorisationPartage a where a.procheAidant.id = :id", AutorisationPartage.class)
                        .setParameter("id", p.getId()).getResultList();
                require(links.stream().anyMatch(a -> a.estValideA(Instant.now()) && patientAccessible(a.getPatient(), actor, clinical)));
            }
            case RegleAlerte ignored -> { /* shared clinical reference catalog, physician writes only */ }
            case Medicament ignored -> { /* shared medication catalog, physician writes only */ }
            default -> throw denied();
        }
    }

    private void patient(Patient p, Utilisateur actor, boolean clinical) { require(patientAccessible(p, actor, clinical)); }

    private boolean patientAccessible(Patient p, Utilisateur actor, boolean clinical) {
        UUID structure = structureId(actor);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        boolean assigned = affectations.findAllByPatientIdAndStatut(p.getId(), StatutAffectation.ACTIVE).stream()
                .anyMatch(a -> a.getStructure().getId().equals(structure) && !a.getDateDebut().isAfter(today)
                        && (a.getDateFin() == null || !a.getDateFin().isBefore(today))
                        && (actor instanceof AdministrateurStructure || a.getProfessionnel().getId().equals(actor.getId())));
        if (!assigned) return false;
        return !clinical || consentements.findAllByPatientIdAndStatut(p.getId(), StatutConsentement.ACCORDE).stream()
                .anyMatch(c -> c.estValideA(Instant.now()));
    }

    private void structure(StructureSante structure, Utilisateur actor) { require(structure.getId().equals(structureId(actor))); }
    private UUID structureId(Utilisateur actor) {
        return switch (actor) {
            case ProfessionnelSante p -> p.getStructure().getId();
            case AdministrateurStructure a -> a.getStructure().getId();
            default -> throw denied();
        };
    }

    private Utilisateur acteur() {
        if (!(SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken token)) throw denied();
        UUID id;
        try { id = UUID.fromString(token.getToken().getSubject()); } catch (IllegalArgumentException invalid) { throw denied(); }
        Utilisateur actor = em.find(Utilisateur.class, id);
        require(actor != null && actor.getStatutCompte() == StatutCompte.ACTIF);
        return (Utilisateur) Hibernate.unproxy(actor);
    }

    private Map<String, UUID> references(Method method, Object[] args) {
        Map<String, UUID> refs = new LinkedHashMap<>();
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Object arg = args[i];
            if (arg instanceof UUID id) refs.put(parameters[i].getName(), id);
            else if (arg != null && parameters[i].isAnnotationPresent(RequestBody.class)) {
                if (!arg.getClass().isRecord()) throw denied();
                for (RecordComponent field : arg.getClass().getRecordComponents()) {
                    if (field.getType() != UUID.class) continue;
                    try {
                        UUID id = (UUID) field.getAccessor().invoke(arg);
                        if (id != null) refs.put(field.getName(), id);
                    } catch (ReflectiveOperationException invalid) { throw new IllegalStateException(invalid); }
                }
            }
        }
        return refs;
    }
    private static void require(boolean condition) { if (!condition) throw denied(); }
    private static AccessDeniedException denied() { return new AccessDeniedException("Accès interdit à cette ressource"); }
}
