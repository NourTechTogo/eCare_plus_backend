package com.nourtech.ecareplus;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class SynchronisationMetierIntegrationTests extends BackendFixture {
    @Test void lotCreeMesureReelleEtRejeuNeDupliquePas() throws Exception {
        var patient=patientId();var token=login();String date=Instant.now().minusSeconds(600).toString();
        String lot="{\"identifiantAppareil\":\"phone-1\",\"elements\":[{\"identifiantLocal\":\"m1\",\"typeOperation\":\"CREER_MESURE\",\"versionSchema\":1,\"dateCreationLocale\":\""+date+"\",\"contenu\":{\"typeMesure\":\"GLYCEMIE\",\"valeurPrincipale\":1.1,\"unite\":\"g/L\",\"dateMesure\":\""+date+"\"}}]}";
        String id=null;
        for(int i=0;i<2;i++){
            var resultat=body(mvc.perform(post("/api/v1/mobile/me/synchronisations").header("Authorization",bearer(token)).contentType("application/json").content(lot))
                .andExpect(status().isOk()).andExpect(jsonPath("$.elements[0].statut").value("SUCCES")).andReturn());
            String suivant=resultat.path("elements").get(0).path("ressourceId").asText();if(id==null)id=suivant;else assertThat(suivant).isEqualTo(id);
        }
        assertThat(jdbc.queryForObject("select count(*) from mesure_sante where patient_id=?",Integer.class,patient)).isEqualTo(1);
        mvc.perform(post("/api/v1/mobile/me/synchronisations").header("Authorization",bearer(token)).contentType("application/json").content(lot.replace("1.1","1.2")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.elements[0].statut").value("CONFLIT"));
    }
    @Test void elementInvalideNePerdPasElementsValidesEtPatientImposeParSession() throws Exception {
        patientId();var token=login();String date=Instant.now().minusSeconds(60).toString();
        String mesure="{\"identifiantLocal\":\"ok\",\"typeOperation\":\"CREER_MESURE\",\"versionSchema\":1,\"dateCreationLocale\":\""+date+"\",\"contenu\":{\"typeMesure\":\"POIDS\",\"valeurPrincipale\":70,\"unite\":\"kg\",\"dateMesure\":\""+date+"\"}}";
        String invalide=mesure.replace("\"ok\"","\"non\"").replace("70,","-1,");
        mvc.perform(post("/api/v1/mobile/me/synchronisations").header("Authorization",bearer(token)).contentType("application/json").content("{\"identifiantAppareil\":\"phone-2\",\"elements\":["+invalide+","+mesure+"]}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.elements[0].statut").value("REJETE")).andExpect(jsonPath("$.elements[1].statut").value("SUCCES"));
    }
}
