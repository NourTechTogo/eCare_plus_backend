package com.nourtech.ecareplus;

import com.nourtech.ecareplus.communication.application.service.DistributionNotificationService;
import com.nourtech.ecareplus.communication.domain.entity.EnvoiNotification;
import com.nourtech.ecareplus.communication.domain.entity.Notification;
import com.nourtech.ecareplus.communication.domain.enumeration.CanalNotification;
import com.nourtech.ecareplus.identity.domain.entity.Utilisateur;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Claim committed outbox rows without invoking a provider or external network. */
class PatientNotificationDistributionTests extends BackendFixture {
    @Autowired DistributionNotificationService distribution;
    record Compte(UUID id, String token, String installation) {}

    @Test void patientAutonomeRecoitSonPushSansAffectationProfessionnelle() throws Exception {
        var compte = inscrire("autonome");
        assertThat(jdbc.queryForObject("select count(*) from affectation_patient where patient_id=?", Integer.class, compte.id())).isZero();
        var envoi = programmer(compte, compte.id());

        var reservation = distribution.reclamer(envoi);

        assertThat(reservation).isNotNull();
        assertThat(reservation.commande().destination()).isEqualTo("push-" + compte.installation());
        assertThat(statut(envoi)).isEqualTo("EN_COURS");
    }

    @Test void retraitDuConsentementAnnuleUnEnvoiDejaProgramme() throws Exception {
        var compte = inscrire("retrait");
        var envoi = programmer(compte, compte.id());
        mvc.perform(post("/api/v1/mobile/me/consentement").header("Authorization", bearer(compte.token()))
                .contentType("application/json").content("{\"accepte\":false}")).andExpect(status().isOk());
        verifierAnnule(envoi);
    }

    @Test void unAutreConsentementNeRemplacePasLeConsentementAuSuiviMedical() throws Exception {
        var compte = inscrire("autre-consentement");
        var envoi = programmer(compte, compte.id());
        jdbc.update("update consentement set type_consentement='AUTRE' where patient_id=?", compte.id());
        verifierAnnule(envoi);
    }

    @Test void terminalDesactiveAnnuleUnEnvoiDejaProgramme() throws Exception {
        var compte = inscrire("terminal");
        var envoi = programmer(compte, compte.id());
        mvc.perform(delete("/api/v1/compte/terminaux/{id}", compte.installation())
                .header("Authorization", bearer(compte.token()))).andExpect(status().isNoContent());
        verifierAnnule(envoi);
    }

    @Test void preferencePushDesactiveeAnnuleUnEnvoiDejaProgramme() throws Exception {
        var compte = inscrire("preference");
        var envoi = programmer(compte, compte.id());
        mvc.perform(patch("/api/v1/compte/preferences-notifications").header("Authorization", bearer(compte.token()))
                .contentType("application/json").content("{\"pushActif\":false}")).andExpect(status().isOk());
        verifierAnnule(envoi);
    }

    @Test void patientEtrangerNePeutPasRecevoirLaNotification() throws Exception {
        var destinataire = inscrire("destinataire");
        var autrePatient = inscrire("etranger");
        verifierAnnule(programmer(destinataire, autrePatient.id()));
    }

    private Compte inscrire(String label) throws Exception {
        String email = label + "-" + suffix + "@example.test";
        var response = body(mvc.perform(post("/api/v1/auth/inscription").contentType("application/json")
                .content(json.writeValueAsString(Map.of("nom", "Patient", "prenoms", "Autonome", "email", email,
                        "telephone", label + "-" + suffix, "motDePasse", password, "consentementAccepte", true,
                        "cguLues", true, "confidentialiteLue", true, "versionDocument", "2026-1"))))
                .andExpect(status().isCreated()).andReturn());
        String token = body(mvc.perform(post("/api/v1/auth/connexion").contentType("application/json")
                .content(json.writeValueAsString(Map.of("identifiant", email, "motDePasse", password))))
                .andExpect(status().isOk()).andReturn()).path("jeton").asText();
        String installation = label + "-" + suffix;
        mvc.perform(put("/api/v1/compte/terminaux/{id}", installation).header("Authorization", bearer(token))
                .contentType("application/json").content(json.writeValueAsString(Map.of("plateforme", "ANDROID", "jetonPush", "push-" + installation))))
                .andExpect(status().isOk());
        return new Compte(UUID.fromString(response.path("id").asText()), token, installation);
    }

    private UUID programmer(Compte compte, UUID patient) {
        return tx.execute(status -> {
            var n = new Notification(em.find(Utilisateur.class, compte.id()), "Information", "Ouvrez votre application", "INFO", CanalNotification.APPLICATION, false);
            em.persist(n);
            distribution.programmer(n, patient);
            em.flush();
            return em.createQuery("select e from EnvoiNotification e where e.evenementId=:id", EnvoiNotification.class)
                    .setParameter("id", n.getId()).getSingleResult().getId();
        });
    }

    private String statut(UUID envoi) { return jdbc.queryForObject("select statut from envoi_notification where id=?", String.class, envoi); }
    private void verifierAnnule(UUID envoi) {
        assertThat(distribution.reclamer(envoi)).isNull();
        assertThat(statut(envoi)).isEqualTo("ANNULE");
    }
}
