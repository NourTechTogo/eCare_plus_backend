package com.nourtech.ecareplus;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RendezVousListingTests extends BackendFixture {
    @Test void agendaListeSeulementLesPatientsAutorisesEtConserveLesActions() throws Exception {
        String patient=patientId().toString();
        String payload="{\"patientId\":\""+patient+"\",\"motif\":\"Suivi fictif\",\"dateHeure\":\""+Instant.now().plusSeconds(86400)+"\",\"lieu\":\"Cabinet\",\"typeRendezVous\":\"SUIVI\"}";
        String id=body(mvc.perform(post("/api/v1/rendez-vous").header("Authorization",bearer(doctorToken)).contentType("application/json").content(payload)).andExpect(status().isCreated()).andReturn()).path("id").asText();
        mvc.perform(get("/api/v1/rendez-vous").param("patientId",patient).header("Authorization",bearer(doctorToken))).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id));
        mvc.perform(get("/api/v1/rendez-vous").header("Authorization",bearer(otherToken))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/v1/rendez-vous").header("Authorization",bearer(adminToken))).andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/rendez-vous/{id}/confirmation",id).header("Authorization",bearer(doctorToken))).andExpect(status().isOk()).andExpect(jsonPath("$.statut").value("CONFIRME"));
    }
}
