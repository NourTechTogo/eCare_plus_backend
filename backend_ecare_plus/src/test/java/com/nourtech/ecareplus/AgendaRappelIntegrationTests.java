package com.nourtech.ecareplus;

import com.fasterxml.jackson.databind.JsonNode;
import com.nourtech.ecareplus.communication.application.service.DistributionNotificationService;
import com.nourtech.ecareplus.communication.application.service.RappelAgendaService;
import com.nourtech.ecareplus.identity.domain.entity.Patient;
import com.nourtech.ecareplus.identity.domain.entity.ProfessionnelSante;
import com.nourtech.ecareplus.organization.domain.entity.AffectationPatient;
import com.nourtech.ecareplus.organization.domain.entity.StructureSante;
import com.nourtech.ecareplus.organization.domain.enumeration.RoleProfessionnel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Claims the real outbox without calling a provider or sending external traffic. */
class AgendaRappelIntegrationTests extends BackendFixture {
    @Autowired RappelAgendaService rappels;
    @Autowired DistributionNotificationService distribution;
    private static final String API = "/api/v1/mobile/me/rendez-vous";

    @Test
    void echeanceCreeUneSeuleNotificationPriveeEtUnSeulEnvoiPatient() throws Exception {
        var account = inscrire("echeance");
        Instant event = Instant.now().plusSeconds(3600).truncatedTo(ChronoUnit.SECONDS);
        JsonNode rdv = creer(account, event, 60);
        Instant due = Instant.parse(rdv.path("rappelLe").asText());
        UUID id = id(rdv);
        assertThat(rappels.declencher(id, due.minusSeconds(1))).isFalse();
        assertThat(notifications(id)).isZero();
        assertThat(rappels.declencher(id, due.plusSeconds(1))).isTrue();
        assertThat(rappels.declencher(id, due.plusSeconds(2))).isFalse();
        assertThat(notifications(id)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select utilisateur_id from notification where ressource_id=?", UUID.class, id))
                .isEqualTo(account.id());
        assertThat(jdbc.queryForObject("select type_notification from notification where ressource_id=?", String.class, id))
                .isEqualTo("RENDEZ_VOUS");
        assertThat(jdbc.queryForObject("select type_ressource from notification where ressource_id=?", String.class, id))
                .isEqualTo("RENDEZ_VOUS_PERSONNEL");
        assertThat(jdbc.queryForObject("select source_ref from notification where ressource_id=?", String.class, id))
                .isEqualTo("agenda:" + id + ":" + rdv.path("rappelId").asText());
        assertThat(jdbc.queryForObject("select count(*) from envoi_notification where evenement_id in "
                + "(select id from notification where ressource_id=?)", Integer.class, id)).isEqualTo(1);
        var claim = distribution.reclamer(envoi(id));
        assertThat(claim).isNotNull();
        assertThat(claim.commande().destination()).isEqualTo("push-" + account.installation());
        mvc.perform(get("/api/v1/compte/notifications").header("Authorization", bearer(account.token())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        assertThat(jdbc.queryForObject("select count(*) from notification where utilisateur_id in (?, ?)",
                Integer.class, doctor, other)).isZero();
    }

    @Test
    void rappelZeroMinuteDisposeDeQuinzeMinutesDeGracePuisLesRappelsExpiresSontAnnules() throws Exception {
        var account = inscrire("grace");
        Instant event = Instant.now().plusSeconds(3600).truncatedTo(ChronoUnit.SECONDS);
        JsonNode grace = creer(account, event, 0);
        assertThat(rappels.declencher(id(grace), event.plusSeconds(14 * 60))).isTrue();
        JsonNode expired = creer(account, event, 0);
        assertThat(rappels.declencher(id(expired), event.plusSeconds(16 * 60))).isFalse();
        assertThat(statutRappel(expired)).isEqualTo("ANNULE");
        assertThat(notifications(id(expired))).isZero();
        assertThat(rappels.declencher(id(expired), event.plusSeconds(17 * 60))).isFalse();
    }

    @Test
    void reportMasqueAncienneNotificationAnnuleEnvoiEtConserveLaConfidentialiteDuRappel() throws Exception {
        var account = inscrire("report-rappel");
        Instant event = Instant.now().plusSeconds(3600).truncatedTo(ChronoUnit.SECONDS);
        JsonNode original = creer(account, event, 60);
        assertThat(rappels.declencher(id(original), event.minusSeconds(3599))).isTrue();
        UUID oldOutbox = envoi(id(original));
        JsonNode updated = body(mvc.perform(patch(API + "/{id}", id(original))
                        .header("Authorization", bearer(account.token())).contentType("application/json")
                        .content(json.writeValueAsString(Map.of("sourceRef", UUID.randomUUID(),
                                "version", original.path("version").asLong(), "motif", "Report personnel",
                                "dateHeure", event.plusSeconds(86400).toString(), "fuseau", "Africa/Lome", "rappelMinutes", 30))))
                .andExpect(status().isOk()).andReturn());
        assertThat(updated.path("rappelId").asText()).isNotEqualTo(original.path("rappelId").asText());
        verifierInvalide(account, id(original), oldOutbox);
        assertThat(statutRappel(original)).isEqualTo("ANNULE");
        assertThat(jdbc.queryForObject("select agenda_personnel_id from rappel where id=?", UUID.class,
                UUID.fromString(original.path("rappelId").asText()))).isEqualTo(id(original));
        tx.executeWithoutResult(status -> em.persist(new AffectationPatient(
                em.find(Patient.class, account.id()), em.find(ProfessionnelSante.class, doctor),
                em.find(StructureSante.class, structure), RoleProfessionnel.MEDECIN_REFERENT,
                LocalDate.now().minusDays(1), true)));
        mvc.perform(get("/api/v1/rappels/{id}", original.path("rappelId").asText())
                        .header("Authorization", bearer(doctorToken))).andExpect(status().isForbidden());
    }

    @Test
    void annulationApresDeclenchementMasqueNotificationEtInvalideUneReservationExistante() throws Exception {
        var account = inscrire("annule-rappel");
        Instant event = Instant.now().plusSeconds(3600).truncatedTo(ChronoUnit.SECONDS);
        JsonNode rdv = creer(account, event, 60);
        assertThat(rappels.declencher(id(rdv), event.minusSeconds(3599))).isTrue();
        UUID outbox = envoi(id(rdv));
        assertThat(distribution.reclamer(outbox)).isNotNull();
        mvc.perform(delete(API + "/{id}", id(rdv)).header("Authorization", bearer(account.token()))
                        .contentType("application/json").content(json.writeValueAsString(Map.of(
                                "sourceRef", UUID.randomUUID(), "version", rdv.path("version").asLong()))))
                .andExpect(status().isOk());
        verifierInvalide(account, id(rdv), outbox);
        assertThat(rappels.declencher(id(rdv), event.minusSeconds(3598))).isFalse();
        assertThat(notifications(id(rdv))).isEqualTo(1);
    }

    @Test
    void retraitConsentementOuCompteInactifAnnuleLeRappelSansNotification() throws Exception {
        for (boolean withdraw : new boolean[]{true, false}) {
            var account = inscrire(withdraw ? "consent-rappel" : "inactif-rappel");
            Instant event = Instant.now().plusSeconds(3600).truncatedTo(ChronoUnit.SECONDS);
            JsonNode rdv = creer(account, event, 60);
            if (withdraw) {
                mvc.perform(post("/api/v1/mobile/me/consentement").header("Authorization", bearer(account.token()))
                                .contentType("application/json").content("{\"accepte\":false}"))
                        .andExpect(status().isOk());
            } else {
                jdbc.update("update utilisateur set statut_compte='DESACTIVE' where id=?", account.id());
            }
            assertThat(rappels.declencher(id(rdv), event.minusSeconds(3599))).isFalse();
            assertThat(statutRappel(rdv)).isEqualTo("ANNULE");
            assertThat(notifications(id(rdv))).isZero();
        }
    }

    @Test
    void deuxWorkersConcurrentsProduisentUneSeuleNotificationEtUnSeulEnvoi() throws Exception {
        var account = inscrire("concurrent-rappel");
        Instant event = Instant.now().plusSeconds(3600).truncatedTo(ChronoUnit.SECONDS);
        JsonNode rdv = creer(account, event, 60);
        var start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> { start.await(); return rappels.declencher(id(rdv), event.minusSeconds(3599)); });
            var second = executor.submit(() -> { start.await(); return rappels.declencher(id(rdv), event.minusSeconds(3599)); });
            start.countDown();
            boolean a = first.get(20, TimeUnit.SECONDS);
            boolean b = second.get(20, TimeUnit.SECONDS);
            assertThat(a ^ b).isTrue();
        } finally {
            executor.shutdownNow();
        }
        assertThat(notifications(id(rdv))).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from envoi_notification where evenement_id in "
                + "(select id from notification where ressource_id=?)", Integer.class, id(rdv))).isEqualTo(1);
    }

    private void verifierInvalide(Account account, UUID rdv, UUID outbox) throws Exception {
        assertThat(jdbc.queryForObject("select count(*) from notification where ressource_id=? and date_suppression is not null",
                Integer.class, rdv)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select statut from envoi_notification where id=?", String.class, outbox)).isEqualTo("ANNULE");
        assertThat(distribution.reclamer(outbox)).isNull();
        mvc.perform(get("/api/v1/compte/notifications").header("Authorization", bearer(account.token())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
    }

    private int notifications(UUID rdv) {
        return jdbc.queryForObject("select count(*) from notification where ressource_id=?", Integer.class, rdv);
    }

    private UUID envoi(UUID rdv) {
        return jdbc.queryForObject("select e.id from envoi_notification e join notification n on n.id=e.evenement_id where n.ressource_id=?",
                UUID.class, rdv);
    }

    private String statutRappel(JsonNode rdv) {
        return jdbc.queryForObject("select statut from rappel where id=?", String.class, UUID.fromString(rdv.path("rappelId").asText()));
    }

    private UUID id(JsonNode rdv) { return UUID.fromString(rdv.path("id").asText()); }

    private JsonNode creer(Account account, Instant event, int minutes) throws Exception {
        return body(mvc.perform(post(API).header("Authorization", bearer(account.token())).contentType("application/json")
                        .content(json.writeValueAsString(Map.of("sourceRef", UUID.randomUUID(), "motif", "Agenda privé",
                                "dateHeure", event.toString(), "fuseau", "Africa/Lome", "rappelMinutes", minutes))))
                .andExpect(status().isCreated()).andReturn());
    }

    private Account inscrire(String label) throws Exception {
        String email = label + "-" + suffix + "@example.test";
        var response = body(mvc.perform(post("/api/v1/auth/inscription").contentType("application/json")
                        .content(json.writeValueAsString(Map.of("nom", "Patient", "prenoms", "Agenda", "email", email,
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
        return new Account(UUID.fromString(response.path("id").asText()), token, installation);
    }

    private record Account(UUID id, String token, String installation) {}
}
