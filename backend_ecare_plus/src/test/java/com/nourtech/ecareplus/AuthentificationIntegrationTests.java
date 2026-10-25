package com.nourtech.ecareplus;

import com.nourtech.ecareplus.auth.infrastructure.security.JwtService;
import com.nourtech.ecareplus.identity.application.service.MotDePasseService;
import com.nourtech.ecareplus.identity.domain.entity.Patient;
import com.nourtech.ecareplus.identity.domain.enumeration.Sexe;
import com.nourtech.ecareplus.identity.domain.enumeration.StatutCompte;
import com.nourtech.ecareplus.identity.infrastructure.persistence.UtilisateurRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthentificationIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private MotDePasseService motDePasseService;

    @Autowired
    private com.nourtech.ecareplus.consent.infrastructure.persistence.ConsentementRepository consentements;

    @Autowired
    private com.nourtech.ecareplus.auth.infrastructure.security.JwtProperties jwtProperties;

    @Test
    void inscriptionCreeUnPatientActifSansDonneesMedicalesFictives() throws Exception {
        mockMvc.perform(post("/api/v1/auth/inscription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inscriptionJson(" Patient.Mobile@Example.Test ", " +22890001001 ")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroPatient").value(org.hamcrest.Matchers.startsWith("PAT-")))
                .andExpect(jsonPath("$.nom").value("Ayi"))
                .andExpect(jsonPath("$.prenoms").value("Ama"))
                .andExpect(jsonPath("$.email").value("patient.mobile@example.test"))
                .andExpect(jsonPath("$.telephone").value("+22890001001"))
                .andExpect(jsonPath("$.statutCompte").value("ACTIF"))
                .andExpect(jsonPath("$.motDePasse").doesNotExist())
                .andExpect(jsonPath("$.motDePasseHash").doesNotExist());

        Patient patient = (Patient) utilisateurRepository
                .findByEmailIgnoreCase("patient.mobile@example.test")
                .orElseThrow();
        assertThat(patient.getNumeroPatient()).startsWith("PAT-");
        assertThat(patient.getSexe()).isEqualTo(Sexe.NON_RENSEIGNE);
        assertThat(patient.getDateNaissance()).isNull();
        assertThat(patient.getLanguePreferee()).isEqualTo("fr");
        assertThat(patient.getStatutCompte()).isEqualTo(StatutCompte.ACTIF);
        assertThat(patient.getMotDePasseHash()).isNotEqualTo("MotDePasseSolide!2026");
        assertThat(motDePasseService.correspond("MotDePasseSolide!2026", patient.getMotDePasseHash())).isTrue();
        assertThat(consentements.findAllByPatientIdAndStatut(patient.getId(),
                com.nourtech.ecareplus.consent.domain.enumeration.StatutConsentement.ACCORDE))
                .singleElement().satisfies(consentement -> {
                    assertThat(consentement.getVersionDocument()).isEqualTo("2026-1");
                    assertThat(consentement.getAdresseIp()).isNotBlank();
                    assertThat(consentement.getDateAcceptation()).isNotNull();
                });
    }

    @Test
    void inscriptionSansPreuveCompleteNeCreeAucunCompte() throws Exception {
        String email = "consentement-manquant@example.test";
        String valide = inscriptionJson(email, "+22890001999");
        for (String invalide : java.util.List.of(
                valide.replace("\"consentementAccepte\": true", "\"consentementAccepte\": false"),
                valide.replace("\"cguLues\": true", "\"cguLues\": false"),
                valide.replace("\"confidentialiteLue\": true", "\"confidentialiteLue\": false"),
                valide.replace("\"versionDocument\": \"2026-1\"", "\"versionDocument\": null"))) {
            mockMvc.perform(post("/api/v1/auth/inscription").contentType(MediaType.APPLICATION_JSON).content(invalide))
                    .andExpect(status().isBadRequest());
            assertThat(utilisateurRepository.findByEmailIgnoreCase(email)).isEmpty();
        }
        mockMvc.perform(post("/api/v1/auth/inscription").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"Ayi\",\"prenoms\":\"Ama\",\"email\":\"consentement-manquant@example.test\",\"telephone\":\"+22890001999\",\"motDePasse\":\"MotDePasseSolide!2026\"}"))
                .andExpect(status().isBadRequest());
        assertThat(utilisateurRepository.findByEmailIgnoreCase(email)).isEmpty();
    }

    @Test
    void inscriptionRefuseUnEmailDejaUtilise() throws Exception {
        String corps = inscriptionJson("doublon@example.test", "+22890001002");
        mockMvc.perform(post("/api/v1/auth/inscription").contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/inscription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inscriptionJson(" DOUBLON@example.test ", "+22890001003")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflit métier"));

        assertThat(utilisateurRepository.findByEmailIgnoreCase("doublon@example.test")).isPresent();
        assertThat(utilisateurRepository.findByTelephone("+22890001003")).isEmpty();
    }

    @Test
    void inscriptionRefuseUnTelephoneDejaUtilise() throws Exception {
        mockMvc.perform(post("/api/v1/auth/inscription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inscriptionJson("telephone1@example.test", "+22890001004")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/inscription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inscriptionJson("telephone2@example.test", " +22890001004 ")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflit métier"));

        assertThat(utilisateurRepository.findByTelephone("+22890001004")).isPresent();
        assertThat(utilisateurRepository.findByEmailIgnoreCase("telephone2@example.test")).isEmpty();
    }

    @Test
    void connexionParEmailRetourneUnJwtAvecLeProfilPatient() throws Exception {
        inscrire("connexion.email@example.test", "+22890001010");

        mockMvc.perform(post("/api/v1/auth/connexion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(connexionJson(" CONNEXION.EMAIL@example.test ", "MotDePasseSolide!2026")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jeton").isNotEmpty())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.expireLe").isNotEmpty())
                .andExpect(jsonPath("$.profil.email").value("connexion.email@example.test"));
    }

    @Test
    void connexionParTelephoneRetourneUnJwt() throws Exception {
        inscrire("connexion.telephone@example.test", "+22890001011");

        mockMvc.perform(post("/api/v1/auth/connexion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(connexionJson(" +22890001011 ", "MotDePasseSolide!2026")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jeton").isNotEmpty());
    }

    @Test
    void connexionNeDivulguePasSiIdentifiantOuMotDePasseEstIncorrect() throws Exception {
        inscrire("connexion.refus@example.test", "+22890001012");

        String detailCompteInconnu = detailErreurConnexion("inconnu@example.test", "MotDePasseSolide!2026");
        String detailMotDePasseFaux = detailErreurConnexion("connexion.refus@example.test", "MotDePasseFaux!2026");

        assertThat(detailCompteInconnu).isEqualTo("Identifiant ou mot de passe incorrect");
        assertThat(detailMotDePasseFaux).isEqualTo(detailCompteInconnu);
    }

    @Test
    void apiMetierRefuseUnAccesAnonymeEtUnJetonPatient() throws Exception {
        Patient patient = inscrire("connexion.role@example.test", "+22890001013");

        mockMvc.perform(get("/api/v1/patients/{id}", patient.getId()))
                .andExpect(status().isUnauthorized());

        String jeton = connecter("connexion.role@example.test", "MotDePasseSolide!2026");
        mockMvc.perform(get("/api/v1/patients/{id}", patient.getId())
                        .header("Authorization", "Bearer " + jeton))
                .andExpect(status().isForbidden());
    }

    @Test
    void apiRefuseUnJwtExpire() throws Exception {
        Patient patient = inscrire("connexion.expire@example.test", "+22890001014");
        JwtService jwtService = new JwtService(
                jwtProperties.secret(),
                Duration.ofSeconds(1),
                Clock.fixed(Instant.now().minusSeconds(120), ZoneOffset.UTC)
        );
        String jetonExpire = jwtService.emettre(patient).jeton();

        mockMvc.perform(get("/api/v1/patients/{id}", patient.getId())
                        .header("Authorization", "Bearer " + jetonExpire))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void autoriseLeClientWebLocalParCors() throws Exception {
        mockMvc.perform(options("/api/v1/auth/connexion")
                        .header(HttpHeaders.ORIGIN, "http://localhost:52345")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(resultat -> assertThat(resultat.getResponse().getHeader(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("http://localhost:52345"));
    }

    private Patient inscrire(String email, String telephone) throws Exception {
        mockMvc.perform(post("/api/v1/auth/inscription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inscriptionJson(email, telephone)))
                .andExpect(status().isCreated());
        return (Patient) utilisateurRepository.findByEmailIgnoreCase(email).orElseThrow();
    }

    private String connecter(String identifiant, String motDePasse) throws Exception {
        MvcResult resultat = mockMvc.perform(post("/api/v1/auth/connexion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(connexionJson(identifiant, motDePasse)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(resultat.getResponse().getContentAsString()).path("jeton").asText();
    }

    private String detailErreurConnexion(String identifiant, String motDePasse) throws Exception {
        MvcResult resultat = mockMvc.perform(post("/api/v1/auth/connexion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(connexionJson(identifiant, motDePasse)))
                .andExpect(status().isUnauthorized())
                .andReturn();
        JsonNode probleme = objectMapper.readTree(resultat.getResponse().getContentAsString());
        return probleme.path("detail").asText();
    }

    private static String inscriptionJson(String email, String telephone) {
        return """
                {
                  "nom": " Ayi ",
                  "prenoms": " Ama ",
                  "email": "%s",
                  "telephone": "%s",
                  "motDePasse": "MotDePasseSolide!2026",
                  "consentementAccepte": true,
                  "cguLues": true,
                  "confidentialiteLue": true,
                  "versionDocument": "2026-1"
                }
                """.formatted(email, telephone);
    }

    private static String connexionJson(String identifiant, String motDePasse) {
        return """
                {
                  "identifiant": "%s",
                  "motDePasse": "%s"
                }
                """.formatted(identifiant, motDePasse);
    }
}
