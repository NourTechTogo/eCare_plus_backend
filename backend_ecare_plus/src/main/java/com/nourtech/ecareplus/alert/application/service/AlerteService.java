package com.nourtech.ecareplus.alert.application.service;

import com.nourtech.ecareplus.alert.application.command.GenererAlerteCommand;
import com.nourtech.ecareplus.alert.domain.entity.Alerte;
import com.nourtech.ecareplus.alert.domain.entity.RegleAlerte;
import com.nourtech.ecareplus.alert.domain.enumeration.OrigineAlerte;
import com.nourtech.ecareplus.alert.domain.enumeration.StatutAlerte;
import com.nourtech.ecareplus.alert.domain.enumeration.TypeAlerte;
import com.nourtech.ecareplus.alert.infrastructure.persistence.AlerteRepository;
import com.nourtech.ecareplus.alert.infrastructure.persistence.RegleAlerteRepository;
import com.nourtech.ecareplus.care.domain.entity.MesureSante;
import com.nourtech.ecareplus.care.infrastructure.persistence.MesureSanteRepository;
import com.nourtech.ecareplus.identity.domain.entity.Patient;
import com.nourtech.ecareplus.identity.domain.entity.ProfessionnelSante;
import com.nourtech.ecareplus.identity.infrastructure.persistence.PatientRepository;
import com.nourtech.ecareplus.identity.infrastructure.persistence.ProfessionnelSanteRepository;
import com.nourtech.ecareplus.shared.application.exception.ConflitMetierException;
import com.nourtech.ecareplus.shared.application.exception.RessourceIntrouvableException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Objects;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import com.nourtech.ecareplus.organization.domain.entity.AffectationPatient;
import com.nourtech.ecareplus.organization.domain.enumeration.StatutAffectation;
import com.nourtech.ecareplus.identity.domain.enumeration.StatutCompte;
import com.nourtech.ecareplus.communication.domain.entity.Notification;
import com.nourtech.ecareplus.communication.domain.enumeration.CanalNotification;

@Service
@Transactional
public class AlerteService {

    private final AlerteRepository alerteRepository;
    private final PatientRepository patientRepository;
    private final MesureSanteRepository mesureSanteRepository;
    private final RegleAlerteRepository regleAlerteRepository;
    private final ProfessionnelSanteRepository professionnelRepository;

    @PersistenceContext
    private EntityManager em;
    @org.springframework.beans.factory.annotation.Autowired private com.nourtech.ecareplus.communication.application.service.DistributionNotificationService distribution;

    public AlerteService(
            AlerteRepository alerteRepository,
            PatientRepository patientRepository,
            MesureSanteRepository mesureSanteRepository,
            RegleAlerteRepository regleAlerteRepository,
            ProfessionnelSanteRepository professionnelRepository
    ) {
        this.alerteRepository = alerteRepository;
        this.patientRepository = patientRepository;
        this.mesureSanteRepository = mesureSanteRepository;
        this.regleAlerteRepository = regleAlerteRepository;
        this.professionnelRepository = professionnelRepository;
    }

    public Alerte generer(GenererAlerteCommand commande) {
        Patient patient = patientRepository.findById(commande.patientId())
                .orElseThrow(() -> new RessourceIntrouvableException("Patient", commande.patientId()));

        MesureSante mesure = null;
        if (commande.mesureId() != null) {
            mesure = mesureSanteRepository.findById(commande.mesureId())
                    .orElseThrow(() -> new RessourceIntrouvableException("MesureSante", commande.mesureId()));
            if (!patient.memeIdentiteQue(mesure.getPatient())) {
                throw new IllegalArgumentException("La mesure n'appartient pas au patient de l'alerte");
            }
        }

        RegleAlerte regle = null;
        if (commande.regleId() != null) {
            regle = regleAlerteRepository.findById(commande.regleId())
                    .orElseThrow(() -> new RessourceIntrouvableException("RegleAlerte", commande.regleId()));
            if (mesure == null) {
                throw new IllegalArgumentException("Une règle ne peut être liée à une alerte manuelle sans mesure");
            }
            if (regle.getTypeMesure() != mesure.getTypeMesure()) {
                throw new IllegalArgumentException("La règle ne correspond pas au type de la mesure");
            }
            if (alerteRepository.existsByRegleIdAndMesureId(regle.getId(), mesure.getId())) {
                throw new ConflitMetierException("Une alerte existe déjà pour cette règle et cette mesure");
            }
        }

        Alerte alerte = alerteRepository.save(new Alerte(
                patient,
                mesure,
                regle,
                commande.typeAlerte(),
                commande.titre(),
                commande.description(),
                commande.niveauPriorite()
        ));
        return alerte;
    }

    /**
     * Évalue toutes les règles actives correspondant à une nouvelle mesure.
     * La période d'observation d'une règle est exprimée en minutes.
     */
    public List<Alerte> evaluerAutomatiquement(MesureSante mesure) {
        List<RegleAlerte> candidates = regleAlerteRepository.findAllByTypeMesureAndActifTrue(mesure.getTypeMesure()).stream()
                .filter(r -> r.getUnite() != null && r.getUnite().equals(mesure.getUnite()))
                .filter(r -> r.getPatientId() == null || r.getPatientId().equals(mesure.getPatient().getId()))
                .filter(r -> r.getStructureId() == null || (mesure.getDossier().getStructure()!=null&&r.getStructureId().equals(mesure.getDossier().getStructure().getId())))
                .filter(r -> r.contexteApplicable(mesure.getContexteMesure()))
                .toList();
        return candidates.stream()
                .filter(r -> r.getPatientId() != null || candidates.stream().noneMatch(p -> p.getPatientId() != null
                        && Objects.equals(p.getComposante(), r.getComposante())))
                .filter(regle -> doitDeclencher(regle, mesure))
                .max(java.util.Comparator.comparingInt((RegleAlerte r)->r.getNiveauPriorite().ordinal()).thenComparing(RegleAlerte::getNom))
                .stream()
                .filter(regle -> !alerteRepository.existsByRegleIdAndMesureId(regle.getId(), mesure.getId()))
                .map(regle -> creerAlerteAutomatique(regle, mesure))
                .toList();
    }

    private boolean doitDeclencher(RegleAlerte regle, MesureSante mesure) {
        Instant debut = mesure.getDateMesure().minus(Duration.ofMinutes(regle.getPeriodeObservation()));
        if(regle.getOperateur().equals("VARIATION_ABSOLUE")){
            var precedente=mesureSanteRepository.findAllByPatientIdAndTypeMesureAndDateMesureBetween(mesure.getPatient().getId(),mesure.getTypeMesure(),debut,mesure.getDateMesure()).stream()
                .filter(m->m.getDateMesure().isBefore(mesure.getDateMesure())&&m.getDossier().getId().equals(mesure.getDossier().getId())&&m.getUnite().equals(regle.getUnite()))
                .min(java.util.Comparator.comparing(MesureSante::getDateMesure).thenComparing(MesureSante::getId));
            return precedente.isPresent()&&regle.evaluerMesure(mesure.getValeurPrincipale().subtract(precedente.get().getValeurPrincipale()).abs(),1);
        }
        int occurrences = (int) mesureSanteRepository
                .findAllByPatientIdAndTypeMesureAndDateMesureBetween(
                        mesure.getPatient().getId(), mesure.getTypeMesure(), debut, mesure.getDateMesure()
                ).stream()
                .filter(candidate -> candidate.getDossier().getId().equals(mesure.getDossier().getId()))
                .filter(candidate -> candidate.getUnite().equals(regle.getUnite()))
                .filter(candidate -> regle.contexteApplicable(candidate.getContexteMesure()))
                .filter(candidate -> regle.depasseSeuil(regle.valeurEvaluee(candidate)))
                .count();
        return regle.evaluerMesure(regle.valeurEvaluee(mesure), occurrences);
    }

    private Alerte creerAlerteAutomatique(RegleAlerte regle, MesureSante mesure) {
        Alerte alerte = new Alerte(
                mesure.getPatient(),
                mesure,
                regle,
                TypeAlerte.SEUIL_MESURE,
                regle.getNom(),
                "Une valeur est hors de la plage de suivi. Contactez votre équipe soignante."
                        + (regle.getNiveauPriorite() == com.nourtech.ecareplus.alert.domain.enumeration.NiveauPriorite.CRITIQUE
                           ? " En cas d'urgence, contactez les services d'urgence." : ""),
                regle.getNiveauPriorite(),
                OrigineAlerte.AUTOMATIQUE
        );
        alerteRepository.saveAndFlush(alerte);
        if (em != null) {
            List<AffectationPatient> affectations = mesure.getDossier().getStructure()==null ? List.of() : em.createQuery(
                    "select a from AffectationPatient a join fetch a.professionnel where a.patient.id=:patient and a.structure.id=:structure and a.statut=:statut", AffectationPatient.class)
                    .setParameter("patient", mesure.getPatient().getId())
                    .setParameter("structure", mesure.getDossier().getStructure().getId())
                    .setParameter("statut", StatutAffectation.ACTIVE).getResultList().stream()
                    .filter(a -> !a.getDateDebut().isAfter(java.time.LocalDate.now(java.time.ZoneOffset.UTC)))
                    .filter(a -> a.getDateFin() == null || !a.getDateFin().isBefore(java.time.LocalDate.now(java.time.ZoneOffset.UTC)))
                    .filter(a -> a.getProfessionnel().getStatutCompte() == StatutCompte.ACTIF).toList();
            affectations.stream().filter(AffectationPatient::isProfessionnelPrincipal).findFirst()
                    .or(() -> affectations.stream().findFirst()).ifPresent(a -> alerte.affecter(a.getProfessionnel()));
            publierNotification(mesure.getPatient(), "Nouvelle alerte de suivi", alerte.getDescription(),mesure.getPatient().getId(),alerte.getId());
            affectations.stream().map(AffectationPatient::getProfessionnel).distinct().forEach(p ->
                    publierNotification(p, "Une alerte nécessite votre attention", "Ouvrez le dossier de votre patient dans eCare+.",mesure.getPatient().getId(),alerte.getId()));
        }
        return alerteRepository.save(alerte);
    }

    private void publierNotification(com.nourtech.ecareplus.identity.domain.entity.Utilisateur destinataire, String titre, String message,UUID patientId,UUID alerteId) {
        Notification notification = new Notification(destinataire, titre, message, "ALERTE", CanalNotification.APPLICATION, false);
        // Publication to the authenticated inbox, not a claim of push/SMS delivery.
        notification.envoyer(Instant.now());
        notification.rattacherRessource("ALERTE",alerteId);
        em.persist(notification);
        distribution.programmer(notification,patientId);
    }

    public Alerte affecter(UUID alerteId, UUID professionnelId) {
        Alerte alerte = charger(alerteId);
        ProfessionnelSante professionnel = professionnelRepository.findById(professionnelId)
                .orElseThrow(() -> new RessourceIntrouvableException("ProfessionnelSante", professionnelId));
        verifierProfessionnelAffecte(alerte, professionnel);
        alerte.affecter(professionnel);
        return alerte;
    }

    public Alerte prendreEnCharge(UUID alerteId, Instant maintenant) {
        Alerte alerte = charger(alerteId);
        alerte.prendreEnCharge(maintenant);
        return alerte;
    }

    public Alerte transmettre(UUID alerteId, UUID nouveauProfessionnelId) {
        Alerte alerte = charger(alerteId);
        ProfessionnelSante professionnel = professionnelRepository.findById(nouveauProfessionnelId)
                .orElseThrow(() -> new RessourceIntrouvableException("ProfessionnelSante", nouveauProfessionnelId));
        verifierProfessionnelAffecte(alerte, professionnel);
        alerte.transmettre(professionnel);
        return alerte;
    }

    public Alerte cloturer(UUID alerteId, String motifCloture, Instant maintenant) {
        Alerte alerte = charger(alerteId);
        ProfessionnelSante auteur = null;
        if (org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()
                instanceof org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken token) {
            UUID auteurId = UUID.fromString(token.getToken().getSubject());
            auteur = professionnelRepository.findById(auteurId)
                    .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Professionnel requis pour clôturer"));
            verifierProfessionnelAffecte(alerte,auteur);
        }
        alerte.cloturer(motifCloture, maintenant, auteur);
        return alerte;
    }

    public Alerte rouvrir(UUID alerteId) {
        Alerte alerte = charger(alerteId);
        alerte.rouvrir();
        return alerte;
    }

    public Alerte consulter(UUID alerteId) {
        return charger(alerteId);
    }

    @Transactional(readOnly = true)
    public List<Alerte> listerPourPatient(UUID patientId, StatutAlerte statut) {
        if (!patientRepository.existsById(patientId)) {
            throw new RessourceIntrouvableException("Patient", patientId);
        }
        return alerteRepository.findAllByPatientIdAndStatutOrderByDateCreationDesc(patientId, statut);
    }

    private Alerte charger(UUID alerteId) {
        return alerteRepository.findById(alerteId)
                .orElseThrow(() -> new RessourceIntrouvableException("Alerte", alerteId));
    }

    private void verifierProfessionnelAffecte(Alerte alerte, ProfessionnelSante professionnel) {
        if (em == null) return; // Standalone domain tests; HTTP application always injects EntityManager.
        Instant now = Instant.now();
        java.time.LocalDate today = java.time.LocalDate.ofInstant(now, java.time.ZoneOffset.UTC);
        Long count = em.createQuery("select count(a) from AffectationPatient a where a.patient.id=:patient and a.professionnel.id=:professionnel and a.statut=:statut and a.dateDebut<=:today and (a.dateFin is null or a.dateFin>=:today)", Long.class)
                .setParameter("patient", alerte.getPatient().getId()).setParameter("professionnel", professionnel.getId())
                .setParameter("statut", StatutAffectation.ACTIVE).setParameter("today", today).getSingleResult();
        if (count == 0 || professionnel.getStatutCompte() != StatutCompte.ACTIF
                || !"ACTIF".equals(professionnel.getStatutProfessionnel())
                || professionnel.getStructure().getStatut() != com.nourtech.ecareplus.organization.domain.enumeration.StatutStructure.ACTIVE)
            throw new org.springframework.security.access.AccessDeniedException("Le professionnel cible n'est pas affecté activement à ce patient");
    }
}
