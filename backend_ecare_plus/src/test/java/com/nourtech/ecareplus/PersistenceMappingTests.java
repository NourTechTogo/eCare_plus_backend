package com.nourtech.ecareplus;

import com.nourtech.ecareplus.alert.domain.entity.Alerte;
import com.nourtech.ecareplus.alert.domain.entity.InterventionSuivi;
import com.nourtech.ecareplus.alert.domain.entity.RegleAlerte;
import com.nourtech.ecareplus.alert.domain.enumeration.NiveauPriorite;
import com.nourtech.ecareplus.alert.domain.enumeration.StatutAlerte;
import com.nourtech.ecareplus.alert.domain.enumeration.TypeAlerte;
import com.nourtech.ecareplus.alert.application.command.GenererAlerteCommand;
import com.nourtech.ecareplus.alert.application.service.AlerteService;
import com.nourtech.ecareplus.alert.infrastructure.persistence.AlerteRepository;
import com.nourtech.ecareplus.alert.infrastructure.persistence.InterventionSuiviRepository;
import com.nourtech.ecareplus.alert.infrastructure.persistence.RegleAlerteRepository;
import com.nourtech.ecareplus.audit.domain.entity.JournalActivite;
import com.nourtech.ecareplus.audit.infrastructure.persistence.JournalActiviteRepository;
import com.nourtech.ecareplus.care.application.command.ProgrammerConfirmationPriseCommand;
import com.nourtech.ecareplus.care.application.service.ConfirmationPriseService;
import com.nourtech.ecareplus.care.domain.entity.ConfirmationPrise;
import com.nourtech.ecareplus.care.domain.entity.DeclarationSymptome;
import com.nourtech.ecareplus.care.domain.entity.DossierSuivi;
import com.nourtech.ecareplus.care.domain.entity.Medicament;
import com.nourtech.ecareplus.care.domain.entity.MesureSante;
import com.nourtech.ecareplus.care.domain.entity.ObjectifSuivi;
import com.nourtech.ecareplus.care.domain.entity.ObservationSuivi;
import com.nourtech.ecareplus.care.domain.entity.Pathologie;
import com.nourtech.ecareplus.care.domain.entity.PriseMedicament;
import com.nourtech.ecareplus.care.domain.entity.RapportSuivi;
import com.nourtech.ecareplus.care.domain.entity.Traitement;
import com.nourtech.ecareplus.care.domain.enumeration.NiveauRisque;
import com.nourtech.ecareplus.care.domain.enumeration.SourceMesure;
import com.nourtech.ecareplus.care.domain.enumeration.StatutObjectif;
import com.nourtech.ecareplus.care.domain.enumeration.StatutPathologie;
import com.nourtech.ecareplus.care.domain.enumeration.StatutPrise;
import com.nourtech.ecareplus.care.domain.enumeration.StatutRapport;
import com.nourtech.ecareplus.care.domain.enumeration.TypeMesure;
import com.nourtech.ecareplus.care.infrastructure.persistence.ConfirmationPriseRepository;
import com.nourtech.ecareplus.care.infrastructure.persistence.DeclarationSymptomeRepository;
import com.nourtech.ecareplus.care.infrastructure.persistence.DossierSuiviRepository;
import com.nourtech.ecareplus.care.infrastructure.persistence.MedicamentRepository;
import com.nourtech.ecareplus.care.infrastructure.persistence.MesureSanteRepository;
import com.nourtech.ecareplus.care.infrastructure.persistence.ObjectifSuiviRepository;
import com.nourtech.ecareplus.care.infrastructure.persistence.ObservationSuiviRepository;
import com.nourtech.ecareplus.care.infrastructure.persistence.PathologieRepository;
import com.nourtech.ecareplus.care.infrastructure.persistence.PriseMedicamentRepository;
import com.nourtech.ecareplus.care.infrastructure.persistence.RapportSuiviRepository;
import com.nourtech.ecareplus.care.infrastructure.persistence.TraitementRepository;
import com.nourtech.ecareplus.communication.domain.entity.ContenuEducatif;
import com.nourtech.ecareplus.communication.domain.entity.Notification;
import com.nourtech.ecareplus.communication.domain.entity.Rappel;
import com.nourtech.ecareplus.communication.domain.entity.RendezVous;
import com.nourtech.ecareplus.communication.domain.enumeration.CanalNotification;
import com.nourtech.ecareplus.communication.domain.enumeration.RecurrenceRappel;
import com.nourtech.ecareplus.communication.domain.enumeration.StatutEnvoiNotification;
import com.nourtech.ecareplus.communication.domain.enumeration.StatutRendezVous;
import com.nourtech.ecareplus.communication.domain.enumeration.StatutValidationContenu;
import com.nourtech.ecareplus.communication.domain.enumeration.TypeRappel;
import com.nourtech.ecareplus.communication.infrastructure.persistence.ContenuEducatifRepository;
import com.nourtech.ecareplus.communication.infrastructure.persistence.NotificationRepository;
import com.nourtech.ecareplus.communication.infrastructure.persistence.RappelRepository;
import com.nourtech.ecareplus.communication.infrastructure.persistence.RendezVousRepository;
import com.nourtech.ecareplus.consent.domain.entity.AutorisationPartage;
import com.nourtech.ecareplus.consent.domain.entity.Consentement;
import com.nourtech.ecareplus.consent.domain.enumeration.StatutAutorisation;
import com.nourtech.ecareplus.consent.domain.enumeration.TypeDonneePartage;
import com.nourtech.ecareplus.consent.infrastructure.persistence.AutorisationPartageRepository;
import com.nourtech.ecareplus.consent.infrastructure.persistence.ConsentementRepository;
import com.nourtech.ecareplus.identity.domain.entity.Medecin;
import com.nourtech.ecareplus.identity.domain.entity.Patient;
import com.nourtech.ecareplus.identity.domain.entity.ProcheAidant;
import com.nourtech.ecareplus.identity.domain.enumeration.Sexe;
import com.nourtech.ecareplus.identity.infrastructure.persistence.PatientRepository;
import com.nourtech.ecareplus.identity.infrastructure.persistence.UtilisateurRepository;
import com.nourtech.ecareplus.organization.domain.entity.AffectationPatient;
import com.nourtech.ecareplus.organization.domain.enumeration.RoleProfessionnel;
import com.nourtech.ecareplus.organization.domain.enumeration.StatutAffectation;
import com.nourtech.ecareplus.organization.domain.entity.StructureSante;
import com.nourtech.ecareplus.organization.infrastructure.persistence.AffectationPatientRepository;
import com.nourtech.ecareplus.organization.infrastructure.persistence.StructureSanteRepository;
import com.nourtech.ecareplus.subscription.domain.entity.AbonnementStructure;
import com.nourtech.ecareplus.subscription.domain.enumeration.FrequencePaiement;
import com.nourtech.ecareplus.subscription.infrastructure.persistence.AbonnementStructureRepository;
import com.nourtech.ecareplus.sync.application.service.SynchronisationService;
import com.nourtech.ecareplus.sync.domain.entity.ElementSynchronisation;
import com.nourtech.ecareplus.sync.domain.enumeration.StatutSynchronisation;
import com.nourtech.ecareplus.sync.infrastructure.persistence.ElementSynchronisationRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class PersistenceMappingTests {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private StructureSanteRepository structureRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private AffectationPatientRepository affectationRepository;

    @Autowired
    private DossierSuiviRepository dossierRepository;

    @Autowired
    private ObjectifSuiviRepository objectifRepository;

    @Autowired
    private PathologieRepository pathologieRepository;

    @Autowired
    private MesureSanteRepository mesureSanteRepository;

    @Autowired
    private ConsentementRepository consentementRepository;

    @Autowired
    private AutorisationPartageRepository autorisationRepository;

    @Autowired
    private JournalActiviteRepository journalRepository;

    @Autowired
    private ElementSynchronisationRepository synchronisationRepository;

    @Autowired
    private SynchronisationService synchronisationService;

    @Autowired
    private AbonnementStructureRepository abonnementRepository;

    @Autowired
    private TraitementRepository traitementRepository;

    @Autowired
    private MedicamentRepository medicamentRepository;

    @Autowired
    private PriseMedicamentRepository priseMedicamentRepository;

    @Autowired
    private ConfirmationPriseRepository confirmationPriseRepository;

    @Autowired
    private RappelRepository rappelRepository;

    @Autowired
    private RendezVousRepository rendezVousRepository;

    @Autowired
    private RegleAlerteRepository regleAlerteRepository;

    @Autowired
    private AlerteRepository alerteRepository;

    @Autowired
    private InterventionSuiviRepository interventionSuiviRepository;

    @Autowired
    private DeclarationSymptomeRepository declarationSymptomeRepository;

    @Autowired
    private ObservationSuiviRepository observationSuiviRepository;

    @Autowired
    private RapportSuiviRepository rapportSuiviRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ContenuEducatifRepository contenuEducatifRepository;

    @Autowired
    private AlerteService alerteService;

    @Autowired
    private ConfirmationPriseService confirmationPriseService;

    @Test
    void persisteLeGrapheOrganisationEtConsentement() {
        StructureSante structure = structureRepository.save(new StructureSante(
                "Centre eCare+",
                "CENTRE_MEDICAL",
                "Boulevard circulaire",
                "Lomé",
                "Maritime",
                "+22822000000",
                "contact@ecare.test",
                "MS-EC-001"
        ));
        structure.activer();

        Patient patient = patientRepository.save(new Patient(
                "PAT-1001",
                "Amouzou",
                "Ama",
                Sexe.FEMININ,
                LocalDate.of(1982, 6, 15),
                "+22890001001",
                "ama@example.test",
                "argon2-hash",
                "fr"
        ));
        patient.activerCompte();
        patient.activerSuivi();

        Medecin medecin = (Medecin) utilisateurRepository.save(new Medecin(
                "PRO-001",
                "ORDRE-001",
                "Mensah",
                "Koffi",
                Sexe.MASCULIN,
                LocalDate.of(1975, 4, 2),
                "+22890001002",
                "medecin@example.test",
                "argon2-hash",
                "fr",
                "Médecine interne",
                "Diabétologie",
                LocalDate.of(2020, 1, 1),
                structure
        ));
        ProcheAidant proche = (ProcheAidant) utilisateurRepository.save(new ProcheAidant(
                "Amouzou",
                "Emefa",
                Sexe.FEMININ,
                LocalDate.of(1984, 2, 12),
                "+22890001003",
                "emefa@example.test",
                "argon2-hash",
                "fr",
                "SOEUR"
        ));

        AffectationPatient affectation = affectationRepository.save(new AffectationPatient(
                patient,
                medecin,
                structure,
                RoleProfessionnel.MEDECIN_REFERENT,
                LocalDate.now(),
                true
        ));
        DossierSuivi dossier = dossierRepository.save(new DossierSuivi(patient, structure, "MENSUELLE"));
        ObjectifSuivi objectif = objectifRepository.save(new ObjectifSuivi(
                dossier,
                medecin,
                "Tension systolique",
                "Maintenir la tension dans la cible validée",
                new BigDecimal("110"),
                new BigDecimal("135"),
                "mmHg",
                LocalDate.now(),
                LocalDate.now().plusMonths(3)
        ));
        Pathologie pathologie = pathologieRepository.save(new Pathologie(
                dossier,
                "Diabète de type 2",
                "E11",
                "Diagnostic déclaré par le patient",
                LocalDate.now().minusYears(2),
                NiveauRisque.MODERE
        ));
        MesureSante mesure = mesureSanteRepository.save(new MesureSante(
                patient,
                dossier,
                TypeMesure.TENSION_ARTERIELLE,
                new BigDecimal("125"),
                new BigDecimal("80"),
                "mmHg",
                Instant.now(),
                "Au repos",
                null,
                SourceMesure.PATIENT,
                null
        ));
        Traitement traitement = traitementRepository.save(new Traitement(
                dossier,
                "Metformine 500mg",
                "Traitement de fond du diabète de type 2",
                LocalDate.now(),
                null,
                "Dr Mensah Koffi",
                "Prendre au cours des repas"
        ));
        Medicament medicament = medicamentRepository.save(new Medicament(
                "Glucophage", "Metformine", "Comprimé", "500", "mg"
        ));
        PriseMedicament prise = priseMedicamentRepository.save(new PriseMedicament(
                traitement, medicament, new BigDecimal("1"), "comprimé", "2 fois par jour",
                LocalTime.of(8, 0), "MATIN", "Orale", null
        ));
        ConfirmationPrise confirmation = confirmationPriseRepository.save(
                new ConfirmationPrise(prise, Instant.now().minusSeconds(60))
        );
        confirmation.confirmer(null, Instant.now());
        Rappel rappel = rappelRepository.save(new Rappel(
                patient, prise, TypeRappel.PRISE_MEDICAMENT, "Prise de Metformine",
                "N'oubliez pas votre comprimé du matin", Instant.now().plusSeconds(3600),
                RecurrenceRappel.QUOTIDIEN, CanalNotification.SMS, null
        ));
        RendezVous rendezVous = rendezVousRepository.save(new RendezVous(
                patient, "Consultation de suivi", Instant.now().plusSeconds(86400),
                "Centre eCare+", "CONSULTATION", null
        ));
        RegleAlerte regleAlerte = regleAlerteRepository.save(new RegleAlerte(
                "Hyperglycémie répétée", "Détecte une glycémie élevée répétée", TypeMesure.GLYCEMIE, ">",
                new BigDecimal("180"), null, 2, 24, NiveauPriorite.ELEVE
        ));
        regleAlerte.validerRegle(Instant.now());
        regleAlerte.activerRegle();
        Alerte alerte = alerteRepository.save(new Alerte(
                patient, mesure, regleAlerte, TypeAlerte.SEUIL_MESURE, "Glycémie élevée détectée",
                "Deux mesures consécutives au-dessus du seuil", NiveauPriorite.ELEVE
        ));
        alerte.affecter(medecin);
        alerte.prendreEnCharge(Instant.now());
        InterventionSuivi intervention = interventionSuiviRepository.save(new InterventionSuivi(
                alerte, "Appel téléphonique", "Contact du patient pour vérifier son état", Instant.now()
        ));
        DeclarationSymptome declaration = declarationSymptomeRepository.save(new DeclarationSymptome(
                patient, "Vertiges", "MODEREE", "Vertiges en fin de journée", null, false
        ));
        ObservationSuivi observation = observationSuiviRepository.save(new ObservationSuivi(
                dossier, medecin, "Patient observant bien son traitement", "SUIVI", false
        ));
        RapportSuivi rapport = rapportSuiviRepository.save(new RapportSuivi(
                dossier, "Bilan trimestriel", LocalDate.now().minusMonths(3), LocalDate.now(), null, "PDF"
        ));
        rapport.marquerGenere("/rapports/" + dossier.getId() + "-t1.pdf");
        Notification notification = notificationRepository.save(new Notification(
                patient, "Nouveau rapport disponible", "Votre bilan trimestriel est prêt",
                "RAPPORT", CanalNotification.EMAIL, false
        ));
        notification.envoyer(Instant.now());
        ContenuEducatif contenu = contenuEducatifRepository.save(new ContenuEducatif(
                medecin, "Bien vivre avec le diabète", "Conseils pratiques",
                "Contenu détaillé sur l'alimentation et l'activité physique", "ARTICLE", "fr", "DIABETE", null
        ));
        contenu.validerContenu();
        contenu.publier(Instant.now());
        Consentement consentement = consentementRepository.save(new Consentement(
                patient,
                "PARTAGE_PROCHE",
                "Partage des rappels avec la sœur du patient",
                "1.0",
                "127.0.0.1",
                Instant.now()
        ));
        AutorisationPartage autorisation = new AutorisationPartage(
                patient,
                proche,
                consentement,
                TypeDonneePartage.RAPPELS,
                Instant.now().minusSeconds(1),
                null,
                true,
                true
        );
        autorisation.activer(Instant.now());
        autorisationRepository.save(autorisation);

        ElementSynchronisation synchronisation = synchronisationService.recevoir(
                patient.getId(),
                "MESURE_SANTE",
                "android-test-1",
                "local-1",
                Map.of("type", "TENSION", "systolique", 125, "diastolique", 80),
                Instant.now()
        );
        ElementSynchronisation doublon = synchronisationService.recevoir(
                patient.getId(),
                "MESURE_SANTE",
                "android-test-1",
                "local-1",
                Map.of("type", "TENSION", "systolique", 999, "diastolique", 999),
                Instant.now()
        );
        assertThat(doublon.getId()).isEqualTo(synchronisation.getId());
        AbonnementStructure abonnement = new AbonnementStructure(
                structure,
                "PILOTE",
                LocalDate.now(),
                LocalDate.now().plusYears(1),
                50,
                new BigDecimal("250000.00"),
                FrequencePaiement.ANNUELLE
        );
        abonnement.activer();
        abonnementRepository.save(abonnement);
        journalRepository.save(new JournalActivite(
                medecin,
                "CONSULTER_DOSSIER",
                "DOSSIER_SUIVI",
                dossier.getId(),
                "127.0.0.1",
                "JUnit",
                "SUCCES"
        ));

        entityManager.flush();
        entityManager.clear();

        assertThat(patientRepository.findByNumeroPatient("PAT-1001")).isPresent();
        assertThat(affectationRepository.findAllByPatientIdAndStatut(patient.getId(), StatutAffectation.ACTIVE))
                .extracting(AffectationPatient::getId)
                .containsExactly(affectation.getId());
        assertThat(objectifRepository.findAllByDossierIdAndStatut(dossier.getId(), StatutObjectif.ACTIF))
                .extracting(ObjectifSuivi::getId)
                .containsExactly(objectif.getId());
        assertThat(pathologieRepository.findAllByDossierIdAndStatut(dossier.getId(), StatutPathologie.ACTIVE))
                .extracting(Pathologie::getId)
                .containsExactly(pathologie.getId());
        assertThat(mesureSanteRepository.findAllByPatientIdOrderByDateMesureDesc(patient.getId()))
                .extracting(MesureSante::getId)
                .containsExactly(mesure.getId());
        assertThat(traitementRepository.findById(traitement.getId())).isPresent();
        assertThat(priseMedicamentRepository.findAllByTraitementIdAndActif(traitement.getId(), true))
                .extracting(PriseMedicament::getId)
                .containsExactly(prise.getId());
        assertThat(confirmationPriseRepository.findAllByPriseMedicamentId(prise.getId()))
                .extracting(ConfirmationPrise::getStatut)
                .containsExactly(StatutPrise.PRISE);
        assertThat(rappelRepository.findById(rappel.getId())).isPresent();
        assertThat(rendezVousRepository.findAllByPatientId(patient.getId()))
                .extracting(RendezVous::getId)
                .containsExactly(rendezVous.getId());
        assertThat(rendezVous.getStatut()).isEqualTo(StatutRendezVous.PLANIFIE);
        assertThat(regleAlerteRepository.findAllByActif(true))
                .extracting(RegleAlerte::getId)
                .contains(regleAlerte.getId());
        assertThat(alerteRepository.findAllByPatientIdAndStatutOrderByDateCreationDesc(
                patient.getId(), StatutAlerte.PRISE_EN_CHARGE
        ))
                .extracting(Alerte::getId)
                .containsExactly(alerte.getId());
        assertThat(interventionSuiviRepository.findAllByAlerteId(alerte.getId()))
                .extracting(InterventionSuivi::getId)
                .containsExactly(intervention.getId());
        assertThat(declarationSymptomeRepository.findAllByPatientId(patient.getId()))
                .extracting(DeclarationSymptome::getId)
                .containsExactly(declaration.getId());
        assertThat(observationSuiviRepository.findAllByDossierId(dossier.getId()))
                .extracting(ObservationSuivi::getId)
                .containsExactly(observation.getId());
        assertThat(rapportSuiviRepository.findAllByDossierId(dossier.getId()))
                .extracting(RapportSuivi::getId)
                .containsExactly(rapport.getId());
        assertThat(rapport.getStatut()).isEqualTo(StatutRapport.GENERE);
        assertThat(notificationRepository.findAllByUtilisateurIdAndStatutEnvoi(patient.getId(), StatutEnvoiNotification.ENVOYEE))
                .extracting(Notification::getId)
                .containsExactly(notification.getId());
        assertThat(contenuEducatifRepository.findAllByStatutValidation(StatutValidationContenu.PUBLIE))
                .extracting(ContenuEducatif::getId)
                .containsExactly(contenu.getId());
        assertThat(autorisationRepository.findAllByPatientIdAndStatut(patient.getId(), StatutAutorisation.ACTIVE))
                .hasSize(1);
        assertThat(synchronisationRepository
                .findByUtilisateurIdAndIdentifiantAppareilAndIdentifiantLocal(
                        patient.getId(),
                        "android-test-1",
                        "local-1"
                ))
                .get()
                .extracting(ElementSynchronisation::getStatut)
                .isEqualTo(StatutSynchronisation.RECU);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void lesActionsSensiblesSurAlerteEtConfirmationPriseSontJournalisees() {
        StructureSante structure = structureRepository.save(new StructureSante(
                "Centre Audit",
                "CENTRE_MEDICAL",
                "Boulevard de l'audit",
                "Lomé",
                "Maritime",
                "+22822000200",
                "audit@ecare.test",
                "MS-EC-AUDIT"
        ));
        Patient patient = patientRepository.save(new Patient(
                "PAT-AUDIT-001",
                "Adjo",
                "Sena",
                Sexe.FEMININ,
                LocalDate.of(1990, 3, 1),
                "+22890002001",
                "audit.patient@example.test",
                "argon2-hash",
                "fr"
        ));
        Medecin medecin = (Medecin) utilisateurRepository.save(new Medecin(
                "PRO-AUDIT-001",
                "ORDRE-AUDIT-001",
                "Mensah",
                "Koffi",
                Sexe.MASCULIN,
                LocalDate.of(1975, 4, 2),
                "+22890002002",
                "audit.medecin@example.test",
                "argon2-hash",
                "fr",
                "Médecine interne",
                "Diabétologie",
                LocalDate.of(2020, 1, 1),
                structure
        ));
        DossierSuivi dossier = dossierRepository.save(new DossierSuivi(patient, structure, "MENSUELLE"));
        structure.activer();
        medecin.activerCompte();
        structureRepository.save(structure);
        utilisateurRepository.save(medecin);
        affectationRepository.save(new AffectationPatient(patient, medecin, structure,
                RoleProfessionnel.MEDECIN_REFERENT, LocalDate.now(), true));
        Traitement traitement = traitementRepository.save(new Traitement(
                dossier, "Metformine 500mg", null, LocalDate.now(), null, "Dr Mensah", null
        ));
        Medicament medicament = medicamentRepository.save(new Medicament(
                "Glucophage", "Metformine", "Comprimé", "500", "mg"
        ));
        PriseMedicament prise = priseMedicamentRepository.save(new PriseMedicament(
                traitement, medicament, new BigDecimal("1"), "comprimé", "2 fois par jour",
                LocalTime.of(8, 0), "MATIN", "Orale", null
        ));

        Alerte alerte = alerteService.generer(new GenererAlerteCommand(
                patient.getId(), null, null, TypeAlerte.AUTRE, "Alerte de test", null, NiveauPriorite.FAIBLE
        ));
        alerteService.affecter(alerte.getId(), medecin.getId());
        alerteService.prendreEnCharge(alerte.getId(), Instant.now());
        alerteService.cloturer(alerte.getId(), "Résolu après vérification", Instant.now());

        ConfirmationPrise confirmation = confirmationPriseService.programmer(
                new ProgrammerConfirmationPriseCommand(prise.getId(), Instant.now())
        );
        confirmationPriseService.confirmer(confirmation.getId(), null, Instant.now());

        assertThat(journalRepository.findAll())
                .filteredOn(entree -> "ALERTE".equals(entree.getRessource()) && alerte.getId().equals(entree.getRessourceId()))
                .extracting(JournalActivite::getAction)
                .containsExactlyInAnyOrder(
                        "GENERER_ALERTE", "AFFECTER_ALERTE", "PRENDRE_EN_CHARGE_ALERTE", "CLOTURER_ALERTE"
                );

        assertThat(journalRepository.findAll())
                .filteredOn(entree ->
                        "CONFIRMATION_PRISE".equals(entree.getRessource())
                                && confirmation.getId().equals(entree.getRessourceId())
                )
                .extracting(JournalActivite::getAction)
                .containsExactlyInAnyOrder(
                        "PROGRAMMER_CONFIRMATION_PRISE", "CONFIRMER_PRISE_MEDICAMENT"
                );

        assertThat(journalRepository.findAll())
                .filteredOn(entree -> "CONFIRMER_PRISE_MEDICAMENT".equals(entree.getAction()))
                .first()
                .satisfies(entree -> {
                    assertThat(entree.getUtilisateur()).isNull();
                });
    }
}
