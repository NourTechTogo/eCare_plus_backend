package com.nourtech.ecareplus;

import org.junit.jupiter.api.Test;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BackendAccessContractTests extends BackendFixture {
    @Test void ancienneRouteNePeutPasInventerUnEnvoiExterne() throws Exception {
        var notification=body(mvc.perform(post("/api/v1/notifications").header("Authorization",bearer(doctorToken)).contentType("application/json").content("{\"utilisateurId\":\""+doctor+"\",\"titre\":\"Test\",\"message\":\"Test\",\"typeNotification\":\"INFO\",\"canal\":\"PUSH\",\"sensible\":false}" )).andExpect(status().isCreated()).andReturn());
        mvc.perform(patch("/api/v1/notifications/{id}/envoi",notification.path("id").asText()).header("Authorization",bearer(doctorToken))).andExpect(status().isConflict());
    }
    @Test void refuseAccesCliniqueSansAffectationOuConsentement() throws Exception {
        var patient = create(false).path("id").asText();
        mvc.perform(get("/api/v1/portail/patients/{id}/mesures", patient).header("Authorization",bearer(doctorToken))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/portail/patients/{id}", patient).header("Authorization",bearer(otherToken))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/portail/patients/{id}/mesures", patient).header("Authorization",bearer(adminToken))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/mobile/me/mesures").header("Authorization",bearer(login()))).andExpect(status().isForbidden());
    }
    @Test void refuseAncienJetonApresRevocation() throws Exception {
        create(true); var token=login();
        mvc.perform(post("/api/v1/auth/deconnexion").header("Authorization",bearer(token))).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/mobile/me").header("Authorization",bearer(token))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/mobile/me")).andExpect(status().isUnauthorized());
    }
}
