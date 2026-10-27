package com.nourtech.ecareplus;

import com.fasterxml.jackson.databind.JsonNode;
import com.nourtech.ecareplus.alert.domain.entity.Alerte;
import com.nourtech.ecareplus.alert.domain.entity.RegleAlerte;
import com.nourtech.ecareplus.alert.domain.enumeration.NiveauPriorite;
import com.nourtech.ecareplus.alert.domain.enumeration.TypeAlerte;
import com.nourtech.ecareplus.care.domain.entity.MesureSante;
import com.nourtech.ecareplus.identity.domain.entity.Patient;
import com.nourtech.ecareplus.identity.domain.entity.ProfessionnelSante;
import com.nourtech.ecareplus.organization.domain.entity.AffectationPatient;
import com.nourtech.ecareplus.organization.domain.entity.StructureSante;
import com.nourtech.ecareplus.organization.domain.enumeration.RoleProfessionnel;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** HTTP requests commit separately and exercise the real JWT/resource authorization filters. */
class PatientAutonomieIntegrationTests extends BackendFixture {

    @Test
    void inscriptionCreeUnDossierPersonnelSansStructureNiAffectation() throws Exception {
        Account account = inscrire("autonome");
        assertThat(jdbc.queryForObject("select count(*) from affectation_patient where patient_id = ?",
                Integer.class, account.id())).isZero();
        // Check before GET: registration must persist the dossier, not rely on a lazy read side effect.
        assertThat(jdbc.queryForObject("select count(*) from dossier_suivi where patient_id = ? and structure_id is null",
                Integer.class, account.id())).isEqualTo(1);

        JsonNode dossier = dossier(account.token());
        assertThat(UUID.fromString(dossier.path("id").asText())).isNotNull();
        assertThat(dossier.path("structure").isNull()).isTrue();
        assertThat(dossier.path("statut").asText()).isEqualTo("ACTIF");
        assertThat(dossier.path("resumeMedical").isNull()).isTrue();
        mvc.perform(get("/api/v1/mobile/me/professionnels").header("Authorization", bearer(account.token())))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        assertThat(dossier(account.token()).path("id").asText()).isEqualTo(dossier.path("id").asText());
    }

    @Test
    void patientNonAffecteEnregistreTensionEtGlycemieEtRelitSonHistorique() throws Exception {
        Account account = inscrire("mesures");
        JsonNode tension = enregistrer(account.token(), "TENSION_ARTERIELLE", 125, 80);
        JsonNode glycemie = enregistrer(account.token(), "GLYCEMIE", 110, null);

        assertThat(tension.path("patientId").asText()).isEqualTo(account.id().toString());
        assertThat(tension.path("valeurSecondaire").asInt()).isEqualTo(80);
        assertThat(glycemie.path("patientId").asText()).isEqualTo(account.id().toString());
        assertThat(glycemie.path("source").asText()).isEqualTo("PATIENT");
        mvc.perform(get("/api/v1/mobile/me/mesures").header("Authorization", bearer(account.token())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].id", containsInAnyOrder(tension.path("id").asText(), glycemie.path("id").asText())))
                .andExpect(jsonPath("$.content[*].patientId", containsInAnyOrder(account.id().toString(), account.id().toString())));
        mvc.perform(get("/api/v1/mobile/me/mesures").param("typeMesure", "GLYCEMIE")
                        .header("Authorization", bearer(account.token())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(glycemie.path("id").asText()));
        assertThat(jdbc.queryForObject("select count(*) from affectation_patient where patient_id = ?",
                Integer.class, account.id())).isZero();
    }

    @Test
    void deconnexionPuisConnexionConserveLeDossierEtLesMesures() throws Exception {
        Account account = inscrire("session");
        String dossierId = dossier(account.token()).path("id").asText();
        String mesureId = enregistrer(account.token(), "GLYCEMIE", 110, null).path("id").asText();
        mvc.perform(post("/api/v1/auth/deconnexion").header("Authorization", bearer(account.token())))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/mobile/me/dossier").header("Authorization", bearer(account.token())))
                .andExpect(status().isUnauthorized());

        String freshToken = connecter(account.email());
        assertThat(dossier(freshToken).path("id").asText()).isEqualTo(dossierId);
        mvc.perform(get("/api/v1/mobile/me/mesures").header("Authorization", bearer(freshToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(mesureId));
    }

    @Test
    void autrePatientNeLitNiNeModifieLesRessourcesPersonnelles() throws Exception {
        Account owner = inscrire("proprietaire");
        Account stranger = inscrire("etranger");
        String dossierId = dossier(owner.token()).path("id").asText();
        String mesureId = enregistrer(owner.token(), "GLYCEMIE", 110, null).path("id").asText();
        for (String uri : new String[]{"/api/v1/portail/patients/" + owner.id(),
                "/api/v1/portail/patients/" + owner.id() + "/mesures",
                "/api/v1/dossiers-suivi/" + dossierId, "/api/v1/mesures-sante/" + mesureId}) {
            mvc.perform(get(uri).header("Authorization", bearer(stranger.token())))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(patch("/api/v1/mesures-sante/{id}", mesureId)
                        .header("Authorization", bearer(stranger.token())).contentType("application/json")
                        .content("{\"valeurPrincipale\":999,\"unite\":\"mg/dL\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/mobile/me/mesures").header("Authorization", bearer(stranger.token())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        assertThat(jdbc.queryForObject("select valeur_principale from mesure_sante where id = ?",
                java.math.BigDecimal.class, UUID.fromString(mesureId))).isEqualByComparingTo("110");
    }

    @Test
    void dossierPersonnelRestePriveMemeApresUneAffectation() throws Exception {
        Account account = inscrire("prive");
        String dossierId = dossier(account.token()).path("id").asText();
        String mesureId = enregistrer(account.token(), "GLYCEMIE", 110, null).path("id").asText();
        verifierRefusProfessionnel(account.id(), dossierId, mesureId);
        tx.executeWithoutResult(status -> em.persist(new AffectationPatient(
                em.find(Patient.class, account.id()), em.find(ProfessionnelSante.class, other),
                em.find(StructureSante.class, structure), RoleProfessionnel.MEDECIN_REFERENT,
                LocalDate.now().minusDays(1), true)));
        // An assignment authorizes institutional care, not automatic sharing of the personal dossier.
        for (String uri : new String[]{"/api/v1/dossiers-suivi/" + dossierId, "/api/v1/mesures-sante/" + mesureId}) {
            mvc.perform(get(uri).header("Authorization", bearer(otherToken))).andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/v1/portail/patients/{id}/mesures", account.id())
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void alertesPersonnellesSontConsultablesSeulementParLePatientConcerne() throws Exception {
        Account owner = inscrire("alerte");
        Account stranger = inscrire("sansalerte");
        UUID mesureId = UUID.fromString(enregistrer(owner.token(), "GLYCEMIE", 110, null).path("id").asText());
        // A persisted alert exercises access without inventing or enabling a medical threshold.
        UUID alerteId = tx.execute(status -> {
            var alerte = new Alerte(em.find(Patient.class, owner.id()), em.find(MesureSante.class, mesureId),
                    null, TypeAlerte.AUTRE, "Information personnelle", "Test de confidentialite", NiveauPriorite.FAIBLE);
            em.persist(alerte);
            return alerte.getId();
        });
        mvc.perform(get("/api/v1/mobile/me/alertes").header("Authorization", bearer(owner.token())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(alerteId.toString()));
        mvc.perform(get("/api/v1/mobile/me/alertes").header("Authorization", bearer(stranger.token())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/v1/alertes/{id}", alerteId).header("Authorization", bearer(stranger.token())))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/alertes/{id}", alerteId).header("Authorization", bearer(otherToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void parcoursPatientDejaAffecteConserveSonDossierDeStructure() throws Exception {
        JsonNode created = create(true);
        String token = login();
        mvc.perform(get("/api/v1/mobile/me/dossier").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(created.path("dossierId").asText()))
                .andExpect(jsonPath("$.structure").value("Centre " + suffix));
        JsonNode mesure = enregistrer(token, "GLYCEMIE", 110, null);
        mvc.perform(get("/api/v1/portail/patients/{id}/mesures", created.path("id").asText())
                        .header("Authorization", bearer(doctorToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(mesure.path("id").asText()));
    }

    @Test
    void traitementDeclareSansMedecinAlimentePlanningEtObservance() throws Exception {
        Account account = inscrire("traitement");
        String day = LocalDate.now(java.time.ZoneOffset.UTC).minusDays(1).toString();
        JsonNode traitement = declarerTraitement(account.token(), traitementJson(UUID.randomUUID(), day));
        for (String field : new String[]{"traitementId", "medicamentId", "priseId"}) {
            assertThat(UUID.fromString(traitement.path(field).asText())).isNotNull();
        }
        assertThat(traitement.hasNonNull("versionPrise")).isTrue();
        assertThat(traitement.path("fuseau").asText()).isEqualTo("UTC");
        assertThat(traitement.path("joursSemaine").size()).isEqualTo(7);
        JsonNode planning = body(mvc.perform(get("/api/v1/mobile/me/planning-traitements")
                        .param("du", day).param("au", day).header("Authorization", bearer(account.token())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.occurrences.length()").value(1))
                .andExpect(jsonPath("$.occurrences[0].dosage").value("10")).andReturn());
        JsonNode occurrence = planning.path("occurrences").get(0);
        mvc.perform(post("/api/v1/mobile/me/prises/{id}/reponse", occurrence.path("id").asText())
                        .header("Authorization", bearer(account.token())).contentType("application/json")
                        .content(json.writeValueAsString(Map.of("action", "PRIS", "dateAction", day + "T08:00:00Z",
                                "sourceRef", UUID.randomUUID().toString(), "version", occurrence.path("version").asLong()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.statut").value("PRISE"));
        mvc.perform(get("/api/v1/mobile/me/observance").param("du", day).param("au", day)
                        .header("Authorization", bearer(account.token())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.prisesPrevues").value(1))
                .andExpect(jsonPath("$.prisesConfirmees").value(1)).andExpect(jsonPath("$.taux").value(100));
        mvc.perform(get("/api/v1/portail/patients/{id}/planning-traitements", account.id())
                        .param("du", day).param("au", day).header("Authorization", bearer(otherToken)))
                .andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("select count(*) from affectation_patient where patient_id = ?",
                Integer.class, account.id())).isZero();
        verifierMedicamentPersonnelNonPartage(traitement.path("medicamentId").asText());
    }

    private void verifierMedicamentPersonnelNonPartage(String medicamentId) throws Exception {
        for (String token : new String[]{doctorToken, adminToken}) {
            mvc.perform(get("/api/v1/medicaments/{id}", medicamentId).header("Authorization", bearer(token)))
                    .andExpect(status().isForbidden());
            mvc.perform(patch("/api/v1/medicaments/{id}", medicamentId).header("Authorization", bearer(token))
                            .contentType("application/json").content("""
                                    {"nomCommercial":"Modifie","denomination":"Modifie","forme":"comprimé","dosage":"20","uniteDosage":"mg"}
                                    """))
                    .andExpect(status().isForbidden());
        }
        // Use an authorized institutional treatment so denial must concern the private medication reference.
        JsonNode assigned = create(true);
        UUID prescription = tx.execute(status -> {
            var dossier = em.find(com.nourtech.ecareplus.care.domain.entity.DossierSuivi.class,
                    UUID.fromString(assigned.path("dossierId").asText()));
            var traitement = new com.nourtech.ecareplus.care.domain.entity.Traitement(dossier,
                    "Prescription institutionnelle", null, LocalDate.now().minusDays(1), null, "Medecin", null);
            em.persist(traitement);
            return traitement.getId();
        });
        mvc.perform(get("/api/v1/traitements/{id}", prescription).header("Authorization", bearer(doctorToken)))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/traitements/{id}/prises", prescription).header("Authorization", bearer(doctorToken))
                        .contentType("application/json").content(json.writeValueAsString(Map.of(
                                "medicamentId", medicamentId, "quantite", 1, "unite", "comprimé",
                                "frequence", "QUOTIDIEN", "heurePrevue", "08:00:00", "voieAdministration", "orale"))))
                .andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("select count(*) from prise_medicament where traitement_id = ?",
                Integer.class, prescription)).isZero();
        assertThat(jdbc.queryForObject("select dosage from medicament where id = ?", String.class,
                UUID.fromString(medicamentId))).isEqualTo("10");
    }

    @Test
    void reessayerUneMesurePersonnelleApresAffectationConserveSonIdentiteEtSonDossier() throws Exception {
        Account account = inscrire("reessai");
        UUID source = UUID.randomUUID();
        String request = json.writeValueAsString(Map.of("typeMesure", "GLYCEMIE", "valeurPrincipale", 110,
                "unite", "mg/dL", "dateMesure", "2026-10-20T08:00:00Z", "sourceRef", source.toString()));
        JsonNode first = body(mvc.perform(post("/api/v1/mobile/me/mesures").header("Authorization", bearer(account.token()))
                        .contentType("application/json").content(request)).andExpect(status().isCreated()).andReturn());
        UUID personalDossier = jdbc.queryForObject("select dossier_id from mesure_sante where id = ?",
                UUID.class, UUID.fromString(first.path("id").asText()));
        UUID institutionalDossier = tx.execute(status -> {
            var patient = em.find(Patient.class, account.id());
            var institution = em.find(StructureSante.class, structure);
            em.persist(new AffectationPatient(patient, em.find(ProfessionnelSante.class, doctor), institution,
                    RoleProfessionnel.MEDECIN_REFERENT, LocalDate.now().minusDays(1), true));
            var dossier = new com.nourtech.ecareplus.care.domain.entity.DossierSuivi(patient, institution, "MENSUELLE");
            em.persist(dossier);
            return dossier.getId();
        });
        assertThat(dossier(account.token()).path("id").asText()).isEqualTo(institutionalDossier.toString());
        JsonNode retry = body(mvc.perform(post("/api/v1/mobile/me/mesures").header("Authorization", bearer(account.token()))
                        .contentType("application/json").content(request)).andExpect(status().isCreated()).andReturn());
        assertThat(retry.path("id")).isEqualTo(first.path("id"));
        assertThat(jdbc.queryForObject("select dossier_id from mesure_sante where id = ?",
                UUID.class, UUID.fromString(first.path("id").asText()))).isEqualTo(personalDossier);
        assertThat(jdbc.queryForObject("select count(*) from mesure_sante where patient_id = ?",
                Integer.class, account.id())).isEqualTo(1);
        mvc.perform(post("/api/v1/mobile/me/mesures").header("Authorization", bearer(account.token()))
                        .contentType("application/json").content(request.replace("\"valeurPrincipale\":110", "\"valeurPrincipale\":111")))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/v1/portail/patients/{id}/mesures", account.id()).header("Authorization", bearer(doctorToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void reessayerUneDeclarationDeTraitementEstIdempotentEtRefuseUnCorpsDifferent() throws Exception {
        Account account = inscrire("idempotence");
        String request = traitementJson(UUID.randomUUID(), LocalDate.now(java.time.ZoneOffset.UTC).toString());
        JsonNode first = declarerTraitement(account.token(), request);
        JsonNode retry = declarerTraitement(account.token(), request);
        for (String field : new String[]{"traitementId", "medicamentId", "priseId", "versionPrise"}) {
            assertThat(retry.path(field)).isEqualTo(first.path(field));
        }
        mvc.perform(post("/api/v1/mobile/me/traitements").header("Authorization", bearer(account.token()))
                        .contentType("application/json").content(request.replace("Traitement personnel", "Autre traitement")))
                .andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("select count(*) from traitement t join dossier_suivi d on d.id = t.dossier_id where d.patient_id = ?",
                Integer.class, account.id())).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from prise_medicament p join traitement t on t.id = p.traitement_id join dossier_suivi d on d.id = t.dossier_id where d.patient_id = ?",
                Integer.class, account.id())).isEqualTo(1);
    }

    @Test
    void ancienPatientSansDossierNiAffectationObtientUnDossierPersonnelDepuisSonProfil() throws Exception {
        UUID id = tx.execute(status -> {
            var patient = new Patient("LEGACY-" + suffix, "Ancien", "Patient",
                    com.nourtech.ecareplus.identity.domain.enumeration.Sexe.NON_RENSEIGNE, null,
                    "legacy-" + suffix, "legacy-" + suffix + "@example.test", passwords.hacher(password), "fr");
            patient.activerCompte();
            em.persist(patient);
            em.persist(new com.nourtech.ecareplus.consent.domain.entity.Consentement(patient, "TELESUIVI",
                    "Consentement de test", "2026-1", "127.0.0.1", java.time.Instant.now().minusSeconds(1)));
            return patient.getId();
        });
        assertThat(jdbc.queryForObject("select count(*) from dossier_suivi where patient_id = ?", Integer.class, id)).isZero();
        String token = connecter("legacy-" + suffix + "@example.test");
        JsonNode profile = body(mvc.perform(get("/api/v1/mobile/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.dossierId").isNotEmpty()).andReturn());
        assertThat(dossier(token).path("id").asText()).isEqualTo(profile.path("dossierId").asText());
        assertThat(dossier(token).path("structure").isNull()).isTrue();
        mvc.perform(get("/api/v1/mobile/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.dossierId").value(profile.path("dossierId").asText()));
        assertThat(jdbc.queryForObject("select count(*) from dossier_suivi where patient_id = ? and structure_id is null",
                Integer.class, id)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from affectation_patient where patient_id = ?", Integer.class, id)).isZero();
    }

    @Test
    void patientAutonomeSansConsentementNeConservePasLesAccesCliniques() throws Exception {
        Account account = inscrire("consentement");
        jdbc.update("update consentement set statut = 'RETIRE', date_retrait = ? where patient_id = ?",
                java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC), account.id());
        for (String uri : new String[]{"/api/v1/mobile/me/dossier", "/api/v1/mobile/me/mesures", "/api/v1/mobile/me/alertes"}) {
            mvc.perform(get(uri).header("Authorization", bearer(account.token()))).andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/v1/mobile/me/planning-traitements").param("du", "2026-10-20").param("au", "2026-10-20")
                        .header("Authorization", bearer(account.token())))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/mobile/me/mesures").header("Authorization", bearer(account.token()))
                        .contentType("application/json").content("""
                                {"typeMesure":"GLYCEMIE","valeurPrincipale":110,"unite":"mg/dL","dateMesure":"2026-10-20T08:00:00Z"}
                                """))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/mobile/me/traitements").header("Authorization", bearer(account.token()))
                        .contentType("application/json").content(traitementJson(UUID.randomUUID(), "2026-10-20")))
                .andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("select count(*) from mesure_sante where patient_id = ?", Integer.class, account.id())).isZero();
    }

    @Test
    void regleGlobaleDeclencheUneAlertePersonnelleSansNotifierNiAffecterUnProfessionnel() throws Exception {
        Account account = inscrire("regleglobale");
        String context = "fixture-globale-" + suffix;
        UUID rule = tx.execute(status -> {
            // Test-only synthetic threshold; context isolates this fixture from every other patient/test.
            var regle = new RegleAlerte("Fixture autonomie " + suffix, "Evaluation sans structure",
                    com.nourtech.ecareplus.care.domain.enumeration.TypeMesure.GLYCEMIE, ">",
                    new java.math.BigDecimal("100"), null, 1, 60, NiveauPriorite.CRITIQUE);
            regle.definirPortee(null, null, "PRINCIPALE", "mg/dL");
            regle.definirContexte(context);
            regle.validerRegle(java.time.Instant.now());
            regle.activerRegle();
            em.persist(regle);
            return regle.getId();
        });
        try {
            for (int i = 0; i < 2; i++) {
                if (i == 1) {
                    tx.executeWithoutResult(status -> em.persist(new AffectationPatient(
                            em.find(Patient.class, account.id()), em.find(ProfessionnelSante.class, doctor),
                            em.find(StructureSante.class, structure), RoleProfessionnel.MEDECIN_REFERENT,
                            LocalDate.now().minusDays(1), true)));
                }
                JsonNode mesure = body(mvc.perform(post("/api/v1/mobile/me/mesures")
                                .header("Authorization", bearer(account.token())).contentType("application/json")
                                .content(json.writeValueAsString(Map.of("typeMesure", "GLYCEMIE", "valeurPrincipale", 110,
                                        "unite", "mg/dL", "dateMesure", "2026-10-20T0" + (8 + i) + ":00:00Z",
                                        "contexteMesure", context))))
                        .andExpect(status().isCreated()).andReturn());
                UUID alert = jdbc.queryForObject("select id from alerte where mesure_id = ? and regle_id = ?",
                        UUID.class, UUID.fromString(mesure.path("id").asText()), rule);
                assertThat(jdbc.queryForObject("select count(*) from alerte where id = ? and professionnel_id is null and origine = 'AUTOMATIQUE'",
                        Integer.class, alert)).isEqualTo(1);
                assertThat(jdbc.queryForObject("select count(*) from notification where ressource_id = ? and utilisateur_id = ? and type_ressource = 'ALERTE'",
                        Integer.class, alert, account.id())).isEqualTo(1);
                assertThat(jdbc.queryForObject("select count(*) from notification where ressource_id = ? and utilisateur_id <> ?",
                        Integer.class, alert, account.id())).isZero();
                mvc.perform(get("/api/v1/compte/notifications").header("Authorization", bearer(account.token())))
                        .andExpect(status().isOk()).andExpect(jsonPath("$.content[*].ressourceId",
                                org.hamcrest.Matchers.hasItem(alert.toString())));
                mvc.perform(get("/api/v1/mobile/me/alertes").header("Authorization", bearer(account.token())))
                        .andExpect(status().isOk()).andExpect(jsonPath("$.content[*].id",
                                org.hamcrest.Matchers.hasItem(alert.toString())));
            }
            mvc.perform(get("/api/v1/compte/notifications").header("Authorization", bearer(doctorToken)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        } finally {
            tx.executeWithoutResult(status -> em.find(RegleAlerte.class, rule).desactiverRegle());
        }
    }

    @Test
    void repetitionInstitutionnelleNeComptePasUneMesureDuDossierPersonnel() throws Exception {
        Account account = inscrire("repetition");
        String context = "fixture-repetition-" + suffix;
        UUID rule = tx.execute(status -> {
            var regle = new RegleAlerte("Fixture repetition " + suffix, "Deux occurrences dans un meme dossier",
                    com.nourtech.ecareplus.care.domain.enumeration.TypeMesure.GLYCEMIE, ">",
                    new java.math.BigDecimal("100"), null, 2, 60, NiveauPriorite.CRITIQUE);
            regle.definirPortee(null, null, "PRINCIPALE", "mg/dL");
            regle.definirContexte(context);
            regle.validerRegle(java.time.Instant.now());
            regle.activerRegle();
            em.persist(regle);
            return regle.getId();
        });
        try {
            JsonNode personal = body(mvc.perform(post("/api/v1/mobile/me/mesures")
                            .header("Authorization", bearer(account.token())).contentType("application/json")
                            .content(json.writeValueAsString(Map.of("typeMesure", "GLYCEMIE", "valeurPrincipale", 110,
                                    "unite", "mg/dL", "dateMesure", "2026-10-20T08:00:00Z", "contexteMesure", context))))
                    .andExpect(status().isCreated()).andReturn());
            UUID institutionalDossier = tx.execute(status -> {
                var patient = em.find(Patient.class, account.id());
                var institution = em.find(StructureSante.class, structure);
                em.persist(new AffectationPatient(patient, em.find(ProfessionnelSante.class, doctor), institution,
                        RoleProfessionnel.MEDECIN_REFERENT, LocalDate.now().minusDays(1), true));
                var dossier = new com.nourtech.ecareplus.care.domain.entity.DossierSuivi(patient, institution, "MENSUELLE");
                em.persist(dossier);
                return dossier.getId();
            });
            assertThat(dossier(account.token()).path("id").asText()).isEqualTo(institutionalDossier.toString());
            JsonNode institutional = body(mvc.perform(post("/api/v1/mobile/me/mesures")
                            .header("Authorization", bearer(account.token())).contentType("application/json")
                            .content(json.writeValueAsString(Map.of("typeMesure", "GLYCEMIE", "valeurPrincipale", 110,
                                    "unite", "mg/dL", "dateMesure", "2026-10-20T08:10:00Z", "contexteMesure", context))))
                    .andExpect(status().isCreated()).andReturn());
            assertThat(jdbc.queryForObject("select dossier_id from mesure_sante where id = ?", UUID.class,
                    UUID.fromString(personal.path("id").asText()))).isNotEqualTo(institutionalDossier);
            assertThat(jdbc.queryForObject("select dossier_id from mesure_sante where id = ?", UUID.class,
                    UUID.fromString(institutional.path("id").asText()))).isEqualTo(institutionalDossier);
            assertThat(jdbc.queryForObject("select count(*) from alerte where patient_id = ? and regle_id = ?",
                    Integer.class, account.id(), rule)).isZero();
            assertThat(jdbc.queryForObject("select count(*) from notification where utilisateur_id = ? and type_notification = 'ALERTE'",
                    Integer.class, doctor)).isZero();
            mvc.perform(get("/api/v1/compte/notifications").header("Authorization", bearer(doctorToken)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        } finally {
            tx.executeWithoutResult(status -> em.find(RegleAlerte.class, rule).desactiverRegle());
        }
    }

    private JsonNode declarerTraitement(String token, String request) throws Exception {
        return body(mvc.perform(post("/api/v1/mobile/me/traitements").header("Authorization", bearer(token))
                        .contentType("application/json").content(request))
                .andExpect(status().isCreated()).andReturn());
    }

    private String traitementJson(UUID sourceRef, String day) {
        return """
                {"libelle":"Traitement personnel","dateDebut":"%s","dateFin":null,
                 "medicament":{"nomCommercial":"Fictif","dosage":"10","uniteDosage":"mg","forme":"comprimé"},
                 "prise":{"quantite":1,"unite":"comprimé","heurePrevue":"08:00","fuseau":"UTC","joursSemaine":[1,2,3,4,5,6,7]},
                 "sourceRef":"%s"}
                """.formatted(day, sourceRef);
    }

    private void verifierRefusProfessionnel(UUID patient, String dossier, String mesure) throws Exception {
        for (String uri : new String[]{"/api/v1/portail/patients/" + patient,
                "/api/v1/portail/patients/" + patient + "/mesures",
                "/api/v1/dossiers-suivi/" + dossier, "/api/v1/mesures-sante/" + mesure}) {
            mvc.perform(get(uri).header("Authorization", bearer(otherToken))).andExpect(status().isForbidden());
        }
    }

    private Account inscrire(String label) throws Exception {
        String email = label + "-" + suffix + "@example.test";
        JsonNode response = body(mvc.perform(post("/api/v1/auth/inscription").contentType("application/json")
                        .content(json.writeValueAsString(Map.of("nom", "Patient", "prenoms", "Autonome",
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

    private JsonNode dossier(String token) throws Exception {
        return body(mvc.perform(get("/api/v1/mobile/me/dossier").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn());
    }

    private JsonNode enregistrer(String token, String type, int principale, Integer secondaire) throws Exception {
        var request = new java.util.LinkedHashMap<String, Object>();
        request.put("typeMesure", type);
        request.put("valeurPrincipale", principale);
        request.put("valeurSecondaire", secondaire);
        request.put("unite", type.equals("GLYCEMIE") ? "mg/dL" : "mmHg");
        request.put("dateMesure", "2026-10-20T08:00:00Z");
        return body(mvc.perform(post("/api/v1/mobile/me/mesures").header("Authorization", bearer(token))
                        .contentType("application/json").content(json.writeValueAsString(request)))
                .andExpect(status().isCreated()).andReturn());
    }

    private record Account(UUID id, String email, String token) {}
}
