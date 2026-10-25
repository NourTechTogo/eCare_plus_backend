package com.nourtech.ecareplus;
import org.junit.jupiter.api.Test;
import com.nourtech.ecareplus.alert.domain.entity.Alerte;
import com.nourtech.ecareplus.alert.domain.enumeration.*;
import com.nourtech.ecareplus.identity.domain.entity.Patient;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class ResumeTelesuiviIntegrationTests extends BackendFixture {
    @Test void resumeAutoriseEtAlerteTrieeParGraviteAvantDate()throws Exception{
        var patient=patientId();
        tx.executeWithoutResult(s->{var p=em.find(Patient.class,patient);em.persist(new Alerte(p,null,null,TypeAlerte.AUTRE,"Critique","Démo",NiveauPriorite.CRITIQUE));em.flush();em.persist(new Alerte(p,null,null,TypeAlerte.AUTRE,"Faible récente","Démo",NiveauPriorite.FAIBLE));});
        mvc.perform(get("/api/v1/portail/alertes").param("size","1").header("Authorization",bearer(doctorToken))).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].titre").value("Critique")).andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/api/v1/portail/patients/{id}/resume",patient).header("Authorization",bearer(doctorToken))).andExpect(status().isOk()).andExpect(jsonPath("$.patient.id").value(patient.toString())).andExpect(jsonPath("$.alertesOuvertes.totalElements").value(2));
        mvc.perform(get("/api/v1/portail/patients/{id}/resume",patient).header("Authorization",bearer(otherToken))).andExpect(status().isForbidden());
    }
}
