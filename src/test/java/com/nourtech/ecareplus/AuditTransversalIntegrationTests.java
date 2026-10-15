package com.nourtech.ecareplus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nourtech.ecareplus.audit.domain.entity.JournalActivite;
import com.nourtech.ecareplus.audit.infrastructure.persistence.JournalActiviteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.security.Principal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class AuditTransversalIntegrationTests {

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.nourtech.ecareplus.auth.infrastructure.security.AutorisationRessources autorisations;

    private static final String EMAIL = "audit.transversal@example.test";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JournalActiviteRepository journalRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void viderJournal() {
        org.mockito.Mockito.when(autorisations.filtrer(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenAnswer(call -> call.getArgument(1));
        journalRepository.deleteAll();
    }

    @Test
    void journaliseSuccesConsultationEtEchecAvecContexteHttp() throws Exception {
        MvcResult creation = mockMvc.perform(post("/api/v1/patients")
                        .with(requete -> {
                            requete.setRemoteAddr("10.20.30.40");
                            return requete;
                        })
                        .header("User-Agent", "ecare-mobile-test/1.0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "numeroPatient": "PAT-AUDIT-TRANSVERSAL",
                                  "nom": "Audit",
                                  "prenoms": "Transversal",
                                  "sexe": "FEMININ",
                                  "dateNaissance": "1990-03-01",
                                  "telephone": "+22890009999",
                                  "email": "audit.transversal@example.test",
                                  "motDePasse": "MotDePasseTresSolide!2026",
                                  "languePreferee": "fr"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        UUID patientId = UUID.fromString(objectMapper.readTree(creation.getResponse().getContentAsString())
                .path("utilisateur").path("id").asText());

        JournalActivite creationAudit = journalRepository
                .findAllByRessourceAndRessourceId("PATIENT", patientId, org.springframework.data.domain.Pageable.unpaged())
                .getContent().stream()
                .filter(entree -> "CREER_PATIENT".equals(entree.getAction()))
                .findFirst()
                .orElseThrow();
        assertThat(creationAudit.getResultat()).isEqualTo("SUCCES");
        assertThat(creationAudit.getAdresseIp()).isEqualTo("10.20.30.40");
        assertThat(creationAudit.getAppareil()).isEqualTo("ecare-mobile-test/1.0");
        assertThat(creationAudit.getUtilisateur()).isNull();

        Principal principal = () -> EMAIL;
        mockMvc.perform(get("/api/v1/patients/{id}", patientId).principal(principal))
                .andExpect(status().isOk());

        JournalActivite consultationAudit = journalRepository
                .findAllByRessourceAndRessourceId("PATIENT", patientId, org.springframework.data.domain.Pageable.unpaged())
                .getContent().stream()
                .filter(entree -> "CONSULTER_PATIENT".equals(entree.getAction()))
                .findFirst()
                .orElseThrow();
        assertThat(consultationAudit.getUtilisateur().getId()).isEqualTo(patientId);

        UUID idInconnu = UUID.fromString("00000000-0000-0000-0000-000000000099");
        mockMvc.perform(get("/api/v1/patients/{id}", idInconnu).principal(principal))
                .andExpect(status().isNotFound());

        JournalActivite echecAudit = journalRepository
                .findAllByRessourceAndRessourceId("PATIENT", idInconnu, org.springframework.data.domain.Pageable.unpaged())
                .getContent().stream()
                .filter(entree -> "CONSULTER_PATIENT".equals(entree.getAction()))
                .findFirst()
                .orElseThrow();
        assertThat(echecAudit.getResultat()).isEqualTo("ECHEC");
        assertThat(echecAudit.getUtilisateur().getId()).isEqualTo(patientId);
    }
}
