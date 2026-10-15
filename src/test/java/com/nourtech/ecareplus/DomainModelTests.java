package com.nourtech.ecareplus;

import com.nourtech.ecareplus.alert.domain.entity.Alerte;
import com.nourtech.ecareplus.alert.domain.entity.InterventionSuivi;
import com.nourtech.ecareplus.alert.domain.entity.RegleAlerte;
import com.nourtech.ecareplus.alert.domain.enumeration.NiveauPriorite;
import com.nourtech.ecareplus.alert.domain.enumeration.TypeAlerte;
import com.nourtech.ecareplus.care.domain.entity.ConfirmationPrise;
import com.nourtech.ecareplus.care.domain.entity.DeclarationSymptome;
import com.nourtech.ecareplus.care.domain.entity.Medicament;
import com.nourtech.ecareplus.care.domain.entity.MesureSante;
import com.nourtech.ecareplus.care.domain.entity.ObjectifSuivi;
import com.nourtech.ecareplus.care.domain.entity.DossierSuivi;
import com.nourtech.ecareplus.care.domain.entity.ObservationSuivi;
import com.nourtech.ecareplus.care.domain.entity.Pathologie;
import com.nourtech.ecareplus.care.domain.entity.PriseMedicament;
import com.nourtech.ecareplus.care.domain.entity.RapportSuivi;
import com.nourtech.ecareplus.care.domain.entity.Traitement;
import com.nourtech.ecareplus.care.domain.enumeration.SourceMesure;
import com.nourtech.ecareplus.care.domain.enumeration.TypeMesure;
import com.nourtech.ecareplus.communication.domain.entity.ContenuEducatif;
import com.nourtech.ecareplus.communication.domain.entity.Notification;
import com.nourtech.ecareplus.communication.domain.entity.Rappel;
import com.nourtech.ecareplus.communication.domain.entity.RendezVous;
import com.nourtech.ecareplus.communication.domain.enumeration.CanalNotification;
import com.nourtech.ecareplus.communication.domain.enumeration.RecurrenceRappel;
import com.nourtech.ecareplus.communication.domain.enumeration.StatutRappel;
import com.nourtech.ecareplus.communication.domain.enumeration.TypeRappel;
import com.nourtech.ecareplus.consent.domain.entity.AutorisationPartage;
import com.nourtech.ecareplus.consent.domain.entity.Consentement;
import com.nourtech.ecareplus.consent.domain.enumeration.StatutAutorisation;
import com.nourtech.ecareplus.consent.domain.enumeration.StatutConsentement;
import com.nourtech.ecareplus.consent.domain.enumeration.TypeDonneePartage;
import com.nourtech.ecareplus.identity.domain.entity.Patient;
import com.nourtech.ecareplus.identity.domain.entity.Medecin;
import com.nourtech.ecareplus.identity.domain.entity.ProcheAidant;
import com.nourtech.ecareplus.identity.domain.enumeration.Sexe;
import com.nourtech.ecareplus.organization.domain.entity.StructureSante;
import com.nourtech.ecareplus.subscription.domain.entity.AbonnementStructure;
import com.nourtech.ecareplus.subscription.domain.enumeration.FrequencePaiement;
import com.nourtech.ecareplus.subscription.domain.enumeration.StatutAbonnement;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class DomainModelTests {

    @Test
    void retirerUnConsentementInvalideImmediatementLePartage() {
        Patient patient = patient();
        ProcheAidant proche = procheAidant();
        Instant maintenant = Instant.parse("2026-09-27T10:00:00Z");
        Consentement consentement = new Consentement(
                patient,
                "PARTAGE_PROCHE",
                "Partage limité des rappels",
                "1.0",
                "127.0.0.1",
                maintenant.minusSeconds(60)
        );
        AutorisationPartage autorisation = new AutorisationPartage(
                patient,
                proche,
                consentement,
                TypeDonneePartage.RAPPELS,
                maintenant.minusSeconds(30),
                null,
                true,
                true
        );

        autorisation.activer(maintenant);
        assertThat(autorisation.estValideA(maintenant)).isTrue();

        consentement.retirer(maintenant.plusSeconds(1));

        assertThat(consentement.getStatut()).isEqualTo(StatutConsentement.RETIRE);
        assertThat(autorisation.estValideA(maintenant.plusSeconds(2))).isFalse();
    }

    @Test
    void refuseUneAutorisationSansConsentementValide() {
        Patient patient = patient();
        Instant maintenant = Instant.parse("2026-09-27T10:00:00Z");
        Consentement consentement = new Consentement(
                patient,
                "PARTAGE_PROCHE",
                "Partage limité",
                "1.0",
                "127.0.0.1",
                maintenant.minusSeconds(60)
        );
        consentement.retirer(maintenant.minusSeconds(30));
        AutorisationPartage autorisation = new AutorisationPartage(
                patient,
                procheAidant(),
                consentement,
                TypeDonneePartage.MESURES,
                maintenant,
                null,
                true,
                false
        );

        assertThatIllegalStateException().isThrownBy(() -> autorisation.activer(maintenant));
        assertThat(autorisation.getStatut()).isEqualTo(StatutAutorisation.EN_ATTENTE);
    }

    @Test
    void refuseDesBornesObjectifInversees() {
        StructureSante structure = structure();
        DossierSuivi dossier = new DossierSuivi(patient(), structure, "MENSUELLE");
        Medecin medecin = new Medecin(
                "PRO-TEST",
                "ORDRE-TEST",
                "Mensah",
                "Koffi",
                Sexe.MASCULIN,
                LocalDate.of(1975, 1, 1),
                "+22890000003",
                "medecin@example.test",
                "hash",
                "fr",
                "Médecine interne",
                "Diabétologie",
                LocalDate.of(2020, 1, 1),
                structure
        );

        assertThatIllegalArgumentException().isThrownBy(() -> new ObjectifSuivi(
                dossier,
                medecin,
                "Tension systolique",
                null,
                new BigDecimal("140"),
                new BigDecimal("120"),
                "mmHg",
                LocalDate.now(),
                null
        ));
    }

    @Test
    void desactiverUnePathologieDejaInactiveEchoue() {
        StructureSante structure = structure();
        DossierSuivi dossier = new DossierSuivi(patient(), structure, "MENSUELLE");
        Pathologie pathologie = new Pathologie(dossier, "Hypertension artérielle", "I10", null, null, null);

        pathologie.desactiver();

        assertThatIllegalStateException().isThrownBy(pathologie::desactiver);
    }

    @Test
    void refuseUneTensionArterielleSansValeurDiastolique() {
        StructureSante structure = structure();
        Patient patient = patient();
        DossierSuivi dossier = new DossierSuivi(patient, structure, "MENSUELLE");

        assertThatIllegalArgumentException().isThrownBy(() -> new MesureSante(
                patient,
                dossier,
                TypeMesure.TENSION_ARTERIELLE,
                new BigDecimal("125"),
                null,
                "mmHg",
                Instant.now(),
                null,
                null,
                SourceMesure.PATIENT,
                null
        ));
    }

    @Test
    void refuseUneGlycemieHorsPlagePlausible() {
        StructureSante structure = structure();
        Patient patient = patient();
        DossierSuivi dossier = new DossierSuivi(patient, structure, "MENSUELLE");

        assertThatIllegalArgumentException().isThrownBy(() -> new MesureSante(
                patient,
                dossier,
                TypeMesure.GLYCEMIE,
                new BigDecimal("2000"),
                null,
                "mg/dL",
                Instant.now(),
                null,
                null,
                SourceMesure.PATIENT,
                null
        ));
    }

    @Test
    void refuseDeModifierUneMesureDejaConfirmee() {
        StructureSante structure = structure();
        Patient patient = patient();
        DossierSuivi dossier = new DossierSuivi(patient, structure, "MENSUELLE");
        MesureSante mesure = new MesureSante(
                patient,
                dossier,
                TypeMesure.POIDS,
                new BigDecimal("72.500"),
                null,
                "kg",
                Instant.now(),
                null,
                null,
                SourceMesure.PATIENT,
                null
        );

        mesure.confirmer();

        assertThatIllegalStateException().isThrownBy(() -> mesure.modifier(
                new BigDecimal("73.000"), null, "kg", null, null
        ));
        assertThatIllegalStateException().isThrownBy(mesure::confirmer);
    }

    @Test
    void refuseUnTraitementAvecDateFinAvantDateDebut() {
        StructureSante structure = structure();
        DossierSuivi dossier = new DossierSuivi(patient(), structure, "MENSUELLE");

        assertThatIllegalArgumentException().isThrownBy(() -> new Traitement(
                dossier,
                "Metformine",
                null,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 5, 1),
                "Dr Mensah",
                null
        ));
    }

    @Test
    void terminerUnTraitementDejaTermineEchoue() {
        StructureSante structure = structure();
        DossierSuivi dossier = new DossierSuivi(patient(), structure, "MENSUELLE");
        Traitement traitement = new Traitement(
                dossier, "Metformine", null, LocalDate.of(2026, 6, 1), null, "Dr Mensah", null
        );

        traitement.terminer();

        assertThatIllegalStateException().isThrownBy(traitement::terminer);
    }

    @Test
    void refuseUnePriseAvecQuantiteNulleOuNegative() {
        StructureSante structure = structure();
        DossierSuivi dossier = new DossierSuivi(patient(), structure, "MENSUELLE");
        Traitement traitement = new Traitement(
                dossier, "Metformine", null, LocalDate.of(2026, 6, 1), null, "Dr Mensah", null
        );
        Medicament medicament = new Medicament("Glucophage", "Metformine", "Comprimé", "500", "mg");

        assertThatIllegalArgumentException().isThrownBy(() -> new PriseMedicament(
                traitement, medicament, BigDecimal.ZERO, "comprimé", "2 fois par jour",
                LocalTime.of(8, 0), "MATIN", "Orale", null
        ));
    }

    @Test
    void confirmerUnePriseDejaTraiteeEchoue() {
        StructureSante structure = structure();
        DossierSuivi dossier = new DossierSuivi(patient(), structure, "MENSUELLE");
        Traitement traitement = new Traitement(
                dossier, "Metformine", null, LocalDate.of(2026, 6, 1), null, "Dr Mensah", null
        );
        Medicament medicament = new Medicament("Glucophage", "Metformine", "Comprimé", "500", "mg");
        PriseMedicament prise = new PriseMedicament(
                traitement, medicament, BigDecimal.ONE, "comprimé", "2 fois par jour",
                LocalTime.of(8, 0), "MATIN", "Orale", null
        );
        Instant maintenant = Instant.parse("2026-09-27T08:05:00Z");
        ConfirmationPrise confirmation = new ConfirmationPrise(prise, maintenant.minusSeconds(300));

        confirmation.confirmer(null, maintenant);

        assertThatIllegalStateException().isThrownBy(() -> confirmation.signalerOubli(null));
    }

    @Test
    void marquerCommeLuUnRappelNonEnvoyeEchoue() {
        Instant maintenant = Instant.parse("2026-09-27T08:00:00Z");
        Rappel rappel = new Rappel(
                patient(), null, TypeRappel.AUTRE, "Rappel test", "Message de test",
                maintenant, RecurrenceRappel.AUCUNE, CanalNotification.PUSH, null
        );

        assertThatIllegalStateException().isThrownBy(rappel::marquerCommeLu);

        rappel.envoyer();
        rappel.marquerCommeLu();

        assertThatIllegalStateException().isThrownBy(rappel::annuler);
    }

    @Test
    void replanifieUnRappelRecurrentApresChaqueEnvoiConfirme() {
        Instant premiereOccurrence = Instant.parse("2026-09-27T08:00:00Z");
        Rappel rappel = new Rappel(
                patient(), null, TypeRappel.AUTRE, "Mesure quotidienne", "Pensez à saisir votre mesure",
                premiereOccurrence, RecurrenceRappel.QUOTIDIEN, CanalNotification.PUSH,
                Instant.parse("2026-09-29T08:00:00Z")
        );

        rappel.envoyer(Instant.parse("2026-09-27T08:00:10Z"));

        assertThat(rappel.getStatut()).isEqualTo(StatutRappel.PLANIFIE);
        assertThat(rappel.getDateHeurePrevue()).isEqualTo(Instant.parse("2026-09-28T08:00:00Z"));
        assertThat(rappel.getDerniereExecution()).isEqualTo(Instant.parse("2026-09-27T08:00:10Z"));

        rappel.envoyer(Instant.parse("2026-09-28T08:00:05Z"));
        assertThat(rappel.getDateHeurePrevue()).isEqualTo(Instant.parse("2026-09-29T08:00:00Z"));

        rappel.envoyer(Instant.parse("2026-09-29T08:00:03Z"));
        assertThat(rappel.getStatut()).isEqualTo(StatutRappel.TERMINE);
    }

    @Test
    void marquerCommeRealiseUnRendezVousNonConfirmeEchoue() {
        RendezVous rendezVous = new RendezVous(
                patient(), "Consultation de suivi", Instant.parse("2026-10-01T09:00:00Z"),
                "Clinique pilote", "CONSULTATION", null
        );

        assertThatIllegalStateException().isThrownBy(rendezVous::marquerCommeRealise);

        rendezVous.confirmer();
        rendezVous.marquerCommeRealise();

        assertThatIllegalStateException().isThrownBy(rendezVous::confirmer);
    }

    @Test
    void refuseActiverUneRegleNonValidee() {
        RegleAlerte regle = new RegleAlerte(
                "Hyperglycémie répétée", null, TypeMesure.GLYCEMIE, ">",
                new BigDecimal("180"), null, 2, 24, NiveauPriorite.ELEVE
        );

        assertThatIllegalStateException().isThrownBy(regle::activerRegle);

        regle.validerRegle(Instant.parse("2026-09-27T10:00:00Z"));
        regle.activerRegle();

        assertThat(regle.isActif()).isTrue();
    }

    @Test
    void evaluerMesureExigeLeNombreDoccurrencesMinimal() {
        RegleAlerte regle = new RegleAlerte(
                "Hyperglycémie répétée", null, TypeMesure.GLYCEMIE, ">",
                new BigDecimal("180"), null, 2, 24, NiveauPriorite.ELEVE
        );
        regle.validerRegle(Instant.now());
        regle.activerRegle();

        assertThat(regle.evaluerMesure(new BigDecimal("200"), 1)).isFalse();
        assertThat(regle.evaluerMesure(new BigDecimal("200"), 2)).isTrue();
        assertThat(regle.evaluerMesure(new BigDecimal("150"), 3)).isFalse();
    }

    @Test
    void refusePrendreEnChargeUneAlerteNonAffectee() {
        Alerte alerte = new Alerte(
                patient(), null, null, TypeAlerte.SEUIL_MESURE, "Glycémie élevée",
                null, NiveauPriorite.ELEVE
        );

        assertThatIllegalStateException().isThrownBy(() -> alerte.prendreEnCharge(Instant.now()));
    }

    @Test
    void rouvrirUneAlerteClotureeRemetLeStatutANouvelle() {
        Alerte alerte = new Alerte(
                patient(), null, null, TypeAlerte.SEUIL_MESURE, "Glycémie élevée",
                null, NiveauPriorite.ELEVE
        );
        Instant maintenant = Instant.parse("2026-09-27T10:00:00Z");

        alerte.cloturer("Fausse alerte", maintenant);
        assertThatIllegalStateException().isThrownBy(() -> alerte.cloturer("Autre motif", maintenant));

        alerte.rouvrir();

        assertThat(alerte.getDateCloture()).isNull();
        assertThat(alerte.getMotifCloture()).isNull();
    }

    @Test
    void cloturerUneInterventionEffaceLActionSuivante() {
        Alerte alerte = new Alerte(
                patient(), null, null, TypeAlerte.SEUIL_MESURE, "Glycémie élevée",
                null, NiveauPriorite.ELEVE
        );
        InterventionSuivi intervention = new InterventionSuivi(
                alerte, "Appel téléphonique", null, Instant.parse("2026-09-27T11:00:00Z")
        );
        intervention.definirActionSuivante("Contrôler la glycémie demain", Instant.parse("2026-09-28T08:00:00Z"));

        intervention.cloturerIntervention("Patient rassuré, glycémie normalisée");

        assertThat(intervention.getActionSuivante()).isNull();
        assertThat(intervention.getDateProchaineAction()).isNull();
    }

    @Test
    void evaluerUnSymptomeNonTransmisEchoue() {
        DeclarationSymptome declaration = new DeclarationSymptome(
                patient(), "Douleur thoracique", "MODEREE", null, null, false
        );

        assertThatIllegalStateException().isThrownBy(declaration::marquerCommeEvalue);

        declaration.transmettreAuProfessionnel();
        declaration.marquerCommeEvalue();

        assertThatIllegalStateException().isThrownBy(() -> declaration.modifierDeclaration(
                "Douleur thoracique", "SEVERE", null, true
        ));
    }

    @Test
    void archiverUneObservationDejaArchiveeEchoue() {
        StructureSante structure = structure();
        DossierSuivi dossier = new DossierSuivi(patient(), structure, "MENSUELLE");
        ObservationSuivi observation = new ObservationSuivi(
                dossier, medecin(structure), "Patient anxieux lors de la consultation", "COMPORTEMENT", true
        );

        observation.archiverObservation();

        assertThatIllegalStateException().isThrownBy(observation::archiverObservation);
    }

    @Test
    void archiverUnRapportNonGenereEchoue() {
        StructureSante structure = structure();
        DossierSuivi dossier = new DossierSuivi(patient(), structure, "MENSUELLE");
        RapportSuivi rapport = new RapportSuivi(
                dossier, "Bilan trimestriel", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31), null, "PDF"
        );

        assertThatIllegalStateException().isThrownBy(rapport::archiver);

        rapport.marquerGenere("/rapports/bilan-t1.pdf");
        rapport.archiver();

        assertThatIllegalStateException().isThrownBy(() -> rapport.marquerGenere("/autre/chemin.pdf"));
    }

    @Test
    void marquerCommeLueUneNotificationNonEnvoyeeEchoue() {
        Notification notification = new Notification(
                patient(), "Nouveau contenu disponible", "Un nouvel article vous est proposé",
                "INFO", CanalNotification.PUSH, false
        );

        assertThatIllegalStateException().isThrownBy(() -> notification.marquerCommeLue(Instant.now()));

        notification.envoyer(Instant.now());
        notification.marquerCommeLue(Instant.now());

        assertThatIllegalStateException().isThrownBy(notification::annuler);
    }

    @Test
    void publierUnContenuNonValideEchoue() {
        ContenuEducatif contenu = new ContenuEducatif(
                medecin(structure()), "Bien vivre avec le diabète", null,
                "Conseils pratiques pour le quotidien", "ARTICLE", "fr", "DIABETE", null
        );

        assertThatIllegalStateException().isThrownBy(() -> contenu.publier(Instant.now()));

        contenu.validerContenu();
        contenu.publier(Instant.now());

        assertThatIllegalStateException().isThrownBy(() -> contenu.modifierContenu(
                "Nouveau titre", null, "Nouveau contenu", null
        ));
    }

    @Test
    void abonnementActifRespecteLaLimiteDePatients() {
        AbonnementStructure abonnement = new AbonnementStructure(
                structure(),
                "PILOTE",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2027, 8, 31),
                50,
                new BigDecimal("250000.00"),
                FrequencePaiement.ANNUELLE
        );

        abonnement.activer();

        assertThat(abonnement.getStatut()).isEqualTo(StatutAbonnement.ACTIF);
        assertThat(abonnement.autoriseNombrePatients(49)).isTrue();
        assertThat(abonnement.autoriseNombrePatients(50)).isFalse();
    }

    private static Patient patient() {
        return new Patient(
                "PAT-0001",
                "Doe",
                "Jane",
                Sexe.FEMININ,
                LocalDate.of(1985, 3, 20),
                "+22890000001",
                "jane@example.test",
                "hash",
                "fr"
        );
    }

    private static ProcheAidant procheAidant() {
        return new ProcheAidant(
                "Doe",
                "John",
                Sexe.MASCULIN,
                LocalDate.of(1980, 1, 1),
                "+22890000002",
                "john@example.test",
                "hash",
                "fr",
                "CONJOINT"
        );
    }

    private static StructureSante structure() {
        return new StructureSante(
                "Clinique pilote",
                "CLINIQUE",
                "1 rue de la Santé",
                "Lomé",
                "Maritime",
                "+22822000000",
                "contact@clinique.test",
                "AUT-001"
        );
    }

    private static Medecin medecin(StructureSante structure) {
        return new Medecin(
                "PRO-TEST-2",
                "ORDRE-TEST-2",
                "Adjo",
                "Sena",
                Sexe.FEMININ,
                LocalDate.of(1980, 5, 10),
                "+22890000004",
                "medecin2@example.test",
                "hash",
                "fr",
                "Médecine générale",
                "Endocrinologie",
                LocalDate.of(2018, 1, 1),
                structure
        );
    }
}
