package com.nourtech.ecareplus;

import com.nourtech.ecareplus.auth.application.service.*;
import com.nourtech.ecareplus.auth.infrastructure.security.JwtService;
import com.nourtech.ecareplus.alert.domain.entity.Alerte;
import com.nourtech.ecareplus.alert.domain.enumeration.*;
import com.nourtech.ecareplus.care.domain.entity.*;
import com.nourtech.ecareplus.care.domain.enumeration.*;
import com.nourtech.ecareplus.consent.domain.entity.Consentement;
import com.nourtech.ecareplus.identity.domain.entity.*;
import com.nourtech.ecareplus.identity.domain.enumeration.Sexe;
import com.nourtech.ecareplus.identity.application.service.MotDePasseService;
import com.nourtech.ecareplus.messaging.domain.entity.Conversation;
import com.nourtech.ecareplus.organization.domain.entity.*;
import com.nourtech.ecareplus.organization.domain.enumeration.RoleProfessionnel;
import com.nourtech.ecareplus.shared.application.exception.ConflitMetierException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** No enclosing test transaction: HTTP requests must commit/rollback as in production. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(SecurityRegressionTests.MailConfiguration.class)
class SecurityRegressionTests {
    @Autowired MockMvc mvc;
    @Autowired EntityManager em;
    @Autowired TransactionTemplate tx;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtService jwt;
    @Autowired MotDePasseService passwords;
    @Autowired ReinitialisationMotDePasseService reset;
    @Autowired TestMail mail;
    private UUID patient, doctor, otherDoctor, measure, otherMeasure, conversation, structure, otherStructure, dossier, foreignAlert;
    private String patientToken, doctorToken, otherToken, adminToken, email;

    @BeforeEach
    void seed() {
        tx.executeWithoutResult(status -> {
            String suffix = UUID.randomUUID().toString().substring(0, 12);
            StructureSante a = structure("A-" + suffix);
            StructureSante b = structure("B-" + suffix);
            Patient p = new Patient("PAT-" + suffix, "Patient", "Test", Sexe.NON_RENSEIGNE,
                    null, "P-" + suffix, "p-" + suffix + "@example.test", passwords.hacher("AncienSecret!2026"), "fr");
            p.activerCompte(); em.persist(p);
            Medecin d = doctor(a, "A-" + suffix);
            Medecin other = doctor(b, "B-" + suffix);
            AdministrateurStructure admin = new AdministrateurStructure("Admin", "Test", Sexe.NON_RENSEIGNE,
                    null, "ADM-" + suffix, "admin-" + suffix + "@example.test", "hash", "fr", "Direction", "STRUCTURE", a);
            admin.activerCompte(); em.persist(admin);
            em.persist(new AffectationPatient(p, d, a, RoleProfessionnel.MEDECIN_REFERENT, LocalDate.now().minusDays(1), true));
            em.persist(new AffectationPatient(p, other, b, RoleProfessionnel.MEDECIN_SECONDAIRE, LocalDate.now().minusDays(1), false));
            em.persist(new Consentement(p, "TELESUIVI", "Suivi", "v1", "127.0.0.1", Instant.now().minusSeconds(60)));
            DossierSuivi da = new DossierSuivi(p, a, "MENSUELLE"); em.persist(da);
            DossierSuivi db = new DossierSuivi(p, b, "MENSUELLE"); em.persist(db);
            MesureSante ma = measure(p, da); MesureSante mb = measure(p, db);
            Alerte alertA = new Alerte(p, ma, null, TypeAlerte.SEUIL_MESURE, "A", "Test A", NiveauPriorite.MOYEN);
            Alerte alertB = new Alerte(p, mb, null, TypeAlerte.SEUIL_MESURE, "B", "Test B", NiveauPriorite.MOYEN);
            em.persist(alertA); em.persist(alertB); foreignAlert = alertB.getId();
            Conversation c = new Conversation(p, d); em.persist(c);
            patient = p.getId(); doctor = d.getId(); otherDoctor = other.getId();
            structure = a.getId(); otherStructure = b.getId(); dossier = da.getId();
            measure = ma.getId(); otherMeasure = mb.getId(); conversation = c.getId();
            email = p.getEmail(); patientToken = jwt.emettre(p).jeton();
            doctorToken = jwt.emettre(d).jeton(); otherToken = jwt.emettre(other).jeton();
            adminToken = jwt.emettre(admin).jeton();
        });
    }

    @Test void professionnelNeLitPasLeDossierDuneAutreStructureDuMemePatient() throws Exception {
        mvc.perform(get("/api/v1/mesures-sante/{id}", otherMeasure).header("Authorization", bearer(doctorToken)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/mesures-sante/{id}", measure).header("Authorization", bearer(doctorToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(measure.toString()));
    }

    @Test void professionnelNeModifiePasUneMesureExterieure() throws Exception {
        mvc.perform(patch("/api/v1/mesures-sante/{id}/confirmation", otherMeasure)
                .header("Authorization", bearer(doctorToken))).andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("select valeur_confirmee from mesure_sante where id = ?", Boolean.class, otherMeasure)).isFalse();
    }

    @Test void retraitDuConsentementEtFinAffectationRevoquentLacces() throws Exception {
        jdbc.update("update consentement set statut = 'RETIRE', date_retrait = ? where patient_id = ?", OffsetDateTime.now(ZoneOffset.UTC), patient);
        mvc.perform(get("/api/v1/mesures-sante/{id}", measure).header("Authorization", bearer(doctorToken)))
                .andExpect(status().isForbidden());
        jdbc.update("update consentement set statut = 'ACCORDE', date_retrait = null where patient_id = ?", patient);
        jdbc.update("update affectation_patient set statut = 'TERMINEE' where professionnel_id = ?", doctor);
        mvc.perform(get("/api/v1/mesures-sante/{id}", measure).header("Authorization", bearer(doctorToken)))
                .andExpect(status().isForbidden());
    }

    @Test void medecinNePeutPasCreerUnAdministrateur() throws Exception {
        mvc.perform(post("/api/v1/administrateurs-structure").header("Authorization", bearer(doctorToken))
                .contentType("application/json").content("""
                    {"nom":"Admin","prenoms":"Test","sexe":"NON_RENSEIGNE","telephone":"+22899999999",
                    "email":"admin-test@example.test","motDePasse":"SecretValide!2026","languePreferee":"fr",
                    "fonctionAdministrative":"Direction","niveauAcces":"STRUCTURE","structureId":"%s"}
                    """.formatted(structure))).andExpect(status().isForbidden());
    }

    @Test void compteSuspenduNeConservePasLaccesAvecSonAncienJwt() throws Exception {
        jdbc.update("update utilisateur set statut_compte = 'SUSPENDU' where id = ?", doctor);
        mvc.perform(get("/api/v1/mesures-sante/{id}", measure).header("Authorization", bearer(doctorToken)))
                .andExpect(status().isForbidden());
    }

    @Test void listesAlertesExcluentLesMesuresDesAutresStructures() throws Exception {
        mvc.perform(get("/api/v1/alertes/patients/{id}", patient).header("Authorization", bearer(doctorToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].titre").value("A"));
        mvc.perform(get("/api/v1/alertes/{id}", foreignAlert).header("Authorization", bearer(doctorToken)))
                .andExpect(status().isForbidden());
    }

    @Test void administrateurGereSaStructureSansAccederAuxDonneesCliniques() throws Exception {
        mvc.perform(patch("/api/v1/structures/{id}/suspension", otherStructure).header("Authorization", bearer(adminToken)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/mesures-sante/{id}", measure).header("Authorization", bearer(adminToken)))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/structures/{id}/suspension", structure).header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());
    }

    @Test void referencesDeCorpsNePermettentPasUneAffectationInterStructure() throws Exception {
        String body = """
                {"patientId":"%s","professionnelId":"%s","structureId":"%s",
                 "role":"MEDECIN_SECONDAIRE","dateDebut":"2026-01-01","professionnelPrincipal":false}
                """.formatted(patient, otherDoctor, structure);
        mvc.perform(post("/api/v1/affectations-patients").header("Authorization", bearer(adminToken))
                .contentType("application/json").content(body)).andExpect(status().isForbidden());
    }

    @Test void observationNePeutPasUsurperUnAutreProfessionnel() throws Exception {
        mvc.perform(post("/api/v1/dossiers-suivi/{id}/observations", dossier).header("Authorization", bearer(doctorToken))
                .contentType("application/json").content("""
                {"professionnelId":"%s","typeObservation":"SUIVI","contenu":"Test","confidentielle":false}
                """.formatted(otherDoctor))).andExpect(status().isForbidden());
    }

    @Test void affectationFutureNeDonnePasEncoreAcces() throws Exception {
        jdbc.update("update affectation_patient set date_debut = ? where professionnel_id = ?", LocalDate.now().plusDays(1), doctor);
        mvc.perform(get("/api/v1/mesures-sante/{id}", measure).header("Authorization", bearer(doctorToken)))
                .andExpect(status().isForbidden());
    }

    @Test void conversationsSeSerialisentSansSessionHibernateOuverte() throws Exception {
        mvc.perform(get("/api/v1/mobile/conversations").header("Authorization", bearer(patientToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].professionnelNom").value("Medecin Test"));
        mvc.perform(get("/api/v1/mobile/conversations/{id}/messages", conversation)
                .header("Authorization", bearer(otherToken))).andExpect(status().isForbidden());
    }

    @Test void messagerieRevoqueLeProfessionnelApresFinAffectation() throws Exception {
        jdbc.update("update affectation_patient set statut = 'TERMINEE' where professionnel_id = ?", doctor);
        mvc.perform(get("/api/v1/mobile/conversations").header("Authorization", bearer(doctorToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/v1/mobile/conversations/{id}/messages", conversation)
                .header("Authorization", bearer(doctorToken))).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/mobile/conversations/{id}/messages", conversation)
                .header("Authorization", bearer(patientToken)).contentType("application/json")
                .content("{\"contenu\":\"Bonjour\"}")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/mobile/conversations/{id}/messages", conversation)
                .header("Authorization", bearer(patientToken))).andExpect(status().isOk());
    }

    @Test void messagerieRefuseUnProfessionnelSuspenduEtUnConsentementRetire() throws Exception {
        jdbc.update("update professionnel_sante set statut_professionnel = 'SUSPENDU' where id = ?", doctor);
        mvc.perform(get("/api/v1/mobile/conversations/{id}/messages", conversation)
                .header("Authorization", bearer(doctorToken))).andExpect(status().isForbidden());
        jdbc.update("update professionnel_sante set statut_professionnel = 'ACTIF' where id = ?", doctor);
        jdbc.update("update consentement set statut = 'RETIRE', date_retrait = ? where patient_id = ?", OffsetDateTime.now(ZoneOffset.UTC), patient);
        mvc.perform(get("/api/v1/mobile/conversations/{id}/messages", conversation)
                .header("Authorization", bearer(doctorToken))).andExpect(status().isForbidden());
    }

    @Test void cinqMauvaisCodesSontComptabilisesEtBloquentLeBonCode() {
        reset.demander(email);
        String code = mail.code(email);
        String wrong = code.equals("0000") ? "0001" : "0000";
        for (int i = 0; i < 5; i++) assertThatThrownBy(() -> reset.verifier(email, wrong)).isInstanceOf(ConflitMetierException.class);
        assertThat(jdbc.queryForObject("select nombre_essais from demande_reinitialisation where utilisateur_id = ?", Integer.class, patient)).isEqualTo(5);
        assertThatThrownBy(() -> reset.verifier(email, code)).isInstanceOf(ConflitMetierException.class);
    }

    @Test void jetonExpireNeChangePasLeMotDePasse() {
        String token = resetToken();
        jdbc.update("update demande_reinitialisation set expire_le = ? where utilisateur_id = ?", OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(60), patient);
        assertThatThrownBy(() -> reset.reinitialiser(token, "NouveauSecret!2026")).isInstanceOf(ConflitMetierException.class);
        assertThat(passwords.correspond("AncienSecret!2026", hash())).isTrue();
    }

    @Test void jetonValideChangeLeMotDePasseUneSeuleFois() {
        String token = resetToken();
        reset.reinitialiser(token, "NouveauSecret!2026");
        assertThat(passwords.correspond("NouveauSecret!2026", hash())).isTrue();
        assertThatThrownBy(() -> reset.reinitialiser(token, "EncoreUnSecret!2026")).isInstanceOf(ConflitMetierException.class);
        assertThatThrownBy(() -> reset.reinitialiser("jeton-malforme", "EncoreUnSecret!2026")).isInstanceOf(ConflitMetierException.class);
    }

    private String resetToken() { reset.demander(email); return reset.verifier(email, mail.code(email)); }
    private String hash() { return jdbc.queryForObject("select mot_de_passe_hash from utilisateur where id = ?", String.class, patient); }
    private String bearer(String token) { return "Bearer " + token; }
    private StructureSante structure(String n) {
        StructureSante s = new StructureSante(n, "CLINIQUE", "Adresse", "Lomé", "Maritime", n, null, n);
        s.activer(); em.persist(s); return s;
    }
    private Medecin doctor(StructureSante s, String n) {
        Medecin d = new Medecin(n, n, "Test", "Medecin", Sexe.NON_RENSEIGNE, null, n,
                n + "@example.test", "hash", "fr", "Médecine", "Suivi", LocalDate.now(), s);
        d.activerCompte(); em.persist(d); return d;
    }
    private MesureSante measure(Patient p, DossierSuivi d) {
        MesureSante m = new MesureSante(p, d, TypeMesure.GLYCEMIE, new BigDecimal("110"), null,
                "mg/dL", Instant.now(), null, null, SourceMesure.PATIENT, "SYNCHRONISEE");
        em.persist(m); return m;
    }
    static class TestMail implements EnvoiOtp {
        final Map<String,String> codes = new ConcurrentHashMap<>();
        public void envoyer(String destination, String code) { codes.put(destination, code); }
        public String code(String destination) { return codes.get(destination); }
    }
    @TestConfiguration static class MailConfiguration {
        @Bean @Primary TestMail testMail() { return new TestMail(); }
    }
}
