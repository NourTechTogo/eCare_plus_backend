package com.nourtech.ecareplus;

import com.nourtech.ecareplus.communication.domain.entity.Notification;
import com.nourtech.ecareplus.communication.domain.enumeration.CanalNotification;
import com.nourtech.ecareplus.identity.domain.entity.Medecin;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class NotificationInboxTests extends BackendFixture {
    String notification() {
        return tx.execute(s -> {
            var n = new Notification(em.getReference(Medecin.class, doctor), "Information", "Contenu privé", "INFO", CanalNotification.APPLICATION, false);
            n.envoyer(Instant.now()); em.persist(n); em.flush(); return n.getId().toString();
        });
    }
    @Test void archiverDesarchiverSupprimerEtAnnulerSontPersistantsIdempotents() throws Exception {
        String id=notification();
        for(int i=0;i<2;i++) mvc.perform(post("/api/v1/compte/notifications/{id}/archivage",id).header("Authorization",bearer(doctorToken))).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/compte/notifications").header("Authorization",bearer(doctorToken))).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/v1/compte/notifications?archivees=true").header("Authorization",bearer(doctorToken))).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(id)).andExpect(jsonPath("$.content[0].dateArchivage").isNotEmpty());
        mvc.perform(post("/api/v1/compte/notifications/{id}/lecture",id).header("Authorization",bearer(doctorToken))).andExpect(status().isNoContent());
        for(int i=0;i<2;i++) mvc.perform(post("/api/v1/compte/notifications/{id}/desarchivage",id).header("Authorization",bearer(doctorToken))).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/compte/notifications").header("Authorization",bearer(doctorToken))).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].dateLecture").isNotEmpty());
        for(int i=0;i<2;i++) mvc.perform(delete("/api/v1/compte/notifications/{id}",id).header("Authorization",bearer(doctorToken))).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/compte/notifications").header("Authorization",bearer(doctorToken))).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(post("/api/v1/compte/notifications/{id}/lecture",id).header("Authorization",bearer(doctorToken))).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/compte/notifications/{id}/restauration",id).header("Authorization",bearer(doctorToken))).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/compte/notifications").header("Authorization",bearer(doctorToken))).andExpect(jsonPath("$.content[0].id").value(id));
    }
    @Test void chaqueMutationEstPriveeEtAuthentifiee() throws Exception {
        String id=notification();
        for(String action: new String[]{"archivage","desarchivage","restauration","lecture"}) {
            mvc.perform(post("/api/v1/compte/notifications/{id}/{action}",id,action).header("Authorization",bearer(otherToken))).andExpect(status().isForbidden());
            mvc.perform(post("/api/v1/compte/notifications/{id}/{action}",id,action)).andExpect(status().isUnauthorized());
        }
        mvc.perform(delete("/api/v1/compte/notifications/{id}",id).header("Authorization",bearer(otherToken))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/compte/notifications/{id}",id)).andExpect(status().isUnauthorized());
    }
}
