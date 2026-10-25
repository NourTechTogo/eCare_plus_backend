package com.nourtech.ecareplus;
import org.junit.jupiter.api.Test;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class PreferenceNotificationIntegrationTests extends BackendFixture {
    @Test void preferencesEtConsentementParCanalSontPropresAuCompte() throws Exception {
        patientId();var token=login();
        mvc.perform(get("/api/v1/compte/preferences-notifications").header("Authorization",bearer(token))).andExpect(status().isOk()).andExpect(jsonPath("$.whatsappActif").value(false));
        mvc.perform(patch("/api/v1/compte/preferences-notifications").header("Authorization",bearer(token)).contentType("application/json").content("{\"whatsappActif\":true}" )).andExpect(status().isBadRequest());
        mvc.perform(patch("/api/v1/compte/preferences-notifications").header("Authorization",bearer(token)).contentType("application/json").content("{\"whatsappActif\":true,\"versionConsentement\":\"whatsapp-v1\"}" )).andExpect(status().isOk()).andExpect(jsonPath("$.whatsappActif").value(true));
        mvc.perform(patch("/api/v1/compte/preferences-notifications").header("Authorization",bearer(token)).contentType("application/json").content("{\"whatsappActif\":false}" )).andExpect(status().isOk()).andExpect(jsonPath("$.whatsappActif").value(false));
        mvc.perform(get("/api/v1/compte/preferences-notifications").header("Authorization",bearer(doctorToken))).andExpect(status().isOk()).andExpect(jsonPath("$.whatsappActif").value(false));
    }
    @Test void terminalLieALaSessionEtRevoqueALaDeconnexion() throws Exception {
        patientId();var token=login();String installation="installation-"+suffix;
        mvc.perform(put("/api/v1/compte/terminaux/{id}",installation).header("Authorization",bearer(token)).contentType("application/json")
            .content("{\"plateforme\":\"ANDROID\",\"jetonPush\":\"secret-push-"+suffix+"\"}" )).andExpect(status().isOk()).andExpect(jsonPath("$.jetonPush").doesNotExist()).andExpect(jsonPath("$.actif").value(true));
        mvc.perform(delete("/api/v1/compte/terminaux/{id}",installation).header("Authorization",bearer(otherToken))).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/auth/deconnexion").header("Authorization",bearer(token))).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/compte/terminaux").header("Authorization",bearer(login()))).andExpect(status().isOk()).andExpect(jsonPath("$[0].actif").value(false));
    }
}
