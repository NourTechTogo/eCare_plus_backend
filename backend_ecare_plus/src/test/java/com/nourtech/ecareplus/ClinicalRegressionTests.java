package com.nourtech.ecareplus;

import com.nourtech.ecareplus.care.domain.entity.*;
import com.nourtech.ecareplus.care.domain.enumeration.*;
import com.nourtech.ecareplus.communication.domain.entity.Rappel;
import com.nourtech.ecareplus.communication.domain.enumeration.*;
import com.nourtech.ecareplus.identity.domain.entity.Patient;
import com.nourtech.ecareplus.identity.domain.enumeration.Sexe;
import com.nourtech.ecareplus.organization.domain.entity.StructureSante;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.*;
import static org.assertj.core.api.Assertions.*;

/** Regression tests exercising the actual domain, no Mockito/native agent required. */
class ClinicalRegressionTests {
    private static final Instant AT = Instant.parse("2026-10-24T08:00:00Z");

    @Test void acceptsGlucoseInGramsPerLitreAndStoresCanonicalMilligrams() {
        MesureSante m = mesure("GLYCEMIE", "1.20", null, "g/L");
        assertThat(m.getValeurPrincipale()).isEqualByComparingTo("120");
        assertThat(m.getUnite()).isEqualTo("mg/dL");
    }
    @Test void convertsMillimolesExplicitly() {
        MesureSante m = mesure("GLYCEMIE", "5.5", null, "mmol/L");
        assertThat(m.getValeurPrincipale()).isEqualByComparingTo("99.1");
        assertThat(m.getUnite()).isEqualTo("mg/dL");
    }
    @Test void rejectsAUnitBelongingToAnotherMeasurement() {
        assertThatIllegalArgumentException().isThrownBy(() -> mesure("GLYCEMIE", "120", null, "kg"));
        assertThatIllegalArgumentException().isThrownBy(() -> mesure("POIDS", "72", null, "mmHg"));
    }
    @Test void supportsTemperatureAndOxygenWithPlausibilityBounds() {
        assertThat(mesure("TEMPERATURE", "37.2", null, "°C").getUnite()).isEqualTo("°C");
        assertThat(mesure("SPO2", "98", null, "%").getValeurPrincipale()).isEqualByComparingTo("98");
        assertThatIllegalArgumentException().isThrownBy(() -> mesure("SPO2", "101", null, "%"));
        assertThatIllegalArgumentException().isThrownBy(() -> mesure("TEMPERATURE", "200", null, "°C"));
    }
    @Test void invalidModificationDoesNotCorruptAnExistingMeasurement() {
        MesureSante m = mesure("GLYCEMIE", "120", null, "mg/dL");
        assertThatIllegalArgumentException().isThrownBy(() -> m.modifier(new BigDecimal("999999"), null, "mg/dL", null, null));
        assertThat(m.getValeurPrincipale()).isEqualByComparingTo("120");
    }
    @Test void rejectsFutureDatedMeasurementsEvenOutsideTheHttpLayer() {
        Patient patient = patient();
        assertThatIllegalArgumentException().isThrownBy(() -> new MesureSante(patient, dossier(patient), TypeMesure.POIDS,
                new BigDecimal("72"), null, "kg", Instant.now().plusSeconds(60), null, null, SourceMesure.PATIENT, null));
    }
    @Test void postponedIntakeCanBeConfirmedButNotConfirmedTwice() {
        ConfirmationPrise c = confirmation();
        c.reporter(AT.plusSeconds(3600));
        c.confirmer("Prise déclarée", AT.plusSeconds(3601));
        assertThat(c.getStatut()).isEqualTo(StatutPrise.PRISE);
        assertThatIllegalStateException().isThrownBy(() -> c.confirmer(null, AT.plusSeconds(3602)));
    }
    @Test void postponingAnIntakeMustMoveItForward() {
        ConfirmationPrise c = confirmation();
        assertThatIllegalArgumentException().isThrownBy(() -> c.reporter(AT.minusSeconds(1)));
        assertThat(c.getDatePrevue()).isEqualTo(AT);
    }
    @Test void aRecurringReminderCanBeReadWithoutLosingItsNextOccurrence() {
        Rappel r = rappel(AT.plusSeconds(86400));
        r.envoyer(AT);
        r.marquerCommeLu();
        assertThat(r.getStatut()).isEqualTo(StatutRappel.PLANIFIE);
        assertThat(r.getDateHeurePrevue()).isEqualTo(AT.plusSeconds(86400));
        assertThatIllegalStateException().isThrownBy(r::marquerCommeLu);
        r.envoyer(AT.plusSeconds(86400));
        r.marquerCommeLu();
        assertThat(r.getStatut()).isEqualTo(StatutRappel.TERMINE);
    }
    @Test void anUnsentReminderCannotBeRead() {
        assertThatIllegalStateException().isThrownBy(rappel(null)::marquerCommeLu);
    }

    @Test void rulesCanEvaluateDiastolicInsteadOfSystolic() {
        var regle = new com.nourtech.ecareplus.alert.domain.entity.RegleAlerte("Diastolique test", null,
                TypeMesure.TENSION_ARTERIELLE, ">=", new BigDecimal("90"), null, 1, 1,
                com.nourtech.ecareplus.alert.domain.enumeration.NiveauPriorite.ELEVE);
        regle.definirPortee(null, null, "DIASTOLIQUE", "mmHg");
        assertThat(regle.valeurEvaluee(mesure("TENSION_ARTERIELLE", "130", "95", "mmHg")))
                .isEqualByComparingTo("95");
        assertThatIllegalArgumentException().isThrownBy(() -> regle.valeurEvaluee(mesure("POIDS", "72", null, "kg")));
    }
    @Test void fastingRuleDoesNotApplyToAnotherContext() {
        var regle = new com.nourtech.ecareplus.alert.domain.entity.RegleAlerte("Glycémie test", null,
                TypeMesure.GLYCEMIE, ">", new BigDecimal("110"), null, 1, 1,
                com.nourtech.ecareplus.alert.domain.enumeration.NiveauPriorite.ELEVE);
        regle.definirContexte("a_jeun");
        assertThat(regle.contexteApplicable("À jeun")).isTrue();
        assertThat(regle.contexteApplicable("post_prandiale")).isFalse();
        assertThat(regle.contexteApplicable(null)).isFalse();
    }

    private MesureSante mesure(String type, String value, String second, String unit) {
        Patient p = patient();
        return new MesureSante(p, dossier(p), TypeMesure.valueOf(type), new BigDecimal(value),
                second == null ? null : new BigDecimal(second), unit, AT, null, null, SourceMesure.PATIENT, "SYNCHRONISEE");
    }
    private ConfirmationPrise confirmation() {
        Patient p = patient();
        Traitement traitement = new Traitement(dossier(p), "Traitement test", null, LocalDate.of(2026,1,1), null, "Médecin test", null);
        Medicament medicament = new Medicament("Médicament test", "Test", "Comprimé", "1", "mg");
        PriseMedicament prise = new PriseMedicament(traitement, medicament, BigDecimal.ONE, "comprimé", "QUOTIDIEN",
                LocalTime.of(8,0), "MATIN", "Orale", null);
        return new ConfirmationPrise(prise, AT);
    }
    private Rappel rappel(Instant until) {
        return new Rappel(patient(), null, TypeRappel.AUTRE, "Rappel test", "Données fictives",
                AT, RecurrenceRappel.QUOTIDIEN, CanalNotification.PUSH, until);
    }
    private Patient patient() {
        return new Patient("TEST-CLINIQUE", "Test", "Patiente", Sexe.FEMININ, LocalDate.of(1980,1,1),
                "+22890000999", "clinique@example.test", "hash", "fr");
    }
    private DossierSuivi dossier(Patient p) {
        StructureSante s = new StructureSante("Structure test", "CLINIQUE", "Adresse test", "Lomé", "Maritime",
                "+22822000999", "structure@example.test", "TEST-CLINIQUE");
        return new DossierSuivi(p, s, "QUOTIDIEN");
    }
}
