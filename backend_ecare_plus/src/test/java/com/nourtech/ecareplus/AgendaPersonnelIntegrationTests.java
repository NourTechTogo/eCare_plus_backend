package com.nourtech.ecareplus;

import com.fasterxml.jackson.databind.JsonNode;
import com.nourtech.ecareplus.identity.domain.entity.Patient;
import com.nourtech.ecareplus.identity.domain.entity.ProfessionnelSante;
import com.nourtech.ecareplus.organization.domain.entity.AffectationPatient;
import com.nourtech.ecareplus.organization.domain.entity.StructureSante;
import com.nourtech.ecareplus.organization.domain.enumeration.RoleProfessionnel;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Committed HTTP transactions exercise ownership, command receipts and reminder persistence. */
class AgendaPersonnelIntegrationTests extends BackendFixture {
    private static final String API = "/api/v1/mobile/me/rendez-vous";
    private static final String DATE = "2035-06-12T10:00:00Z";

    @Test
    void patientAutonomeCreeEtRelitSonAgendaSansAffectationNiNotificationProfessionnelle() throws Exception {
        Account account = inscrire("agenda");
        Map<String, Object> request = commande(UUID.randomUUID(), DATE, 60);
        JsonNode created = creer(account, request);
        assertThat(created.path("patientId").asText()).isEqualTo(account.id().toString());
        assertThat(created.path("motif").asText()).isEqualTo("Consultation personnelle");
        assertThat(created.path("dateHeure").asText()).isEqualTo(DATE);
        assertThat(created.path("fuseau").asText()).isEqualTo("Africa/Lome");
        assertThat(created.path("professionnelNom").asText()).isEqualTo("Dr choisi librement");
        assertThat(created.path("statut").asText()).isEqualTo("PLANIFIE");
        assertThat(created.hasNonNull("version")).isTrue();
        assertThat(UUID.fromString(created.path("rappelId").asText())).isNotNull();
        assertThat(created.path("rappelLe").asText()).isEqualTo("2035-06-12T09:00:00Z");
        assertThat(detail(account, created.path("id").asText())).isEqualTo(created);
        mvc.perform(get(API).header("Authorization", bearer(connecter(account.email()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(created.path("id").asText()));
        assertThat(compter("rendez_vous", account.id())).isEqualTo(1);
        assertThat(compter("rappel", account.id())).isEqualTo(1);
        assertThat(compter("affectation_patient", account.id())).isZero();
        assertThat(jdbc.queryForObject("select count(*) from notification where utilisateur_id in (?, ?)",
                Integer.class, doctor, other)).isZero();
    }

    @Test
    void agendaEtRappelRestentPrivesAvantEtApresUneAffectation() throws Exception {
        Account owner = inscrire("prive");
        Account stranger = inscrire("etranger");
        JsonNode created = creer(owner, commande(UUID.randomUUID(), DATE, 60));
        String id = created.path("id").asText();
        for (int assignment = 0; assignment < 2; assignment++) {
            if (assignment == 1) {
                tx.executeWithoutResult(status -> em.persist(new AffectationPatient(
                        em.find(Patient.class, owner.id()), em.find(ProfessionnelSante.class, doctor),
                        em.find(StructureSante.class, structure), RoleProfessionnel.MEDECIN_REFERENT,
                        LocalDate.now().minusDays(1), true)));
            }
            for (String token : new String[]{stranger.token(), doctorToken, otherToken, adminToken}) {
                mvc.perform(get(API + "/{id}", id).header("Authorization", bearer(token)))
                        .andExpect(status().isForbidden());
                Map<String, Object> update = commande(UUID.randomUUID(), DATE, 60);
                update.put("version", created.path("version").asLong());
                mvc.perform(patch(API + "/{id}", id).header("Authorization", bearer(token))
                                .contentType("application/json").content(json.writeValueAsString(update)))
                        .andExpect(status().isForbidden());
                mvc.perform(delete(API + "/{id}", id).header("Authorization", bearer(token))
                                .contentType("application/json").content(json.writeValueAsString(
                                        Map.of("sourceRef", UUID.randomUUID(), "version", created.path("version").asLong()))))
                        .andExpect(status().isForbidden());
                mvc.perform(get("/api/v1/rappels/{id}", created.path("rappelId").asText())
                                .header("Authorization", bearer(token))).andExpect(status().isForbidden());
            }
            mvc.perform(get("/api/v1/rendez-vous/{id}", id).header("Authorization", bearer(doctorToken)))
                    .andExpect(status().isForbidden());
            mvc.perform(get("/api/v1/rendez-vous").header("Authorization", bearer(doctorToken)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$[*].id", not(hasItem(id))));
            var filtered=mvc.perform(get("/api/v1/rendez-vous").param("patientId", owner.id().toString())
                            .header("Authorization", bearer(doctorToken)));
            if(assignment==0)filtered.andExpect(status().isForbidden());
            else filtered.andExpect(status().isOk()).andExpect(jsonPath("$[*].id", not(hasItem(id))));
        }
        mvc.perform(get(API).header("Authorization", bearer(stranger.token())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        assertThat(detail(owner, id)).isEqualTo(created);
    }

    @Test
    void creationEstIdempotenteEtRefuseLaReutilisationDeCleAvecUnAutreContenu() throws Exception {
        Account account = inscrire("idempotence");
        Map<String, Object> request = commande(UUID.randomUUID(), DATE, 60);
        JsonNode first = creer(account, request);
        assertThat(creer(account, request)).isEqualTo(first);
        assertThat(compter("rendez_vous", account.id())).isEqualTo(1);
        assertThat(compter("rappel", account.id())).isEqualTo(1);
        request.put("motif", "Contenu différent");
        mvc.perform(post(API).header("Authorization", bearer(account.token())).contentType("application/json")
                        .content(json.writeValueAsString(request))).andExpect(status().isConflict());
        assertThat(detail(account, first.path("id").asText())).isEqualTo(first);
    }

    @Test
    void modificationReplanifieLeRappelEtConserveLeRecuMalgreUneModificationUlterieure() throws Exception {
        Account account = inscrire("report");
        Map<String, Object> creation = commande(UUID.randomUUID(), DATE, 60);
        JsonNode first = creer(account, creation);
        String id = first.path("id").asText();
        Map<String, Object> update = commande(UUID.randomUUID(), "2035-06-13T14:00:00Z", 30);
        update.put("version", first.path("version").asLong());
        JsonNode second = modifier(account, id, update);
        assertThat(second.path("version").asLong()).isGreaterThan(first.path("version").asLong());
        assertThat(second.path("rappelLe").asText()).isEqualTo("2035-06-13T13:30:00Z");
        assertThat(second.path("rappelId").asText()).isNotEqualTo(first.path("rappelId").asText());
        assertThat(statutRappel(first)).isEqualTo("ANNULE");
        assertThat(statutRappel(second)).isEqualTo("PLANIFIE");
        assertThat(modifier(account, id, update)).isEqualTo(second);
        assertThat(compter("rappel", account.id())).isEqualTo(2);
        Map<String, Object> stale = new LinkedHashMap<>(update);
        stale.put("sourceRef", UUID.randomUUID().toString());
        mvc.perform(patch(API + "/{id}", id).header("Authorization", bearer(account.token()))
                        .contentType("application/json").content(json.writeValueAsString(stale)))
                .andExpect(status().isConflict());
        Map<String, Object> thirdRequest = commande(UUID.randomUUID(), "2035-06-14T14:00:00Z", null);
        thirdRequest.put("version", second.path("version").asLong());
        JsonNode third = modifier(account, id, thirdRequest);
        assertThat(third.path("rappelId").isNull()).isTrue();
        assertThat(third.path("rappelLe").isNull()).isTrue();
        assertThat(statutRappel(second)).isEqualTo("ANNULE");
        assertThat(modifier(account, id, update)).isEqualTo(second);
        assertThat(creer(account, creation)).isEqualTo(first);
        assertThat(detail(account, id)).isEqualTo(third);
        update.put("motif", "Même clé, autre modification");
        mvc.perform(patch(API + "/{id}", id).header("Authorization", bearer(account.token()))
                        .contentType("application/json").content(json.writeValueAsString(update)))
                .andExpect(status().isConflict());
        assertThat(compter("rendez_vous", account.id())).isEqualTo(1);
        assertThat(compter("rappel", account.id())).isEqualTo(2);
    }

    @Test
    void annulationLogiqueEstIdempotenteEtAliasConserveLeMemeRecu() throws Exception {
        Account account = inscrire("annulation");
        JsonNode first = creer(account, commande(UUID.randomUUID(), DATE, 60));
        String id = first.path("id").asText();
        Map<String, Object> cancellation = Map.of("sourceRef", UUID.randomUUID().toString(),
                "version", first.path("version").asLong());
        JsonNode cancelled = body(mvc.perform(delete(API + "/{id}", id)
                        .header("Authorization", bearer(account.token())).contentType("application/json")
                        .content(json.writeValueAsString(cancellation)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.statut").value("ANNULE")).andReturn());
        assertThat(cancelled.path("version").asLong()).isGreaterThan(first.path("version").asLong());
        assertThat(statutRappel(first)).isEqualTo("ANNULE");
        JsonNode repeated = body(mvc.perform(post(API + "/{id}/annulation", id)
                        .header("Authorization", bearer(account.token())).contentType("application/json")
                        .content(json.writeValueAsString(cancellation))).andExpect(status().isOk()).andReturn());
        assertThat(repeated).isEqualTo(cancelled);
        assertThat(detail(account, id)).isEqualTo(cancelled);
        mvc.perform(get(API).header("Authorization", bearer(account.token())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get(API).param("inclureAnnules", "true").header("Authorization", bearer(account.token())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(id));
        mvc.perform(delete(API + "/{id}", id).header("Authorization", bearer(account.token()))
                        .contentType("application/json").content(json.writeValueAsString(Map.of(
                                "sourceRef", UUID.randomUUID(), "version", first.path("version").asLong()))))
                .andExpect(status().isConflict());
        assertThat(compter("rendez_vous", account.id())).isEqualTo(1);
        assertThat(compter("rappel", account.id())).isEqualTo(1);
    }

    @Test
    void periodeAgendaFiltreEtExigeDeuxBornesInstantValides() throws Exception {
        Account account = inscrire("periode");
        JsonNode selected = creer(account, commande(UUID.randomUUID(), DATE, null));
        creer(account, commande(UUID.randomUUID(), "2035-06-20T10:00:00Z", null));
        mvc.perform(get(API).param("du", "2035-06-12T00:00:00Z").param("au", "2035-06-13T00:00:00Z")
                        .header("Authorization", bearer(account.token())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(selected.path("id").asText()));
        for (String bound : new String[]{"du", "au"}) {
            mvc.perform(get(API).param(bound, DATE).header("Authorization", bearer(account.token())))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(get(API).param("du", "2035-06-13T00:00:00Z").param("au", "2035-06-12T00:00:00Z")
                        .header("Authorization", bearer(account.token()))).andExpect(status().isBadRequest());
        mvc.perform(get(API).param("du", "2035-06-12").param("au", "2035-06-13")
                        .header("Authorization", bearer(account.token()))).andExpect(status().isBadRequest());
    }

    @Test
    void validationRefuseCommandesInvalidesSansCreerDeRessources() throws Exception {
        Account account = inscrire("validation");
        for (Map.Entry<String, Object> invalid : Map.<String, Object>of(
                "fuseau", "Mars/Olympus", "rappelMinutes", -1, "sourceRef", "pas-un-uuid",
                "motif", "   ", "dateHeure", "2035-06-12").entrySet()) {
            Map<String, Object> request = commande(UUID.randomUUID(), DATE, 60);
            request.put(invalid.getKey(), invalid.getValue());
            refuserCreation(account, request);
        }
        Map<String, Object> excessive = commande(UUID.randomUUID(), DATE, 10081);
        refuserCreation(account, excessive);
        for (String missing : new String[]{"sourceRef", "motif", "dateHeure", "fuseau"}) {
            Map<String, Object> request = commande(UUID.randomUUID(), DATE, 60);
            request.remove(missing);
            refuserCreation(account, request);
        }
        assertThat(compter("rendez_vous", account.id())).isZero();
        assertThat(compter("rappel", account.id())).isZero();
        JsonNode first = creer(account, commande(UUID.randomUUID(), DATE, 0));
        assertThat(first.path("rappelLe").asText()).isEqualTo(DATE);
        JsonNode maximum = creer(account, commande(UUID.randomUUID(), DATE, 10080));
        assertThat(Instant.parse(maximum.path("rappelLe").asText())).isEqualTo(Instant.parse(DATE).minusSeconds(10080L * 60));
        Map<String, Object> update = commande(UUID.randomUUID(), DATE, null);
        mvc.perform(patch(API + "/{id}", first.path("id").asText()).header("Authorization", bearer(account.token()))
                        .contentType("application/json").content(json.writeValueAsString(update)))
                .andExpect(status().isBadRequest());
        mvc.perform(delete(API + "/{id}", first.path("id").asText()).header("Authorization", bearer(account.token()))
                        .contentType("application/json").content(json.writeValueAsString(Map.of("sourceRef", UUID.randomUUID()))))
                .andExpect(status().isBadRequest());
        assertThat(detail(account, first.path("id").asText())).isEqualTo(first);
    }

    @Test
    void rendezVousProfessionnelHistoriqueResteAccessibleSansEntrerDansAgendaPersonnel() throws Exception {
        JsonNode patient = create(true);
        String token = login();
        JsonNode historical = body(mvc.perform(post("/api/v1/rendez-vous").header("Authorization", bearer(doctorToken))
                        .contentType("application/json").content(json.writeValueAsString(Map.of(
                                "patientId", patient.path("id").asText(), "motif", "Suivi professionnel",
                                "dateHeure", DATE, "typeRendezVous", "CONSULTATION", "lieu", "Centre"))))
                .andExpect(status().isCreated()).andReturn());
        mvc.perform(get("/api/v1/rendez-vous/{id}", historical.path("id").asText())
                        .header("Authorization", bearer(doctorToken))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/rendez-vous").header("Authorization", bearer(doctorToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[*].id", hasItem(historical.path("id").asText())));
        mvc.perform(get(API).header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        JsonNode personal = creer(new Account(UUID.fromString(patient.path("id").asText()), "unused", token),
                commande(UUID.randomUUID(), DATE, null));
        mvc.perform(get("/api/v1/rendez-vous").header("Authorization", bearer(doctorToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[*].id", hasItem(historical.path("id").asText())))
                .andExpect(jsonPath("$[*].id", not(hasItem(personal.path("id").asText()))));
    }

    private Map<String, Object> commande(UUID sourceRef, String date, Integer minutes) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("sourceRef", sourceRef.toString());
        request.put("motif", "Consultation personnelle");
        request.put("dateHeure", date);
        request.put("fuseau", "Africa/Lome");
        request.put("lieu", "Cabinet choisi");
        request.put("professionnelNom", "Dr choisi librement");
        request.put("commentaire", "Note privée du patient");
        request.put("rappelMinutes", minutes);
        return request;
    }

    private JsonNode creer(Account account, Map<String, Object> request) throws Exception {
        return body(mvc.perform(post(API).header("Authorization", bearer(account.token()))
                        .contentType("application/json").content(json.writeValueAsString(request)))
                .andExpect(status().isCreated()).andReturn());
    }

    private void refuserCreation(Account account, Map<String, Object> request) throws Exception {
        mvc.perform(post(API).header("Authorization", bearer(account.token()))
                        .contentType("application/json").content(json.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    private JsonNode modifier(Account account, String id, Map<String, Object> request) throws Exception {
        return body(mvc.perform(patch(API + "/{id}", id).header("Authorization", bearer(account.token()))
                        .contentType("application/json").content(json.writeValueAsString(request)))
                .andExpect(status().isOk()).andReturn());
    }

    private JsonNode detail(Account account, String id) throws Exception {
        return body(mvc.perform(get(API + "/{id}", id).header("Authorization", bearer(account.token())))
                .andExpect(status().isOk()).andReturn());
    }

    private int compter(String table, UUID patient) {
        return jdbc.queryForObject("select count(*) from " + table + " where patient_id = ?", Integer.class, patient);
    }

    private String statutRappel(JsonNode appointment) {
        return jdbc.queryForObject("select statut from rappel where id = ?", String.class,
                UUID.fromString(appointment.path("rappelId").asText()));
    }

    private Account inscrire(String label) throws Exception {
        String email = label + "-" + suffix + "@example.test";
        JsonNode response = body(mvc.perform(post("/api/v1/auth/inscription").contentType("application/json")
                        .content(json.writeValueAsString(Map.of("nom", "Patient", "prenoms", "Agenda",
                                "email", email, "telephone", label + "-" + suffix, "motDePasse", password,
                                "consentementAccepte", true, "cguLues", true, "confidentialiteLue", true,
                                "versionDocument", "2026-1"))))
                .andExpect(status().isCreated()).andReturn());
        return new Account(UUID.fromString(response.path("id").asText()), email, connecter(email));
    }

    private String connecter(String email) throws Exception {
        return body(mvc.perform(post("/api/v1/auth/connexion").contentType("application/json")
                        .content(json.writeValueAsString(Map.of("identifiant", email, "motDePasse", password))))
                .andExpect(status().isOk()).andReturn()).path("jeton").asText();
    }

    private record Account(UUID id, String email, String token) {}
}
