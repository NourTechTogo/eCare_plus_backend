package com.nourtech.ecareplus;
import org.junit.jupiter.api.Test;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;
class ReferencementQrIntegrationTests extends BackendFixture {
    @Test void referencementConsommeJetonSansDupliquerCompteNiDossier() throws Exception {
        var patient=patientId();var token=login();
        var qr=body(mvc.perform(post("/api/v1/mobile/me/referencements").header("Authorization",bearer(token))).andExpect(status().isCreated()).andReturn());
        String secret=qr.path("jeton").asText();assertThat(secret).hasSizeGreaterThanOrEqualTo(43);
        String corps="{\"jeton\":\""+secret+"\",\"professionnelId\":\""+doctor+"\",\"versionDossier\":0}";
        mvc.perform(post("/api/v1/portail/referencements/resolution").header("Authorization",bearer(adminToken)).contentType("application/json").content("{\"jeton\":\""+secret+"\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.patientId").value(patient.toString())).andExpect(jsonPath("$.allergies").doesNotExist());
        mvc.perform(post("/api/v1/portail/referencements").header("Authorization",bearer(adminToken)).contentType("application/json").content(corps)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(patient.toString()));
        mvc.perform(post("/api/v1/portail/referencements").header("Authorization",bearer(adminToken)).contentType("application/json").content(corps)).andExpect(status().isConflict());
        // Admission must preserve exactly one institutional dossier AND the independent personal dossier.
        assertThat(jdbc.queryForObject("select count(*) from dossier_suivi where patient_id=? and structure_id=?",Integer.class,patient,structure)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from dossier_suivi where patient_id=? and structure_id is null",Integer.class,patient)).isEqualTo(1);
    }
    @Test void nouvelleGenerationRevoqueAncienEtRefuseActeurPatient() throws Exception {
        patientId();var token=login();
        var ancien=body(mvc.perform(post("/api/v1/mobile/me/referencements").header("Authorization",bearer(token))).andExpect(status().isCreated()).andReturn());
        mvc.perform(post("/api/v1/mobile/me/referencements").header("Authorization",bearer(token))).andExpect(status().isCreated());
        mvc.perform(post("/api/v1/portail/referencements/resolution").header("Authorization",bearer(adminToken)).contentType("application/json").content("{\"jeton\":\""+ancien.path("jeton").asText()+"\"}"))
            .andExpect(status().isConflict());
    }
}
